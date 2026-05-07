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

    private val _selectedSpot = MutableStateFlow<SpotSummary?>(null)
    val selectedSpot: StateFlow<SpotSummary?> = _selectedSpot.asStateFlow()

    fun loadSpotsForBbox(
        minLng: Double,
        minLat: Double,
        maxLng: Double,
        maxLat: Double,
    ) {
        viewModelScope.launch {
            runCatching { api.getSpotsByBbox(minLng, minLat, maxLng, maxLat) }
                .onSuccess { _spots.value = it.spots }
        }
    }

    fun selectSpot(id: String) {
        _selectedSpot.value = _spots.value.find { it.id == id }
    }

    fun clearSelectedSpot() {
        _selectedSpot.value = null
    }
}
