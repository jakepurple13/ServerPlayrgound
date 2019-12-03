@file:Suppress("RegExpRedundantEscape")

package com.example

import com.google.gson.Gson
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

enum class ShowType {
    MOVIE,
    SHOW
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
    LIVE_ACTION("https://www.putlocker.fyi/a-z-shows/"),
    RECENT_LIVE_ACTION("https://www1.putlocker.fyi/recent-episodes/", true),
    LIVE_ACTION_MOVIES("https://www1.putlocker.fyi/a-z-movies/", movie = true);

    companion object SourceUrl {
        fun getSourceFromUrl(url: String): Source? = when (url) {
            ANIME.link -> ANIME
            CARTOON.link -> CARTOON
            DUBBED.link -> DUBBED
            ANIME_MOVIES.link -> ANIME_MOVIES
            CARTOON_MOVIES.link -> CARTOON_MOVIES
            RECENT_ANIME.link -> RECENT_ANIME
            RECENT_CARTOON.link -> RECENT_CARTOON
            LIVE_ACTION.link -> LIVE_ACTION
            RECENT_LIVE_ACTION.link -> RECENT_LIVE_ACTION
            else -> null
        }
    }
}

internal enum class ShowSource {
    GOGOANIME, ANIMETOON, PUTLOCKER, NONE;

    companion object {
        fun getSourceType(url: String) = when {
            url.contains("gogoanime") -> GOGOANIME
            url.contains("animetoon") -> ANIMETOON
            url.contains("putlocker") -> PUTLOCKER
            else -> NONE
        }
    }
}

/**
 * Info about the show, name and url
 */
open class ShowInfo(val name: String, val url: String, internal var type: ShowType = ShowType.SHOW) {
    override fun toString(): String = "$name: $url"
}

/**
 * The actual api!
 */
class ShowApi(private val source: Source) {

    companion object {
        fun getAll() = getSources(Source.ANIME, Source.CARTOON, Source.CARTOON_MOVIES, Source.DUBBED, Source.LIVE_ACTION)
        fun getAllMovies(): List<ShowInfo> = getSources(Source.CARTOON_MOVIES, Source.ANIME_MOVIES, Source.LIVE_ACTION_MOVIES)
        fun getAllRecent(): List<ShowInfo> = getSources(Source.RECENT_ANIME, Source.RECENT_CARTOON, Source.RECENT_LIVE_ACTION)
        fun getSources(vararg source: Source): List<ShowInfo> = source.map { ShowApi(it).showInfoList }.flatten()
        fun getEverything(): List<ShowInfo> = getSources(
                Source.ANIME,
                Source.CARTOON,
                Source.CARTOON_MOVIES,
                Source.DUBBED,
                Source.LIVE_ACTION,
                Source.LIVE_ACTION_MOVIES
        )
    }

    private var doc: Document = Jsoup.connect(source.link).get()

    /**
     * returns a list of the show's from the wanted source
     */
    val showInfoList: List<ShowInfo>
        get() = if (source.recent) getRecentList() else getList()

    private fun getList(): List<ShowInfo> = when (ShowSource.getSourceType(source.link)) {
        ShowSource.GOGOANIME -> if (source == Source.ANIME_MOVIES || source.movie) gogoAnimeMovies() else gogoAnimeAll()
        ShowSource.PUTLOCKER -> if (source == Source.LIVE_ACTION_MOVIES || source.movie) putlockerMovies() else putlockerShows()
        ShowSource.ANIMETOON -> doc.allElements.select("td").select("a[href^=http]").map {
            ShowInfo(it.text(), it.attr("abs:href"), if (source.movie) ShowType.MOVIE else ShowType.SHOW)
        }
        ShowSource.NONE -> emptyList()
    }.sortedBy(ShowInfo::name)

    private fun putlockerShows() =
        doc.select("a.az_ls_ent").map { ShowInfo(it.text(), it.attr("abs:href"), ShowType.SHOW) }

    private fun putlockerMovies(): List<ShowInfo> {
        fun getMovieFromPage(document: Document) = document.allElements.select("div.col-6").map {
            ShowInfo(it.select("span.mov_title").text(), it.select("a.thumbnail").attr("abs:href"), ShowType.MOVIE)
        }
        return doc.allElements.select("ul.pagination-az").select("a.page-link").pmap { p ->
            println(p.attr("abs:href"))
            val page = Jsoup.connect(p.attr("abs:href")).get()
            val listPage = page.allElements.select("li.page-item")
            val lastPage = listPage[listPage.size - 2].text().toInt()
            (1..lastPage).pmap {
                if (it == 1) getMovieFromPage(page) else getMovieFromPage(
                        Jsoup.connect(
                                "${Source.LIVE_ACTION_MOVIES.link}page/$it/${p.attr("abs:href").split("/").last()}"
                        ).get()
                )
            }.flatten()
        }.flatten()
    }

