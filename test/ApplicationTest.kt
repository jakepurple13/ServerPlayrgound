package com.example

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.html.HtmlContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import java.io.File
import kotlin.collections.set
import kotlin.random.Random
import kotlin.reflect.KCallable
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.test.Test


class ApplicationTest {

    @Before
    fun setup() {
        Loged.FILTER_BY_CLASS_NAME = "com.example"
        Loged.OTHER_CLASS_FILTER { !it.contains("Framing", true) }
    }

    data class EpisodeInfo(val name: String, val url: String)
    data class ShowInformation(
            val name: String = "",
            val image: String = "",
            val url: String = "",
            val description: String = "",
            val episodeList: List<EpisodeInfo> = emptyList(),
            val genres: List<String> = emptyList()
    )

    data class ShowInfoCom(val name: String, val url: String)

    data class FullShowInfo(val showInfoCom: ShowInfoCom, val showInformation: ShowInformation)

    private fun getFullShowInfo(db: Database) = transaction(db) {
        Shows.selectAll().map {
            FullShowInfo(
                    ShowInfoCom(it[Shows.name], it[Shows.url]),
                    Episodes.select { Episodes.show eq it[Shows.id] }.map { ep ->
                        ShowInformation(
                                ep[Episodes.name],
                                ep[Episodes.image],
                                ep[Episodes.url],
                                ep[Episodes.description],
                                EpisodeLists.select { EpisodeLists.episode eq ep[Episodes.id] }
                                    .map { EpisodeInfo(it[EpisodeLists.name], it[EpisodeLists.url]) }
                        )
                    }[0]
            )
        }.sortedBy { it.showInfoCom.name }
    }

