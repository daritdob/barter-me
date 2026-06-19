package com.example.ui.screens

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.categoryPinColor
import com.example.data.model.ListingEntity
import com.example.ui.components.glassmorphic
import com.example.ui.viewmodel.BarterViewModel
import androidx.compose.ui.layout.ContentScale
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

private const val DEFAULT_MAP_ZOOM = 14.0

/** Builds a simple circular map pin drawable tinted with the category color. */
private fun buildPinDrawable(fillColor: Int, sizePx: Int, strokePx: Int): Drawable =
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fillColor)
        setStroke(strokePx, android.graphics.Color.WHITE)
        setSize(sizePx, sizePx)
        setBounds(0, 0, sizePx, sizePx)
    }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NearbyMatchesMapView(
    viewModel: BarterViewModel,
    listings: List<ListingEntity>,
    onNavigateToChat: (Int) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val myProfile by viewModel.myProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val me = myProfile ?: return

    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedListing by remember { mutableStateOf<ListingEntity?>(null) }

    val pinSizePx = with(density) { 26.dp.roundToPx() }
    val meSizePx = with(density) { 30.dp.roundToPx() }
    val strokePx = with(density) { 2.dp.roundToPx() }
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()

    // Live OpenStreetMap tile map. osmdroid needs no API key, so the debug
    // build renders real, pannable/zoomable tiles out of the box.
    val mapView = remember {
        Configuration.getInstance().apply {
            // A non-default user-agent is required by the OSM tile servers.
            userAgentValue = context.packageName
            // Keep all tile cache inside app-private storage so no external
            // storage permission is needed on API 24+.
            val base = File(context.cacheDir, "osmdroid")
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            // We provide our own teal-styled zoom FABs below.
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(DEFAULT_MAP_ZOOM)
            controller.setCenter(GeoPoint(me.latitude, me.longitude))
        }
    }

    // Forward Compose/Activity lifecycle to the osmdroid MapView.
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDarkMode) Color(0xFF0F111A) else Color(0xFFF3F4F6)
            )
            .testTag("nearby_map_container")
    ) {
        // 1. LIVE OSM TILE MAP WITH MARKERS
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { map ->
                // Invert tiles for a dark-theme friendly map.
                map.overlayManager.tilesOverlay.setColorFilter(
                    if (isDarkMode) TilesOverlay.INVERT_COLORS else null
                )

                map.overlays.clear()

                // User's own location marker (teal, matches the theme).
                val meMarker = Marker(map).apply {
                    position = GeoPoint(me.latitude, me.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = buildPinDrawable(primaryArgb, meSizePx, strokePx)
                    title = me.locationName
                    setOnMarkerClickListener { _, _ ->
                        selectedListing = null
                        true
                    }
                }
                map.overlays.add(meMarker)

                // Nearby listing pins colored by category.
                listings.forEach { listing ->
                    val pinColor = categoryPinColor(listing.categoryHave).toArgb()
                    val marker = Marker(map).apply {
                        position = GeoPoint(listing.latitude, listing.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = buildPinDrawable(pinColor, pinSizePx, strokePx)
                        title = listing.haveItem
                        setOnMarkerClickListener { _, _ ->
                            selectedListing = listing
                            map.controller.animateTo(position)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                map.invalidate()
            }
        )

        // 2. FLOATING TOP CONTROLS (ZOOM, RECENTER compass)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Recenter Target Compass Button
            SmallFloatingActionButton(
                onClick = {
                    mapView.controller.animateTo(GeoPoint(me.latitude, me.longitude))
                    mapView.controller.setZoom(DEFAULT_MAP_ZOOM)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("map_recenter_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter map on my location"
                )
            }

            // Zoom In Button (+)
            SmallFloatingActionButton(
                onClick = { mapView.controller.zoomIn() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("map_zoomin_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom map in"
                )
            }

            // Zoom Out Button (-)
            SmallFloatingActionButton(
                onClick = { mapView.controller.zoomOut() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("map_zoomout_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom map out"
                )
            }
        }

        // Floating Map Compass Info/Legend Bar (Top-Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .glassmorphic(cornerRadius = 14.dp, borderWidth = 1.dp, isDarkTheme = isDarkMode)
                .background(Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = "Active Compass",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = "NEIGHBORHOOD MAP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${listings.size} near ${me.locationName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. SLIDE-UP DETAIL DRAWER OVERLAY CARD (FOR SELECTED PIN)
        AnimatedVisibility(
            visible = selectedListing != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .fillMaxWidth()
        ) {
            selectedListing?.let { listing ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .glassmorphic(cornerRadius = 24.dp, borderWidth = 1.dp, isDarkTheme = isDarkMode),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Header area with Creator and Avatar info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AsyncImage(
                                    model = listing.ownerAvatar,
                                    contentDescription = "Listing author profile pic",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = listing.ownerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (listing.isOwnerVerified) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Verified User Icon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Star score icon",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "${listing.ownerRating} (${listing.ownerRatingCount} ratings)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Close dialog action button
                            IconButton(
                                onClick = { selectedListing = null },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss trade detail preview card",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Midrow: Offer Swap Grid Items
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Listing item photo if captured offline
                            if (listing.photoUri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = listing.photoUri,
                                        contentDescription = "Trade Item snap",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            } else {
                                // Dynamic Category Graphic Placeholder
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (listing.categoryHave.lowercase()) {
                                            "photography" -> Icons.Default.PhotoCamera
                                            "cleaning" -> Icons.Default.CleaningServices
                                            "design" -> Icons.Default.Palette
                                            "education" -> Icons.Default.School
                                            "tech" -> Icons.Default.Computer
                                            "catering" -> Icons.Default.Restaurant
                                            else -> Icons.Default.CompareArrows
                                        },
                                        contentDescription = "Category fallback thumbnail icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        contentColor = MaterialTheme.colorScheme.secondary
                                    ) {
                                        Text(listing.categoryHave, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        text = "•  ${viewModel.getDistanceTo(listing)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "Offers: ${listing.haveItem}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Wants in return: ${listing.needItem}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (listing.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = listing.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Lower section CTAs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Bookmark / Save option
                            OutlinedIconToggleButton(
                                checked = listing.isSaved,
                                onCheckedChange = { viewModel.toggleSaveListing(listing) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.size(48.dp).testTag("map_save_toggle_${listing.id}")
                            ) {
                                Icon(
                                    imageVector = if (listing.isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = "Save trade listing trigger",
                                    tint = if (listing.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // View Profile of owner
                            OutlinedButton(
                                onClick = { onNavigateToProfile(listing.ownerId) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Text("Trust Profile", style = MaterialTheme.typography.labelLarge)
                            }

                            // Big Chat Now Button
                            Button(
                                onClick = { onNavigateToChat(listing.id) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("map_chat_now_cta_${listing.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Chat, contentDescription = "Launch live communication channel", modifier = Modifier.size(16.dp))
                                    Text("Chat Now", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
