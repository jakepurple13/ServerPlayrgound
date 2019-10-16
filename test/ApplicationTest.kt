package com.example

import io.ktor.html.HtmlContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jsoup.Jsoup
import kotlin.collections.set
import kotlin.test.Test

class ApplicationTest {

    @Test
    fun genericEnumTest() {
        /*randomEnum<Source>().apply { prettyLog(this) }
        Source.values().random().apply { prettyLog(this) }
        Source::class.random().apply { prettyLog(this) }

        randomEnum<ChatServer.MessageType>().apply { prettyLog(this) }
        ChatServer.MessageType.values().random().apply { prettyLog(this) }
        ChatServer.MessageType::class.random().apply { prettyLog(this) }*/
    }

    @Test
    fun putMovie() {
        //val q = ShowApi(Source.RECENT_ANIME).showInfoList
        //prettyLog(q.toPrettyJson())

        //val f = ShowApi.getSources(Source.RECENT_LIVE_ACTION).getEpisodeApi(0).episodeList
        //prettyLog(f.toPrettyJson())
        //puid=179694722
        /*val show = ShowInfo("HOTWPO", "https://www3.putlocker.fyi/history-of-the-world-1/", ShowType.MOVIE)
        val ep = EpisodeApi(show)
        prettyLog(ep)
        val v = VideoLinkApi(ep.episodeList[0].url)
        prettyLog(v.getVideoLink())
        //val s = VideoLinkApi("https://www3.putlocker.fyi/dude-wheres-my-car/").getVideoLink()
        //prettyLog(s)
        val s1 = VideoLinkApi("https://www3.putlocker.fyi/embed-src/26499").getVideoLink()
        prettyLog(s1)*/
        /*val json = Jsoup.connect("https://www3.putlocker.fyi/history-of-the-world-1/").get()
        val xml = Jsoup.connect("https://www3.putlocker.fyi/embed-src/26499").get()
        prettyLog(json)
        prettyLog(xml)*/
        val video = Jsoup.connect("https://oload.tv/embed/SF0MjzSOY0I/History.of.the.World.Part.1.1981.mp4").get()
        prettyLog(video)

    }

    fun <T> Any.check(vararg checkAgainst: T): Boolean {
        for(c in checkAgainst) {
            if(this != c) {
                return false
            }
        }
        return true
    }

    @Test
    fun jokeTest() {
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3")
        runBlocking {

        }
    }

    @Test
    fun htTest() {
        val highScores = mutableMapOf<String, MutableList<MusicUserInfo>>()
        highScores["big blue ball"] = mutableListOf(MusicUserInfo("asdf", "asdf", "asdf"))
        highScores["jfla"] = mutableListOf(MusicUserInfo("asdf", "asdf", "asdf"))
        val html = createHTML(true, xhtmlCompatible = true)
            .table {
                id = "highList"
                classes = classes + "darkTable"
                prettyLog(attributesEntries)
                highScores.keys.forEach { info ->
                    tr {
                        td {
                            unsafe {
                                +info
                            }
                        }
                        //}
                        highScores[info]!!.forEach {
                            //tr {
                            td {
                                unsafe {
                                    +"${it.name} | ${it.score}"
                                }
                            }
                        }
                    }
                }
            }
        prettyLog(html)
    }

    @Test
    fun testRoot() {
        /*withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO WORLD!", response.content)
            }
        }*/
        val s = HtmlContent(HttpStatusCode.OK) {
            addElements()
        }
        prettyLog(s)
        val html = createHTML(true, xhtmlCompatible = true)
            .html {
                addElements()
            }
        prettyLog(html)
    }

    private fun HTML.addElements() {
        val list = listOf("asdf", "asdf", "<i>asdf</i>")
        body {
            table {
                list.forEach { info ->
                    tr {
                        td {
                            unsafe {
                                +info
                            }
                        }
                    }
                }
            }
        }
    }
}
