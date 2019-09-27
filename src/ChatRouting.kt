package com.example

import com.google.gson.Gson
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun Route.chatRoute() {
    route("/chat") {
        val clients = Collections.synchronizedSet(LinkedHashSet<ChatClient>())

        webSocket("/ws") {
            // this: WebSocketSession ->

            // First of all we get the session.
            val session = call.sessions.get<ChatSession>()

            // We check that we actually have a session. We should always have one,
            // since we have defined an interceptor before to set one.
            if (session == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                return@webSocket
            }

            // We notify that a member joined by calling the server handler [memberJoin]
            // This allows to associate the session id to a specific WebSocket connection.
            server.memberJoin(session.id, this)

            try {
                // We starts receiving messages (frames).
                // Since this is a coroutine. This coroutine is suspended until receiving frames.
                // Once the connection is closed, this consumeEach will finish and the code will continue.
                incoming.consumeEach { frame ->
                    // Frames can be [Text], [Binary], [Ping], [Pong], [Close].
                    // We are only interested in textual messages, so we filter it.
                    if (frame is Frame.Text) {
                        // Now it is time to process the text sent from the user.
                        // At this point we have context about this connection, the session, the text and the server.
                        // So we have everything we need.
                        receivedMessage(session.id, frame.readText())
                    }
                }
            } finally {
                // Either if there was an error, of it the connection was closed gracefully.
                // We notify the server that the member left.
                server.memberLeft(session.id, this)
            }
        }
        static {
            // This marks index.html from the 'web' folder in resources as the default file to serve.
            defaultResource("chat.html", "web")
            // This serves files from the 'web' folder in the application resources.
            resources("web")
        }
    }
}

class ChatClient(val session: DefaultWebSocketSession) {
    companion object {
        var lastId = AtomicInteger(0)
    }

    val id = lastId.getAndIncrement()
    val name = "user$id"
}

val server = ChatServer()

/**
 * A chat session is identified by a unique nonce ID. This nonce comes from a secure random source.
 */
data class ChatSession(val id: String)

data class Action(val type: String, val json: String)
data class TypingIndicator(val isTyping: Boolean)
data class DownloadMessages(val download: Boolean)
data class Profile(val username: String?, val image: String?)

enum class ChatCommands(val command: String, val helpText: String = "") {
    DYK("/dyk", "Prints a random Did You Know fact."), WHO("/who", "Shows who's on the server."),
    SHOW(
        "/show ",
        "Displays info about a show from the database. If you type @ first, you can autocomplete a show. [@ only works on web]"
    ),
    HELP("/help", "Shows this message."), PRIVATE_MESSAGE("/pm ", "Private message someone"),
    ACTION("/me", "Show an action. It will be in all italics."), JOKE("/dailyjoke", "Prints a daily joke"),
    CHUCK_NORRIS("/chucknorris", "Display a Chuck Norris Fact"), EVIL_INSULT("/insult", "Display an evil insult")
}

/**
 * We received a message. Let's process it.
 */
private suspend fun receivedMessage(id: String, command: String) {
    // We are going to handle commands (text starting with '/') and normal messages
    try {
        val action = Gson().fromJson<Action>(command, Action::class.java)
        when (action.type) {
            "Typing" -> {
                val typing = Gson().fromJson<TypingIndicator>(action.json, TypingIndicator::class.java)
                server.isTyping(id, typing)
            }
            "Download" -> {
                prettyLog(command)
                //val download = Gson().fromJson<DownloadMessages>(action.json, DownloadMessages::class.java)
                server.downloadMessages(id)
            }
            "Profile" -> {
                prettyLog(command)
                val profile = Gson().fromJson<Profile>(action.json, Profile::class.java)
                profile.username?.let {
                    server.memberRenamed(id, it)
                }
                profile.image?.let {
                    server.memberImageChange(id, it)
                }
            }
        }
    } catch (e: Exception) {
        prettyLog(command)
        when {
            command.startsWith(ChatCommands.DYK.command) -> server.didYouKnow()
            command.startsWith(ChatCommands.JOKE.command) -> server.joke(id)
            command.startsWith(ChatCommands.CHUCK_NORRIS.command) -> server.ChuckNorris(id)
            command.startsWith(ChatCommands.EVIL_INSULT.command) -> server.getEvilInsult(id)
            // The command `who` responds the user about all the member names connected to the user.
            command.startsWith(ChatCommands.WHO.command) -> server.who(id)
            command.startsWith(ChatCommands.SHOW.command) -> {
                val showName = command.removePrefix("/show")
                server.getShow(DbSettings.db, showName, id)
            }
            // The command 'help' allows users to get a list of available commands.
            command.startsWith(ChatCommands.HELP.command) -> server.help(id)
            command.startsWith(ChatCommands.ACTION.command) -> server.actionMessage(id, command.removePrefix("/me"))
            command.startsWith(ChatCommands.PRIVATE_MESSAGE.command) -> {
                val recipient = command.removePrefix("/pm ").split(" ")[0].trim()
                server.sendTo(
                    recipient,
                    id,
                    command.removePrefix("/pm ").split(" ").drop(1).joinToString(" ").trim()
                )
            }
            // If no commands matched at this point, we notify about it.
            command.startsWith("/") -> server.sendTo(
                id,
                "server::help",
                "Unknown command ${command.takeWhile { !it.isWhitespace() }}"
            )
            // Handle a normal message.
            else -> server.message(id, command)
        }
    }
}
