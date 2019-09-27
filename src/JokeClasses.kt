package com.example

// result generated from /json

data class JokeBase(val success: Success?, val contents: Contents?)

data class Contents(val jokes: List<Jokes451967594>?, val copyright: String?)

data class Joke(val title: String?, val lang: String?, val length: String?, val clean: String?, val racial: String?, val id: String?, val text: String?)

data class Jokes451967594(val description: String?, val language: String?, val background: String?, val category: String?, val date: String?, val joke: Joke?)

data class Success(val total: Number?)

fun getJoke() = getAPIRequest<JokeBase>("https://api.jokes.one/jod")?.contents?.jokes?.get(0)?.joke