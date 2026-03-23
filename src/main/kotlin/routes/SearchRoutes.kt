package pl.wiktorlacki.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import org.koin.ktor.ext.get
import pl.wiktorlacki.clients.KoleoClient
import pl.wiktorlacki.services.SeatService
import kotlin.text.toIntOrNull

fun Routing.searchRoutes() {
    get("/search/seat/{id}") {
        val connectionId =
            call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound, "Connection not found.")
        val result = get<SeatService>().findBestSeat(connectionId)
        call.respond(result)
    }
    get("/search/station/{name}") {
        val stationName =
            call.parameters["name"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Station name is required."
            )
        val stations = get<KoleoClient>().findStationsByName(stationName)?.stations ?: emptyList()
        call.respond(stations)
    }
    get("/search/connection") {
        val startStation =
            call.queryParameters["start"]?.toIntOrNull() ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Start station is required."
            )
        val endStation =
            call.queryParameters["end"]?.toIntOrNull() ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "End station is required."
            )
        val date = call.queryParameters["date"] ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            "Date is required."
        )
        val stations = get<KoleoClient>().findConnections(startStation, endStation, date) ?: emptyList()
        call.respond(stations)
    }
}