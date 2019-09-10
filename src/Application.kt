@file:Suppress("RegExpRedundantEscape")

package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.auth.principal
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.features.origin
import io.ktor.freemarker.FreeMarker
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.html.respondHtml
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.onClick
import kotlinx.html.p
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
open class Application {
    companion object {
        @JvmStatic fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
    }
}
fun Application.module() {
    System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
    val db = DbSettings.db

    GlobalScope.launch {
        //getAllShows(db)
        //getAllShowsAndEpisodes(db)
        //prettyLog(ShowApi(Source.LIVE_ACTION_MOVIES).showInfoList.joinToString { "$it\n" })
        //updateShows(db)
        //val cssGridLayout = "https://grid.layoutit.com/"
        //createEverything(db, ShowApi.getAllMovies())
        //prettyLog(ShowApi(Source.LIVE_ACTION_MOVIES).showInfoList)
    }

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost()
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
        masking = false
    }
    install(Sessions) {
        cookie<ChatSession>("SESSION")
    }
    intercept(ApplicationCallPipeline.Features) {
        if (call.sessions.get<ChatSession>() == null) {
            call.sessions.set(ChatSession(generateNonce()))
        }
    }
    install(StatusPages) {
        exception<InvalidCredentialsException> { exception ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
    }
    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT) // Pretty Prints the JSON
        }
    }
    install(FreeMarker) {
        templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
    }
    routing {
        post("/login-register") {
            val post = call.receive<LoginRegister>()
            val user = users.getOrPut(post.user) { User(post.user, post.password) }
            if (user.password != post.password) throw InvalidCredentialsException("Invalid credentials")
            call.respond(mapOf("token" to simpleJwt.sign(user.name)))
        }
        route("/snippets") {
            get {
                call.respond(mapOf("snippets" to synchronized(snippets) { snippets.toList() }))
            }
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
                    snippets += Snippet(principal.name, post.snippet.text)
                    call.respond(mapOf("OK" to true))
                }
            }
        }

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

        route("/nsi/{name}") {
            get {
                prettyLog(call.request.origin.remoteHost)
                val name = call.parameters["name"]!!

                data class EpisodeApiInfo(
                    val name: String = "",
                    val image: String = "",
                    val url: String = "",
                    val description: String = "",
                    val episodeList: List<EpisodeList> = emptyList()
                )

                var episode = EpisodeApiInfo()

                val source = when (name[0]) {
                    'p' -> "putlocker"
                    'g' -> "gogoanime"
                    'a' -> "animetoon"
                    else -> ""
                }
                transaction(db) {
                    try {
                        val e = Episode.find {
                            Episodes.url like "%$source%" and (Episodes.url like "%${name.substring(1)}%")
                        }.toList()
                        val list = EpisodeList.find { EpisodeLists.episode eq e[0].id }.toList()
                        episode = EpisodeApiInfo(
                            e[0].name,
                            e[0].image,
                            e[0].url,
                            e[0].description,
                            list
                        )
                    } catch (ignored: Exception) {

                    }
                }
                call.respond(
                    FreeMarkerContent(
                        "epview.ftl",
                        mapOf("data" to episode)
                    )
                )
            }
        }

        route("/api") {
            get("/about") {
                //TODO: Make api about

            }
            get("/video/{url}.json") {
                val url = call.parameters["url"]!!
                val vla = VideoLinkApi(url.replace("_", "/")).getVideoLink()
                call.respond(mapOf("VideoLink" to vla))
            }
            get("/all.json") {
                var list = listOf<ShowInfo>()
                transaction(db) {
                    list = Show.all().sortedBy { it.name }.map { ShowInfo(it.name, it.url) }
                }
                call.respond(list)
            }
            get("/userAll.json") {
                var list = listOf<ShowInfo>()
                transaction(db) {
                    list = Show.all().sortedBy { it.name }.map { ShowInfo(it.name, it.url) }
                }
                call.respond(mapOf("Shows" to list))
            }
            get("/nsi/{name}.json") {
                val name = call.parameters["name"]!!

                data class EpListInfo(val name: String, val url: String)
                data class EpisodeApiInfo(
                    val name: String = "",
                    val image: String = "",
                    val url: String = "",
                    val description: String = "",
                    val episodeList: List<EpListInfo> = emptyList()
                )

                val source = when (name[0]) {
                    'p' -> "putlocker"
                    'g' -> "gogoanime"
                    'a' -> "animetoon"
                    else -> ""
                }

                var episode = EpisodeApiInfo()

                transaction(db) {
                    val e = Episode.find {
                        Episodes.url like "%$source%" and (Episodes.url like "%${name.substring(
                            1
                        )}%")
                    }.toList()
                    val i = e[0]
                    val l = EpisodeList.find { EpisodeLists.episode eq i.id }.toList()
                    val list = l.map { EpListInfo(it.name, it.url) }
                    episode = EpisodeApiInfo(
                        i.name,
                        i.image,
                        i.url,
                        i.description,
                        list
                    )
                }
                call.respond(mapOf("EpisodeInfo" to episode))
            }
            get("/r{type}.json") {
                when (call.parameters["type"]!!) {
                    "c" -> Source.RECENT_CARTOON
                    "a" -> Source.RECENT_ANIME
                    "l" -> Source.RECENT_LIVE_ACTION
                    else -> null
                }?.let {
                    val s = ShowApi(it).showInfoList
                    call.respond(mapOf("shows" to synchronized(s) { s.toList() }))
                }
            }
        }
        route("/updateShows") {
            get {
                val starting = "Running at ${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())}"
                prettyLog(starting)
                val time = measureTimeMillis { updateShows(db).join() }
                val finished = "Finished after $time at ${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())}"
                prettyLog(starting + "\n" + finished)
                call.respondHtml {
                    body {
                        p {
                            +(starting + "\n" + finished)
                        }
                    }
                }
            }
        }
        route("/") {
            get {
                prettyLog(call.request.origin.remoteHost)
                call.respond(FreeMarkerContent("boottabletwo.ftl", null))
            }
            get("/old") {
                var list = listOf<Show>()
                transaction(db) {
                    list = Show.all().sortedBy { it.name }
                }
                call.respond(
                    FreeMarkerContent(
                        "boottable.ftl",
                        mapOf("data" to list)
                    )
                )
            }
        }
        static("/static") {
            resources("static")
        }
    }

}