    private fun gogoAnimeAll(): List<ShowInfo> = doc.allElements.select("ul.arrow-list").select("li").map {
        ShowInfo(
                it.text(),
                it.select("a[href^=http]").attr("abs:href"),
                if (source.movie) ShowType.MOVIE else ShowType.SHOW
        )
    }

    private fun gogoAnimeMovies(): List<ShowInfo> =
        gogoAnimeAll().filter { it.name.contains("movie", ignoreCase = true) }

    private fun getRecentList(): List<ShowInfo> = when (ShowSource.getSourceType(source.link)) {
        ShowSource.GOGOANIME -> doc.allElements.select("div.dl-item").map {
            val tempUrl = it.select("div.name").select("a[href^=http]").attr("abs:href")
            ShowInfo(it.select("div.name").text(), tempUrl.substring(0, tempUrl.indexOf("/episode")))
        }
        ShowSource.PUTLOCKER -> doc.allElements.select("div.col-6").map {
            val url = it.select("a.thumbnail").attr("abs:href")
            ShowInfo(it.select("span.mov_title").text(), url.substring(0, url.indexOf("season")))
        }
        ShowSource.ANIMETOON -> {
            var listOfStuff = doc.allElements.select("div.left_col").select("table#updates").select("a[href^=http]")
            if (listOfStuff.size == 0) listOfStuff =
                doc.allElements.select("div.s_left_col").select("table#updates").select("a[href^=http]")
            listOfStuff.map { ShowInfo(it.text(), it.attr("abs:href")) }.filter { !it.name.contains("Episode") }
        }
        ShowSource.NONE -> emptyList()
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
    val name: String = when (ShowSource.getSourceType(source.url)) {
        ShowSource.PUTLOCKER -> doc.select("li.breadcrumb-item").last().text()
        ShowSource.GOGOANIME -> doc.select("div.anime-title").text()
        ShowSource.ANIMETOON -> doc.select("div.right_col h1").text()
        ShowSource.NONE -> ""
    }

    /**
     * The url of the image
     */
    val image: String = when (ShowSource.getSourceType(source.url)) {
        ShowSource.PUTLOCKER -> doc.select("div.thumb").select("img[src^=http]")
        ShowSource.GOGOANIME -> doc.select("div.animeDetail-image").select("img[src^=http]")
        ShowSource.ANIMETOON -> doc.select("div.left_col").select("img[src^=http]#series_image")
        ShowSource.NONE -> null
    }?.attr("abs:src") ?: ""

    /**
     * the genres of the show
     */
    val genres: List<String> = when(ShowSource.getSourceType(source.url)) {
        ShowSource.PUTLOCKER -> doc.select(".mov-desc").select("p:contains(Genre)")
        ShowSource.GOGOANIME -> doc.select("div.animeDetail-item:contains(Genres)")
        ShowSource.ANIMETOON -> doc.select("span.red_box")
        ShowSource.NONE -> null
    }?.select("a[href^=http]")?.eachText() ?: emptyList()

    /**
     * the description
     */
    val description: String = when (ShowSource.getSourceType(source.url)) {
        ShowSource.PUTLOCKER -> try {
            val imdb = getAPIRequest<IMDB>("http://www.omdbapi.com/?t=$name&plot=full&apikey=e91b86ee")
            check(imdb != null && imdb.Year != null) { throw Exception("Cannot be null") }
            "Years Active: ${imdb.Year}\nReleased: ${imdb.Released}\n${imdb.Plot}"
        } catch (e: Exception) {
            var textToReturn = ""
            val para = doc.select(".mov-desc").select("p")
            for (i in para.withIndex()) {
                textToReturn += when (i.index) {
                    1 -> "Release: "
                    2 -> "Genre: "
                    3 -> "Director: "
                    4 -> "Stars: "
                    5 -> "Synopsis: "
                    else -> ""
                } + i.value.text() + "\n"
            }
            textToReturn
        }
        ShowSource.GOGOANIME -> doc.select("p.anime-details").text()
        ShowSource.ANIMETOON -> doc.allElements.select("div#series_details").let { element ->
            if (element.select("span#full_notes").hasText())
                element.select("span#full_notes").text().removeSuffix("less")
            else
                element.select("div:contains(Description:)").select("div").text().let {
                    try {
                        it.substring(it.indexOf("Description: ") + 13, it.indexOf("Category: "))
                    } catch (e: StringIndexOutOfBoundsException) {
                        it
                    }
                }
        }
        ShowSource.NONE -> ""
    }.let { if (it.isNullOrBlank()) "Sorry, an error has occurred" else it }.trim()

    /**
     * the list of episodes
     */
    val episodeList: List<EpisodeInfo>
        get() = when (ShowSource.getSourceType(source.url)) {
            ShowSource.PUTLOCKER -> {
                if (source.type == ShowType.MOVIE) {
                    val info = "var post = \\{\"id\":\"(.*?)\"\\};".toRegex().toPattern().matcher(doc.html())
                    if (info.find()) {
                        listOf(EpisodeInfo(name, "https://www.putlocker.fyi/embed-src/${info.group(1)}"))
                    } else emptyList()
                } else {
                    doc.select("div.col-lg-12").select("div.row").select("a.btn-episode").map {
                        EpisodeInfo(it.attr("title"), "https://www.putlocker.fyi/embed-src/${it.attr("data-pid")}")
                    }
                }
            }
            ShowSource.GOGOANIME -> doc.select("ul.check-list").select("li").map {
                val urlInfo = it.select("a[href^=http]")
                val epName = urlInfo.text().let { info -> if (info.contains(name)) info.substring(name.length) else info }.trim()
                EpisodeInfo(epName, urlInfo.attr("abs:href"))
            }.distinctBy(EpisodeInfo::name)
            ShowSource.ANIMETOON -> {
                fun getStuff(document: Document) =
                    document.allElements.select("div#videos").select("a[href^=http]").map {
                        EpisodeInfo(it.text(), it.attr("abs:href"))
                    }
                getStuff(doc) + doc.allElements.select("ul.pagination").select(" button[href^=http]").map {
                    getStuff(Jsoup.connect(it.attr("abs:href")).get())
                }.flatten()
            }
            ShowSource.NONE -> emptyList()
        }

    fun <T> map(episode: EpisodeApi.() -> T): T = episode()

    override fun toString(): String = "$name - ${episodeList.size} eps - $description"
}

/**
 * Actual Episode info, name and url
 */
class EpisodeInfo(name: String, url: String) : ShowInfo(name, url)

class VideoLinkApi(private val url: String) {
    fun getVideoLink(): String = when (ShowSource.getSourceType(url)) {
        ShowSource.PUTLOCKER -> {
            val d = "<iframe[^>]+src=\"([^\"]+)\"[^>]*><\\/iframe>".toRegex().toPattern().matcher(getHtml(url))
            if (d.find()) {
                val a = "<p[^>]+id=\"videolink\">([^>]*)<\\/p>".toRegex().toPattern().matcher(getHtml(d.group(1)!!))
                if (a.find()) "https://verystream.com/gettoken/${a.group(1)!!}?mime=true" else ""
            } else ""
        }
        ShowSource.GOGOANIME -> Jsoup.connect(url).get().select("a[download^=http]").attr("abs:download")
        ShowSource.ANIMETOON -> {
            val html = Jsoup.connect(url).get().select("iframe[src^=http]").eachAttr("abs:src").firstOrNullMap {
                try {
                    Jsoup.connect(it).get().html()
                } catch (e: Exception) {
                    null
                }
            } ?: ""
            val reg = "var video_links = (\\{.*?\\});".toRegex().toPattern().matcher(html)
            if (reg.find()) Gson().fromJson(reg.group(1), NormalLink::class.java).normal!!.storage!![0].link!! else ""
        }
        ShowSource.NONE -> ""
    }

