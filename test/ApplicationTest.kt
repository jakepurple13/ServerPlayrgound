package com.example

import io.ktor.html.HtmlContent
import io.ktor.http.HttpStatusCode
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.test.Test

class ApplicationTest {

    @Test
    fun genericEnumTest() {
        randomEnum<Source>().apply { prettyLog(this) }
        Source.values().random().apply { prettyLog(this) }
        Source::class.random().apply { prettyLog(this) }

        randomEnum<ChatServer.MessageType>().apply { prettyLog(this) }
        ChatServer.MessageType.values().random().apply { prettyLog(this) }
        ChatServer.MessageType::class.random().apply { prettyLog(this) }



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
