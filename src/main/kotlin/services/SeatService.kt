package pl.wiktorlacki.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.wiktorlacki.clients.KoleoClient
import pl.wiktorlacki.models.*
import java.time.OffsetDateTime

class SeatService : KoinComponent {
    private val client by inject<KoleoClient>()
    private val scoringEngine: SeatScoringEngine by inject<SeatScoringEngine>()

    private val BLACKLISTED_BRAND_IDS = setOf(
        3 // Polregio no reservation
    )

    suspend fun findBestSeat(connectionId: String): SeatSearchResult {
        val connection = client.findConnection(connectionId) ?: error("Connection $connectionId not found.")
        val trainLegs = connection.legs.filterIsInstance<TrainLeg>()

        val result = withContext(Dispatchers.IO) {
            trainLegs.map { leg ->
                async {
                    if (leg.commercialBrandId in BLACKLISTED_BRAND_IDS) {
                        return@async TrainSeatGroup(
                            trainName = leg.trainFullName,
                            seatsList = emptyList()
                        )
                    }

                    val segments = leg.stopsInLeg.windowed(size = 2)
                    val seatAvailability = calculateSeatAvailability(leg.trainNr, segments)
                    val segmentsDurations = calculateJourneySegmentsDuration(segments)

                    val bestSeatsInLeg = scoringEngine.calculateTopSeats(seatAvailability, segmentsDurations)
                    val stationsIds = bestSeatsInLeg.flatMap {
                        listOf(
                            leg.stopsInLeg[it.startIndex].stationId,
                            leg.stopsInLeg[it.endIndex].stationId
                        )
                    }.distinct()

                    val stationsNamesById = withContext(Dispatchers.IO) {
                        stationsIds
                            .map { async { it to client.findStationNameById(it.toString())?.name } }
                            .awaitAll()
                            .toMap()
                    }

                    val formattedSeats = bestSeatsInLeg.map {
                        SeatDisplay(
                            seatNumber = it.seat.seatNr,
                            seatCarriage = it.seat.carriageNr,
                            duration = it.duration,
                            startStop = stationsNamesById[leg.stopsInLeg[it.startIndex].stationId] ?: "Nieznany",
                            endStop = stationsNamesById[leg.stopsInLeg[it.endIndex].stationId] ?: "Nieznany",
                        )
                    }

                    TrainSeatGroup(
                        trainName = leg.trainFullName,
                        seatsList = formattedSeats
                    )
                }
            }
        }

        return SeatSearchResult(result.awaitAll())
    }


    private fun calculateJourneySegmentsDuration(segments: List<List<Stop>>) =
        segments.map { (currentStop, nextStop) ->
            val startInstant = OffsetDateTime.parse(currentStop.departure)
            val endInstant = OffsetDateTime.parse(nextStop.arrival)

            endInstant.toEpochSecond() - startInstant.toEpochSecond()
        }

    private suspend fun calculateSeatAvailability(trainNr: Int, segments: List<List<Stop>>): Map<Seat, List<Int>> {
        // check if direct route has availability, if not check each segment separately and merge results
        val firstStation = segments.first().first()
        val lastStation = segments.last().last()

        val route = findConnection(firstStation.stationId, lastStation.stationId, firstStation.departure)
            ?: error("No route found for segment ${firstStation.stationId} -> ${lastStation.stationId}")
        val realRouteId = client.getRealConnectionId(route.uuid)
        val directSeatAvailability = client.getSeatAvailability(realRouteId!!, trainNr, 5)

        if (directSeatAvailability?.seats?.any { it.state == "FREE" } ?: false) {
            val seatAvailability = mutableMapOf<Seat, List<Int>>()
            val allSegments = 0.until(segments.size).toList()
            directSeatAvailability.seats.forEach {
                seatAvailability[it] = allSegments
            }

            println("Direct availability found for train $trainNr on route ${firstStation.stationId} -> ${lastStation.stationId}, skipping segment checks.")
            return seatAvailability
        }

        val segmentSeatAvailability = withContext(Dispatchers.IO) {
            segments.withIndex()
                .map { (segmentId, segment) ->
                    async {
                        val (currentStop, nextStop) = segment
                        val route = findConnection(currentStop.stationId, nextStop.stationId, currentStop.departure)
                            ?: error("No route found for segment ${currentStop.stationId} -> ${nextStop.stationId}")
                        val realRouteId = client.getRealConnectionId(route.uuid)
                        val segmentSeatAvailability = client.getSeatAvailability(realRouteId!!, trainNr, 5)

                        segmentId to segmentSeatAvailability
                    }
                }
                .awaitAll()
        }

        val seatAvailability = mutableMapOf<Seat, List<Int>>()
        segmentSeatAvailability.forEach { (segment, availability) ->
            availability?.seats
                ?.filter { it.state == "FREE" }
                ?.forEach {
                    seatAvailability[it] = (seatAvailability[it] ?: emptyList()) + segment
                }
        }

        segmentSeatAvailability
            .filter { it.second == null || it.second!!.seats == null }
            .forEach { (segment, _) ->
                seatAvailability.keys.forEach { seat ->
                    seatAvailability[seat] = (seatAvailability[seat] ?: emptyList()) + segment
                }
            }

        return seatAvailability
    }

    private suspend fun findConnection(departureStationId: Int, arrivalStationId: Int, date: String): Connection? {
        val routes =
            client.findConnections(departureStationId, arrivalStationId, date.split("+")[0]) //dumb ahh koleo api
        return routes?.firstOrNull()
    }
}