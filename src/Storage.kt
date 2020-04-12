package dev.pgordon

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection.TRANSACTION_SERIALIZABLE

val path = "/Users/pavelgordon/Library/Application Support/Anki2/Pavel/collection.anki2"

class Storage {
    var stats: Map<String, DeckStats> = mutableMapOf()
    fun persistStats(stats: Map<String, DeckStats>) {
        this.stats = stats
        println()
    }

//    fun fetchFromDb(): Map<String, DeckStats> {
//        var stats: Map<String, DeckStats> = mutableMapOf()
//        transaction {
//            addLogger(StdOutSqlLogger)
//            val obj = Gson().fromJson(Col.all().first().decks, JsonObject::class.java)
//            val mapOfDeckIdToDeck = obj
//                .entrySet().map { it.key to it.value.asJsonObject["name"].asString }.toMap()
//
//            val cardsMappedByDeckName = Card.all().groupBy { it -> it.did }
//                .map { mapOfDeckIdToDeck.getOrDefault(it.key.toString(), "defaultname1") to it.value }.toMap()
//
//            stats = cardsMappedByDeckName.map { it ->
//                it.key to DeckStats(
//                    new = it.value.count { it.type == 0 },
//                    learned = it.value.count { it.type == 2 },
//                    matured = it.value.count { it.type == 1 }//actually idk
//                )
//            }.toMap()
//        }
//
//        return stats
//
//    }

    var db: Database = Database.connect("jdbc:sqlite:$path").also {
        TransactionManager.manager.defaultIsolationLevel =
            TRANSACTION_SERIALIZABLE // Or Connection.TRANSACTION_READ_UNCOMMITTED
    }

    fun restartConnection(path: String) {
        Database.connect("jdbc:sqlite:$path").also {
            TransactionManager.manager.defaultIsolationLevel =
                TRANSACTION_SERIALIZABLE // Or Connection.TRANSACTION_READ_UNCOMMITTED
        }
    }

}