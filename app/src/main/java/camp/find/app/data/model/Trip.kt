package camp.find.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TripSummary(
    val id: String,
    val slug: String,
    val name: String,
    val summary: String,
    val days: Int,
    val km: Double,
    val heroPath: String? = null,
    val spotCount: Int,
)

@Serializable
data class TripsResponse(val trips: List<TripSummary>)
