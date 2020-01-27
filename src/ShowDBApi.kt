package com.example

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ShowDBApi(val db: Database, val showList: List<ShowInfo>) {

    private val showDB = false

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

    fun getFullShowInfo() = transaction(db) {
        Shows.selectAll().mapNotNull {
            try {
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
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.showInfoCom.name }
    }

    fun getFullShowInfos() = transaction(db) {
        Episodes.selectAll().mapNotNull { ep ->
            try {
                /*FullShowInfo(
                        Shows.select { Shows.id eq ep[Episodes.show] }.map { ShowInfoCom(it[Shows.name], it[Shows.url]) }[0],
                        ShowInformation(
                                ep[Episodes.name],
                                ep[Episodes.image],
                                ep[Episodes.url],
                                ep[Episodes.description],
                                EpisodeLists.select { EpisodeLists.episode eq ep[Episodes.id] }
                                    .map { EpisodeInfo(it[EpisodeLists.name], it[EpisodeLists.url]) }
                        )
                )*/
                ShowInformation(
                        ep[Episodes.name],
                        ep[Episodes.image],
                        ep[Episodes.url],
                        ep[Episodes.description],
                        EpisodeLists.select { EpisodeLists.episode eq ep[Episodes.id] }
                            .map { EpisodeInfo(it[EpisodeLists.name], it[EpisodeLists.url]) }
                )
            } catch (e: Exception) {
                null
            }
        }.filter { it.name.isNotBlank() && !it.description.equals("Sorry, an error has occurred", true) }.sortedBy { it.name }
    }

    fun getAlphabet(checkLevel: List<String>): List<EpisodeApiInfo> {
        return when (showDB) {
            true -> {
                val checker: (ShowInfo) -> Boolean = if (checkLevel.contains("0")) {
                    { !it.name[0].isLetter() }
                } else {
                    { it.name[0].toString() in checkLevel }
                }
                showList.filter(checker).map {
                    EpisodeApiInfo(
                            it.name,
                            "Something went wrong",
                            it.url,
                            "Sorry, something went wrong"
                    )
                }
            }
            false -> {
                val checker: (EpisodeApiInfo) -> Boolean = if (checkLevel.contains("0")) {
                    { !(it.name.firstOrNull()?.isLetter() ?: true) }
                } else {
                    { it.name.firstOrNull()?.let { it.toString() in checkLevel } ?: false }
                }
                var list = listOf<EpisodeApiInfo>()
                transaction(db) {
                    list = Episodes.selectAll().map {
                        EpisodeApiInfo(
                                it[Episodes.name],
                                it[Episodes.image],
                                it[Episodes.url],
                                it[Episodes.description]
                        )
                    }.filter(checker)
                    /*list = Episodes.select {
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
                    }*/
                }
                list
            }
        }.sortedBy { it.name }
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
                    /*val e = EpisodeApi(s).map {
                        EpisodeApiInfo(name, image, source.url, description, episodeList.map { EpListInfo(it.name, it.url) })
                    }
                    episode = EpisodeApiInfo(
                        e.name,
                        e.image,
                        e.source.url,
                        e.description,
                        e.episodeList.map { EpListInfo(it.name, it.url) })*/
                    episode = EpisodeApi(s).map {
                        EpisodeApiInfo(
                                name,
                                image,
                                source.url,
                                description,
                                episodeList.map { EpListInfo(it.name, it.url) },
                                genres)
                    }
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