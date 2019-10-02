package com.example

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext

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
        val actualQuestionText = if (questionText.isNullOrBlank()) {
            "Something went wrong, here's a freebie.\n${answers(answer)}"
        } else {
            questionText
        }
        val quizQuestions = QuizQuestions(
            actualQuestionText,
            listOf(
                answers(answer),
                answers(list.randomRemove()),
                answers(list.randomRemove()),
                answers(list.randomRemove())
            ).shuffled(),
            answers(answer)
        )
        qList += quizQuestions
    }
    call.respond(qList)
}