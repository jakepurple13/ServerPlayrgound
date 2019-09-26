package com.example

import com.google.gson.Gson
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
import kotlin.random.Random

private fun makeMusicApiRequest(s: String): String? {
    val url = "https://api.musixmatch.com/ws/1.1/$s&apikey=67053f507ef88fc99c544f4d7052dfa8"
    return makeAPIRequest(url)
}

data class TrackInfo(val name: String, val id: Number)
data class QuizQuestions(val question: String, val choices: Array<String>, val correctAnswer: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuizQuestions

        if (question != other.question) return false
        if (!choices.contentEquals(other.choices)) return false
        if (correctAnswer != other.correctAnswer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = question.hashCode()
        result = 31 * result + choices.contentHashCode()
        result = 31 * result + correctAnswer.hashCode()
        return result
    }
}

data class MusicUserInfo(val name: String, val artist: String, val score: String)

fun <T> MutableList<T>.randomRemove(): T {
    return removeAt(Random.nextInt(0, size))
}

fun Routing.musicGameApi() {
    val highScores = mutableMapOf<String, MutableList<MusicUserInfo>>()//.apply { withDefault { mutableListOf() } }

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
                            arrayOf(
                                track.name,
                                list.randomRemove().name,
                                list.randomRemove().name,
                                list.randomRemove().name
                            ),
                            track.name
                        )
                        qList += quizQuestions
                    } catch (e: Exception) {

                    }
                }
                call.respond(qList)
            } catch (e: Exception) {
                prettyLog(j)
                call.respond(j.toString())
            }
        }
        get("/highScores.json") {
            call.respond(highScores)
            /*val html = createHTML(true, xhtmlCompatible = true)
                .dl {
                    id = "highList"
                    highScores.keys.forEach { info ->
                        dt {
                            unsafe {
                                +info
                            }
                        }
                        highScores[info]!!.forEach {
                            dd {
                                unsafe {
                                    +"${it.name} | ${it.score}"
                                }
                            }
                        }
                    }
                }
            call.respond(html)*/
        }
        post("/") {
            val info = call.receive<MusicUserInfo>()
            prettyLog(info)
            if (!highScores.containsKey(info.artist)) {
                highScores[info.artist] = mutableListOf()
            }
            highScores[info.artist]!!.add(info)
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