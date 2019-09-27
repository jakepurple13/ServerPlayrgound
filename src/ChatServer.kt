package com.example

import com.google.gson.Gson
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.close
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger


class ChatUser(var name: String, var image: String = "https://www.w3schools.com/w3images/bandmember.jpg")

/**
 * Class in charge of the logic of the chat server.
 * It contains handlers to events and commands to send messages to specific users in the server.
 */
class ChatServer {
    /**
     * Atomic counter used to get unique user-names based on the maximum users the server had.
     */
    private val usersCounter = AtomicInteger()

    /**
     * A concurrent map associating session IDs to user names.
     */
    private val memberNames = ConcurrentHashMap<String, ChatUser>()

    /**
     * Associates a session-id to a set of websockets.
     * Since a browser is able to open several tabs and windows with the same cookies and thus the same session.
     * There might be several opened sockets for the same client.
     */
    private val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

    /**
     * A list of the latest messages sent to the server, so new members can have a bit context of what
     * other people was talking about before joining.
     */
    private val lastMessages = LinkedList<SendMessage>()

    private val peopleWhoAreTyping = ConcurrentHashMap<String, Boolean>()

    /**
     * Handles that a member identified with a session id and a socket joined.
     */
    suspend fun memberJoin(member: String, socket: WebSocketSession) {
        // Checks if this user is already registered in the server and gives him/her a temporal name if required.
        //val name = memberNames.computeIfAbsent(member) { ChatUser("user${usersCounter.incrementAndGet()}") }
        val name = memberNames.computeIfAbsent(member) {
            usersCounter.incrementAndGet()
            var n = randomName()
            while (memberNames.values.any { it.name == n }) {
                n = randomName()
            }
            ChatUser(n)
        }

        // Associates this socket to the member id.
        // Since iteration is likely to happen more frequently than adding new items,
        // we use a `CopyOnWriteArrayList`.
        // We could also control how many sockets we would allow per client here before appending it.
        // But since this is a sample we are not doing it.
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)

        // Only when joining the first socket for a member notifies the rest of the users.
        if (list.size == 1) {
            broadcastUserUpdate()
            val sendMessage = SendMessage(ChatUser("Server"), "Connected as ${name.name}", MessageType.SERVER)
            members[member]?.send(Frame.Text(sendMessage.toJson()))
        }

