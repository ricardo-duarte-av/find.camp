package camp.find.app.ui.spot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import camp.find.app.data.model.Review
import camp.find.app.data.model.SpotSummary
import camp.find.app.data.network.SpotsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SpotDetailUiState {
    data object Loading : SpotDetailUiState
    data class Success(val spot: SpotSummary) : SpotDetailUiState
    data class Error(val message: String) : SpotDetailUiState
}

class SpotDetailViewModel(
    private val spotId: String,
    private val api: SpotsApi = SpotsApi(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<SpotDetailUiState>(SpotDetailUiState.Loading)
    val uiState: StateFlow<SpotDetailUiState> = _uiState.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _nextCursor = MutableStateFlow<String?>(null)
    val nextCursor: StateFlow<String?> = _nextCursor.asStateFlow()

    private var loadingReviews = false

    init {
        loadSpot()
        loadReviews()
    }

    private fun loadSpot() {
        viewModelScope.launch {
            _uiState.value = try {
                SpotDetailUiState.Success(api.getSpotById(spotId).spot)
            } catch (e: Exception) {
                SpotDetailUiState.Error(e.message ?: "Failed to load spot")
            }
        }
    }

    fun loadReviews() {
        if (loadingReviews) return
        loadingReviews = true
        viewModelScope.launch {
            try {
                val response = api.getSpotReviews(spotId, _nextCursor.value)
                _reviews.value = _reviews.value + response.reviews
                _nextCursor.value = response.nextCursor
            } catch (_: Exception) {
            } finally {
                loadingReviews = false
            }
        }
    }
}
