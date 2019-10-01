package com.example

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

fun randomName(): String = listOf(
    "Austin", "Bob", "Chuck", "Darren", "Eric", "Frank", "Gary", "Harry", "Jacob", "Kevin", "Logan",
    "Martin", "Norman", "Odin", "Peter", "Ryan", "Steven", "Tom", "Way", "Yosef",
    "Anna", "Bobbette", "Cherry", "Dana", "Erica", "Freja", "Gina", "Hana", "Jenna", "Kari", "Lynda",
    "Maya", "Nora", "Odina", "Patrica", "Rachel", "Sherry", "Tanya", "Whitney", "Yerenny",
    "Chris", "Daniel", "Jordan"
).random()

inline fun <reified T : Enum<T>> randomEnum() = enumValues<T>().random()

inline fun <reified T : Enum<T>> KClass<T>.random() = enumValues<T>().random()

fun <T> MutableList<T>.randomRemove(): T {
    return removeAt(Random.nextInt(0, size))
}

fun Any.toJson(): String = Gson().toJson(this)

fun Any.toPrettyJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(this)

val envKind get() = System.getenv("KTOR_ENV")
val isDev get() = envKind == "dev"
val isProd get() = envKind != "dev"

fun PipelineContext<Unit, ApplicationCall>.isPrivateApi(): Boolean = call.request.local.port == 9090

suspend fun timeAction(block: () -> Job): Pair<Long, Long> {
    val start = System.currentTimeMillis()
    val time = measureTimeMillis {
        block().join()
    }
    return Pair(start, time)
}

fun prettyLog(msg: Any?) {
    //the main message to be logged
    var logged = msg.toString()
    //the arrow for the stack trace
    val arrow = "${9552.toChar()}${9655.toChar()}\t"
    //the stack trace
    val stackTraceElement = Thread.currentThread().stackTrace

    val elements = listOf(*stackTraceElement)
    val wanted = elements.filter { it.className.contains("example") && !it.methodName.contains("prettyLog") }

    var loc = "\n"

    for (i in wanted.indices.reversed()) {
        val fullClassName = wanted[i].className
        //get the method name
        val methodName = wanted[i].methodName
        //get the file name
        val fileName = wanted[i].fileName
        //get the line number
        val lineNumber = wanted[i].lineNumber
        //add this to location in a format where we can click on the number in the console
        loc += "$fullClassName.$methodName($fileName:$lineNumber)"

        if (wanted.size > 1 && i - 1 >= 0) {
            val typeOfArrow: Char =
                if (i - 1 > 0)
                    9568.toChar() //middle arrow
                else
                    9562.toChar() //ending arrow
            loc += "\n\t$typeOfArrow$arrow"
        }
    }

    logged += loc

    println(SimpleDateFormat("MM/dd hh:mm:ss.SSS a").format(System.currentTimeMillis())!! + ": " + logged + "\n")
}