package com.example

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ShowDBApi(val db: Database, val showList: List<ShowInfo>) {

    private val showDB = true

    fun getAll(): List<ShowInfo> {
        return when (showDB) {
            true -> {
                showList
            }
            false -> {
                var list: List<ShowInfo> = emptyList()
                transaction(db) {
                    list = Shows.selectAll().map { ShowInfo(it[Shows.name], it[Shows.url]) }.sortedBy { it.name }
                }
                list
            }
        }
    }

    fun getAlphabet(checkLevel: List<String>): List<EpisodeApiInfo> {
        return when (showDB) {
            true -> {
                showList.filter { it.name[0].toString() in checkLevel }.map {
                    EpisodeApiInfo(
                        it.name,
                        "Something went wrong",
                        it.url,
                        "Sorry, something went wrong"
                    )
                }
            }
            false -> {
                var list = listOf<EpisodeApiInfo>()
                transaction(db) {
                    list = Episodes.select {
                        try {
                            Episodes.name.substring(1, 1) inList checkLevel
                        } catch (e: Exception) {
                            Episodes.name neq Episodes.name
                        }
                    }.map {
                        EpisodeApiInfo(
                            it[Episodes.name],
                            it[Episodes.image],
                            it[Episodes.url],
                            it[Episodes.description]
                        )
                    }.sortedBy { it.name }
                }
                list
            }
        }
    }

    fun getType(type: String): List<ShowInfo> {
        return when (showDB) {
            true -> {
                showList.filter { it.url.contains(type) }
            }
            false -> {
                var list: List<ShowInfo> = emptyList()
                transaction(db) {
                    list = Shows.select { Shows.url like "%$type%" }.map { ShowInfo(it[Shows.name], it[Shows.url]) }
                        .sortedBy { it.name }
                }
                list
            }
        }
    }

    fun randomShow(): ShowInfo {
        return when (showDB) {
            true -> {
                showList.random()
            }
            false -> {
                var showInfo = ShowInfo("", "")
                transaction(db) {
                    showInfo = Shows.selectAll().map { ShowInfo(it[Shows.name], it[Shows.url]) }.random()
                }
                showInfo
            }
        }
    }

    fun getEpisodeInfo(name: String): EpisodeApiInfo? {
        var episode: EpisodeApiInfo? = null
        when (showDB) {
            true -> {
                val s = showList.find { it.url.contains(name.substring(1)) }
                if (s != null) {
                    val e = EpisodeApi(s)
                    episode = EpisodeApiInfo(
                        e.name,
                        e.image,
                        e.source.url,
                        e.description,
                        e.episodeList.map { EpListInfo(it.name, it.url) })
                }
            }
            false -> {
                val source = when (name[0]) {
                    'p' -> "putlocker"
                    'g' -> "gogoanime"
                    'a' -> "animetoon"
                    else -> ""
                }
                transaction(db) {
                    try {
                        val e = Episodes.select {
                            Episodes.url like "%$source%" and (Episodes.url like "%${name.substring(1)}%")
                        }.toList()

                        if (e.isNotEmpty()) {
                            val list = EpisodeLists.select { EpisodeLists.episode eq e[0][Episodes.id] }
                                .map { EpListInfo(it[EpisodeLists.name], it[EpisodeLists.url]) }
                            episode = EpisodeApiInfo(
                                e[0][Episodes.name],
                                e[0][Episodes.image],
                                e[0][Episodes.url],
                                e[0][Episodes.description],
                                list
                            )
                        }
                    } catch (ignored: Exception) {

                    }
                }
            }
        }
        return episode
    }

}