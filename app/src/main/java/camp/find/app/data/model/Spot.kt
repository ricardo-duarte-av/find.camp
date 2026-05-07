package camp.find.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotSummary(
    val id: String,
    val slug: String,
    val name: String,
    val region: String,
    val country: String,
    val type: String,
    val lat: Double,
    val lng: Double,
    val priceEur: Double,
    val ratingAvg: Double,
    val reviewCount: Int,
    val imgUrl: String? = null,
    val amenities: List<String>,
    val addedByName: String? = null,
    val daysAgo: Int,
)

@Serializable
data class SpotsResponse(
    val spots: List<SpotSummary>,
    val capped: Boolean,
)

@Serializable
data class SpotDetailResponse(val spot: SpotSummary)
