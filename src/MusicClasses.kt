package com.example

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.io.File

private fun makeMusicApiRequest(s: String): String? {
    val url = "https://api.musixmatch.com/ws/1.1/$s&apikey=67053f507ef88fc99c544f4d7052dfa8"
    return makeAPIRequest(url)
}

data class TrackInfo(val name: String, val id: Number)
data class QuizQuestions(val question: String, val choices: List<String>, val correctAnswer: String)

data class MusicUserInfo(val name: String, val artist: String, val score: String)

fun Application.musicHighScoreSetup(highScoreFile: File) {
    if (highScoreFile.exists()) {
        val list = Gson().fromJson<MutableList<MusicUserInfo>>(
            highScoreFile.readText(),
            object : TypeToken<MutableList<MusicUserInfo>>() {}.type
        )
        highScores.addAll(list)
    }
}

fun Application.musicHighScoreSave(highScoreFile: File) {
    if (!highScoreFile.exists()) {
        highScoreFile.createNewFile()
    }
    highScoreFile.writeText(highScores.toPrettyJson())
}

val highScores = mutableListOf<MusicUserInfo>()

fun Routing.musicGameApi() {
    route("/music") {
        get("/music_get_quiz_from={artist}.json") {
            val s = "track.search?q_artist=${call.parameters["artist"]!!}&page_size=100&page=1&f_has_lyrics=1"
            val j = makeMusicApiRequest(s)
            try {
                val q = Gson().fromJson<MusicBase>(j, MusicBase::class.java)
                val list = q.message?.body?.track_list?.map {
                    TrackInfo(
                        it.track!!.track_name!!,
                        it.track.track_id!!
                    )
                }!!.shuffled().toMutableList()
                val qList = mutableListOf<QuizQuestions>()
                while (list.size > 4) {
                    val track = list.randomRemove()
                    val snip = "track.snippet.get?track_id=${track.id}"
                    val snipId = makeMusicApiRequest(snip)
                    try {
                        val snippetQuiz = Gson().fromJson<SnippetBase>(snipId, SnippetBase::class.java)
                        val text = snippetQuiz.message?.body?.snippet?.snippet_body
                        val quizQuestions = QuizQuestions(
                            text!!,
                            listOf(
                                track.name,
                                list.randomRemove().name,
                                list.randomRemove().name,
                                list.randomRemove().name
                            ),
                            track.name
                        )
                        qList += quizQuestions
                    } catch (e: Exception) {
                        continue
                    }
                }
                call.respond(qList)
            } catch (e: Exception) {
                prettyLog(j)
                call.respond(j.toString())
            }
        }
        get("/mobileHighScores.json") {
            var text = ""
            val hs = highScores.groupBy { it.artist }
            hs.keys.forEach { info ->
                text+=info + "\n"
                hs[info]?.forEach {
                    text+="\t${it.name} | ${it.score}\n"
                }
            }
            call.respond(text)
        }
        get("/highScores.json") {
            val hs = highScores.groupBy { it.artist }
            val html = createHTML(true, xhtmlCompatible = true)
                .table {
                    id = "highList"
                    classes = classes + "darkTable"
                    hs.keys.forEach { info ->
                        tr {
                            td {
                                unsafe {
                                    +info
                                }
                            }
                            hs[info]?.forEach {
                                td {
                                    unsafe {
                                        +"${it.name} | ${it.score}"
                                    }
                                }
                            }
                        }
                    }
                }
            call.respond(html)
        }
        post("/") {
            val info = call.receive<MusicUserInfo>()
            prettyLog(info)
            highScores+=info
            call.respond(mapOf("submitted" to true))
        }
        static {
            // This marks index.html from the 'web' folder in resources as the default file to serve.
            defaultResource("musicgame.html", "web")
            // This serves files from the 'web' folder in the application resources.
            resources("web")
        }
    }
}

data class MusicBase(val message: Message?)

data class Body(val track_list: List<Track_list615124581>?)

data class Header(val status_code: Number?, val execute_time: Number?, val available: Number?)

data class Message(val header: Header?, val body: Body?)

data class Music_genre(
    val music_genre_id: Number?,
    val music_genre_parent_id: Number?,
    val music_genre_name: String?,
    val music_genre_name_extended: String?,
    val music_genre_vanity: String?
)

data class Music_genre_list1397122927(val music_genre: Music_genre?)

data class Music_genre_list1457697838(val music_genre: Music_genre?)

data class Music_genre_list1777044154(val music_genre: Music_genre?)

data class Music_genre_list180020636(val music_genre: Music_genre?)

data class Music_genre_list1865556572(val music_genre: Music_genre?)

data class Music_genre_list1917713930(val music_genre: Music_genre?)

data class Music_genre_list201831901(val music_genre: Music_genre?)

data class Music_genre_list673761987(val music_genre: Music_genre?)

data class Primary_genres(val music_genre_list: List<Music_genre_list1457697838>?)

data class Track(
    val track_id: Number?,
    val track_name: String?,
    val track_name_translation_list: List<Any>?,
    val track_rating: Number?,
    val commontrack_id: Number?,
    val instrumental: Number?,
    val explicit: Number?,
    val has_lyrics: Number?,
    val has_subtitles: Number?,
    val has_richsync: Number?,
    val num_favourite: Number?,
    val album_id: Number?,
    val album_name: String?,
    val artist_id: Number?,
    val artist_name: String?,
    val track_share_url: String?,
    val track_edit_url: String?,
    val restricted: Number?,
    val updated_time: String?,
    val primary_genres: Primary_genres?
)

data class Track_list615124581(val track: Track?)

data class SnippetBase(val message: SnippetMessage?)

data class SnippetBody(val snippet: MusicSnippet?)

data class SnippetHeader(val status_code: Number?, val execute_time: Number?)

data class SnippetMessage(val header: SnippetHeader?, val body: SnippetBody?)

data class MusicSnippet(
    val snippet_id: Number?,
    val snippet_language: String?,
    val restricted: Number?,
    val instrumental: Number?,
    val snippet_body: String?,
    val script_tracking_url: String?,
    val pixel_tracking_url: String?,
    val html_tracking_url: String?,
    val updated_time: String?
)