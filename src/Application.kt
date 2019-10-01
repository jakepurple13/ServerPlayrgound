@file:Suppress("RegExpRedundantEscape")

package com.example

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
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.util.generateNonce
import io.ktor.util.pipeline.PipelineContext
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.util.concurrent.TimeUnit

fun main(args: Array<String>): Unit {
    val env = applicationEngineEnvironment {
        module {
            module()
        }
        // Private API
        connector {
            host = "127.0.0.1"
            port = 9090
        }
        // Public API
        connector {
            host = "0.0.0.0"
            port = 8080
        }
    }
    //val server = embeddedServer(Netty, port = 8080, module = Application::module).start(wait = false)
    val server = embeddedServer(Netty, env).start(wait = false)
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(1, 5, TimeUnit.SECONDS)
    })
    Thread.currentThread().join()
}

fun Application.module() {
    System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3")
    //prettyLog("${System.getenv("JDBC_DATABASE_URL")} and ${System.getenv("KTOR_ENV")}")

    val db = DbSettings.db

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")

    val highScoreFile = File("resources/database/highscores.json")

    monitoring(highScoreFile)
    installing(simpleJwt)
    timeSave(highScoreFile)
    database(db)
    routing(db, simpleJwt)
}

private fun Application.timeSave(highScoreFile: File) {
    GlobalScope.launch {
        while (true) {
            //Every 5 minutes save the highscore information
            delay(300000L)
            prettyLog("Saving")
            musicHighScoreSave(highScoreFile)
        }
    }
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

        suspend fun PipelineContext<Unit, ApplicationCall>.getOrUpdateShows(block: () -> Job) {
            if (isPrivateApi()) {
                val sf = timeAction {
                    block()
                }
                val starting = "Running at ${SimpleDateFormat("MM/dd hh:mm a").format(sf.first)}"
                prettyLog(starting)
                val finished =
                    "Finished after ${sf.second} at ${SimpleDateFormat("MM/dd hh:mm a").format(System.currentTimeMillis())}"
                prettyLog(starting + "\n" + finished)
                call.respondHtml {
                    body {
                        p {
                            +(starting + "\n" + finished)
                        }
                    }
                }
            } else {
                notFound("You should not be here")
            }
        }

        route("/private") {
            get("/{action}Shows") {
                when (call.parameters["action"]!!) {
                    "update" -> {
                        getOrUpdateShows {
                            updateShows(db)
                        }
                    }
                    "get" -> {
                        getOrUpdateShows {
                            createEverything(
                                db,
                                ShowApi.getSources(
                                    Source.ANIME,
                                    Source.DUBBED,
                                    Source.CARTOON,
                                    Source.CARTOON_MOVIES,
                                    Source.LIVE_ACTION
                                )
                            )
                        }
                    }
                    else -> {
                        notFound("Wrong link")
                    }
                }
            }
        }

        route("/shows") {
            get {
                call.respondRedirect("/shows/0-9", true)
            }
            get("/{level}") {
                val level = call.parameters["level"]!!
                val checkLevel = when (level.toLowerCase()) {
                    "0-9" -> ('0'..'9')
                    in (('a'..'z') + ('A'..'Z')).map { "$it" } -> listOf(level.toLowerCase(), level.toUpperCase())
                    else -> null
                }?.map { "$it" }
                if (!checkLevel.isNullOrEmpty()) {
                    var list = listOf<EpisodeApiInfo>()
                    transaction(db) {
                        list = Episodes.select {
                            try {
                                Episodes.name.substring(1, 1) inList checkLevel
                            } catch (e: Exception) {
                                Episodes.name neq Episodes.name
                            }
                        }.map {
                            EpisodeApiInfo(
                                it[Episodes.name],
                                it[Episodes.image],
                                it[Episodes.url],
                                it[Episodes.description]
                            )
                        }.sortedBy { it.name }
                    }
                    call.respond(FreeMarkerContent("table.ftl", mapOf("data" to list)))
                } else {
                    notFound("Unable to Retrieve")
                }
            }
        }

        route("/") {
            get {
                prettyLog(call.request.origin.port)
                prettyLog(call.request.local.port)
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

inline fun <reified T> makeServerRequest(url: String): T? = getAPIRequest<T>("http://127.0.0.1:9090/$url")

fun makeServerRequest(url: String): String? = makeAPIRequest("http://127.0.0.1:9090/$url")

fun makeAPIRequest(url: String, requestConfig: Request.Builder.() -> Request.Builder = { this }): String? {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .requestConfig()
        .build()

    client.newCall(request).execute().use { response -> return response.body?.string() }
}

inline fun <reified T> getAPIRequest(url: String, requestConfig: Request.Builder.() -> Request.Builder = { this }): T? {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .requestConfig()
        .build()

    client.newCall(request).execute().use { response ->
        return response.body?.string()?.let {
            Gson().fromJson(it, T::class.java)
        }
    }
}
