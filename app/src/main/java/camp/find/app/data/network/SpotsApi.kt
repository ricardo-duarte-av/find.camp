package camp.find.app.data.network

import camp.find.app.BuildConfig
import camp.find.app.data.model.ReviewsResponse
import camp.find.app.data.model.SpotDetailResponse
import camp.find.app.data.model.SpotsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

class SpotsApi {
    private val base = BuildConfig.BASE_URL

    suspend fun getSpotsByBbox(
        minLng: Double,
        minLat: Double,
        maxLng: Double,
        maxLat: Double,
    ): SpotsResponse = get("$base/api/v1/spots?bbox=$minLng,$minLat,$maxLng,$maxLat")

    suspend fun getSpotById(id: String): SpotDetailResponse =
        get("$base/api/v1/spots/$id")

    suspend fun getSpotReviews(id: String, cursor: String? = null): ReviewsResponse {
        val url = buildString {
            append("$base/api/v1/spots/$id/reviews")
            if (cursor != null) append("?cursor=${cursor}")
        }
        return get(url)
    }

    private suspend inline fun <reified T> get(url: String): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        ApiClient.http.newCall(request).execute().use { response ->
            ApiClient.json.decodeFromString(response.body!!.string())
        }
    }
}
