package pl.wiktorlacki.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

@Serializable
data class SeatDisplay(
    val seatNumber: Int,
    val seatCarriage: Int,
    val duration: Long,
    val startStop: String,
    val endStop: String
)

@Serializable
data class TrainSeatGroup(
    val trainName: String,
    val seatsList: List<SeatDisplay>
)

@Serializable
data class SeatSearchResult(
    val seats: List<TrainSeatGroup>
)

@Serializable
data class ConnectionRequest(
    val departure_after: String,
    val start_id: Int,
    val end_id: Int,
    val only_direct: Boolean
)

@Serializable
data class StationDetails(
    val id: Int,
    val name: String,
    @SerialName("name_slug") val nameSlug: String,
    val latitude: Double,
    val longitude: Double,
    val hits: Int,
    val ibnr: Int? = null,
    val city: String,
    val region: String,
    val country: String,
    @SerialName("localised_name") val localisedName: String? = null,
    @SerialName("is_group") val isGroup: Boolean,
    @SerialName("has_announcements") val hasAnnouncements: Boolean,
    @SerialName("is_nearby_station_enabled") val isNearbyStationEnabled: Boolean,
    @SerialName("is_livesearch_displayable") val isLivesearchDisplayable: Boolean,
    val type: String,
    @SerialName("time_zone") val timeZone: String,
    @SerialName("transport_mode") val transportMode: String,
    @SerialName("attribute_definition_id") val attributeDefinitionId: String? = null
)

@Serializable
data class StationsResponse(
    val stations: List<Station>
)

@Serializable
data class Station(
    val id: Int,
    val name: String,
    @SerialName("name_slug") val nameSlug: String,
    val ibnr: Int? = null,
    @SerialName("localised_name") val localisedName: String,
    @SerialName("on_demand") val onDemand: Boolean,
    val type: String
)


@Serializable
data class Connection(
    val uuid: String,
    @SerialName("eol_response_version") val eolResponseVersion: Int,
    val departure: String,
    val arrival: String,
    @SerialName("origin_station_id") val originStationId: Int,
    @SerialName("destination_station_id") val destinationStationId: Int,
    val duration: Int,
    val changes: Int,
    val constrictions: List<String> = emptyList(),
    val legs: List<Leg>
)

@Serializable
@SerialName("walk_leg")
data class WalkLeg(
    @SerialName("origin_station_id") val originStationId: Int,
    @SerialName("destination_station_id") val destinationStationId: Int,
    val departure: String,
    val arrival: String,
    val duration: Int,
    @SerialName("footpath_duration") val footpathDuration: Int
) : Leg()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("leg_type")
sealed class Leg

@Serializable
@SerialName("train_leg")
data class TrainLeg(
    @SerialName("train_id") val trainId: Int,
    @SerialName("train_nr") val trainNr: Int,
    @SerialName("train_name") val trainName: String,
    @SerialName("train_full_name") val trainFullName: String,
    @SerialName("operating_day") val operatingDay: String,
    @SerialName("commercial_brand_id") val commercialBrandId: Int,
    @SerialName("internal_brand_id") val internalBrandId: Int,
    val constrictions: List<String> = emptyList(),
    @SerialName("train_attributes") val trainAttributes: List<TrainAttribute>,
    @SerialName("origin_station_id") val originStationId: Int,
    @SerialName("destination_station_id") val destinationStationId: Int,
    val departure: String,
    val arrival: String,
    val duration: Int,
    @SerialName("departure_platform") val departurePlatform: String,
    @SerialName("departure_track") val departureTrack: String,
    @SerialName("arrival_platform") val arrivalPlatform: String,
    @SerialName("arrival_track") val arrivalTrack: String,
    @SerialName("stops_before_leg") val stopsBeforeLeg: List<Stop> = emptyList(),
    @SerialName("stops_in_leg") val stopsInLeg: List<Stop> = emptyList(),
    @SerialName("stops_after_leg") val stopsAfterLeg: List<Stop> = emptyList()
) : Leg()

@Serializable
@SerialName("station_change_leg")
data class StationChangeLeg(
    val duration: Int,
    @SerialName("station_id") val stationId: Int
) : Leg()

@Serializable
data class TrainAttribute(
    @SerialName("attribute_definition_id") val attributeDefinitionId: Int,
    val annotation: String
)

@Serializable
data class Stop(
    @SerialName("station_id") val stationId: Int,
    val arrival: String,
    val departure: String,
    @SerialName("commercial_brand_id") val commercialBrandId: Int,
    @SerialName("internal_brand_id") val internalBrandId: Int,
    @SerialName("train_nr") val trainNr: Int,
    val platform: String,
    val track: String,
    @SerialName("for_alighting") val forAlighting: Boolean,
    @SerialName("for_boarding") val forBoarding: Boolean,
    @SerialName("request_stop") val requestStop: Boolean
)

@Serializable
data class ConnectionIdResponse(
    @SerialName("connection_id") val connectionId: String
)


@Serializable
data class SeatAvailabilityResponse(
    @SerialName("special_compartment_types") val specialCompartmentTypes: List<JsonObject> = emptyList(),
    val seats: List<Seat>?
)

@Serializable
data class Seat(
    @SerialName("carriage_nr") val carriageNr: Int,
    @SerialName("seat_nr") val seatNr: Int,
    @SerialName("special_compartment_type_id") val specialCompartmentTypeId: Int? = null,
    val state: String,
    @SerialName("placement_id") val placementId: Int? = null
)