        // Sends the user the latest messages from this server to let the member have a bit context.
        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message.toJson()))
        }
    }

    /**
     * Handles a [member] idenitified by its session id renaming [to] a specific name.
     */
    suspend fun memberRenamed(member: String, to: String) {
        // Re-sets the member name.
        memberNames[member]?.name = to
        // Notifies everyone in the server about this change.
        broadcastUserUpdate()
    }

    /**
     * Handles a [member] idenitified by its session id renaming [to] a specific name.
     */
    suspend fun memberImageChange(member: String, newImage: String) {
        // Re-sets the member name.
        //val oldName = memberNames.put(member, to) ?: member
        memberNames[member]?.image = newImage
        // Notifies everyone in the server about this change.
        //broadcast("server", "Member renamed from $member to $to")
        broadcastUserUpdate()
    }

    /**
     * Handles that a [member] with a specific [socket] left the server.
     */
    suspend fun memberLeft(member: String, socket: WebSocketSession) {
        // Removes the socket connection for this member
        val connections = members[member]
        connections?.remove(socket)

        // If no more sockets are connected for this member, let's remove it from the server
        // and notify the rest of the users about this event.
        if (connections != null && connections.isEmpty()) {
            val name = memberNames.remove(member)?.name ?: member
            broadcast("server", "Member left: $name.", MessageType.SERVER)
            broadcastUserUpdate()
        }
    }

    /**
     * Handles the 'who' command by sending the member a list of all all members names in the server.
     */
    suspend fun who(sender: String) {
        val text = memberNames.values.joinToString(prefix = "[server::who] ") { it.name }
        val sendMessage = SendMessage(ChatUser("Server"), text, MessageType.SERVER)
        members[sender]?.send(Frame.Text(sendMessage.toJson()))
    }

    /**
     * Handles the 'help' command by sending the member a list of available commands.
     */
    suspend fun help(sender: String) {
        //val text = "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} [server::help] Possible commands are: /user, /help, /me and /who"
        val sendMessage = SendMessage(
            ChatUser("Server"),
            "[server::help] Possible commands are: ${ChatCommands.values().joinToString(", ") { "${it.command.trim()} (${it.helpText})" }}",
            MessageType.SERVER
        )
        members[sender]?.send(Frame.Text(sendMessage.toJson()))
        //members[sender]?.send(Frame.Text("[server::help] Possible commands are: /user, /help, /me and /who"))
        /*broadcast(
            sender,
            "[server::help] Possible commands are: /user, /help, /me and /who",
            MessageType.SERVER,
            sender
        )*/
    }

    private fun getMemberByUsername(userName: String) = memberNames.search(1L) { id, user ->
        if (user.name == userName)
            id
        else
            null
    }

    /**
     * Handles sending to a [recipient] from a [sender] a [message].
     *
     * Both [recipient] and [sender] are identified by its session-id.
     */
    suspend fun sendTo(recipient: String, sender: String, message: String) {
        val sendToUser = getMemberByUsername(recipient)
        if (sendToUser == null) {
            val sendMessage = SendMessage(ChatUser("Server"), "User not found", MessageType.SERVER)
            members[sender]?.send(Frame.Text(sendMessage.toJson()))
        } else {
            val user = memberNames[sender]!!
            val sendMessage =
                SendMessage(user, "(${user.name} => $recipient) $message", MessageType.MESSAGE, data = "pm")
            members[sendToUser]?.send(Frame.Text(sendMessage.toJson()))
            members[sender]?.send(Frame.Text(sendMessage.toJson()))
        }
        prettyLog("$recipient\n$sendToUser\n$message")
    }

    data class EpisodeApiInfo(
        val name: String = "",
        val image: String = "",
        val url: String = "",
        val description: String = ""
    )

    suspend fun getShow(db: Database, showToSearch: String, sender: String) {
        var s: List<EpisodeApiInfo> = listOf()
        transaction(db) {
            s = Episode.find { Episodes.name like "%${showToSearch.trim()}%" }
                .map { EpisodeApiInfo(it.name, it.image, it.url, it.description) }
        }
        prettyLog(s)
        if (s.isEmpty()) {
            val sendMessage = SendMessage(ChatUser("Server"), "Show not found", MessageType.SERVER)
            members[sender]?.send(Frame.Text(sendMessage.toJson()))
        } else {
            broadcast(sender, "Displaying Show: ", MessageType.EPISODE, Gson().toJson(s))
        }
    }

    /**
     * Handles a [message] sent from a [sender] by notifying the rest of the users.
     */
    suspend fun message(sender: String, message: String) {
        // Pre-format the message to be send, to prevent doing it for all the users or connected sockets.
        val name = memberNames[sender]?.name ?: sender
        val formatted = "$name: $message"
        // Sends this pre-formatted message to all the members in the server.
        broadcast(sender, formatted, MessageType.MESSAGE)
    }

    /**
     * Handles a [message] sent from a [sender] by notifying the rest of the users.
     */
    suspend fun actionMessage(sender: String, message: String) {
        // Pre-format the message to be send, to prevent doing it for all the users or connected sockets.
        val name = memberNames[sender]?.name ?: sender
        val formatted = "[i]$name$message[/i]"

        // Sends this pre-formatted message to all the members in the server.
        broadcast(sender, formatted, MessageType.MESSAGE)
    }

    data class DidYouKnowFact(
        val id: String,
        val text: String,
        val source_url: String,
        val language: String,
        val permalink: String
    )

    suspend fun didYouKnow() {
        val url = "https://uselessfacts.jsph.pl/random.json?language=en"
        val d = Gson().fromJson(makeAPIRequest(url), DidYouKnowFact::class.java)
        broadcast("Server", d.text, MessageType.MESSAGE)
    }

    suspend fun joke(sender: String) {
        val j = getJoke()
        if (j != null) {
            broadcast("Server", "${j.title}\n${j.text}", MessageType.MESSAGE)
        } else {
            val sendMessage =
                SendMessage(ChatUser("Server"), "Something went wrong getting the joke", MessageType.SERVER)
            members[sender]?.send(Frame.Text(sendMessage.toJson()))
        }
    }

    suspend fun ChuckNorris(sender: String) {
        val j = getChuckNorris()?.let {
            broadcast("Chuck Norris", it, MessageType.MESSAGE)
        }
        if (j == null) {
            val sendMessage =
                SendMessage(ChatUser("Server"), "Chuck Norris did not want to come", MessageType.SERVER)
            members[sender]?.send(Frame.Text(sendMessage.toJson()))
        }
    }

    suspend fun getEvilInsult(sender: String) {
        val j = getEvilInsult()?.let {
            broadcast("EvilInsult", it, MessageType.MESSAGE)
        }
        if (j == null) {
            val sendMessage =
                SendMessage(ChatUser("Server"), "Something went wrong getting the insult", MessageType.SERVER)
            members[sender]?.send(Frame.Text(sendMessage.toJson()))
        }
    }

    enum class MessageType {
        MESSAGE, EPISODE, SERVER, INFO, TYPING_INDICATOR, DOWNLOADING
    }

    suspend fun downloadMessages(sender: String) {
        val html = createHTML(true, xhtmlCompatible = true)
            .html {
                body {
                    table {
                        lastMessages.forEach { info ->
                            tr {
                                td {
                                    unsafe {
                                        +"${info.time} ${info.message}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        val sendMessage = SendMessage(ChatUser("Server"), "", MessageType.DOWNLOADING, html)
        members[sender]?.send(Frame.Text(sendMessage.toJson()))
    }

    suspend fun isTyping(sender: String, typingIndicator: TypingIndicator) {
        peopleWhoAreTyping[sender] = typingIndicator.isTyping
        val people = peopleWhoAreTyping.filter { it.value }.map { memberNames[it.key]!!.name }
        val peopleToShow = if (people.size > 3) "People" else people.joinToString(", ")
        val check = typingIndicator.isTyping || people.isNotEmpty()
        val text = if (check) "$peopleToShow are typing..." else ""
        val sendMessage = SendMessage(ChatUser("Server"), text, MessageType.TYPING_INDICATOR).toJson()
        members.forEach {
            it.value.send(Frame.Text(sendMessage))
        }
    }

    data class SendMessage(val user: ChatUser, val message: String, val type: MessageType?, val data: Any? = null) {
        val time = SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())!!
        fun toJson(): String = Gson().toJson(this)
    }

    suspend fun sendServerMessage(msg: String) {
        broadcast(SendMessage(ChatUser("Server"), msg, MessageType.SERVER).toJson())
    }

    /**
     * Sends a [message] to all the members in the server, including all the connections per member.
     */
    private suspend fun broadcast(message: String) {
        members.values.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }

    private suspend fun broadcastUserUpdate() {
        /*members.values.forEach { sockets ->
            sockets.send(Frame.Text(SendMessage(
                ChatUser("Server"),
                "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} ",
                MessageType.INFO,
                memberNames.values.joinToString("\n") { it.name }
            ).toJson()))
        }*/
        val message = SendMessage(
            ChatUser("Server"),
            "",
            MessageType.INFO,
            memberNames.values
        ).toJson()
        members.values.forEach { sockets ->
            sockets.send(Frame.Text(message))
        }
    }

    /**
     * Sends a [message] to all the members in the server, including all the connections per member.
     */
    private suspend fun broadcast(message: String, recipient: String) {
        members[recipient]?.send(Frame.Text(message))
    }

    /**
     * Sends a [message] coming from a [sender] to all the members in the server, including all the connections per member.
     */
    private suspend fun broadcast(
        sender: String,
        message: String,
        type: MessageType = MessageType.MESSAGE,
        data: Any? = null
    ) {
        //val name = memberNames[sender]?.name ?: sender
        //val text = "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} $message"
        val sendMessage = SendMessage(memberNames[sender] ?: ChatUser("Server"), message, type, data)
        broadcast(sendMessage.toJson())
        prettyLog(sendMessage.toJson())
        if (type != MessageType.TYPING_INDICATOR) {
            synchronized(lastMessages) {
                lastMessages.add(sendMessage)
                if (lastMessages.size > 100) {
                    lastMessages.removeFirst()
                }
            }
        }
    }

    /**
     * Sends a [message] to a list of [this] [WebSocketSession].
     */
    private suspend fun List<WebSocketSession>.send(frame: Frame) {
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
                } catch (ignore: ClosedSendChannelException) {
                    // at some point it will get closed
                }
            }
        }
    }
}