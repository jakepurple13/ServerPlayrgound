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
    fun project() {
        prettyLog("<body(.*?)>".findRegex(htmlInfo))
        prettyLog("abbcccddddeeeee".mostCommonCharacter())
        prettyLog("abcde".mostCommonCharacter())
        prettyLog(htmlInfo.mostCommonCharacter())
        prettyLog(htmlInfo.characterMapper())
        prettyLog(htmlInfo.wordMapper())
    }

    private fun String.mostCommonCharacter(): Pair<Char, Int>? =
        replace(" ", "").groupingBy { it }.eachCount().maxBy { it.value }?.toPair()

    private fun String.characterMapper() =
        replace(" ", "").groupingBy { it }.eachCount().map { Pair(it.key, it.value) }.sortedByDescending { it.second }.toMap()

    private fun String.wordMapper() =
        split(" ").groupingBy { it }.eachCount().map { Pair(it.key, it.value) }.sortedByDescending { it.second }.toMap()

    private fun String.findRegex(string: String, groupNumber: Int = 1) =
        toRegex().find(string)?.groupValues?.getOrNull(groupNumber)

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

    private val htmlInfo = """
    <!doctype html>
    <html>
    
    <head>
        <!--should use the same as the default extension favicon-->
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link id="gsFavicon" rel="icon" href="img/ic_suspendy_16x16.png" />
        <link rel="stylesheet" type="text/css" href="css/suspended.css">
        <!--no point localising the gsTitle as it requires js to do so, and we may as well just set the real title instead-->
        <!--ideally it will show no title at all until the real one loads, so we use the ZERO WIDTH NO-BREAK SPACE character-->
        <title id="gsTitle">...</title>
        <meta name="google" content="notranslate">
    </head>
    
    <body class="suspended-page hide-initially">
    
        <script type="text/html" id="previewTemplate">
        <img id="gsPreviewImg" class="gsPreviewImg" />
        </script>
    
        <script type="text/html" id="donateTemplate">
            <link id="donateCss" rel="stylesheet" type="text/css" href="css/donate.css">
            <img id="dudePopup" src="img/suspendy-guy.png">
            <div id="donateBubble" class="donateBubble">
                <p class="donate-text" data-i18n="__MSG_html_suspended_donation_question__"></p>
                <div id="donateButtons" class="donateButtons" />
            </div>
        </script>
    
        <script type="text/html" id="toastTemplate">
            <div class="toast-content">
                <h1 data-i18n="__MSG_html_suspended_toast_not_connected__"></h1>
                <p data-i18n="__MSG_html_suspended_toast_reload_disabled__"></p>
            </div>
        </script>
    
        <div id="gsTopBar" class="gsTopBar">
            <div id="gsTopBarTitleWrap" class="hideOverflow gsTopBarTitleWrap">
                <div id="faviconWrap" class="faviconWrap">
                    <img id="gsTopBarImg" class="gsTopBarImg" />
                </div>
                <span id="gsTopBarTitle" class="gsTopBarTitle"></span>
            </div>
            <div class="hideOverflow">
                <a id="gsTopBarUrl" class="gsTopBarUrl"></a>
            </div>
        </div>
    
        <div id="suspendedMsg" class="suspendedMsg">
            <div class="snoozyWrapper">
                <img id="snoozyImg" src="img/snoozy_tab.svg" />
                <div id="snoozySpinner"></div>
            </div>
            <div class="suspendedTextWrap">
                <div id="suspendedMsg-instr" class="suspendedMsg-instr">
                    <div data-i18n="__MSG_html_suspended_click_to_reload__"></div>
                </div>
                <div class="suspendedMsg-shortcut">
                    <span id="hotkeyWrapper" class="hotkeyWrapper"></span>
                </div>
            </div>
        </div>
        <div class="watermark">
            The Great Suspender
        </div>
    
        <div id="refreshSpinner"></div>
    </body>
    
    </html>
    """
}
