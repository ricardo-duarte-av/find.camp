package camp.find.app.data.network

import camp.find.app.BuildConfig
import camp.find.app.data.model.SpotsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class SpotsApi {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getSpotsByBbox(
        minLng: Double,
        minLat: Double,
        maxLng: Double,
        maxLat: Double,
    ): SpotsResponse = withContext(Dispatchers.IO) {
        val url = "${BuildConfig.BASE_URL}/api/v1/spots" +
            "?bbox=$minLng,$minLat,$maxLng,$maxLat"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            json.decodeFromString(response.body!!.string())
        }
    }
}
