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
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.util.pipeline.PipelineContext
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.p
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.substring
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

fun main(args: Array<String>): Unit {
    val server = embeddedServer(Netty, port = 8080, module = Application::module).start(wait = false)
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(1, 5, TimeUnit.SECONDS)
    })
    Thread.currentThread().join()
}

fun Application.module() {
    System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3")
    prettyLog("${System.getenv("JDBC_DATABASE_URL")} and ${System.getenv("KTOR_ENV")}")

    val db = DbSettings.db

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")

    val highScoreFile = File("resources/database/highscores.json")

    monitoring(highScoreFile)
    installing(simpleJwt)
    database(db)
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

private fun Application.monitoring(highScoreFile: File) {
    environment.monitor.subscribe(ApplicationStarting) {
        println("Starting")
    }
    environment.monitor.subscribe(ApplicationStarted) {
        println("Started")
        musicHighScoreSetup(highScoreFile)
    }
    environment.monitor.subscribe(ApplicationStopPreparing) {
        println("Stop Preparing")
        musicHighScoreSave(highScoreFile)
    }
    environment.monitor.subscribe(ApplicationStopping) {
        println("Stopping")
    }
    environment.monitor.subscribe(ApplicationStopped) {
        println("Stopped")
    }
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
        musicGameApi()

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
                val time = measureTimeMillis {
                    /*createEverything(
                        db,
                        ShowApi(Source.RECENT_CARTOON).showInfoList
                    ).join()*//*updateShows(db).join()*/
                }
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

fun makeAPIRequest(url: String): String? {
    val client = OkHttpClient();
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response -> return response.body?.string() }
}

inline fun <reified T> getAPIRequest(url: String): T? {
    val client = OkHttpClient();
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        return response.body?.string()?.let {
            Gson().fromJson(it, T::class.java)
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

class User(val name: String, val password: String)

val users = Collections.synchronizedMap(
    listOf(User("test", "test"))
        .associateBy { it.name }
        .toMutableMap()
)

class InvalidCredentialsException(message: String) : RuntimeException(message)

class LoginRegister(val user: String, val password: String)
