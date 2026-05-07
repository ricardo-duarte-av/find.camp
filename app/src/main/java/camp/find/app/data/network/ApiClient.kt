package camp.find.app.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

object ApiClient {
    val http = OkHttpClient()
    val json = Json { ignoreUnknownKeys = true }
    val jsonMediaType = "application/json".toMediaType()
}
