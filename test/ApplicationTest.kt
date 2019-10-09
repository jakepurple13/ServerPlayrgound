package com.example

import io.ktor.html.HtmlContent
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.stream.createHTML
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