    private fun getFullShowInfos(db: Database) = transaction(db) {
        Episodes.selectAll().mapNotNull { ep ->
            try {
                ShowDBApi.ShowInformation(
                        ep[Episodes.name],
                        ep[Episodes.image],
                        ep[Episodes.url],
                        ep[Episodes.description],
                        EpisodeLists.select { EpisodeLists.episode eq ep[Episodes.id] }
                            .map { ShowDBApi.EpisodeInfo(it[EpisodeLists.name], it[EpisodeLists.url]) }
                )
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name }
    }

    @Test
    fun getApiTest() {
        //val s = "http://arcane-fortress-22748.herokuapp.com/api/user/all.json"
        val db = DbSettings.db
        prettyLog(getFullShowInfos(db).toPrettyJson())
    }

    @Test
    fun getApiTest2() {
        //val s = "http://arcane-fortress-22748.herokuapp.com/api/user/all.json"
        val db = DbSettings.db
        val json = ShowDBApi(db, listOf()).getFullShowInfos()
        val split = json.partition { it.url.contains("putlocker") }.let {
            mapOf(
                    "putlocker" to it.first
            ) + it.second.partition { it.url.contains("gogoanime", true) }.let {
                mapOf(
                        "gogoanime" to it.first,
                        "animetoon" to it.second
                )
            }
        }
        prettyLog("Total - ${json.size}\n${split.entries.joinToString("\n") { "${it.key} - ${it.value.size}" }}")
        val shows = File("resources/database/allshows2.json")
        shows.createNewFile()
        val showList = mapOf("shows" to json)
        shows.writeText(showList.toPrettyJson())
    }

    data class ShowsInfo(val shows: List<ShowDBApi.ShowInformation>)

    @Test
    fun fromJsonStuff() {
        val shows = Gson().fromJson<ShowsInfo>(File("resources/database/allshows2.json").readText(), ShowsInfo::class.java)
        val split = shows.shows.partition { it.url.contains("putlocker") }.let {
            mapOf(
                    "putlocker" to it.first
            ) + it.second.partition { it.url.contains("gogoanime", true) }.let {
                mapOf(
                        "gogoanime" to it.first,
                        "animetoon" to it.second
                )
            }
        }
        prettyLog("Total - ${shows.shows.size}\n${split.entries.joinToString("\n") { "${it.key} - ${it.value.size}" }}")
    }

    @Test
    fun fromJsonStuff2() {
        val shows = Gson().fromJson<ShowsInfo>(File("resources/database/allshows2.json").readText(), ShowsInfo::class.java).shows
        val split = shows.groupBy { ShowSource.getSourceType(it.url).name.toLowerCase() }
        val mapped = split.entries.map { "${it.key} - ${it.value.size}" }
        val m = mapped + "-".repeat(mapped.maxBy { it.length }!!.length) + "Total - ${shows.size}"
        Loged.f(m)
    }

    @Test
    fun fromJsonStuff3() {
        val shows = Gson().fromJson<ShowsInfo>(File("resources/database/allshows2.json").readText(), ShowsInfo::class.java).shows
        val split = shows.groupBy { ShowSource.getSourceType(it.url) }
        val show = split[ShowSource.PUTLOCKER]!!.filter { it.episodeList.isNotEmpty() }.random()
        val splitted = show.episodeList.groupBy { "Season \\d".toRegex().find(it.name)?.groupValues?.getOrElse(0) { "Season 1" } ?: "Season 1" }
        val mapped = splitted.entries.map { "${it.key} - ${it.value.size}" }
        Loged.f(mapped, show.name)
        Loged.f(show)
    }

    @Test
    fun fromJsonStuff4() {
        fromJsonStuff2()
        //val shows = Gson().fromJson<ShowsInfo>(File("resources/database/allshows2.json").readText(), ShowsInfo::class.java).shows
        //    .filter { it.episodeList.isNotEmpty() }
        val showsonly = File("resources/database/showsonly.json")
        val shows = Gson().fromJson<List<ShowInfo>>(showsonly.readText(), object : TypeToken<List<ShowInfo>>() {}.type)
            .filter { it.type == ShowType.SHOW }
            .mapNotNull {
                try {
                    EpisodeApi(it)
                } catch (e: Exception) {
                    null
                }
            }
            .map {
                it.map {
                    ShowDBApi.ShowInformation(
                            name,
                            image,
                            source.url,
                            description,
                            episodeList.map { ShowDBApi.EpisodeInfo(it.name, it.url) },
                            genres
                    )
                }
            }
        val showing = File("resources/database/episodesonly.json")
        showing.createNewFile()
        showing.writeText(shows.toPrettyJson())
        /*for(i in shows) {
            val seasons = i.episodeList.groupBy { "Season \\d".toRegex().find(it.name)?.groupValues?.getOrElse(0) { "Season 1" } ?: "Season 1" }
            val mapped = seasons.entries.map { "${it.key} - ${it.value.size}" }
            Loged.f(mapped, i.name, ShowSource.getSourceType(i.url).name)
        }*/
        /*for (i in shows) {
            Loged.f(i, i.name, ShowSource.getSourceType(i.url).name, showSeasons = true)
        }*/
        /*val split = shows.groupBy { ShowSource.getSourceType(it.url) }
        val show = split[ShowSource.PUTLOCKER]!!.filter { it.episodeList.isNotEmpty() }.random()
        val splitted = show.episodeList.groupBy { "Season \\d".toRegex().find(it.name)?.groupValues?.getOrElse(0) { "Season 1" } ?: "Season 1" }
        val mapped = splitted.entries.map { "${it.key} - ${it.value.size}" }
        Loged.f(mapped, show.name)
        Loged.f(show)*/
        /*val s = shows.map { it.frame(true) }
        val file = File("resources/database/niceLookingShows.txt")
        file.writeText(s.joinToString("\n\n"))*/
        /*val s = shows.map { it!!.frame(true) }
        val file = File("resources/database/niceLookingShowsTwo.txt")
        file.writeText(s.joinToString("\n\n"))*/
    }

    fun Loged.f(
            msg: ShowDBApi.ShowInformation, tag: String = msg.name, infoText: String = TAG,
            showPretty: Boolean = SHOW_PRETTY, threadName: Boolean = WITH_THREAD_NAME, showSeasons: Boolean = true
    ) = f(listOf(
            "Name: ${msg.name}",
            "Url: ${msg.url}",
            "Image: ${msg.image}",
            "Genres: ${msg.genres.joinToString(", ") { it }}",
            "Description: ${msg.description.replace("\n", " ")}",
            "Episodes:",
            *msg.let {
                if (showSeasons) {
                    it.episodeList.groupBy { "Season \\d".toRegex().find(it.name)?.groupValues?.getOrElse(0) { "Season 1" } ?: "Season 1" }.map {
                        listOf(listOf("      ${it.key}"), it.value.map { "            $it" }).flatten()
                    }.flatten()
                } else {
                    it.episodeList.map { "      $it" }
                }
            }.toTypedArray()
    ), tag, infoText, showPretty, threadName)

    fun ShowDBApi.ShowInformation.frame(showSeasons: Boolean = true) = listOf(
            "Name: $name",
            "Url: $url",
            "Image: $image",
            "Genres: ${genres.joinToString(", ") { it }}",
            "Description: ${description.replace("\n", " ")}",
            "Episodes:",
            *this.let {
                if (showSeasons) {
                    it.episodeList.groupBy { "Season \\d".toRegex().find(it.name)?.groupValues?.getOrElse(0) { "Season 1" } ?: "Season 1" }.map {
                        listOf(listOf("      ${it.key}"), it.value.map { "            $it" }).flatten()
                    }.flatten()
                } else {
                    it.episodeList.map { "      $it" }
                }
            }.toTypedArray()
    ).frame(FrameType.BOX.apply {
        frame.top = this@frame.name
    })

    private fun List<ShowInfo>.randomShow(): EpisodeApi = EpisodeApi(random())

    @Test
    fun genreAdder() {
        val p = ShowApi.getSources(Source.LIVE_ACTION).randomShow().genres
        val g = ShowApi.getSources(Source.ANIME).randomShow().genres
        val a = ShowApi.getSources(Source.CARTOON).randomShow().genres
        prettyLog("$p\n$g\n$a")
    }

    @Test
    fun vidTest() {
        val s = ShowApi.getSources(Source.CARTOON, Source.DUBBED).randomShow().episodeList.random().url
        val f = VideoLinkApi(s).getVideoLink()
        prettyLog(f)
    }

    @Test
    fun putMovTest() {
        /*val f = ShowApi(Source.LIVE_ACTION_MOVIES).showInfoList
        prettyLog(f.size)
        prettyLog(f.filter { it.name.contains("SpaceBalls", ignoreCase = true) })*/
        val g = EpisodeApi(ShowInfo("Spaceballs", "https://www.putlocker.fyi/spaceballs", ShowType.MOVIE))
        prettyLog(g)
        val s = g.episodeList.firstOrNull()?.url?.let {
            prettyLog(it)
            VideoLinkApi(it).getVideoLink()
        }
        prettyLog(s)
        val f = ShowApi.getSources(Source.LIVE_ACTION).getEpisodeApi(0)
        prettyLog(f)
        val s1 = f.episodeList.firstOrNull()?.url?.let {
            prettyLog(it)
            VideoLinkApi(it).getVideoLink()
        }
        prettyLog(s1)
    }

    // The math function. The algorithm will be searching for the max value of it across the infinity.
    fun f(x: Int): Int = -(x - 3) * (x - 3) + 5
    //Two different ways to do this
    //val f: (Int) -> Int = { x -> -(x - 3) * (x - 3) + 5 }

    inner class Answer(parent: Answer? = null) : Comparable<Answer> {
        var x = 0

        init {
            x = parent?.let { it.x + Random.nextInt(-5, 5) } ?: Random.nextInt(-100, 100)
        }

        override fun compareTo(other: Answer): Int = f(x).compareTo(f(other.x))
    }

    fun main() {
        var s = ""
        val populationSize = 10
        val iterations = 20
        var population = mutableListOf<Answer>()
        for (i in 0..populationSize) {
            population.add(Answer())
        }
        for (i in 0..iterations) {
            for (j in 0..populationSize) {
                population.add(Answer(population[j]))
            }
            population.sortDescending()
            population = population.subList(0, populationSize)
            s += "${population.map { Pair(it.x, f(it.x)) }.joinToString("\t\t")}\n"
        }
        val x = population[0].x
        println("$s\nAnswer found: ($x, ${f(x)}).")
    }

    @Test
    fun geneticEvolutionCode() {
        main()
    }

    fun <T> T?.ifNull(block: () -> Unit) = if (this == null) block().let { true } else false
    fun <T> T?.ifNotNull(block: T.() -> Unit) = if (this != null) block().let { true } else false
    infix fun Boolean.orElse(elseBlock: () -> Unit) = elseBlock()

    fun smallLittleFunctionTest() {
        val f: Int? = null
        f.ifNotNull {
            //stuff happens here
        } orElse {

        }

        f.ifNull {

        } orElse {

        }
    }

    private suspend fun relocate() = 4
    private suspend fun locate() {
        val f: Int? = null
        f.ifNotNull {
            //relocate() //<-- error that its not in a suspend function
        } orElse {
            //relocate() //<-- error that its not in a suspend function
        }
    }

    @Test
    fun syncTest() {
        val f = ShowApi(Source.LIVE_ACTION_MOVIES).showInfoList
        prettyLog(f)
    }

    @Test
    fun genericEnumTest() {
        /*randomEnum<Source>().apply { prettyLog(this) }
        Source.values().random().apply { prettyLog(this) }
        Source::class.random().apply { prettyLog(this) }

        randomEnum<ChatServer.MessageType>().apply { prettyLog(this) }
        ChatServer.MessageType.values().random().apply { prettyLog(this) }
        ChatServer.MessageType::class.random().apply { prettyLog(this) }*/
        data class Contact(val name: String, val phone: String)

        data class Car(val name: String, val brand: String)
        data class Phone(val name: String, val brand: String)

        val contact = Contact("Jake", "Phone")
        val car = Car("Mini", "Cooper")
        val car2 = Car("TT", "Audi")
        val carList = listOf(car, car2)

        val f0 = contact.customMap { Phone(name, phone) }
        val f = contact.customMap(car) { Phone(name, it.brand) }
        val f1 = contact.customListMap(car, car2) { Phone(name, it.brand) }
        val f2 = contact.customCollectionMap(carList) { Phone(name, it.brand) }

        prettyLog(f0)
        prettyLog(f)
        prettyLog(f1)
        prettyLog(f2)

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
    fun asdf() {
        val f = createHTML(true)
            .html {
                head {
                    +"Header"
                }
                body {
                    +"Here in the body!"
                }
            }

        prettyLog(f)
    }

    @Test
    fun fiboTest() {
        prettyLog(fibo(13))
        prettyLog(fibo2(13))
        prettyLog(13.fibonacci())

        val helloWorld = "Hello World! How are you doing?"

        var f = ""

        fun String.addToString() {
            f += this + "\n"
        }

        helloWorld.frame().addToString()
        helloWorld.frame(rtl = false).addToString()
        helloWorld.frame(rtl = true).addToString()
        helloWorld.frame("+", true).addToString()
        helloWorld.frame("+", false).addToString()
        helloWorld.frame("-", "-", "|", "|").addToString()
        helloWorld.frame("═", "═", "║", "║").addToString()
        helloWorld.frame("═", "═", "║", "║", "╔", "╗", "╚", "╝").addToString()
        helloWorld.frame("═", "║", "╔", "╗", "╚", "╝").addToString()
        helloWorld.doubleSpeaking().frame("═", "═", "║", "║", "╔", "╗", "╚", "╝").addToString()

        prettyLog("\n" + f)

        println()
        prettyLog(doubleSpeak("Hello World"))
        prettyLog(doubleSpeak2("Hello World"))
        prettyLog(doubleSpeak("Hello World!"))
        prettyLog(doubleSpeak2("Hello World!"))
        prettyLog("bbc".toInt('\r'.toInt()))
    }

    private fun fibo(n: Int): Long = when (n) {
        0, 1 -> n.toLong()
        else -> fibo(n - 1) + fibo(n - 2)
    }

    private fun fibo2(n: Int): Long = if (n in 0..1) n.toLong() else fibo2(n - 1) + fibo2(n - 2)

    private fun Number.fibonacci(): Long = if (this in 0..1) toLong() else fibo2(toInt() - 1) + fibo2(toInt() - 2)

    private fun String.frame(): String {
        val s = replace("\n", " ").split(" ")
        val fullLength = s.maxBy { it.length }!!.length
        val topBottom = "*".repeat(fullLength + 4)
        val middle = s.joinToString(separator = "\n") { "* $it${" ".repeat(fullLength - it.length + 1)}*" }
        return "$topBottom\n$middle\n$topBottom"
    }

    private fun String.frame(rtl: Boolean = false): String {
        val s = replace("\n", " ").split(" ")
        val fullLength = s.maxBy { it.length }!!.length
        val topBottom = "*".repeat(fullLength + 4)
        val space: (String) -> String = { " ".repeat(fullLength - it.length + 1) }
        val mid = s.joinToString(separator = "\n") {
            "*${if (rtl) space(it) else " "}$it${if (rtl) " " else space(it)}*"
        }
        return "$topBottom\n$mid\n$topBottom"
    }

    private fun String.frame(surrounding: String = "*", rtl: Boolean = false): String {
        val s = replace("\n", " ").split(" ")
        val fullLength = s.maxBy { it.length }!!.length
        val topBottom = surrounding.repeat(fullLength + 4)
        val space: (String) -> String = { " ".repeat(fullLength - it.length + 1) }
        val mid = s.joinToString(separator = "\n") {
            "$surrounding${if (rtl) space(it) else " "}$it${if (rtl) " " else space(it)}$surrounding"
        }
        return "$topBottom\n$mid\n$topBottom"
    }

    private fun String.frame(
            top: String = "*",
            bottom: String = top,
            left: String = "*",
            right: String = left,
            rtl: Boolean = false
    ): String {
        val s = replace("\n", " ").split(" ")
        val fullLength = s.maxBy { it.length }!!.length
        val space: (String) -> String = { " ".repeat(fullLength - it.length + 1) }
        val mid = s.joinToString(separator = "\n") {
            "$left${if (rtl) space(it) else " "}$it${if (rtl) " " else space(it)}$right"
        }
        return "${top.repeat(fullLength + 4)}\n$mid\n${bottom.repeat(fullLength + 4)}"
    }

    private fun String.frame(
            topBottom: String = "*",
            sides: String = "*",
            topLeft: String = "*",
            topRight: String = "*",
            bottomLeft: String = "*",
            bottomRight: String = "*",
            rtl: Boolean = false
    ): String {
        val s = replace("\n", " ").split(" ")
        val fullLength = s.maxBy { it.length }!!.length
        val space: (String) -> String = { " ".repeat(fullLength - it.length + 1) }
        val mid = s.joinToString(separator = "\n") {
            "$sides${if (rtl) space(it) else " "}$it${if (rtl) " " else space(it)}$sides"
        }
        val bottomTop = topBottom.repeat(fullLength + 2)
        return "$topLeft$bottomTop$topRight\n$mid\n$bottomLeft$bottomTop$bottomRight"
    }

    private fun String.frame(
            top: String = "*",
            bottom: String = top,
            left: String = "*",
            right: String = left,
            topLeft: String = "*",
            topRight: String = "*",
            bottomLeft: String = "*",
            bottomRight: String = "*",
            rtl: Boolean = false
    ): String {
        val s = replace("\n", " ").split(" ")
        val fullLength = s.maxBy { it.length }!!.length
        val space: (String) -> String = { " ".repeat(fullLength - it.length + 1) }
        val mid = s.joinToString(separator = "\n") {
            "$left${if (rtl) space(it) else " "}$it${if (rtl) " " else space(it)}$right"
        }
        return "$topLeft${top.repeat(fullLength + 2)}$topRight\n$mid\n$bottomLeft${bottom.repeat(fullLength + 2)}$bottomRight"
    }

    class Frame internal constructor(
            var top: String = "", var bottom: String = "",
            var left: String = "", var right: String = "",
            var topLeft: String = "", var topRight: String = "",
            var bottomLeft: String = "", var bottomRight: String = "",
            var topFillIn: String = "", var bottomFillIn: String = ""
    )

    enum class FrameType(val frame: Frame) {
        /**
         * BOX Frame
         * Will look like this
         *
        ```
        ╔==========================╗
        ║ Hello World              ║
        ╚==========================╝
        or
        If the top if modified
        ╔==========Hello===========╗
        ║ World                    ║
        ╚==========================╝
        or
        If the bottom is modified
        ╔==========================╗
        ║ World                    ║
        ╚==========Hello===========╝
        ```
         */
        BOX(Frame("=", "=", "║", "║", "╔", "╗", "╚", "╝", "=", "=")),
        /**
         * ASTERISK Frame
         * Will look like this
         *
        ```
        1.   ****************************
        2.   * Hello World              *
        3.   ****************************
        or
        If the top if modified
        1.   ***********Hello************
        2.   * World                    *
        3.   ****************************
        or
        If the bottom is modified
        1.   ****************************
        2.   * World                    *
        3.   ***********Hello************
        ```
         */
        ASTERISK(Frame("*", "*", "*", "*", "*", "*", "*", "*", "*", "*")),
        /**
         * Plus Frame
         * Will look like this
         *
        ```
        ++++++++++++++++++++++++++++
        + Hello World              +
        ++++++++++++++++++++++++++++
        or
        If the top if modified
        +++++++++++Hello++++++++++++
        + World                    +
        ++++++++++++++++++++++++++++
        or
        If the bottom is modified
        ++++++++++++++++++++++++++++
        + World                    +
        +++++++++++Hello++++++++++++
        ```
         */
        PLUS(Frame("+", "+", "+", "+", "+", "+", "+", "+", "+", "+")),
        /**
         * DIAGONAL Frame
         * Will look like this
         *
        ```
        ╱--------------------------╲
        | Hello World              |
        ╲--------------------------╱
        or
        If the top if modified
        ╱----------Hello-----------╲
        | World                    |
        ╲--------------------------╱
        or
        If the bottom is modified
        ╱--------------------------╲
        | World                    |
        ╲----------Hello-----------╱
        ```
         */
        DIAGONAL(Frame("-", "-", "│", "│", "╱", "╲", "╲", "╱", "-", "-")),
        /**
         * OVAL Frame
         * Will look like this
         *
        ```
        ╭--------------------------╮
        | Hello World              |
        ╰--------------------------╯
        or
        If the top if modified
        ╭----------Hello-----------╮
        | World                    |
        ╰--------------------------╯
        or
        If the bottom is modified
        ╭--------------------------╮
        | World                    |
        ╰----------Hello-----------╯
        ```
         */
        OVAL(Frame("-", "-", "│", "│", "╭", "╮", "╰", "╯", "-", "-")),
        /**
         * BOXED Frame
         * Will look like this
         *
        ```
        ▛▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▜
        ▌ Hello World              ▐
        ▙▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▟
        or
        If the top if modified
        ▛▀▀▀▀▀▀▀▀▀▀Hello▀▀▀▀▀▀▀▀▀▀▀▜
        ▌ World                    ▐
        ▙▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▟
        or
        If the bottom is modified
        ▛▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▜
        ▌ World                    ▐
        ▙▄▄▄▄▄▄▄▄▄▄Hello▄▄▄▄▄▄▄▄▄▄▄▟
        ```
         */
        BOXED(Frame("▀", "▄", "▌", "▐", "▛", "▜", "▙", "▟", "▀", "▄")),
        /**
         * CUSTOM Frame
         * You decide how all of it looks
         */
        CUSTOM(Frame());

        companion object {
            /**
             * Use this to create a custom [FrameType]
             */
            fun CUSTOM(frame: Frame.() -> Unit) = CUSTOM.frame.apply(frame)
        }
    }

    fun String.frame(frameType: FrameType, rtl: Boolean = false) = listOf(this).frame(
            top = frameType.frame.top, bottom = frameType.frame.bottom,
            left = frameType.frame.left, right = frameType.frame.right,
            topLeft = frameType.frame.topLeft, topRight = frameType.frame.topRight,
            bottomLeft = frameType.frame.bottomLeft, bottomRight = frameType.frame.bottomRight,
            topFillIn = frameType.frame.topFillIn, bottomFillIn = frameType.frame.bottomFillIn, rtl = rtl
    )

    fun <T> Collection<T>.frame(frameType: FrameType, rtl: Boolean = false, transform: (T) -> String = { it.toString() }) =
        frame(
                top = frameType.frame.top, bottom = frameType.frame.bottom,
                left = frameType.frame.left, right = frameType.frame.right,
                topLeft = frameType.frame.topLeft, topRight = frameType.frame.topRight,
                bottomLeft = frameType.frame.bottomLeft, bottomRight = frameType.frame.bottomRight,
                topFillIn = frameType.frame.topFillIn, bottomFillIn = frameType.frame.bottomFillIn, rtl = rtl, transform = transform
        )

    fun <T> Collection<T>.frame(
            top: String, bottom: String,
            left: String, right: String,
            topLeft: String, topRight: String,
            bottomLeft: String, bottomRight: String,
            topFillIn: String = "", bottomFillIn: String = "",
            rtl: Boolean = false, transform: (T) -> String = { it.toString() }
    ): String {
        val fullLength = mutableListOf(top, bottom).apply { addAll(this@frame.map(transform)) }.maxBy { it.length }!!.length + 2
        val space: (String) -> String = { " ".repeat(fullLength - it.length - 1) }
        val mid =
            joinToString(separator = "\n") { "$left${if (rtl) space(transform(it)) else " "}$it${if (rtl) " " else space(transform(it))}$right" }
        val space2: (String, Boolean) -> String = { spacing, b -> (if (b) topFillIn else bottomFillIn).repeat((fullLength - spacing.length) / 2) }
        val topBottomText: (String, Boolean) -> String = { s, b ->
            if (s.length == 1) s.repeat(fullLength)
            else space2(s, b).let { spaced -> "$spaced$s${if ((fullLength - s.length) % 2 == 0) "" else (if (b) topFillIn else bottomFillIn)}$spaced" }
        }
        return "$topLeft${topBottomText(top, true)}$topRight\n$mid\n$bottomLeft${topBottomText(bottom, false)}$bottomRight"
    }

    private val toInfoString: (KCallable<*>) -> Pair<String, String> = { it.name to it.returnType.toString() }
    private inline fun <reified T : Any> getMethodNames(): List<Pair<String, String>> = T::class.java.kotlin.memberFunctions.map(toInfoString)
    private inline fun <reified T : Any> getPropertyNames(): List<Pair<String, String>> = T::class.java.kotlin.memberProperties.map(toInfoString)

    private fun doubleSpeak(s: String) = s.split("").joinToString("") { it.repeat(2) }
    //HHeelllloo  wwoorrlldd!!  ::))
    //HHeelllloo  wwoorrlldd!!  ::))
    private fun doubleSpeak2(s: String) = s.map { "$it$it" }.joinToString("")

    private fun String.doubleSpeaking() = map { "$it$it" }.joinToString("")

    private fun doubleSpeak3(s: String) = s.replace(".".toRegex(), "$0$0")

    private fun String.doubleSpeaking2() = replace(Regex("."), "$0$0")

    @Test
    fun doubled() {
        val helloWorld = "Hello World! How are you doing?"
        prettyLog(doubleSpeak3(helloWorld))
        prettyLog(helloWorld.doubleSpeaking2())
    }

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
    fun dslExample() {

        val html = createHTML(prettyPrint = true).html {
            body {
                +"Hello World"
            }
        }

        println(html)

        /*
        <html>
            <body>Hello World</body>
        </html>
        */

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

    private fun htTestTwo() =
        createHTML(true)
            .html {
                lang = "en"
                head {
                    title { +"""Music Quiz""" }
                    style {
                        +"""body {
            font-family: Open Sans, serif;
        }

        h1 {
            text-align: center;
        }

        #title {
            text-decoration: underline;
        }

        #quiz_container {
            text-align: center;
        }

        #quiz {
            text-indent: 10px;
            display: none;
        }

        .button {
            border: 4px solid;
            border-radius: 5px;
            width: fit-content;
            padding-left: 5px;
            padding-right: 5px;
            position: relative;
            float: right;
            background-color: #DCDCDC;
            color: black;
            margin: 0 2px 0 2px;
        }

        .infoText {
            border: 4px solid;
            border-radius: 5px;
            width: fit-content;
            padding-left: 5px;
            padding-right: 5px;
            position: relative;
            float: left;
            background-color: #DCDCDC;
            color: black;
            margin: 0 2px 0 2px;
        }

        .button.active {
            background-color: #F8F8FF;
            color: #525252;
        }

        button {
            position: relative;
            float: right;
        }

        .button a {
            text-decoration: none;
            color: black;
        }

        dd {
            display: block;
            margin-left: 40px;
            color: #aaaaaa;
        }

        dt {
            display: block;
            color: #aaaaaa;
        }

        dl {
            display: block;
            margin: 1em 0;
        }

        #container {
            width: 75%;
            margin: auto;
            padding: 0 25px 40px 10px;
            background-color: #1E90FF;
            border: 4px solid #B0E0E6;
            border-radius: 5px;
            color: #FFFFFF;
            font-weight: bold;
            box-shadow: 5px 5px 5px #888;
        }

        ul {
            list-style-type: none;
            padding: 0;
            margin: 0;
        }

        #next {
            display: none;
        }

        #prev {
            display: none;
        }

        #start {
            display: none;
        }

        table.darkTable {
            font-family: "Arial Black", Gadget, sans-serif;
            border: 2px solid #000000;
            background-color: #4A4A4A;
            width: 80%;
            height: 200px;
            text-align: center;
            border-collapse: collapse;
            border-radius: 5px;
            /*the 5 below is what I added*/
            margin-top: 20px;
            left: 10%;
            right: 10%;
            margin-left: auto;
            margin-right: auto;
        }

        table.darkTable td, table.darkTable th {
            border: 1px solid #4A4A4A;
            padding: 3px 2px;
        }

        table.darkTable tbody td {
            font-size: 13px;
            color: #E6E6E6;
        }

        table.darkTable tr:nth-child(even) {
            background: #888888;
        }

        table.darkTable thead {
            background: #000000;
            border-bottom: 3px solid #000000;
        }

        table.darkTable thead th {
            font-size: 15px;
            font-weight: bold;
            color: #E6E6E6;
            text-align: center;
            border-left: 2px solid #4A4A4A;
        }

        table.darkTable thead th:first-child {
            border-left: none;
        }

        table.darkTable tfoot {
            font-size: 12px;
            font-weight: bold;
            color: #E6E6E6;
            background: #000000;
            background: -moz-linear-gradient(top, #404040 0%, #191919 66%, #000000 100%);
            background: -webkit-linear-gradient(top, #404040 0%, #191919 66%, #000000 100%);
            background: linear-gradient(to bottom, #404040 0%, #191919 66%, #000000 100%);
            border-top: 1px solid #4A4A4A;
        }

        table.darkTable tfoot td {
            font-size: 12px;
        }"""
                    }
                    link {
                        rel = "stylesheet"
                        type = "text/css"
                        href = "https://fonts.googleapis.com/css?family=Open Sans"
                    }
                }
                body {
                    style = "background-color: #202124"
                    div {
                        id = "container"
                        div {
                            id = "title"
                            h1 { +"""Music Quiz""" }
                        }
                        br {
                        }
                        div {
                            id = "quiz_container"
                            div {
                                id = "quiz"
                            }
                        }
                        div(classes = "infoText") {
                            id = "quiz_count"
                            +"""0/0"""
                        }
                        div(classes = "button") {
                            id = "next"
                            a {
                                href = "#"
                                +"""Next"""
                            }
                        }
                        div(classes = "button") {
                            id = "prev"
                            a {
                                href = "#"
                                +"""Prev"""
                            }
                        }
                        div(classes = "button") {
                            id = "start"
                            a {
                                href = "#"
                                +"""Start Over"""
                            }
                        }
                        +"""<!-- <button class='' id='next'>Next</a></button>
    <button class='' id='prev'>Prev</a></button>
    <button class='' id='start'> Start Over</a></button> -->"""
                    }
                    div {
                        id = "highScoreList"
                    }
                    div(classes = "modal fade") {
                        id = "exampleModal"
                        tabIndex = "-1"
                        role = "dialog"
                        attributes["aria-labelledby"] = "exampleModalLabel"
                        attributes["aria-hidden"] = "true"
                        div(classes = "modal-dialog") {
                            role = "document"
                            div(classes = "modal-content") {
                                div(classes = "modal-header") {
                                    h5(classes = "modal-title") {
                                        id = "exampleModalLabel"
                                        +"""Choose an Artist"""
                                    }
                                    button(classes = "close") {
                                        type = ButtonType.button
                                        attributes["data-dismiss"] = "modal"
                                        attributes["aria-label"] = "Close"
                                        span {
                                            attributes["aria-hidden"] = "true"
                                            +"""&times;"""
                                        }
                                    }
                                }
                                div(classes = "modal-body") {
                                    div(classes = "input-group input-group-sm mb-3") {
                                        div(classes = "input-group-prepend") {
                                            span(classes = "input-group-text") {
                                                id = "image_changes"
                                                +"""Choose Artist"""
                                            }
                                        }
                                        input(classes = "form-control") {
                                            type = InputType.text
                                            id = "artist_choice"
                                            attributes["aria-label"] = "Small"
                                            attributes["aria-describedby"] = "inputGroup-sizing-sm"
                                        }
                                    }
                                }
                                div(classes = "modal-footer") {
                                    button(classes = "btn btn-secondary") {
                                        type = ButtonType.button
                                        id = "closeGame"
                                        attributes["data-dismiss"] = "modal"
                                        +"""Close"""
                                    }
                                    button(classes = "btn btn-primary") {
                                        type = ButtonType.button
                                        id = "startGame"
                                        attributes["data-dismiss"] = "modal"
                                        +"""Go!"""
                                    }
                                }
                            }
                        }
                    }
                    div(classes = "modal fade") {
                        id = "highScoreModal"
                        tabIndex = "-1"
                        role = "dialog"
                        attributes["aria-labelledby"] = "exampleModalLabel"
                        attributes["aria-hidden"] = "true"
                        div(classes = "modal-dialog") {
                            role = "document"
                            div(classes = "modal-content") {
                                div(classes = "modal-header") {
                                    h5(classes = "modal-title") {
                                        id = "highScoreLabel"
                                        +"""Score"""
                                    }
                                    button(classes = "close") {
                                        type = ButtonType.button
                                        attributes["data-dismiss"] = "modal"
                                        attributes["aria-label"] = "Close"
                                        span {
                                            attributes["aria-hidden"] = "true"
                                            +"""&times;"""
                                        }
                                    }
                                }
                                div(classes = "modal-body") {
                                    span(classes = "input-group-text") {
                                        id = "scoreArtist"
                                    }
                                    span(classes = "input-group-text") {
                                        id = "scoreScore"
                                    }
                                    div(classes = "input-group input-group-sm mb-3") {
                                        div(classes = "input-group-prepend") {
                                            span(classes = "input-group-text") {
                                                id = "scoreName"
                                                +"""Enter Name"""
                                            }
                                        }
                                        input(classes = "form-control") {
                                            type = InputType.text
                                            id = "highScoreName"
                                            attributes["aria-label"] = "Small"
                                            attributes["aria-describedby"] = "inputGroup-sizing-sm"
                                        }
                                    }
                                }
                                div(classes = "modal-footer") {
                                    button(classes = "btn btn-secondary") {
                                        type = ButtonType.button
                                        id = "noSubmit"
                                        attributes["data-dismiss"] = "modal"
                                        +"""Close"""
                                    }
                                    button(classes = "btn btn-primary") {
                                        type = ButtonType.button
                                        id = "submitScore"
                                        +"""Submit"""
                                    }
                                }
                            }
                        }
                    }
                    script {
                        src = "https://code.jquery.com/jquery-3.3.1.min.js"
                        integrity = "sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8="
                    }
                    link {
                        rel = "stylesheet"
                        href = "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
                        attributes["integrity"] =
                            "sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"
                        attributes["crossorigin"] = "anonymous"
                    }
                    script {
                        src = "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
                        integrity = "sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
                    }
                    script {
                        src =
                            "https://cdn.jsdelivr.net/npm/gasparesganga-jquery-loading-overlay@2.1.6/dist/loadingoverlay.min.js"
                    }
                    +"""<!--<script type="text/javascript" src='questions.json'></script>-->"""
                    script {
                        type = "text/javascript"
                        +"""(function () {

        function highScoreListSetup() {
            $.ajax({
                url: '/music/highScores.json',
                type: "GET",
                dataType: "html",
                success: function (json) {
                    //console.log(json);
                    $("#highList").remove();
                    $("#highScoreList").html(json);
                    /*$("#highList").remove();
                    const scoreList = document.createElement("dl");
                    scoreList.id = "highList";
                    for (let key in json) {
                        let item = document.createElement("dt");
                        item.innerText = key;
                        scoreList.appendChild(item);
                        for (let value in json[key]) {
                            let user = json[key][value];
                            console.log(user);
                            let input = document.createElement("dd");
                            input.innerText = user.name + " | " + user.score;
                            scoreList.appendChild(input);
                        }
                    }
                    $("#highScoreList").append(scoreList);*/
                }
            });
        }

        highScoreListSetup();

        function submitHighScore() {
            $("#highScoreModal").LoadingOverlay("show", {
                image: "",
                text: "Submitting..."
            });
            $.ajax({
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                url: '/music',
                type: "POST",
                dataType: "json",
                data: JSON.stringify({
                    name: $('#highScoreName').val(),
                    artist: $('#artist_choice').val(),
                    score: correct + "/" + questions.length
                }),
                success: function (json) {
                    let hsm = $("#highScoreModal");
                    hsm.LoadingOverlay("hide");
                    let text = json.submitted;
                    let displayText = "";
                    if (text) {
                        displayText = "Submitted Successful";
                    } else {
                        displayText = "Submitted Failed";
                    }
                    $.LoadingOverlay("show", {
                        image: "",
                        text: displayText
                    });
                    setTimeout(function () {
                        if (text) {
                            $('#highScoreModal').modal('hide');
                            highScoreListSetup();
                        }
                        $.LoadingOverlay("hide");
                    }, 2000);
                }
            });
        }

        function scoreModalSetup() {
            document.getElementById("scoreScore").innerText = correct + "/" + questions.length;
            document.getElementById("scoreArtist").innerText = $('#artist_choice').val();
        }

        let questions;

        let questionCounter = 0; //Tracks question number
        let selections = []; //Array containing user choices
        let correct = 0;
        const quiz = $("#quiz"); //Quiz div object

        function startGame() {
            $("#container").LoadingOverlay("show", {
                background: "#B0E0E6"
            });
            $.ajax({
                url: '/music/music_get_quiz_from=' + $('#artist_choice').val() + '.json',
                type: "GET",
                dataType: "json",
                success: function (json) {
                    $("#container").LoadingOverlay("hide");
                    questions = json;
                    $("#start").hide();
                    // Display initial question
                    displayNext();
                },
                error: function (a, b, c) {
                    $("#container").LoadingOverlay("hide");
                    alert("Something went wrong. Please try again")
                }
            });
        }

        function closeGame() {
            $("#start").show();
        }

        $('#exampleModal').modal('show');
        document.getElementById("startGame").onclick = startGame;
        document.getElementById("closeGame").onclick = closeGame;
        document.getElementById("submitScore").onclick = submitHighScore;

        $("#start").show();

        // Click handler for the 'next' button
        $("#next").on("click", function (e) {
            e.preventDefault();
            // Suspend click listener during fade animation
            if (quiz.is(":animated")) {
                return false;
            }
            choose();

            // If no user selection, progress is stopped
            if (isNaN(selections[questionCounter])) {
                alert("Please make a selection!");
            } else {
                questionCounter++;
                displayNext();
            }
        });

        // Click handler for the 'prev' button
        $("#prev").on("click", function (e) {
            e.preventDefault();

            if (quiz.is(":animated")) {
                return false;
            }
            choose();
            questionCounter--;
            displayNext();
        });

        // Click handler for the 'Start Over' button
        $("#start").on("click", function (e) {
            $('#exampleModal').modal('show');
            e.preventDefault();

            if (quiz.is(":animated")) {
                return false;
            }
            questionCounter = 0;
            selections = [];
            correct = 0;
            //displayNext();
            //$("#start").hide();
        });

        // Animates buttons on hover
        $(".button").on("mouseenter", function () {
            $(this).addClass("active");
        });
        $(".button").on("mouseleave", function () {
            $(this).removeClass("active");
        });

        // Creates and returns the div that contains the questions and
        // the answer selections
        function createQuestionElement(index) {
            const qElement = $("<div>", {
                id: "question"
            });

            const header = $("<h2>Question " + (index + 1) + ":</h2>");
            qElement.append(header);

            const question = $("<p>").append(questions[index].question);
            qElement.append(question);

            const radioButtons = createRadios(index);
            qElement.append(radioButtons);

            return qElement;
        }

        // Creates a list of the answer choices as radio inputs
        function createRadios(index) {
            const radioList = $("<ul style='text-align: left;display: inline-block'>");
            let item;
            let input = "";
            for (let i = 0; i < questions[index].choices.length; i++) {
                item = $("<li>");
                input = '<input type="radio" id="' + questions[index].choices[i] + '" name="answer" value=' + i + " />";
                let s = '<label for="' + questions[index].choices[i] + '">' + questions[index].choices[i] + '</label>';
                input += s;
                item.append(input);
                radioList.append(item);
            }
            return radioList;
        }

        // Reads the user selection and pushes the value to an array
        function choose() {
            selections[questionCounter] = +$('input[name="answer"]:checked').val();
        }

        // Displays next requested element
        function displayNext() {
            $('#quiz_count').text(questionCounter + "/" + questions.length);
            quiz.fadeOut(function () {
                $("#question").remove();

                if (questionCounter < questions.length) {
                    const nextQuestion = createQuestionElement(questionCounter);
                    quiz.append(nextQuestion).fadeIn();
                    if (!isNaN(selections[questionCounter])) {
                        $("input[value=" + selections[questionCounter] + "]").prop(
                            "checked",
                            true
                        );
                    }

                    // Controls display of 'prev' button
                    if (questionCounter === 1) {
                        $("#prev").show();
                    } else if (questionCounter === 0) {
                        $("#prev").hide();
                        $("#next").show();
                    }
                } else {
                    const scoreElem = displayScore();
                    quiz.append(scoreElem).fadeIn();
                    $("#next").hide();
                    $("#prev").hide();
                    $("#start").show();
                    scoreModalSetup();
                    $('#highScoreModal').modal('show');
                }
            });
        }

        // Computes score and returns a paragraph element to be displayed
        function displayScore() {
            const score = $("<p>", {id: "question"});

            let numCorrect = 0;
            for (let i = 0; i < selections.length; i++) {
                if (questions[i].choices[selections[i]] === questions[i].correctAnswer) {
                    numCorrect++;
                }
            }

            let aAndQ = "";
            for (let i = 0; i < selections.length; i++) {
                aAndQ += i + ') Your Pick: ' + questions[i].choices[selections[i]] + ' | Correct Answer: ' + questions[i].correctAnswer + "<br /><br />"
            }
            correct = numCorrect;
            score.append(
                "You got " +
                numCorrect +
                " questions out of " +
                questions.length +
                " right!!!<br /><br />" +
                aAndQ
            );
            return score;
        }
    })();"""
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
