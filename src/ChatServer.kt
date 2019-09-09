package com.example

import com.google.gson.Gson
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*


class ChatUser(var name: String, var image: String = "https://www.w3schools.com/w3images/bandmember.jpg")

/**
 * Class in charge of the logic of the chat server.
 * It contains handlers to events and commands to send messages to specific users in the server.
 */
class ChatServer {
    /**
     * Atomic counter used to get unique user-names based on the maxiumum users the server had.
     */
    val usersCounter = AtomicInteger()

    /**
     * A concurrent map associating session IDs to user names.
     */
    val memberNames = ConcurrentHashMap<String, ChatUser>()

    /**
     * Associates a session-id to a set of websockets.
     * Since a browser is able to open several tabs and windows with the same cookies and thus the same session.
     * There might be several opened sockets for the same client.
     */
    val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

    /**
     * A list of the lastest messages sent to the server, so new members can have a bit context of what
     * other people was talking about before joining.
     */
    val lastMessages = LinkedList<String>()

    /**
     * Handles that a member identified with a session id and a socket joined.
     */
    suspend fun memberJoin(member: String, socket: WebSocketSession) {
        // Checks if this user is already registered in the server and gives him/her a temporal name if required.
        val name = memberNames.computeIfAbsent(member) { ChatUser("user${usersCounter.incrementAndGet()}") }

        // Associates this socket to the member id.
        // Since iteration is likely to happen more frequently than adding new items,
        // we use a `CopyOnWriteArrayList`.
        // We could also control how many sockets we would allow per client here before appending it.
        // But since this is a sample we are not doing it.
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)

        // Only when joining the first socket for a member notifies the rest of the users.
        if (list.size == 1) {
            //broadcast("server", "Member joined: $name.")
            val text = "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} Connected"
            val sendMessage = SendMessage(ChatUser("Server"), text, MessageType.SERVER)
            members[member]?.send(Frame.Text(sendMessage.toJson()))
        }

        // Sends the user the latest messages from this server to let the member have a bit context.
        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message))
        }
    }

    /**
     * Handles a [member] idenitified by its session id renaming [to] a specific name.
     */
    fun memberRenamed(member: String, to: String) {
        // Re-sets the member name.
        //val oldName = memberNames.put(member, to) ?: member
        memberNames[member]?.name = to
        // Notifies everyone in the server about this change.
        //broadcast("server", "Member renamed from $member to $to")
    }

    /**
     * Handles a [member] idenitified by its session id renaming [to] a specific name.
     */
    fun memberImageChange(member: String, newImage: String) {
        // Re-sets the member name.
        //val oldName = memberNames.put(member, to) ?: member
        memberNames[member]?.image = newImage
        // Notifies everyone in the server about this change.
        //broadcast("server", "Member renamed from $member to $to")
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
        }
    }

    /**
     * Handles the 'who' command by sending the member a list of all all members names in the server.
     */
    suspend fun who(sender: String) {
        val text =
            "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} ${memberNames.values.joinToString(
                prefix = "[server::who] "
            ) { it.name }}"
        val sendMessage = SendMessage(ChatUser("Server"), text, MessageType.SERVER)
        members[sender]?.send(Frame.Text(sendMessage.toJson()))
        //members[sender]?.send(Frame.Text(memberNames.values.joinToString(prefix = "[server::who] ")))
        /*broadcast(
            sender,
            memberNames.values.joinToString(prefix = "[server::who] ") { it.name },
            MessageType.SERVER,
            sender
        )*/
    }

    /**
     * Handles the 'help' command by sending the member a list of available commands.
     */
    suspend fun help(sender: String) {
        val text =
            "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} [server::help] Possible commands are: /user, /help, /me and /who"
        val sendMessage = SendMessage(ChatUser("Server"), text, MessageType.SERVER)
        members[sender]?.send(Frame.Text(sendMessage.toJson()))
        //members[sender]?.send(Frame.Text("[server::help] Possible commands are: /user, /help, /me and /who"))
        /*broadcast(
            sender,
            "[server::help] Possible commands are: /user, /help, /me and /who",
            MessageType.SERVER,
            sender
        )*/
    }

    /**
     * Handles sending to a [recipient] from a [sender] a [message].
     *
     * Both [recipient] and [sender] are identified by its session-id.
     */
    suspend fun sendTo(recipient: String, sender: String, message: String) {
        val text = "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} $message"
        val sendMessage = SendMessage(memberNames[sender] ?: ChatUser(sender), text, MessageType.MESSAGE)
        members[recipient]?.send(Frame.Text(sendMessage.toJson()))
        //members[recipient]?.send(Frame.Text("[$sender] $message"))
        //broadcast(sender, message, MessageType.MESSAGE, recipient)
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
            s = Episode.find { Episodes.name like "%$showToSearch%" }.map { EpisodeApiInfo(it.name, it.image, it.url, it.description) }
        }
        prettyLog(s)
        broadcast(sender, "Displaying Show: ", MessageType.EPISODE, Gson().toJson(s))
    }

    /**
     * Handles a [message] sent from a [sender] by notifying the rest of the users.
     */
    suspend fun message(sender: String, message: String) {
        // Pre-format the message to be send, to prevent doing it for all the users or connected sockets.
        val name = memberNames[sender]?.name ?: sender
        val formatted = "[$name] $message"

        // Sends this pre-formatted message to all the members in the server.
        broadcast(sender, formatted, MessageType.MESSAGE)

        // Appends the message to the list of [lastMessages] and caps that collection to 100 items to prevent
        // growing too much.
        synchronized(lastMessages) {
            lastMessages.add(formatted)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    /**
     * Handles a [message] sent from a [sender] by notifying the rest of the users.
     */
    suspend fun actionMessage(sender: String, message: String) {
        // Pre-format the message to be send, to prevent doing it for all the users or connected sockets.
        val name = memberNames[sender]?.name ?: sender
        val formatted = "<i>$name $message</i>"

        // Sends this pre-formatted message to all the members in the server.
        broadcast(sender, formatted, MessageType.MESSAGE)

        // Appends the message to the list of [lastMessages] and caps that collection to 100 items to prevent
        // growing too much.
        synchronized(lastMessages) {
            lastMessages.add(formatted)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    enum class MessageType {
        MESSAGE, EPISODE, SERVER
    }

    data class SendMessage(val user: ChatUser, val message: String, val type: MessageType?, val data: String? = null) {
        fun toJson(): String = Gson().toJson(this)
    }

    suspend fun sendServerMessage(msg: String) {
        broadcast(SendMessage(
            ChatUser("Server"),
            "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} $msg",
            MessageType.SERVER
        ).toJson())
    }

    /**
     * Sends a [message] to all the members in the server, including all the connections per member.
     */
    private suspend fun broadcast(message: String) {
        members.values.forEach { socket ->
            socket.send(Frame.Text(message))
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
        data: String? = null
    ) {
        //val name = memberNames[sender]?.name ?: sender
        val text = "${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())} $message"
        val sendMessage = SendMessage(memberNames[sender] ?: ChatUser("Server"), text, type, data)
        broadcast(sendMessage.toJson())
    }

    /**
     * Sends a [message] to a list of [this] [WebSocketSession].
     */
    suspend fun List<WebSocketSession>.send(frame: Frame) {
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