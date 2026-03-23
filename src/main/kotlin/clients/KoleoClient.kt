package pl.wiktorlacki.clients

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import pl.wiktorlacki.models.*

class KoleoClient : KoinComponent {

    companion object {
        private const val BASE_URL = "https://api.koleo.pl/v2/main"
    }

    private val client = HttpClient(OkHttp) {
        engine {
            addNetworkInterceptor(Interceptor { chain ->
                val response: Response = chain.proceed(chain.request())

                if (response.code == 304) { // koleo caching hack
                    return@Interceptor response.newBuilder()
                        .header("Vary", "Origin,Accept-Encoding")
                        .build()
                }
                response
            })
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
        }

        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(HttpCache)

        defaultRequest {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (X11; Linux x86_64; rv:148.0) Gecko/20100101 Firefox/148.0")
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.AcceptLanguage, "pl")
            header("x-koleo-client", "Nuxt-90a70c2")
            header("x-koleo-version", "2")
            header("DNT", "1")
            header("Sec-GPC", "1")
            header("accept-eol-response-version", "2")

        }
    }
    private val logger = LoggerFactory.getLogger(javaClass)

    private suspend inline fun <reified T> request(
        httpMethod: HttpMethod,
        endpoint: String,
        params: Map<String, String> = emptyMap(),
        requestBody: Any? = null
    ): T? {
        val url = "$BASE_URL/$endpoint"

        val response = client.request(url) {
            method = httpMethod

            params.forEach { (key, value) ->
                parameter(key, value)
            }

            requestBody?.let {
                contentType(ContentType.Application.Json)
                setBody(it)
            }
        }

        if (!response.status.isSuccess()) {
            logger.warn("Error: ${response.status} $response ${response.request.content}")
            return null
        }

        return response.body()
    }

    private suspend inline fun <reified T> get(endpoint: String, params: Map<String, String> = emptyMap()) =
        request<T>(HttpMethod.Get, endpoint, params = params)

    private suspend inline fun <reified T> post(
        endpoint: String,
        body: Any? = null
    ) = request<T>(HttpMethod.Post, endpoint, requestBody = body)

    private suspend inline fun <reified T> put(endpoint: String) = request<T>(HttpMethod.Put, endpoint)

    suspend fun findStationsByName(query: String): StationsResponse? {
        return get("livesearch", mapOf("q" to query))
    }

    suspend fun findStationNameById(id: String): StationDetails? {
        return get("stations/by_id/${id}")
    }

    suspend fun findConnections(departureStationId: Int, arrivalStationId: Int, date: String): List<Connection>? {
        return post(
            "eol_connections/search",
            body = ConnectionRequest(
                departure_after = date,
                start_id = departureStationId,
                end_id = arrivalStationId,
                only_direct = false
            )
        )
    }

    suspend fun findConnection(connectionId: String): Connection? {
        return get("eol_connections/$connectionId")
    }

    suspend fun getRealConnectionId(uuid: String): String? {
        val response = put<ConnectionIdResponse>("eol_connections/$uuid/connection_id")
        return response?.connectionId
    }

    suspend fun getSeatAvailability(
        connectionId: String,
        trainNr: Int,
        placeType: Int = 5
    ): SeatAvailabilityResponse? {
        return get("seats_availability/$connectionId/$trainNr/$placeType")
    }
}