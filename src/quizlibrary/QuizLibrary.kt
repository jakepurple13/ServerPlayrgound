package com.example.quizlibrary


import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlin.random.Random

internal fun <T> MutableList<T>.randomRemove(): T = removeAt(Random.nextInt(0, size))

data class QuizQuestions(val question: String, val choices: List<String>, val correctAnswer: String)

data class QuizInfo(
    val questionUrl: String,
    val enterTitle: String,
    val modalTitle: String,
    val highScoreLink: String = "",
    val postHighScoreLink: String = ""
)

suspend fun PipelineContext<Unit, ApplicationCall>.addQuiz(quizInfo: QuizInfo) {
    call.respond(FreeMarkerContent("quizgame.ftl", mapOf("data" to quizInfo)))
}

suspend fun <T> PipelineContext<Unit, ApplicationCall>.quiz(
    list: MutableList<T>,
    question: (T) -> String?,
    answers: (T) -> String
) {
    val qList = mutableListOf<QuizQuestions>()
    while (list.size > 4) {
        val answer = list.randomRemove()
        val questionText = try {
            question(answer)
        } catch (e: Exception) {
            null
        }
        val answerText = answers(answer)
        val actualQuestionText = if (questionText.isNullOrBlank()) {
            "Something went wrong, here's a freebie.\n$answerText"
        } else {
            questionText
        }
        val quizQuestions = QuizQuestions(
            actualQuestionText,
            listOf(
                answerText,
                answers(list.randomRemove()),
                answers(list.randomRemove()),
                answers(list.randomRemove())
            ).shuffled(),
            answerText
        )
        qList += quizQuestions
    }
    call.respond(qList)
}