data class PostSnippet(val snippet: PostSnippet.Text) {
    data class Text(val text: String)
}

data class Snippet(val user: String, val text: String)

val snippets = Collections.synchronizedList(
    mutableListOf(
        Snippet(user = "test", text = "hello"),
        Snippet(user = "test", text = "world")
    )
)

open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

class ChatClient(val session: DefaultWebSocketSession) {
    companion object {
        var lastId = AtomicInteger(0)
    }

    val id = lastId.getAndIncrement()
    val name = "user$id"
}

class User(val name: String, val password: String)

val users = Collections.synchronizedMap(
    listOf(User("test", "test"))
        .associateBy { it.name }
        .toMutableMap()
)

class InvalidCredentialsException(message: String) : RuntimeException(message)

class LoginRegister(val user: String, val password: String)

private val server = ChatServer()

/**
 * A chat session is identified by a unique nonce ID. This nonce comes from a secure random source.
 */
data class ChatSession(val id: String)

/**
 * We received a message. Let's process it.
 */
private suspend fun receivedMessage(id: String, command: String) {
    // We are going to handle commands (text starting with '/') and normal messages
    when {
        // The command `who` responds the user about all the member names connected to the user.
        command.startsWith("/who") -> server.who(id)
        // The command `user` allows the user to set its name.
        command.startsWith("/user") -> {
            // We strip the command part to get the rest of the parameters.
            // In this case the only parameter is the user's newName.
            val newName = command.removePrefix("/user").trim()
            // We verify that it is a valid name (in terms of length) to prevent abusing
            when {
                newName.isEmpty() -> server.sendTo(id, "server::help", "/user [newName]")
                newName.length > 50 -> server.sendTo(
                    id,
                    "server::help",
                    "new name is too long: 50 characters limit"
                )
                else -> server.memberRenamed(id, newName)
            }
        }
        command.startsWith("/show") -> {
            val showName = command.removePrefix("/show")
            server.getShow(DbSettings.db, showName, id)
        }
        command.startsWith("/image") -> {
            // We strip the command part to get the rest of the parameters.
            // In this case the only parameter is the user's newName.
            val newName = command.removePrefix("/image").trim()
            // We verify that it is a valid name (in terms of length) to prevent abusing
            when {
                newName.isEmpty() -> server.sendTo(id, "server::help", "/image [newImage]")
                else -> server.memberImageChange(id, newName)
            }
        }
        // The command 'help' allows users to get a list of available commands.
        command.startsWith("/help") -> server.help(id)
        command.startsWith("/me") -> server.actionMessage(id, command.removePrefix("/me"))
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

//--------------------------


fun prettyLog(msg: Any?) {
    //the main message to be logged
    var logged = msg.toString()
    //the arrow for the stack trace
    val arrow = "${9552.toChar()}${9655.toChar()}\t"
    //the stack trace
    val stackTraceElement = Thread.currentThread().stackTrace

    val elements = listOf(*stackTraceElement)
    val wanted = elements.filter { it.className.contains("example") && !it.methodName.contains("prettyLog") }

    var loc = "\n"

    for (i in wanted.indices.reversed()) {
        val fullClassName = wanted[i].className
        //get the method name
        val methodName = wanted[i].methodName
        //get the file name
        val fileName = wanted[i].fileName
        //get the line number
        val lineNumber = wanted[i].lineNumber
        //add this to location in a format where we can click on the number in the console
        loc += "$fullClassName.$methodName($fileName:$lineNumber)"

        if (wanted.size > 1 && i - 1 >= 0) {
            val typeOfArrow: Char =
                if (i - 1 > 0)
                    9568.toChar() //middle arrow
                else
                    9562.toChar() //ending arrow
            loc += "\n\t$typeOfArrow$arrow"
        }
    }

    logged += loc

    println(logged + "\n")
}
