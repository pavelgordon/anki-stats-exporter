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
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.content.default
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.http.contentType
import io.ktor.request.path
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import java.lang.Thread.sleep

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

val STORAGE: Storage = Storage()

data class Request(
    val action: String,
    val params: Map<String, JsonElement>,
    val version: Int = 6
)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
            accept(ContentType.Application.HalJson)
            accept(ContentType.Application.HalJson)
            accept(ContentType.parse("text/json"))
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
        BrowserUserAgent() // install default browser-like user-agent
        // install(UserAgent) { agent = "some user agent" }
    }


    routing {


        // Static feature. Try to access `/static/ktor_logo.svg`
        static {
            resource("/", "index.html")
            resources("css")
            resources("js")
        }
        //endpoints for local deploy
        get("/anki/stats") {
            val decks = listOf("Italiano", "German")
            val deckStats = decks.map { deck ->
                val cards = client.post<JsonObject> {
                    url("http://localhost:8765")
                    contentType(ContentType.Application.Json)
                    body = Request("findCards", mapOf("query" to JsonPrimitive("deck:$deck")))
                }
                val cardsInfoJson = client.post<JsonObject> {
                    url("http://localhost:8765")
                    contentType(ContentType.Application.Json)
                    body = Request("cardsInfo", mapOf("cards" to cards.asJsonObject["result"]))
                }

                with(cardsInfoJson["result"].asJsonArray) {
                    val maturedCards = this.count {
                        it.asJsonObject["interval"].asNumber.toInt() >= 21
                    }
                    val newCards = this.count {
                        it.asJsonObject["interval"].asNumber.toInt() == 0
                    }
                    val seenCards = this.count() - maturedCards - newCards
                    deck to DeckStats(newCards, seenCards, maturedCards)
                }
            }.toMap()

            val dto = StatsDTO(stats = deckStats)
            kotlin.runCatching {
                val url = call.request.queryParameters["url"]?:"http://localhost:8080"

                val result = client.post<JsonObject> {
                    url("$url/stats")
                    contentType(ContentType.Application.Json)
                    body = dto
                }
                println("result $result")
            }
            call.respond(dto)
        }


        // endpoints for remote deploy
        get("/stats") {
            call.respond(STORAGE.stats)
        }
        post("/stats") {
            val text = call.receiveText()
            val statsDTO = Gson().fromJson(text, StatsDTO::class.java)
            STORAGE.persistStats(statsDTO.stats)
            call.respond(STORAGE.stats)
        }
    }

    if (testing)
        launch {
            sleep(2000)
            // Sample for making a HTTP Client request
            /*
            val message = client.post<JsonSampleClass> {
                url("http://127.0.0.1:8080/path/to/endpoint")
                contentType(ContentType.Application.Json)
                body = JsonSampleClass(hello = "world")
            }
            */

            val message = client.get<String> {
                url("http://127.0.0.1:8080/anki/stats")
            }
            println(message)
        }
}


data class StatsDTO(val stats: Map<String, DeckStats>)
data class DeckStats(val new: Int, val learned: Int, val matured: Int)


class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()


