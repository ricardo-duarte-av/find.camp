package camp.find.app.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import camp.find.app.core.MapStyle
import camp.find.app.data.model.SpotSummary
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val SPOTS_SOURCE = "spots-source"
private const val SPOTS_LAYER = "spots-layer"

@Composable
fun MapScreen(
    onBack: () -> Unit,
    viewModel: MapViewModel = viewModel(),
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val styleUrl = if (isDark) MapStyle.DARK else MapStyle.LIGHT

    MapLibre.getInstance(context)

    val spots by viewModel.spots.collectAsState()
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Push updated spots into the map whenever the list changes
    LaunchedEffect(spots) {
        mapRef.value?.getStyle { style ->
            val collection = spots.toFeatureCollection()
            val source = style.getSourceAs<GeoJsonSource>(SPOTS_SOURCE)
            if (source != null) {
                source.setGeoJson(collection)
            } else {
                style.addSource(GeoJsonSource(SPOTS_SOURCE, collection))
                style.addLayer(
                    CircleLayer(SPOTS_LAYER, SPOTS_SOURCE).withProperties(
                        circleColor("#2E6B3E"),
                        circleRadius(10f),
                        circleStrokeColor("#FFFFFF"),
                        circleStrokeWidth(2f),
                    )
                )
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                getMapAsync { map ->
                    mapRef.value = map
                    map.setStyle(styleUrl) {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(52.3, 5.3))
                            .zoom(7.0)
                            .build()

                        fun loadVisible() {
                            val b = map.projection.visibleRegion.latLngBounds
                            viewModel.loadSpotsForBbox(
                                b.longitudeWest, b.latitudeSouth,
                                b.longitudeEast, b.latitudeNorth,
                            )
                        }

                        loadVisible()
                        map.addOnCameraIdleListener { loadVisible() }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun List<SpotSummary>.toFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(
        map { spot ->
            Feature.fromGeometry(Point.fromLngLat(spot.lng, spot.lat)).also { f ->
                f.addStringProperty("id", spot.id)
                f.addStringProperty("name", spot.name)
                f.addStringProperty("type", spot.type)
            }
        }
    )
