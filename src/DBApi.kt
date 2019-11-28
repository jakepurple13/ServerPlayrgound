package com.example

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

//private const val dbPath = "/Users/jrein/Downloads/kotlin-examples-master/tutorials/mpp-iOS-Android/servertesting/resources/database/takeeight.db"
private const val dbPath = "resources/database/data17.db"
//private const val dbPath = "~/resources/database/datafive.db"
//private const val dbPath = "/Users/jrein/Downloads/kotlin-examples-master/tutorials/mpp-iOS-Android/servertesting/resources/database/moviesreal.db"

object DbSettings {
    val db by lazy {
        /*when {
            isDev -> Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC")
            isProd -> Database.connect(System.getenv("JDBC_DATABASE_URL"), driver = "org.postgresql.Driver")
            else -> Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC")
        }*/
        //Database.connect(System.getenv("JDBC_DATABASE_URL"), driver = "org.postgresql.Driver")
        Database.connect("jdbc:sqlite:$dbPath", "org.sqlite.JDBC")
    }
}

private const val varLength = 3072

object Shows : IntIdTable() {
    val name = text("show_name")
    val url = varchar("show_url", varLength).uniqueIndex()
}

class Show(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Show>(Shows)

    var name by Shows.name
    var url by Shows.url

    override fun toString(): String = "$name: $url"
}

object Episodes : IntIdTable() {
    val url = varchar("episode_url", varLength).uniqueIndex()
    val name = text("episode_name")
    val image = text("episode_image")
    val description = text("episode_des")
    val show = reference("episode_show", Shows)
}

class Episode(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Episode>(Episodes) {
        fun newEpisode(s: Show, episodeApi: EpisodeApi) = new {
            name = episodeApi.name
            description = episodeApi.description
            image = episodeApi.image
            url = episodeApi.source.url
            show = s
        }

        fun newEpisodes(s: EntityID<Int>, episodeApi: EpisodeApi) = Episodes.insert {
            it[name] = episodeApi.name
            it[description] = episodeApi.description
            it[image] = episodeApi.image
            it[url] = episodeApi.source.url
            it[show] = s
        } get Episodes.id
    }

    var url by Episodes.url
    var name by Episodes.name
    var image by Episodes.image
    var description by Episodes.description
    var show by Show referencedOn Episodes.show

    override fun toString(): String = "$name: $url | $description | $image | $show"
}

object EpisodeLists : IntIdTable() {
    val name = text("episodelist_name")
    val url = varchar("episodelist_url", varLength).uniqueIndex()
    val episode = reference("episodelist_episode", Episodes)
}

class EpisodeList(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EpisodeList>(EpisodeLists)

    var name by EpisodeLists.name
    var url by EpisodeLists.url
    var episode by Episode referencedOn EpisodeLists.episode

    override fun toString(): String = "$name: $url | ${episode.name}"
}

fun getAllShows(db: Database) {
    transaction(db) {
        SchemaUtils.create(Shows)

        val list = ShowApi.getAll().sortedBy { it.name }

        for (i in list) {
            Show.new {
                name = i.name
                url = i.url
            }
        }
    }
}

fun getAllShowsAndEpisodes(db: Database) = GlobalScope.launch {

    transaction(db) {

        SchemaUtils.create(Shows, Episodes, EpisodeLists)

        val list = ShowApi.getAll().sortedBy { it.name }

        for ((j, i) in list.withIndex()) {
            val s = Shows.insert {
                it[name] = i.name
                it[url] = i.url
            } get Shows.id
            try {
                val episodeApi = EpisodeApi(i, 30000)
                val e = Episode.newEpisodes(s, episodeApi)
                val epl = episodeApi.episodeList
                for (li in epl) {
                    EpisodeLists.insert {
                        it[name] = li.name
                        it[url] = li.url
                        it[episode] = e
                    }
                }
            } catch (e: Exception) {
                continue
            }

        }
    }
}

/**
 * Use this to get EVERYTHING and store it (except putlocker movies)
 */
fun createEverything(db: Database) = GlobalScope.launch {

    transaction(db) {

        SchemaUtils.create(Shows, Episodes, EpisodeLists)

        val list = ShowApi.getAll().sortedBy { it.name }

        addAllShowInformation(db, list)
    }
}

private fun addAllShowInformation(db: Database, list: List<ShowInfo>) {
    transaction(db) {
        for ((j, i) in list.withIndex()) {
            try {
                val s = if (Show.find { Shows.url eq i.url }.empty()) {
                    Shows.insertAndGetId {
                        it[name] = i.name
                        it[url] = i.url
                    }
                } else {
                    continue
                    //Show.find { Shows.url eq i.url }.toList()[0].id
                }
                val episodeApi = EpisodeApi(i, 30000)
                val e = if (Episode.find { Episodes.show eq s }.empty()) {
                    Episode.newEpisodes(s, episodeApi)
                } else {
                    Episode.find { Episodes.show eq s }.toList()[0].id
                }
                val epl = episodeApi.episodeList
                for (li in epl) {
                    if (EpisodeList.find { EpisodeLists.url eq li.url }.empty()) {
                        EpisodeLists.insert {
                            it[name] = li.name
                            it[url] = li.url
                            it[episode] = e
                        }
                    }
                }
            } catch (e: Exception) {
                commit()
                continue
            } finally {
                commit()
                /*commit()
                continue*/
            }
        }
    }
}

fun createEverything(db: Database, list: List<ShowInfo>) = GlobalScope.launch {

    transaction(db) {

        SchemaUtils.create(Shows, Episodes, EpisodeLists)

        prettyLog("Size is ${list.size}")

        addAllShowInformation(db, list)

    }
}

fun addNewShow(db: Database, show: ShowInfo) {
    transaction(db) {
        try {
            val episodeApi = EpisodeApi(show, 30000)
            val s = Shows.insert {
                it[name] = episodeApi.name
                it[url] = show.url
            } get Shows.id
            val e = Episode.newEpisodes(s, episodeApi)
            val epl = episodeApi.episodeList
            for (li in epl) {
                EpisodeLists.insert {
                    it[name] = li.name
                    it[url] = li.url
                    it[episode] = e
                }
            }
        } catch (e: Exception) {

        }
    }
}

fun updateShows(db: ShowDBApi) = GlobalScope.launch {
    transaction(db.db) {

        val list = ShowApi.getAllRecent()

        for ((j, i) in list.withIndex()) {
            val s = Show.find { Shows.url eq i.url }.toList()
            if (s.isEmpty()) {
                addNewShow(db.db, i)
                continue
            }
            try {
                val episodeApi = EpisodeApi(i, 30000)
                val e = Episode.find { Episodes.show eq s[0].id }.toList()[0]
                val epl = episodeApi.episodeList
                for (li in epl) {
                    if (EpisodeList.find { EpisodeLists.url eq li.url }.empty()) {
                        EpisodeLists.insert {
                            it[name] = li.name
                            it[url] = li.url
                            it[episode] = e.id
                        }
                    }
                }
            } catch (e: Exception) {
                continue
            }

        }

    }
}