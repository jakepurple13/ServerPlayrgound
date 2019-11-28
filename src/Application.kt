@file:Suppress("RegExpRedundantEscape")

package com.example

import com.fasterxml.jackson.databind.SerializationFeature
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dorkbox.notify.Notify
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
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.concurrent.TimeUnit


fun main() {
    System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3")
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

data class FirebaseEpisode(
    val name: String? = null,
    val url: String? = null
)

data class FirebaseShow(
    val name: String? = null,
    val url: String? = null,
    var showNum: Int = 0,
    val episodeInfo: List<FirebaseEpisode>? = null
)

fun Application.firebase() {
    // Use the application default credentials
    /*val serviceAccount = FileInputStream("resources/database/chesstest-3cd2a-firebase-adminsdk-5ytkb-3508b44064.json")
    val credentials = GoogleCredentials.fromStream(serviceAccount)
    val options = FirebaseOptions.Builder()
        .setCredentials(credentials)
        .setDatabaseUrl("https://chesstest-3cd2a.firebaseio.com")
        .build()
    FirebaseApp.initializeApp(options)

    val db = FirestoreClient.getFirestore()

    val ids = db.listCollections().map { it.id }
    val showInfos = db.listCollections().map { it.get().get().toObjects(FirebaseShow::class.java) }

    prettyLog(ids)*/
    //prettyLog("First id: ${ids[0]}: " + showInfos[0].toPrettyJson())
    //prettyLog("Second id: ${ids[1]}:" + showInfos[1].toPrettyJson())

    //FirebaseMessaging.getInstance().subscribeToTopic(ids, "test")

    /*val recent = ShowApi.getAllRecent()

    val both = recent.intersect(showInfos.flatten().distinctBy { it.url }) { one, two -> one.url == two.url }

    prettyLog(both.toPrettyJson())*/

    /*val listOfMessage = arrayListOf<Message>()

    both.forEach {
        val message = Message.builder()
            .putData("show", it.name)
            .setTopic(it.url)
            .build()
        listOfMessage += message
    }

    // Send a message to the devices subscribed to the provided topic.
    val response = FirebaseMessaging.getInstance().sendAll(listOfMessage)
    // Response is a message ID string.
    prettyLog("Successfully sent message: $response")*/

}

fun Application.module() {
    //prettyLog("${System.getenv("JDBC_DATABASE_URL")} and ${System.getenv("KTOR_ENV")}")

    val db = DbSettings.db

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")

    val highScoreFile = File("resources/database/highscores.json")

    val shows = ShowApi.getAll().toMutableList()

    GlobalScope.launch {
        //firebase()
    }

    val file = File("resources/database/movie.json")
    if (file.exists()) {
        try {
            val list = Gson().fromJson<MutableList<ShowInfo>>(
                file.readText(),
                object : TypeToken<MutableList<ShowInfo>>() {}.type
            )
            shows.addAll(list)
        } catch (e: Exception) {

        }
    }

    monitoring(highScoreFile)
    installing(simpleJwt)
    timeSave(highScoreFile, db)
    database(db)
    routing(ShowDBApi(db, shows.sortedBy { it.name }), simpleJwt)
}

private fun Application.timeSave(highScoreFile: File, db: Database) {
    GlobalScope.launch {
        while (true) {
            //Every 5 minutes save the highscore information
            delay(300000L)
            prettyLog("Saving")
            Notify.create()
                .title("Saving")
                .text("Saving files")
                .darkStyle()
                .hideAfter(3000)
                .show()
            musicHighScoreSave(highScoreFile)
        }
    }
}

private fun Application.database(db: Database) {

    /*GlobalScope.launch {
        val file = File("resources/database/movie.json")
        if (!file.exists()) {
            file.createNewFile()
        }
        val s = timeAction {
            GlobalScope.launch {
                val q = ShowApi.getAllMovies()
                file.writeText(q.toPrettyJson())
            }
        }
        prettyLog("${s.first.toHttpDateString()} : ${s.second}")
        Notify.create()
            .title("Finished")
            .text("Finished Getting Movies")
            .darkStyle()
            .hideAfter(3000)
            .show()
    }*/

    GlobalScope.launch {
        //val cssGridLayout = "https://grid.layoutit.com/"
        //createEverything(db, ShowApi.getSources(Source.LIVE_ACTION).take(11))
        //createEverything(db, ShowApi.getSources(Source.ANIME, Source.DUBBED, Source.CARTOON, Source.CARTOON_MOVIES, Source.LIVE_ACTION))
        //createEverything(db, ShowApi.getSources(Source.CARTOON_MOVIES, Source.LIVE_ACTION_MOVIES))
        Notify.create()
            .title("Finished")
            .text("Finished getting sources")
            .darkStyle()
            .hideAfter(3000)
            .show()
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
        prettyLog("Starting")
    }
    environment.monitor.subscribe(ApplicationStarted) {
        prettyLog("Started")
        musicHighScoreSetup(highScoreFile)
    }
    environment.monitor.subscribe(ApplicationStopPreparing) {
        prettyLog("Stop Preparing")
        musicHighScoreSave(highScoreFile)
    }
    environment.monitor.subscribe(ApplicationStopping) {
        prettyLog("Stopping")
    }
    environment.monitor.subscribe(ApplicationStopped) {
        prettyLog("Stopped")
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

private fun Application.routing(dbApi: ShowDBApi, simpleJwt: SimpleJWT) {
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

        firebaseRoute()
        chatRoute()
        api(dbApi)
        musicGameApi()

        route("/nsi/{name}") {
            get {
                prettyLog(call.request.origin.remoteHost)
                val name = call.parameters["name"]!!

                val episode: EpisodeApiInfo? = dbApi.getEpisodeInfo(name)

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
                            updateShows(dbApi)
                        }
                    }
                    "get" -> {
                        getOrUpdateShows {
                            createEverything(
                                dbApi.db,
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
                    call.respond(FreeMarkerContent("table.ftl", mapOf("data" to dbApi.getAlphabet(checkLevel))))
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
                transaction(dbApi.db) {
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
