package camp.find.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import camp.find.app.data.model.SpotSummary
import camp.find.app.data.network.SpotsApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val api = SpotsApi()

    private val _spots = MutableStateFlow<List<SpotSummary>>(emptyList())
    val spots: StateFlow<List<SpotSummary>> = _spots.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSpotsForBbox(
        minLng: Double,
        minLat: Double,
        maxLng: Double,
        maxLat: Double,
    ) {
        viewModelScope.launch {
            try {
                _spots.value = api.getSpotsByBbox(minLng, minLat, maxLng, maxLat).spots
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
