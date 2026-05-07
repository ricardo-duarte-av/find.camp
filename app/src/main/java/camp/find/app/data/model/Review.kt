package camp.find.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String,
    val rating: Int,
    val body: String,
    val authorName: String? = null,
    val daysAgo: Int,
    val createdAt: String,
)

@Serializable
data class ReviewsResponse(
    val reviews: List<Review>,
    val nextCursor: String? = null,
)
