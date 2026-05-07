package camp.find.app.data.network

import camp.find.app.BuildConfig
import camp.find.app.data.model.ReviewsResponse
import camp.find.app.data.model.TripsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class TripsApi {
    private val base = BuildConfig.BASE_URL

    suspend fun getPublicTrips(): TripsResponse =
        get("$base/api/v1/trips")

    suspend fun getReviews(spotId: String, cursor: String? = null): ReviewsResponse =
        get("$base/api/v1/spots/$spotId/reviews" + if (cursor != null) "?cursor=$cursor" else "")

    private suspend inline fun <reified T> get(url: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        ApiClient.http.newCall(request).execute().use { response ->
            ApiClient.json.decodeFromString(response.body!!.string())
        }
    }
}
