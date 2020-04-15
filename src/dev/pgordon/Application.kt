package dev.pgordon

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.BrowserUserAgent
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel.HEADERS
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application.HalJson
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.http.contentType
import io.ktor.request.path
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.slf4j.event.Level


const val LOCAL_ANKI_URL = "http://localhost:8765"

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        gson {}
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
            accept(HalJson)
            accept(ContentType.parse("text/json")) //anki specific type
        }
        install(Logging) { level = HEADERS }
        BrowserUserAgent() // install default browser-like user-agent
    }

    val storage = Storage()
    val gson = Gson()

    routing {
        static {
            resource("/", "index.html")
            resources("css")
            resources("js")
        }
        get("/health") { call.respondText("OK") }


        get("/stats") { call.respond(storage.stats) }
        post("/stats") {
            val statsDTO = gson.fromJson(call.receiveText(), StatsDTO::class.java)
            storage.stats = statsDTO.stats
            call.respond(storage.stats)
        }

        // fetching stats from local anki instance and then delivering them to provided https://$host/stats
        get("/anki/stats") {
            val decks = listOf("Italiano", "German")
            val decksStats = decks.map { deckName ->
                val cards = client.post<JsonObject> {
                    url(LOCAL_ANKI_URL)
                    contentType(Json)
                    body = AnkiRequest("findCards", mapOf("query" to JsonPrimitive("deck:$deckName")))
                }.asJsonObject["result"]
                val cardsInfoJson = client.post<JsonObject> {
                    url(LOCAL_ANKI_URL)
                    contentType(Json)
                    body = AnkiRequest("cardsInfo", mapOf("cards" to cards))
                }

                with(cardsInfoJson["result"].asJsonArray) {
                    val maturedCards = this.count { it.asJsonObject["interval"].asNumber.toInt() >= 21 }
                    val newCards = this.count { it.asJsonObject["interval"].asNumber.toInt() == 0 }
                    val seenCards = this.count() - maturedCards - newCards
                    deckName to DeckStats(newCards, seenCards, maturedCards)
                }
            }.toMap()

            storage.stats = decksStats

            val dto = StatsDTO(stats = decksStats)
            kotlin.runCatching {
                val host = call.request.queryParameters["url"] ?: "http://localhost:8080"
                client.post<JsonObject> {
                    url("$host/stats")
                    contentType(Json)
                    body = dto
                }
            }

            call.respond(dto)
        }
    }
}


data class StatsDTO(val stats: Map<String, DeckStats>)
data class DeckStats(
    val new: Int,     //not yet seen cards
    val learned: Int, //seen and learned cards
    val matured: Int  //seen and learned cards which has due time more than 21 day
)
data class AnkiRequest(
    val action: String,
    val params: Map<String, JsonElement>,
    val version: Int = 6
)