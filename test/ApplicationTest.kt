package com.example

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.html.HtmlContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.io.File
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
        prettyLog(htmlInfo.mostCommonWord())
        prettyLog(loremIpsum.mostCommonCharacter())
        prettyLog(loremIpsum.characterMapper())
        prettyLog(loremIpsum.wordMapper())
        prettyLog(loremIpsum.mostCommonWord())
    }

    private fun String.mostCommonCharacter(): Pair<Char, Int>? =
        replace(" ", "")
            .groupingBy { it }
            .eachCount()
            .maxBy { it.value }?.toPair()

    private fun String.mostCommonWord(): Pair<String, Int>? =
        split(" ")
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxBy { it.value }?.toPair()

    private fun String.characterMapper() =
        replace(" ", "")
            .groupingBy { it }
            .eachCount()
            .map { Pair(it.key, it.value) }
            .sortedByDescending { it.second }
            .toMap()

    private fun String.wordMapper() =
        split(" ")
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .map { Pair(it.key, it.value) }
            .sortedByDescending { it.second }
            .toMap()

    private fun String.findRegex(string: String, groupNumber: Int = 1) =
        toRegex().find(string)?.groupValues?.getOrNull(groupNumber)

    @Test
    fun fiboTest() {
        prettyLog(fibo(13))
        prettyLog(fibo2(13))
        prettyLog(13.fibonacci())
        prettyLog("\n" + "Hello World! How are you doing?".frame())
        prettyLog("\n" + loremIpsum.frame())
        prettyLog(doubleSpeak("Hello World"))
        prettyLog(doubleSpeak2("Hello World"))
        prettyLog(doubleSpeak("Hello World!"))
        prettyLog(doubleSpeak2("Hello World!"))
    }

    private fun fibo(n: Int): Long = when (n) {
        0, 1 -> n.toLong()
        else -> fibo(n - 1) + fibo(n - 2)
    }

    private fun fibo2(n: Int): Long = if (n in 0..1) n.toLong() else fibo2(n - 1) + fibo2(n - 2)

    private fun Number.fibonacci(): Long = if (this in 0..1) toLong() else fibo2(toInt() - 1) + fibo2(toInt() - 2)

    private fun String.frame(): String {
        val s = replace("\n", " ").split(" ")
        val fullLength = s.maxBy { it.length }?.length!!
        val topBottom = "*".repeat(fullLength + 4)
        val middle = s.joinToString(separator = "\n") {
            "* $it${" ".repeat(fullLength - it.length + 1)}*"
        }
        return "$topBottom\n$middle\n$topBottom"
    }

    private fun doubleSpeak(s: String) = s.split("").joinToString(""){it.repeat(2)}
    //HHeelllloo  wwoorrlldd!!  ::))
    //HHeelllloo  wwoorrlldd!!  ::))
    private fun doubleSpeak2(s: String) = {s:String->s.map{"$it$it"}.joinToString("")}
    /**
    https://codegolf.stackexchange.com/questions/188988/ddoouubbllee-ssppeeaakk

    # Kotlin, 42 Chars and 42 bytes

        s.split("").joinToString(""){it.repeat(2)}

    [Try it online!](https://tio.run/##JY09DsIwDIX3nsJkipcMjBUgsbHDBSIRKoNxKscVQlXPHkL7tvej772yMUl9TAKWivnSw9WUZEA4Qi2hjEzmncPwzCS3vJX/YCYLmsYUze9xWQnvSOKjDg1yVo3fw7Y@IcwdNI3NGYtfn9wlMWf4ZOX7Dnp0iN1Sfw "Kotlin – Try It Online")

     */
    @Test
    fun putMovie() {
        val list = Gson().fromJson<MutableList<ShowInfo>>(
            File("resources/database/movie.json").readText(),
            object : TypeToken<MutableList<ShowInfo>>() {}.type
        )
        val e = list.randomShow()
        prettyLog(e)
        val eList = e.episodeList
        prettyLog(eList)
        val v = VideoLinkApi(eList.random().url)
        prettyLog(v.getVideoLink())
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
        //val video = Jsoup.connect("https://oload.tv/embed/SF0MjzSOY0I/History.of.the.World.Part.1.1981.mp4").get()
        //prettyLog(video)
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

    private val loremIpsum =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed turpis elit, dictum vel bibendum vitae, auctor pharetra sem. Nullam fermentum lacus et mollis mollis. Integer elementum placerat nulla non pharetra. In est quam, mollis eget metus ac, laoreet ornare nisi. In facilisis elit et turpis dictum, eu suscipit lorem ultrices. Nullam tempus aliquam neque, in accumsan ligula luctus a. Cras pretium neque non justo gravida dignissim. Proin congue blandit tempor. Sed accumsan leo eu malesuada vestibulum. Cras molestie porta lacus, eget posuere leo consequat in. Maecenas faucibus vulputate sem, eget venenatis erat lobortis sed. Nullam purus mi, feugiat at lorem non, ultricies rhoncus lorem. Vivamus erat urna, lacinia at porttitor pretium, tristique in ex. Ut pulvinar, ligula id fermentum sagittis, sem diam mollis sapien, vel pretium enim diam convallis erat. Morbi finibus turpis vitae vestibulum vulputate.\n" +
                "\n" +
                "Suspendisse potenti. Nam ultrices odio id magna ultrices rutrum non non nisi. Etiam eget magna fermentum, dignissim ligula sed, venenatis mi. Nam id lacus nec tortor efficitur gravida vitae id nulla. Fusce non mauris a tellus eleifend sagittis. Nam sed consectetur libero, ut finibus lacus. Donec iaculis varius nisi, a sagittis augue pellentesque quis. Integer semper dapibus odio euismod interdum. Donec porttitor dolor nec nisi tempor, ac fringilla nisl facilisis. Nulla luctus massa arcu. Proin ut diam auctor, aliquam odio ac, rhoncus enim. Vivamus vestibulum nunc sed mauris cursus viverra. Suspendisse egestas malesuada est posuere cursus. Vestibulum ac augue eget augue volutpat tincidunt eget tempor arcu. Vivamus id nisi tempus, sagittis metus sed, tempor leo. Mauris fermentum iaculis mi, id viverra est ullamcorper in.\n" +
                "\n" +
                "Proin vulputate, dui at viverra imperdiet, tellus ex ultrices eros, vel congue nibh dolor ornare nunc. Suspendisse tempor libero risus, nec mattis elit tincidunt nec. Ut in malesuada nisi. Proin blandit ornare lorem. Mauris a augue et leo vestibulum tincidunt. Sed ut felis et nulla ultricies porta. Morbi eu leo felis. Praesent volutpat ac metus iaculis congue. In vestibulum hendrerit dui at fermentum. Suspendisse eu faucibus magna. Morbi dapibus consectetur lacus, in scelerisque felis fringilla gravida. Proin vel magna posuere, egestas sapien eu, gravida erat. Nullam sed auctor sem. Nullam a dapibus massa.\n" +
                "\n" +
                "Donec lacinia elementum arcu, efficitur ornare nibh. Donec ac erat ante. Duis lobortis mauris vitae ligula feugiat, et vehicula dolor elementum. Sed semper ipsum ipsum, at elementum neque euismod eget. In eget fringilla nulla. Nam nec massa vestibulum, dapibus leo vel, iaculis ante. Integer nec maximus erat. Morbi eget sapien sed enim volutpat suscipit. Sed commodo nibh sit amet nisi tempus tempor. Proin convallis justo enim, eu elementum orci imperdiet sit amet. Quisque lacinia sapien nibh, in vestibulum orci rutrum vel. Proin sit amet egestas neque. Curabitur egestas dolor ligula, at aliquam sem tempus eget. Aenean maximus metus diam, nec finibus ante varius vitae. Suspendisse maximus aliquam arcu nec pellentesque.\n" +
                "\n" +
                "Maecenas nec metus pellentesque felis mattis euismod tincidunt sed urna. In placerat dolor nec convallis eleifend. Mauris gravida porta rutrum. Morbi risus arcu, fermentum pellentesque sapien nec, varius ultricies mi. Aenean neque metus, lobortis ut consectetur sit amet, bibendum fermentum urna. Donec non velit in ante convallis lacinia. Donec placerat justo in libero luctus congue. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Nam quis urna faucibus nunc egestas semper. Vivamus aliquam turpis orci, vel laoreet nunc ultricies lobortis. Ut imperdiet turpis in mauris dictum, et mollis lacus porta."

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