    private fun <T, R> List<T>.firstOrNullMap(block: (T) -> R?): R? {
        for(i in this) {
            return block(i) ?: continue
        }
        return null
    }

    @Throws(IOException::class)
    private fun getHtml(url: String): String {
        // Build and set timeout values for the request.
        val connection = URL(url).openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0")
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
    }
}

internal class NormalLink(var normal: Normal? = null)
internal class Normal(var storage: Array<Storage>? = emptyArray())
data class Storage(
        var sub: String? = null,
        var source: String? = null,
        var link: String? = null,
        var quality: String? = null,
        var filename: String? = null
)

data class IMDB(
        val Title: String?,
        val Year: String?,
        val Rated: String?,
        val Released: String?,
        val Runtime: String?,
        val Genre: String?,
        val Director: String?,
        val Writer: String?,
        val Actors: String?,
        val Plot: String?,
        val Language: String?,
        val Country: String?,
        val Awards: String?,
        val Poster: String?,
        val Ratings: List<Ratings>?,
        val Metascore: String?,
        val imdbRating: String?,
        val imdbVotes: String?,
        val imdbID: String?,
        val Type: String?,
        val DVD: String?,
        val BoxOffice: String?,
        val Production: String?,
        val Website: String?,
        val Response: String?
)

data class Ratings(val Source: String?, val Value: String?)
