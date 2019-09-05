@file:Suppress("RegExpRedundantEscape")

package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.Gson
import freemarker.cache.ClassTemplateLoader
import io.ktor.application.Application
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
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.*
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private const val dbPath =
    "/Users/jrein/Downloads/kotlin-examples-master/tutorials/mpp-iOS-Android/servertesting/resources/database/takeeight.db"

object DbSettings {
    val db by lazy {
        //Database.connect("jdbc:h2:mem:regular;DB_CLOSE_DELAY=-1;", "org.h2.Driver")
        //Database.connect("jdbc:sqlite:/Users/jrein/Downloads/kotlin-examples-master/tutorials/mpp-iOS-Android/servertesting/resources/database/data.db", "org.sqlite.JDBC")
        /*Database.connect(
            "jdbc:h2:~/resources/database/seert.db",
            "org.h2.Driver"
        )*/

        Database.connect(
            "jdbc:h2:$dbPath",
            "org.h2.Driver"
        )
    }
}

fun Application.module() {
    System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
    val db = DbSettings.db

    GlobalScope.launch {
        //getAllShows(db)
        //getAllShowsAndEpisodes(db)
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
        route("/rc") {
            get {
                val s = ShowApi(Source.RECENT_CARTOON).showInfoList
                call.respond(mapOf("shows" to synchronized(s) { s.toList() }))
            }
        }
        route("/ra") {
            get {
                val s = ShowApi(Source.RECENT_ANIME).showInfoList
                call.respond(mapOf("shows" to synchronized(s) { s.toList() }))
            }
        }
        route("/ls") {
            get {
                val s = ShowApi(Source.LIVE_ACTION).showInfoList.toList()
                val filtered = if (call.parameters.contains("name")) s.filter {
                    it.name.contains(
                        call.parameters["name"]!!,
                        true
                    )
                } else s

                call.respond(mapOf("shows" to synchronized(filtered) { filtered }))
            }
        }
        route("/cs") {
            get {
                val s = ShowApi(Source.CARTOON).showInfoList.toList()
                val filtered = if (call.parameters.contains("name")) s.filter {
                    it.name.contains(
                        call.parameters["name"]!!,
                        true
                    )
                } else s
                call.respond(mapOf("shows" to synchronized(s) { filtered }))
            }
        }
        route("/as") {
            get {
                val s = ShowApi(Source.ANIME).showInfoList.toList()
                val filtered = if (call.parameters.contains("name")) s.filter {
                    it.name.contains(
                        call.parameters["name"]!!,
                        true
                    )
                } else s
                call.respond(mapOf("shows" to synchronized(s) { filtered }))
            }
        }
        route("/all") {
            get {
                /*val a = ShowApi(Source.ANIME).showInfoList.toList()
                val c = ShowApi(Source.CARTOON).showInfoList.toList()
                val cm = ShowApi(Source.CARTOON_MOVIES).showInfoList.toList()
                val d = ShowApi(Source.DUBBED).showInfoList.toList()
                val l = ShowApi(Source.LIVE_ACTION).showInfoList.toList()
                val list = a + c + cm + d + l
                val sorted = list.sortedBy { it.name }
                call.respond(mapOf("shows" to synchronized(sorted) { sorted }))*/
                //getAllShowsAndEpisodes(db)
                val list = arrayListOf<ShowInfo>()
                transaction(db) {
                    for (i in Show.all()) {
                        list.add(ShowInfo(i.name, i.url))
                    }
                }
                call.respond(mapOf("shows" to synchronized(list) { list }))
            }
        }
        route("/si") {
            get {
                val type = call.parameters["type"]!!
                val url = call.parameters["url"]!!
                val fullUrl = when {
                    type.contains("putlocker", true) -> "https://www1.putlocker.fyi/show/$url/"
                    type.contains("anime", true) -> "https://www.gogoanime1.com/watch/$url"
                    type.contains("cartoon", true) -> "http://www.animetoon.org/watch-$url"
                    else -> ""
                }
                val episode = EpisodeApi(ShowInfo(url, fullUrl))
                call.respond(mapOf("EpisodeInfo" to episode))
            }
        }
        route("/nsi/{name}") {
            get {
                val name = call.parameters["name"]!!

                data class EpListInfo(val name: String, val url: String)
                data class EpisodeApiInfo(
                    val name: String = "",
                    val image: String = "",
                    val url: String = "",
                    val description: String = "",
                    val episodeList: List<EpListInfo> = emptyList()
                )

                var episode = EpisodeApiInfo()

                val source = when (name[0]) {
                    'p' -> "putlocker"
                    'g' -> "gogoanime"
                    'a' -> "animetoon"
                    else -> ""
                }

                transaction(db) {
                    val e = Episode.all().toList().filter {
                        val list = it.url.split("/")
                        val its = if (list.last().isBlank()) {
                            list[list.size - 2]
                        } else {
                            list.last()
                        }
                        its.equals(name.substring(1), true) && it.url.contains(source, true)
                    }
                    for (i in e) {
                        val l = EpisodeList.all().toList().filter { it.episode.url == i.url }
                        val list = arrayListOf<EpListInfo>()
                        for (j in l) {
                            list += EpListInfo(j.name, j.url)
                        }
                        episode = EpisodeApiInfo(
                            i.name,
                            i.image,
                            i.url,
                            i.description,
                            list
                        )
                        break
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
        route("/api/nsi/{name}") {
            get {
                val name = call.parameters["name"]!!

                data class EpListInfo(val name: String, val url: String)
                data class EpisodeApiInfo(
                    val name: String,
                    val image: String,
                    val url: String,
                    val description: String,
                    val episodeList: List<EpListInfo>
                )

                val episode: ArrayList<EpisodeApiInfo> = arrayListOf()

                transaction(db) {
                    val e = Episode.all().toList().filter { it.name.equals(name.replace("-", " "), true) }
                    for (i in e) {
                        val l = EpisodeList.all().toList().filter { it.episode.url == i.url }
                        val list = arrayListOf<EpListInfo>()
                        for (j in l) {
                            list += EpListInfo(j.name, j.url)
                        }
                        episode += EpisodeApiInfo(
                            i.name,
                            i.image,
                            i.url,
                            i.description,
                            list
                        )
                    }
                }
                call.respond(mapOf("EpisodeInfo" to episode))
            }
        }
        route("/") {
            get("/shows") {
                call.respond(FreeMarkerContent("index.ftl", mapOf("data" to listOf<ShowInfo>())))
            }
            post("/shows") {
                val post = call.receiveParameters()
                val type = post["show_type"]!!
                val url = post["show_name"]!!

                var list = listOf<Show>()
                transaction(db) {
                    debug = false
                    val s = Shows.select { Shows.name like "%$url%" }.toList()
                    prettyLog(s.joinToString { "$it\n" })
                    list = Show.all().toList()
                }

                val source = when {
                    type.contains("ls") -> "putlocker"
                    type.contains("as") -> "gogoanime"
                    type.contains("cs") -> "animetoon"
                    else -> ""
                }

                val filtered = if (url.isEmpty()) list else list.filter { it.name.contains(url) }
                val filtered2 = if (source.isNotEmpty()) filtered.filter { it.url.contains(source) } else filtered

                call.respond(FreeMarkerContent("index.ftl", mapOf("data" to filtered2.toList()), ""))
            }
            get {
                var list = listOf<Show>()
                transaction(db) {
                    list = Show.all().toList()
                }
                call.respond(
                    FreeMarkerContent(
                        "table.ftl",
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

fun getAllShows(db: Database) {
    transaction(db) {
        SchemaUtils.create(Shows)

        val list = ShowApi.getAll().sortedBy { it.name }

        for (i in list) {
            Show.new {
                name = i.name
                url = i.url
            }
        }
    }
}

fun getAllShowsAndEpisodes(db: Database) = GlobalScope.launch {

    transaction(db) {

        SchemaUtils.create(Shows, Episodes, EpisodeLists)

        val list = ShowApi.getAll().sortedBy { it.name }

        for ((j, i) in list.withIndex()) {
            val s = Shows.insert {
                it[name] = i.name
                it[url] = i.url
            } get Shows.id
            try {
                val episodeApi = EpisodeApi(i, 30000)
                val e = Episode.newEpisodes(s, episodeApi)
                val epl = episodeApi.episodeList
                for (li in epl) {
                    /*val el = EpisodeList.new {
                        name = li.name
                        url = li.url
                        episode = e
                    }*/
                    EpisodeLists.insert {
                        it[name] = li.name
                        it[url] = li.url
                        it[episode] = e
                    }
                    //println(el)
                }
                //println("${s.name} and ${e.name}")
            } catch (e: Exception) {
                continue
            }

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

object Shows : IntIdTable() {
    val name = varchar("show_name", 10000)
    val url = varchar("show_url", 10000).primaryKey()
}

class Show(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Show>(Shows)

    var name by Shows.name
    var url by Shows.url

    override fun toString(): String {
        return "$name: $url"
    }
}

object Episodes : IntIdTable() {
    val url = varchar("url", 10000)
    val name = varchar("name", 10000)
    val image = varchar("image_url", 10000)
    val description = varchar("description", 10000)
    val show = reference("show", Shows)
}

class Episode(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Episode>(Episodes) {
        fun newEpisode(s: Show, episodeApi: EpisodeApi) = new {
            name = episodeApi.name
            description = episodeApi.description
            image = episodeApi.image
            url = episodeApi.source.url
            show = s
        }

        fun newEpisodes(s: EntityID<Int>, episodeApi: EpisodeApi) = Episodes.insert {
            it[name] = episodeApi.name
            it[description] = episodeApi.description
            it[image] = episodeApi.image
            it[url] = episodeApi.source.url
            it[show] = s
        } get Episodes.id
    }

    var url by Episodes.url
    var name by Episodes.name
    var image by Episodes.image
    var description by Episodes.description
    var show by Show referencedOn Episodes.show

    override fun toString(): String {
        return "$name: $url | $description | $image | $show"
    }
}

object EpisodeLists : IntIdTable() {
    val name = varchar("name", 10000)
    val url = varchar("url", 10000).primaryKey()
    val episode = reference("episode", Episodes)
}

class EpisodeList(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EpisodeList>(EpisodeLists)

    var name by EpisodeLists.name
    var url by EpisodeLists.url
    var episode by Episode referencedOn EpisodeLists.episode

    override fun toString(): String {
        return "$name: $url | ${episode.name}"
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

enum class Source(val link: String, val recent: Boolean = false, var movie: Boolean = false) {
    //ANIME("http://www.animeplus.tv/anime-list"),
    ANIME("https://www.gogoanime1.com/home/anime-list"),
    CARTOON("http://www.animetoon.org/cartoon"),
    DUBBED("http://www.animetoon.org/dubbed-anime"),
    //ANIME_MOVIES("http://www.animeplus.tv/anime-movies"),
    ANIME_MOVIES("https://www.gogoanime1.com/home/anime-list", movie = true),
    CARTOON_MOVIES("http://www.animetoon.org/movies", movie = true),
    //RECENT_ANIME("http://www.animeplus.tv/anime-updates", true),
    RECENT_ANIME("https://www.gogoanime1.com/home/latest-episodes", true),
    RECENT_CARTOON("http://www.animetoon.org/updates", true),
    LIVE_ACTION("https://www.putlocker.fyi/a-z-shows/");

    companion object SourceUrl {
        fun getSourceFromUrl(url: String): Source {
            return when (url) {
                ANIME.link -> ANIME
                CARTOON.link -> CARTOON
                DUBBED.link -> DUBBED
                ANIME_MOVIES.link -> ANIME_MOVIES
                CARTOON_MOVIES.link -> CARTOON_MOVIES
                RECENT_ANIME.link -> RECENT_ANIME
                RECENT_CARTOON.link -> RECENT_CARTOON
                LIVE_ACTION.link -> LIVE_ACTION
                else -> ANIME
            }
        }
    }
}

/**
 * Info about the show, name and url
 */
open class ShowInfo(val name: String, val url: String) {
    override fun toString(): String {
        return "$name: $url"
    }
}

/**
 * The actual api!
 */
class ShowApi(private val source: Source) {

    companion object {
        fun getAll(): List<ShowInfo> {
            val a = ShowApi(Source.ANIME).showInfoList.toList()
            val c = ShowApi(Source.CARTOON).showInfoList.toList()
            val cm = ShowApi(Source.CARTOON_MOVIES).showInfoList.toList()
            val d = ShowApi(Source.DUBBED).showInfoList.toList()
            val l = ShowApi(Source.LIVE_ACTION).showInfoList.toList()
            return a + c + cm + d + l
        }
    }

    private var doc: Document = Jsoup.connect(source.link).get()

    /**
     * returns a list of the show's from the wanted source
     */
    val showInfoList: List<ShowInfo>
        get() {
            return if (source.recent)
                getRecentList()
            else
                getList()
        }

    private fun getList(): ArrayList<ShowInfo> {
        return if (source.link.contains("gogoanime")) {
            if (source == Source.ANIME_MOVIES || source.movie)
                gogoAnimeMovies()
            else
                gogoAnimeAll()
        } else if (source.link.contains("putlocker")) {
            val d = doc.select("a.az_ls_ent")
            val listOfShows = arrayListOf<ShowInfo>()
            for (i in d) {
                listOfShows += ShowInfo(i.text(), i.attr("abs:href"))
            }
            listOfShows
        } else {
            val lists = doc.allElements
            val listOfStuff = lists.select("td").select("a[href^=http]")
            val listOfShows = arrayListOf<ShowInfo>()
            for (element in listOfStuff) {
                listOfShows.add(
                    ShowInfo(
                        element.text(),
                        element.attr("abs:href")
                    )
                )
            }
            listOfShows.sortBy { it.name }
            listOfShows
        }
    }

    private fun gogoAnimeAll(): ArrayList<ShowInfo> {
        val listOfShows = arrayListOf<ShowInfo>()
        val lists = doc.allElements
        val listOfStuff = lists.select("ul.arrow-list").select("li")
        for (element in listOfStuff) {
            listOfShows.add(
                ShowInfo(
                    element.text(),
                    element.select("a[href^=http]").attr("abs:href")
                )
            )
        }
        listOfShows.sortBy { it.name }
        return listOfShows
    }

    private fun gogoAnimeMovies(): ArrayList<ShowInfo> {
        val list = gogoAnimeAll().filter {
            it.name.contains(
                "movie",
                ignoreCase = true
            )
        } as ArrayList<ShowInfo>
        list.sortBy { it.name }
        return list
    }

    private fun getRecentList(): ArrayList<ShowInfo> {
        return if (source.link.contains("gogoanime")) {
            gogoAnimeRecent()
        } else {
            var listOfStuff = doc.allElements.select("div.left_col").select("table#updates")
                .select("a[href^=http]")
            if (listOfStuff.size == 0) {
                listOfStuff = doc.allElements.select("div.s_left_col").select("table#updates")
                    .select("a[href^=http]")
            }
            val listOfShows = arrayListOf<ShowInfo>()
            for (element in listOfStuff) {
                val showInfo =
                    ShowInfo(element.text(), element.attr("abs:href"))
                if (!element.text().contains("Episode"))
                    listOfShows.add(showInfo)
            }
            listOfShows
        }
    }

    private fun gogoAnimeRecent(): ArrayList<ShowInfo> {
        val listOfStuff =
            doc.allElements.select("div.dl-item")
        val listOfShows = arrayListOf<ShowInfo>()
        for (element in listOfStuff) {
            val tempUrl = element.select("div.name").select("a[href^=http]").attr("abs:href")
            val showInfo = ShowInfo(
                element.select("div.name").text(),
                tempUrl.substring(0, tempUrl.indexOf("/episode"))
            )
            listOfShows.add(showInfo)
        }
        return listOfShows
    }

}

/**
 * If you want to get the Show with all the information now rather than passing it into [EpisodeApi] yourself
 */
fun List<ShowInfo>.getEpisodeApi(index: Int): EpisodeApi = EpisodeApi(this[index])

/**
 * Actual Show information
 */
class EpisodeApi(val source: ShowInfo, timeOut: Int = 10000) {
    private var doc: Document = Jsoup.connect(source.url).timeout(timeOut).get()

    /**
     * The name of the Show
     */
    val name: String
        get() {
            return when {
                source.url.contains("putlocker") -> doc.select("li.breadcrumb-item").last().text()
                source.url.contains("gogoanime") -> doc.select("div.anime-title").text()
                else -> doc.select("div.right_col h1").text()
            }
        }

    /**
     * The url of the image
     */
    val image: String
        get() {
            return when {
                source.url.contains("putlocker") -> doc.select("div.thumb").select("img[src^=http]").attr("abs:src")
                source.url.contains("gogoanime") -> doc.select("div.animeDetail-image").select("img[src^=http]").attr("abs:src")
                else -> doc.select("div.left_col").select("img[src^=http]#series_image").attr("abs:src")
            }

        }

    /**
     * the description
     */
    val description: String
        get() {
            when {
                source.url.contains("putlocker") -> try {
                    throw Exception()
                    /*val client = OkHttpClient()
                    val request = okhttp3.Request.Builder()
                        .url("http://www.omdbapi.com/?t=$name&plot=full&apikey=e91b86ee")
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    val resString = response.body!!.string()
                    val jsonObj = JsonParser().parse(resString).asJsonObject
                    val year = jsonObj.get("Year")
                    val released = jsonObj.get("Released")
                    val plot = jsonObj.get("Plot")
                    return "Years Active: $year\nReleased: $released\n$plot"*/
                } catch (e: Exception) {
                    var textToReturn = ""
                    val des = doc.select(".mov-desc")
                    val para = des.select("p")
                    for (i in para.withIndex()) {
                        val text = when (i.index) {
                            1 -> "Release: "
                            2 -> "Genre: "
                            3 -> "Director: "
                            4 -> "Stars: "
                            5 -> "Synopsis: "
                            else -> ""
                        } + i.value.text()
                        textToReturn += text + "\n"
                    }
                    return textToReturn.trim()
                }
                source.url.contains("gogoanime") -> {
                    val des = doc.select("p.anime-details").text()
                    return if (des.isNullOrBlank() || des.length > 10000) "Sorry, an error has occurred" else des
                }
                else -> {
                    val des =
                        if (doc.allElements.select("div#series_details").select("span#full_notes").hasText())
                            doc.allElements.select("div#series_details").select("span#full_notes").text().removeSuffix(
                                "less"
                            )
                        else {
                            val d = doc.allElements.select("div#series_details")
                                .select("div:contains(Description:)").select("div").text()
                            try {
                                d.substring(d.indexOf("Description: ") + 13, d.indexOf("Category: "))
                            } catch (e: StringIndexOutOfBoundsException) {
                                d
                            }
                        }
                    return if (des.isNullOrBlank()) "Sorry, an error has occurred" else des
                }
            }
        }

    /**
     * The episode list
     */
    val episodeList: List<EpisodeInfo>
        get() {
            var listOfShows = arrayListOf<EpisodeInfo>()
            when {
                source.url.contains("putlocker") -> {
                    val rowList = doc.select("div.col-lg-12").select("div.row")
                    val episodes = rowList.select("a.btn-episode")
                    for (i in episodes) {
                        val ep = EpisodeInfo(
                            i.attr("title"),
                            "https://www.putlocker.fyi/embed-src/${i.attr("data-pid")}"
                        )//i.attr("abs:href"))
                        listOfShows.add(ep)
                    }
                }
                source.url.contains("gogoanime") -> {
                    val stuffList = doc.select("ul.check-list").select("li")
                    val showList = arrayListOf<EpisodeInfo>()
                    for (i in stuffList) {
                        val urlInfo = i.select("a[href^=http]")
                        val epName = if (urlInfo.text().contains(name)) {
                            urlInfo.text().substring(name.length)
                        } else {
                            urlInfo.text()
                        }.trim()
                        showList.add(EpisodeInfo(epName, urlInfo.attr("abs:href")))
                    }
                    listOfShows = showList.distinctBy { it.name } as ArrayList<EpisodeInfo>
                }
                else -> {
                    fun getStuff(url: String) {
                        val doc1 = Jsoup.connect(url).get()
                        val stuffList = doc1.allElements.select("div#videos").select("a[href^=http]")
                        for (i in stuffList) {
                            listOfShows.add(
                                EpisodeInfo(
                                    i.text(),
                                    i.attr("abs:href")
                                )
                            )
                        }
                    }
                    getStuff(source.url)
                    val stuffLists =
                        doc.allElements.select("ul.pagination").select(" button[href^=http]")
                    for (i in stuffLists) {
                        getStuff(i.attr("abs:href"))
                    }
                }
            }
            return listOfShows
        }

    override fun toString(): String {
        return "$name - ${episodeList.size} eps - $description"
    }

}

/**
 * Actual Episode info, name and url
 */
class EpisodeInfo(name: String, url: String) : ShowInfo(name, url) {

    /**
     * returns a url link to the episodes video
     * # Use for anything but movies
     */
    /*fun getVideoLink(): String {
        if (url.contains("putlocker")) {
            val firstHtml = getHtml(url)
            if(firstHtml!=null) {
                val d = "<iframe[^>]+src=\"([^\"]+)\"[^>]*><\\/iframe>".toRegex().toPattern().matcher(firstHtml)
                if (d.find()) {
                    val secondHtml = getHtml(d.group(1)!!)
                    if(secondHtml!=null) {
                        val a = "<p[^>]+id=\"videolink\">([^>]*)<\\/p>".toRegex().toPattern().matcher(secondHtml)
                        if (a.find()) {
                            return "https://verystream.com/gettoken/${a.group(1)!!}?mime=true"
                        }
                    } else {
                        return "Unable to get"
                    }
                }
            } else {
                return "Unable to get"
            }
        } else if (url.contains("gogoanime")) {
            val doc = Jsoup.connect(url).get()
            return doc.select("a[download^=http]").attr("abs:download")
        } else {
            val episodeHtml = getHtml(url)
            if(episodeHtml!=null) {
                val matcher = "<iframe src=\"([^\"]+)\"[^<]+<\\/iframe>".toRegex().toPattern().matcher(episodeHtml)
                val list = arrayListOf<String>()
                while (matcher.find()) {
                    list.add(matcher.group(1)!!)
                }

                val videoHtml = getHtml(list[0])
                if(videoHtml!=null) {
                    val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern().matcher(videoHtml)
                    if (reg.find()) {
                        val d = reg.group(1)
                        val g = Gson()
                        val d1 = g.fromJson(d, NormalLink::class.java)

                        return d1.normal!!.storage!![0].link!!
                    }
                } else {
                    return "Unable to get"
                }
            }
            return "Unable to get"
        }
        return ""
    }*/

    /**
     * returns a url link to the episodes video
     * # Use for movies
     *//*
    fun getVideoLinks(): List<String> {
        if (url.contains("putlocker")) {
            val d = "<iframe[^>]+src=\"([^\"]+)\"[^>]*><\\/iframe>".toRegex().toPattern().matcher(getHtml(url))
            if (d.find()) {
                val a = "<p[^>]+id=\"videolink\">([^>]*)<\\/p>".toRegex().toPattern().matcher(getHtml(d.group(1)!!))
                if (a.find()) {
                    //return arrayListOf("https://verystream.com/gettoken/${a.group(1)!!}?mime=true")
                    val link =
                        getFinalURL(URL("https://verystream.com/gettoken/${a.group(1)!!}?mime=true"))!!.toExternalForm()
                    return arrayListOf(link)
                }
            }
            return arrayListOf("N/A")
        } else if (url.contains("gogoanime")) {
            val doc = Jsoup.connect(url).get()
            return arrayListOf(doc.select("a[download^=http]").attr("abs:download"))
        } else {
            val htmld = getHtml(url)
            val m = "<iframe src=\"([^\"]+)\"[^<]+<\\/iframe>".toRegex().toPattern().matcher(htmld)
            var s = ""
            val list = arrayListOf<String>()
            while (m.find()) {
                val g = m.group(1)!!
                s += g + "\n"
                list.add(g)
            }

            val regex =
                "(http|https):\\/\\/([\\w+?\\.\\w+])+([a-zA-Z0-9\\~\\%\\&\\-\\_\\?\\.\\=\\/])+(part[0-9])+.(\\w*)"

            val htmlc = if (regex.toRegex().toPattern().matcher(list[0]).find()) {
                list
            } else {
                getHtml(list[0])
            }

            when (htmlc) {
                is ArrayList<*> -> {
                    val urlList = arrayListOf<String>()
                    for (info in htmlc) {
                        val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern()
                            .matcher(getHtml(info.toString()))
                        while (reg.find()) {
                            val d = reg.group(1)
                            val g = Gson()
                            val d1 = g.fromJson(d, NormalLink::class.java)
                            urlList.add(d1.normal!!.storage!![0].link!!)

                        }
                    }
                    return urlList
                }
                is String -> {
                    val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern().matcher(htmlc)
                    while (reg.find()) {
                        val d = reg.group(1)
                        val g = Gson()
                        val d1 = g.fromJson(d, NormalLink::class.java)
                        return arrayListOf(d1.normal!!.storage!![0].link!!)
                    }
                }
            }
        }
        return arrayListOf()
    }

    */
    /**
     * returns video information
     * this includes link to video and filename
     * # You can use this for anything. This just returns some extra information.
     *//*
    fun getVideoInfo(): List<Storage> {
        if (url.contains("putlocker")) {
            val d = "<iframe[^>]+src=\"([^\"]+)\"[^>]*><\\/iframe>".toRegex().toPattern().matcher(getHtml(url))
            if (d.find()) {
                val a = "<p[^>]+id=\"videolink\">([^>]*)<\\/p>".toRegex().toPattern().matcher(getHtml(d.group(1)!!))
                if (a.find()) {
                    val stor = Storage()
                    stor.link = "https://verystream.com/gettoken/${a.group(1)!!}?mime=true"
                    stor.filename = name
                    stor.quality = "720"
                    stor.source = "PutLocker"
                    stor.sub = "No"
                    return arrayListOf(stor)
                }
            }
            return arrayListOf()
        } else if (url.contains("gogoanime")) {
            val doc = Jsoup.connect(url).get()
            val storage = Storage()
            storage.link = doc.select("a[download^=http]").attr("abs:download")
            val regex = "^[^\\[]+(.*mp4)".toRegex().toPattern().matcher(storage.link!!)
            storage.filename = if (regex.find()) {
                regex.group(1)!!
            } else {
                val segments = URI(url).path.split("/")
                "${segments[2]} $name.mp4"
            }
            storage.source = url
            storage.quality = "Good"
            storage.sub = "Yes"
            return arrayListOf(storage)
        } else {
            val htmld = getHtml(url)
            val m = "<iframe src=\"([^\"]+)\"[^<]+<\\/iframe>".toRegex().toPattern().matcher(htmld)
            var s = ""
            val list = arrayListOf<String>()
            while (m.find()) {
                val g = m.group(1)!!
                s += g + "\n"
                list.add(g)
            }

            val regex =
                "(http|https):\\/\\/([\\w+?\\.\\w+])+([a-zA-Z0-9\\~\\%\\&\\-\\_\\?\\.\\=\\/])+(part[0-9])+.(\\w*)"

            val htmlc = if (regex.toRegex().toPattern().matcher(list[0]).find()) {
                list
            } else {
                getHtml(list[0])
            }

            when (htmlc) {
                is ArrayList<*> -> {
                    val urlList = arrayListOf<Storage>()
                    for (info in htmlc) {
                        val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern()
                            .matcher(getHtml(info.toString()))
                        while (reg.find()) {
                            val d = reg.group(1)
                            val g = Gson()
                            val d1 = g.fromJson(d, NormalLink::class.java)
                            urlList.add(d1.normal!!.storage!![0])
                        }
                    }
                    return urlList
                }
                is String -> {
                    val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern().matcher(htmlc)
                    while (reg.find()) {
                        val d = reg.group(1)
                        val g = Gson()
                        val d1 = g.fromJson(d, NormalLink::class.java)
                        return arrayListOf(d1.normal!!.storage!![0])
                    }
                }
            }
        }
        return arrayListOf()
    }

    @Throws(IOException::class)
    private fun getHtml(url: String): String? {
        try {
            // Build and set timeout values for the request.
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"
            )
            connection.addRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.addRequestProperty("Referer", "http://thewebsite.com")
            connection.connect()

            // Read and store the result line by line then return the entire string.
            val in1 = connection.getInputStream()
            val reader = BufferedReader(InputStreamReader(in1))
            val html = StringBuilder()
            var line: String? = ""
            while (line != null) {
                line = reader.readLine()
                html.append(line)
            }
            in1.close()

            return html.toString()
        } catch(e: SocketTimeoutException) {
            return null
        }
    }

    private fun getFinalURL(url: URL): URL? {
        try {
            val con = url.openConnection() as HttpURLConnection
            con.instanceFollowRedirects = false
            con.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0"
            )
            con.addRequestProperty("Accept-Language", "en-US,en;q=0.5")
            con.addRequestProperty("Referer", "http://thewebsite.com")
            con.connect()
            //con.getInputStream();
            val resCode = con.responseCode
            if (resCode == HttpURLConnection.HTTP_SEE_OTHER
                || resCode == HttpURLConnection.HTTP_MOVED_PERM
                || resCode == HttpURLConnection.HTTP_MOVED_TEMP
            ) {
                var location = con.getHeaderField("Location")
                if (location.startsWith("/")) {
                    location = url.protocol + "://" + url.host + location
                }
                return getFinalURL(URL(location))
            }
        } catch (e: Exception) {
            println(e.message)
        }

        return url
    }*/

    override fun toString(): String {
        return "$name: $url"
    }
}

internal class NormalLink {
    var normal: Normal? = null

    override fun toString(): String {
        return "ClassPojo [normal = " + normal!!.toString() + "]"
    }
}

internal class Normal {
    var storage: Array<Storage>? = null

    override fun toString(): String {
        return "ClassPojo [storage = $storage]"
    }
}

class Storage {
    var sub: String? = null

    var source: String? = null

    var link: String? = null

    var quality: String? = null

    var filename: String? = null

    override fun toString(): String {
        return "ClassPojo [sub = $sub, source = $source, link = $link, quality = $quality, filename = $filename]"
    }
}