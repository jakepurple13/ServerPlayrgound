@file:Suppress("RegExpRedundantEscape")

package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.Gson
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
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
import io.ktor.routing.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.util.pipeline.PipelineContext
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.html.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

fun main(args: Array<String>): Unit {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
    //io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
    val db = DbSettings.db

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")

    database(db)

    installing(simpleJwt)

    routing(db, simpleJwt)

}

private fun Application.database(db: Database) {
    GlobalScope.launch {
        //getAllShows(db)
        //getAllShowsAndEpisodes(db)
        //prettyLog(ShowApi(Source.LIVE_ACTION_MOVIES).showInfoList.joinToString { "$it\n" })
        //updateShows(db)
        //val cssGridLayout = "https://grid.layoutit.com/"
        //createEverything(db, ShowApi.getAllMovies())
        /*transaction(db) {
            if(Show.all().count()==0) {
                //createEverything(db)
                createEverything(db, ShowApi.getSources(Source.ANIME, Source.DUBBED, Source.CARTOON, Source.CARTOON_MOVIES))
            }
        }*/
        //createEverything(db, ShowApi.getAllRecent())
        //createEverything(db, ShowApi(Source.RECENT_CARTOON).showInfoList)
        //prettyLog(ShowApi(Source.LIVE_ACTION_MOVIES).showInfoList)
        //createEverything(db, ShowApi.getSources(Source.ANIME, Source.DUBBED, Source.CARTOON, Source.CARTOON_MOVIES, Source.LIVE_ACTION))
        //createEverything(db, ShowApi.getSources(Source.ANIME, Source.CARTOON, Source.DUBBED, Source.LIVE_ACTION))
        //createEverything(db, ShowApi.getSources(Source.CARTOON_MOVIES, Source.LIVE_ACTION_MOVIES))
    }

    /*GlobalScope.launch {
        while (true) {
            delay(3600000L)
            //updateShows(db)
        }
    }*/
}

private fun Application.installing(simpleJwt: SimpleJWT) {
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
}

