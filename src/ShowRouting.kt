package com.example

import com.example.quizlibrary.QuizInfo
import com.example.quizlibrary.addQuiz
import com.example.quizlibrary.quiz
import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import kotlinx.html.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.api(db: ShowDBApi) {
    route("/show/quiz") {
        get {
            addQuiz(
                QuizInfo(
                    "/show/quiz/show_type=' + \$('#quiz_choice').val() + '.json",
                    "Show Quiz",
                    "Pick a Source (Gogoanime, Putlocker, Animetoon)"
                )
            )
        }
        addShowQuiz(db.db)
    }
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
                            +"make sure all of the \"/\" are changed to \"<\" when submitting"
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
            val vla = VideoLinkApi(url.replace("<", "/")).getVideoLink()
            call.respond(mapOf("videoLink" to vla))
        }
        webApi(db)
        userApi(db)
    }
}

fun Route.webApi(db: ShowDBApi) {
    route("/web") {
        get("/all.json") {
            val list = db.getAll()
            call.respond(list)
        }
        get("/allEpisodes.json") {
            call.respond(getAllEpisodes(db.db))
        }
        get("/nameAll.json") {
            val list = db.getAll().map { it.name }.sortedBy { it }
            call.respond(list)
        }
        getRecentShowType()
        getShowType(db)
        randomShow(db)
    }
}

fun getAllEpisodes(db: Database): List<ChatServer.EpisodeApiInfo> {
    var list = listOf<ChatServer.EpisodeApiInfo>()
    transaction(db) {
        list = Episodes.selectAll()
            .map {
                ChatServer.EpisodeApiInfo(
                    it[Episodes.name],
                    it[Episodes.image],
                    it[Episodes.url],
                    it[Episodes.description]
                )
            }
            .sortedBy { it.name }.filter { !it.name.isBlank() }
    }
    return list
}

fun Route.userApi(db: ShowDBApi) {
    route("/user") {
        get("/all.json") {
            val list = db.getAll()
            call.respond(mapOf("shows" to list))
        }
        get("/fullInformation.json") {
            val list = db.getFullShowInfos()
            call.respond(mapOf("shows" to list))
        }
        get("/allEpisodes.json") {
            call.respond(mapOf("shows" to getAllEpisodes(db.db)))
        }
        get("/nsi/{name}.json") {
            val name = call.parameters["name"]!!
            val episode = db.getEpisodeInfo(name)
            call.respond(mapOf("EpisodeInfo" to episode))
        }
        getRecentShowType { mapOf("shows" to synchronized(it) { it.toList() }) }
        getShowType(db)
        randomShow(db)
    }
}

fun Route.getShowType(db: ShowDBApi) {
    get("/t{type}.json") {
        val type = call.parameters["type"]!!
        val list = db.getType(type)
        call.respond(list)
    }
}

fun Route.getRecentShowType(showList: (List<ShowInfo>) -> Any = { it }) {
    get("/r{type}.json") {
        when (call.parameters["type"]!!) {
            "c" -> Source.RECENT_CARTOON
            "a" -> Source.RECENT_ANIME
            "l" -> Source.RECENT_LIVE_ACTION
            "all" -> Source.DUBBED
            else -> null
        }?.let {
            val s = if (it == Source.DUBBED) ShowApi.getAllRecent() else ShowApi(it).showInfoList
            call.respond(showList(s))
        }
    }
}

fun Route.randomShow(db: ShowDBApi) {
    get("/random.json") {
        call.respond(db.randomShow())
    }
}

data class EpListInfo(val name: String, val url: String)
data class EpisodeApiInfo(
    val name: String = "",
    val image: String = "",
    val url: String = "",
    val description: String = "",
    val episodeList: List<EpListInfo> = emptyList(),
    val genres: List<String> = emptyList()
)

fun Route.addShowQuiz(db: Database) {
    get("/show_type={type}.json") {
        val type = call.parameters["type"]!!
        var list = mutableListOf<EpisodeApiInfo>()
        transaction(db) {
            list = Episode.find { Episodes.url like "%$type%" }
                .map { EpisodeApiInfo(it.name, it.image, it.url, it.description) }.shuffled().take(100).toMutableList()
        }
        quiz(list, question = {
            it.description
        }, answers = {
            it.name
        })
    }
}