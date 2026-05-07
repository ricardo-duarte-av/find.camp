package camp.find.app.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Shower
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import camp.find.app.BuildConfig
import camp.find.app.data.model.SpotSummary
import camp.find.app.ui.components.WavyLoadingBox
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

// ── Amenity catalogue (keys from the live API) ───────────────────────────────

private data class AmenityInfo(val label: String, val icon: ImageVector)

private val amenityInfo = mapOf(
    "electric"    to AmenityInfo("Electric",  Icons.Outlined.Bolt),
    "water"       to AmenityInfo("Water",     Icons.Outlined.LocalDrink),
    "wifi"        to AmenityInfo("WiFi",      Icons.Outlined.Wifi),
    "shower"      to AmenityInfo("Showers",   Icons.Outlined.Shower),
    "toilet"      to AmenityInfo("Toilets",   Icons.Outlined.Shower),   // no toilet icon in M3
    "dogs"        to AmenityInfo("Dogs OK",   Icons.Outlined.Pets),
    "swimming"    to AmenityInfo("Swimming",  Icons.Outlined.Pool),
)

// ── Type label mapping ────────────────────────────────────────────────────────

private fun typeLabel(key: String) = when (key) {
    "paid"       -> "Campsite"
    "wild"       -> "Wild camping"
    "motorhome"  -> "Motorhome aire"
    "glamping"   -> "Glamping"
    "hostel"     -> "Hostel"
    else         -> key.replaceFirstChar { it.uppercase() }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun SpotSummary.resolvedImageUrl(): String? {
    val url = imgUrl ?: return null
    return if (url.startsWith("http")) url else "${BuildConfig.BASE_URL}$url"
}

private fun SpotSummary.priceLabel() =
    if (priceEur == 0.0) "Free" else "€%.0f/night".format(priceEur)

// ── Sheet ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotPreviewSheet(
    spot: SpotSummary,
    onDismiss: () -> Unit,
    onViewDetails: (String) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            // ── Hero image ────────────────────────────────────────────────────
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(spot.resolvedImageUrl())
                    .crossfade(true)
                    .build(),
                contentDescription = spot.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                loading = {
                    WavyLoadingBox(modifier = Modifier.fillMaxSize())
                },
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))

                // ── Name + type chip ──────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = spot.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(typeLabel(spot.type)) },
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ── Rating + price ────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "%.1f".format(spot.ratingAvg),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "(${spot.reviewCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = spot.priceLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // ── Amenities ─────────────────────────────────────────────────
                if (spot.amenities.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        spot.amenities.forEach { key ->
                            val info = amenityInfo[key]
                            AssistChip(
                                onClick = {},
                                label = { Text(info?.label ?: key) },
                                leadingIcon = info?.let { {
                                    Icon(
                                        it.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                }},
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Action buttons ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { /* requires auth — coming soon */ },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }

                    Button(
                        onClick = {
                            val uri = Uri.parse(
                                "geo:${spot.lat},${spot.lng}?" +
                                    "q=${spot.lat},${spot.lng}(${Uri.encode(spot.name)})"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            Icons.Outlined.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Navigate")
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { onViewDetails(spot.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View full details")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