private fun Application.routing(db: Database, simpleJwt: SimpleJWT) {
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

        chatRoute()
        api(db)

        route("/nsi/{name}") {
            get {
                prettyLog(call.request.origin.remoteHost)
                val name = call.parameters["name"]!!

                var episode: EpisodeApiInfo? = null

                val source = when (name[0]) {
                    'p' -> "putlocker"
                    'g' -> "gogoanime"
                    'a' -> "animetoon"
                    else -> ""
                }
                transaction(db) {
                    try {
                        val e = Episodes.select {
                            Episodes.url like "%$source%" and (Episodes.url like "%${name.substring(1)}%")
                        }.toList()

                        if (e.isNotEmpty()) {
                            val list = EpisodeLists.select { EpisodeLists.episode eq e[0][Episodes.id] }
                                .map { EpListInfo(it[EpisodeLists.name], it[EpisodeLists.url]) }
                            episode = EpisodeApiInfo(
                                e[0][Episodes.name],
                                e[0][Episodes.image],
                                e[0][Episodes.url],
                                e[0][Episodes.description],
                                list
                            )
                        }
                    } catch (ignored: Exception) {

                    }
                }
                if (episode != null)
                    call.respond(FreeMarkerContent("epview.ftl", mapOf("data" to episode)))
                else
                    notFound("Show not found")
            }
        }

        route("/updateShows") {
            get {
                val starting = "Running at ${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())}"
                prettyLog(starting)
                val time = measureTimeMillis { /*updateShows(db).join()*/ }
                val finished =
                    "Finished after $time at ${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())}"
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

        route("/shows/{level}") {
            get {
                val level = call.parameters["level"]!!
                val checkLevel = when (level.toLowerCase()) {
                    "0-9" -> ('0'..'9')
                    "a" -> listOf('a', 'A')
                    "b" -> listOf('b', 'B')
                    "c" -> listOf('c', 'C')
                    "d" -> listOf('d', 'D')
                    "e" -> listOf('e', 'E')
                    "f" -> listOf('f', 'F')
                    "g" -> listOf('g', 'G')
                    "h" -> listOf('h', 'H')
                    "i" -> listOf('i', 'I')
                    "j" -> listOf('j', 'J')
                    "k" -> listOf('k', 'K')
                    "l" -> listOf('l', 'L')
                    "m" -> listOf('m', 'M')
                    "n" -> listOf('n', 'N')
                    "o" -> listOf('o', 'O')
                    "p" -> listOf('p', 'P')
                    "q" -> listOf('q', 'Q')
                    "r" -> listOf('r', 'R')
                    "s" -> listOf('s', 'S')
                    "t" -> listOf('t', 'T')
                    "u" -> listOf('u', 'U')
                    "v" -> listOf('v', 'V')
                    "w" -> listOf('w', 'W')
                    "x" -> listOf('x', 'X')
                    "y" -> listOf('y', 'Y')
                    "z" -> listOf('z', 'Z')
                    else -> null
                }?.map { "$it" }
                var list = listOf<EpisodeApiInfo>()
                if (!checkLevel.isNullOrEmpty()) {
                    transaction(db) {
                        list = Episodes.select {
                            try {
                                Episodes.name.substring(1, 1) inList checkLevel
                            } catch (e: Exception) {
                                Episodes.name neq Episodes.name
                            }
                        }
                            .map {
                                EpisodeApiInfo(
                                    it[Episodes.name],
                                    it[Episodes.image],
                                    it[Episodes.url],
                                    it[Episodes.description]
                                )
                            }
                            .sortedBy { it.name }
                    }
                    call.respond(FreeMarkerContent("table.ftl", mapOf("data" to list)))
                } else {
                    notFound("Unable to Retrieve")
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

suspend fun PipelineContext<Unit, ApplicationCall>.notFound(text: String = "Not Found") {
    call.respondHtml {
        body {
            p {
                +"404! $text"
            }
        }
    }
}

private fun Route.chatRoute() {
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

fun Route.api(db: Database) {
    route("/api") {
        get("/about") {
            call.respondHtml {
                body {
                    dl {
                        dt {
                            +"To get video url"
                        }
                        dd {
                            +"/api/video/{url}.json"
                        }
                        dd {
                            +"make sure all of the \"/\" are changed to \"_\" when submitting"
                        }
                        br { }
                        dt {
                            +"To get all shows in database"
                        }
                        dd {
                            +"/api/user/all.json"
                        }
                        br { }
                        dt {
                            +"To get recent shows"
                        }
                        dd {
                            +"/api/user/r{type}.json"
                        }
                        dd {
                            +"l for TV Shows"
                        }
                        dd {
                            +"c for Cartoons"
                        }
                        dd {
                            +"a for Anime"
                        }
                        dd {
                            +"all for All"
                        }
                        br { }
                        dt {
                            +"To get Show Information"
                        }
                        dd {
                            +"/nsi/{name}.json"
                        }
                        dd {
                            +"name consists of the first letter of the kind of source and the name in all lowercase and hyphens instead of spaces"
                        }
                    }
                }
            }
        }
        get("/video/{url}.json") {
            val url = call.parameters["url"]!!
            val vla = VideoLinkApi(url.replace("_", "/")).getVideoLink()
            call.respond(mapOf("VideoLink" to vla))
        }
        webApi(db)
        userApi(db)
    }
}

fun Route.webApi(db: Database) {
    route("/web") {
        get("/all.json") {
            var list = listOf<ShowInfo>()
            transaction(db) {
                list = Shows.selectAll().map { ShowInfo(it[Shows.name], it[Shows.url]) }.sortedBy { it.name }
            }
            call.respond(list)
        }
        get("/nameAll.json") {
            var list = listOf<String>()
            transaction(db) {
                list = Shows.selectAll().map { it[Shows.name] }.sortedBy { it }
            }
            call.respond(list)
        }
        get("/r{type}.json") {
            when (call.parameters["type"]!!) {
                "c" -> Source.RECENT_CARTOON
                "a" -> Source.RECENT_ANIME
                "l" -> Source.RECENT_LIVE_ACTION
                else -> null
            }?.let {
                val s = ShowApi(it).showInfoList
                call.respond(s)
            }
        }
        getShowType(db)
        randomShow(db)
    }
}

fun Route.userApi(db: Database) {
    route("/user") {
        get("/all.json") {
            var list = listOf<ShowInfo>()
            transaction(db) {
                list = Shows.selectAll().map { ShowInfo(it[Shows.name], it[Shows.url]) }.sortedBy { it.name }
                //list = Show.all().sortedBy { it.name }.map { ShowInfo(it.name, it.url) }
            }
            call.respond(mapOf("Shows" to list))
        }
        get("/nsi/{name}.json") {
            val name = call.parameters["name"]!!

            val source = when (name[0]) {
                'p' -> "putlocker"
                'g' -> "gogoanime"
                'a' -> "animetoon"
                else -> ""
            }

            var episode = EpisodeApiInfo()

            transaction(db) {
                val e = Episodes.select {
                    Episodes.url like "%$source%" and (Episodes.url like "%${name.substring(1)}%")
                }.toList()
                val i = e[0]
                val l = EpisodeLists.select { EpisodeLists.episode eq i[Episodes.id] }
                    .map { EpListInfo(it[EpisodeLists.name], it[EpisodeLists.url]) }
                episode = EpisodeApiInfo(
                    i[Episodes.name],
                    i[Episodes.image],
                    i[Episodes.url],
                    i[Episodes.description],
                    l
                )
            }
            call.respond(mapOf("EpisodeInfo" to episode))
        }
        get("/r{type}.json") {
            when (call.parameters["type"]!!) {
                "c" -> Source.RECENT_CARTOON
                "a" -> Source.RECENT_ANIME
                "l" -> Source.RECENT_LIVE_ACTION
                "all" -> Source.DUBBED
                else -> null
            }?.let {
                val s = if (it == Source.DUBBED) ShowApi.getAllRecent() else ShowApi(it).showInfoList
                call.respond(mapOf("Shows" to synchronized(s) { s.toList() }))
            }
        }
        getShowType(db)
        randomShow(db)
    }
}

fun Route.getShowType(db: Database) {
    get("/t{type}.json") {
        val type = call.parameters["type"]!!
        var list = listOf<ShowInfo>()
        transaction(db) {
            list = Shows.select { Shows.url like "%$type%" }.map { ShowInfo(it[Shows.name], it[Shows.url]) }
                .sortedBy { it.name }
        }
        call.respond(list)
    }
}

fun Route.randomShow(db: Database) {
    get("/random.json") {
        var showInfo = ShowInfo("", "")
        transaction(db) {
            showInfo = Shows.selectAll().map { ShowInfo(it[Shows.name], it[Shows.url]) }.random()
        }
        call.respond(showInfo)
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

class User(val name: String, val password: String)

val users = Collections.synchronizedMap(
    listOf(User("test", "test"))
        .associateBy { it.name }
        .toMutableMap()
)

class InvalidCredentialsException(message: String) : RuntimeException(message)

class LoginRegister(val user: String, val password: String)

data class EpListInfo(val name: String, val url: String)
data class EpisodeApiInfo(
    val name: String = "",
    val image: String = "",
    val url: String = "",
    val description: String = "",
    val episodeList: List<EpListInfo> = emptyList()
) {
    companion object {
        fun fromApi(ea: EpisodeApi) = EpisodeApiInfo(
            ea.name,
            ea.image,
            ea.source.url,
            ea.description,
            ea.episodeList.map { EpListInfo(it.name, it.url) })
        //fun fromDB(ea: Episodes) = EpisodeApiInfo()
    }
}

class ChatClient(val session: DefaultWebSocketSession) {
    companion object {
        var lastId = AtomicInteger(0)
    }

    val id = lastId.getAndIncrement()
    val name = "user$id"
}

private val server = ChatServer()

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
    SHOW("/show ", "Displays info about a show from the database. If you type @ first, you can autocomplete a show."),
    HELP("/help", "Shows this message."), PRIVATE_MESSAGE("/pm ", "Private message someone"),
    ACTION("/me", "Show an action. It will be in all italics.")
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
                    command.removePrefix("/pm ").split(" ").drop(1).joinToString { "$it " }.trim()
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
