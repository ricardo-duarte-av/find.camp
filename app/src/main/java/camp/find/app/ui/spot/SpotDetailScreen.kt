package camp.find.app.ui.spot

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Shower
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import camp.find.app.BuildConfig
import camp.find.app.data.model.Review
import camp.find.app.data.model.SpotSummary
import camp.find.app.ui.components.WavyLoadingBox
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

private data class AmenityInfo(val label: String, val icon: ImageVector)

private val amenityInfo = mapOf(
    "electric"  to AmenityInfo("Electric",  Icons.Outlined.Bolt),
    "water"     to AmenityInfo("Water",     Icons.Outlined.LocalDrink),
    "wifi"      to AmenityInfo("WiFi",      Icons.Outlined.Wifi),
    "shower"    to AmenityInfo("Showers",   Icons.Outlined.Shower),
    "toilet"    to AmenityInfo("Toilets",   Icons.Outlined.Shower),
    "dogs"      to AmenityInfo("Dogs OK",   Icons.Outlined.Pets),
    "swimming"  to AmenityInfo("Swimming",  Icons.Outlined.Pool),
)

private fun typeLabel(key: String) = when (key) {
    "paid"      -> "Campsite"
    "wild"      -> "Wild camping"
    "motorhome" -> "Motorhome aire"
    "glamping"  -> "Glamping"
    "hostel"    -> "Hostel"
    else        -> key.replaceFirstChar { it.uppercase() }
}

private fun SpotSummary.resolvedImageUrl(): String? {
    val url = imgUrl ?: return null
    return if (url.startsWith("http")) url else "${BuildConfig.BASE_URL}$url"
}

private fun SpotSummary.priceLabel() =
    if (priceEur == 0.0) "Free" else "€%.0f/night".format(priceEur)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailScreen(
    spotId: String,
    onBack: () -> Unit,
    viewModel: SpotDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { SpotDetailViewModel(spotId) } }
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val nextCursor by viewModel.nextCursor.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? SpotDetailUiState.Success)?.spot?.name ?: ""
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val s = uiState) {
            is SpotDetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is SpotDetailUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.message, style = MaterialTheme.typography.bodyLarge)
            }

            is SpotDetailUiState.Success -> SpotDetailContent(
                spot = s.spot,
                reviews = reviews,
                hasMore = nextCursor != null,
                onLoadMore = viewModel::loadReviews,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SpotDetailContent(
    spot: SpotSummary,
    reviews: List<Review>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LazyColumn(modifier = modifier.fillMaxSize()) {

        // ── Hero image ────────────────────────────────────────────────────────
        item {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(spot.resolvedImageUrl())
                    .crossfade(true)
                    .build(),
                contentDescription = spot.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(240.dp),
                loading = { WavyLoadingBox(modifier = Modifier.fillMaxSize()) },
            )
        }

        // ── Name, type, rating, price ─────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = spot.name,
                        style = MaterialTheme.typography.headlineSmall,
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
                        text = if (spot.reviewCount > 0) "%.1f".format(spot.ratingAvg) else "No reviews",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (spot.reviewCount > 0) {
                        Text(
                            text = "(${spot.reviewCount})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = spot.priceLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${spot.region}, ${spot.country}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Amenities ─────────────────────────────────────────────────────────
        if (spot.amenities.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
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
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Location card ─────────────────────────────────────────────────────
        item {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Location",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = spot.region,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = countryName(spot.country),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "%.5f, %.5f".format(spot.lat, spot.lng),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse(
                                "geo:${spot.lat},${spot.lng}?" +
                                    "q=${spot.lat},${spot.lng}(${Uri.encode(spot.name)})"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Get directions")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Added by ──────────────────────────────────────────────────────────
        item {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                if (spot.addedByName != null) {
                    Text(
                        text = "Added by ${spot.addedByName} · ${spot.daysAgo}d ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // ── Reviews header ────────────────────────────────────────────────────
        item {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Reviews",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
        }

        if (reviews.isEmpty()) {
            item {
                Text(
                    text = "No reviews yet — be the first!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        } else {
            items(reviews, key = { it.id }) { review ->
                ReviewItem(review = review)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            }

            if (hasMore) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(onClick = onLoadMore) { Text("Load more reviews") }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ReviewItem(review: Review) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = review.authorName ?: "Anonymous",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            StarRating(review.rating)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = review.body,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${review.daysAgo}d ago",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StarRating(rating: Int) {
    Row {
        repeat(5) { i ->
            Icon(
                imageVector = if (i < rating) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (i < rating) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun countryName(iso: String) = when (iso.uppercase()) {
    "NL" -> "Netherlands"
    "BE" -> "Belgium"
    "DE" -> "Germany"
    "FR" -> "France"
    "ES" -> "Spain"
    "PT" -> "Portugal"
    "IT" -> "Italy"
    "AT" -> "Austria"
    "CH" -> "Switzerland"
    "GB" -> "United Kingdom"
    "IE" -> "Ireland"
    "DK" -> "Denmark"
    "SE" -> "Sweden"
    "NO" -> "Norway"
    "FI" -> "Finland"
    "PL" -> "Poland"
    "CZ" -> "Czech Republic"
    "HR" -> "Croatia"
    "GR" -> "Greece"
    "US" -> "United States"
    else -> iso
}
