package com.example

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.freemarker.FreeMarkerContent
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlin.random.Random

data class QuizInfo(
    val questionUrl: String,
    val enterTitle: String,
    val modalTitle: String,
    val highScoreLink: String,
    val postHighScoreLink: String
)

suspend fun PipelineContext<Unit, ApplicationCall>.addQuiz(quizInfo: QuizInfo) {
    call.respond(FreeMarkerContent("quizgame.ftl", mapOf("data" to quizInfo)))
}

suspend fun <T> PipelineContext<Unit, ApplicationCall>.quiz(
    list: MutableList<T>,
    question: (T) -> String?,
    answers: (T) -> String
) {
    fun MutableList<T>.randomRemoveAction(block: (T) -> String) = block(removeAt(Random.nextInt(0, size)))
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
                list.randomRemoveAction(answers),
                list.randomRemoveAction(answers),
                list.randomRemoveAction(answers)
            ).shuffled(),
            answers(answer)
        )
        qList += quizQuestions
    }
    call.respond(qList)
}