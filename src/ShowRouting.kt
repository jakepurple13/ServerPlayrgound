package com.example

import io.ktor.application.call
import io.ktor.html.respondHtml
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import kotlinx.html.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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
            call.respond(mapOf("videoLink" to vla))
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
        get("/allEpisodes.json") {
            call.respond(getAllEpisodes(db))
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

fun Route.userApi(db: Database) {
    route("/user") {
        get("/all.json") {
            var list = listOf<ShowInfo>()
            transaction(db) {
                list = Shows.selectAll().map { ShowInfo(it[Shows.name], it[Shows.url]) }.sortedBy { it.name }
                //list = Show.all().sortedBy { it.name }.map { ShowInfo(it.name, it.url) }
            }
            call.respond(mapOf("shows" to list))
        }
        get("/allEpisodes.json") {
            call.respond(mapOf("shows" to getAllEpisodes(db)))
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
                call.respond(mapOf("shows" to synchronized(s) { s.toList() }))
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
