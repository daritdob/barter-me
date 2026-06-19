package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.example.data.categoryPinColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ListingEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.glassmorphic
import com.example.ui.viewmodel.BarterViewModel
import kotlin.math.*
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    val me = myProfile ?: return

    // Interactive map state coordinates
    var mapOffsetX by remember { mutableStateOf(0f) }
    var mapOffsetY by remember { mutableStateOf(0f) }
    // Interactive scale: default is 35000f dp per coordinate degree (perfect for NY area showing few-mile radius clearly)
    var mapZoomScale by remember { mutableStateOf(45000f) }

    var selectedListing by remember { mutableStateOf<ListingEntity?>(null) }

    // Radar pulse animation state
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweeper")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = androidx.compose.animation.core.FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "PulseScale"
    )

    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "RadarAngle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isDarkMode) Color(0xFF0F111A) else Color(0xFFF3F4F6)
            )
            .testTag("nearby_map_container")
    ) {
        // 1. INLINE INTERACTIVE CANVAS MAP BACKGROUND
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        mapOffsetX += dragAmount.x
                        mapOffsetY += dragAmount.y
                    }
                }
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val centerX = widthPx / 2f
            val centerY = heightPx / 2f

            val currentDensity = LocalDensity.current
            val scaleInDp = with(currentDensity) { mapZoomScale / density }

            // Math base Projection calculation
            val cosLat = cos(Math.toRadians(me.latitude))

            val mapGridColor = if (isDarkMode) Color(0xFF2E334D).copy(alpha = 0.35f) else Color(0xFFD1D5DB).copy(alpha = 0.5f)
            val radarCirclesColor = if (isDarkMode) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF4F46E5).copy(alpha = 0.08f)
            val radarSweepColor = if (isDarkMode) Color(0xFF6366F1).copy(alpha = 0.04f) else Color(0xFF4F46E5).copy(alpha = 0.03f)

            // Canvas drawing grid and concentric radar rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background grid lines (every 100 pixels based on pan offset)
                val gridGap = 160f
                val startX = mapOffsetX % gridGap
                var cX = startX
                while (cX < size.width) {
                    drawLine(
                        color = mapGridColor,
                        start = Offset(cX, 0f),
                        end = Offset(cX, size.height),
                        strokeWidth = 1f
                    )
                    cX += gridGap
                }

                val startY = mapOffsetY % gridGap
                var cY = startY
                while (cY < size.height) {
                    drawLine(
                        color = mapGridColor,
                        start = Offset(0f, cY),
                        end = Offset(size.width, cY),
                        strokeWidth = 1f
                    )
                    cY += gridGap
                }

                // Focus/Home screen coordinate center offset based on current drag position
                val homeCenterX = centerX + mapOffsetX
                val homeCenterY = centerY + mapOffsetY

                // Concentric local distance radar rings: 1 mile, 3 miles, 5 miles, 12 miles
                // 1 mile is approx 0.0145 degrees
                val degreeMiles = 0.0145f
                val listMiles = listOf(1f, 3f, 5f, 10f)

                listMiles.forEach { mile ->
                    val deltaDegree = degreeMiles * mile
                    val radius = deltaDegree * scaleInDp
                    
                    drawCircle(
                        color = radarCirclesColor,
                        radius = radius,
                        center = Offset(homeCenterX, homeCenterY),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    )
                }

                // Drawing Radar glow line sweep
                val sweepRadius = degreeMiles * 12f * scaleInDp
                val angleRad = Math.toRadians(radarAngle.toDouble())
                val endPointX = homeCenterX + sweepRadius * cos(angleRad).toFloat()
                val endPointY = homeCenterY + sweepRadius * sin(angleRad).toFloat()

                drawLine(
                    color = radarSweepColor.copy(alpha = radarSweepColor.alpha * 2.5f),
                    start = Offset(homeCenterX, homeCenterY),
                    end = Offset(endPointX, endPointY),
                    strokeWidth = 3f
                )
            }

            // 2. CENTRAL ME POSITION ICON (USER'S BASE)
            val homeCenterX = centerX + mapOffsetX
            val homeCenterY = centerY + mapOffsetY

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (homeCenterX - with(currentDensity) { 20.dp.toPx() }).toInt(),
                            (homeCenterY - with(currentDensity) { 20.dp.toPx() }).toInt()
                        )
                    }
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer locator halo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = pulseScale,
                            scaleY = pulseScale
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f * pulseAlpha),
                            shape = CircleShape
                        )
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "My location marker pin",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 3. SECTOR/LISTING PIN MARKERS
            listings.forEach { listing ->
                // Calculate Mercator projection offset from base location
                val deltaLat = listing.latitude - me.latitude
                val deltaLng = listing.longitude - me.longitude

                // Compress longitude by cosine of latitude to maintain precise aspect ratio
                val xDiff = deltaLng * cosLat
                val yDiff = deltaLat

                val pinX = centerX + (xDiff * scaleInDp) + mapOffsetX
                val pinY = centerY - (yDiff * scaleInDp) + mapOffsetY

                val isSelected = selectedListing?.id == listing.id

                val categoryColor = remember(listing.categoryHave) {
                    categoryPinColor(listing.categoryHave)
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (pinX - with(currentDensity) { 18.dp.toPx() }).toInt(),
                                (pinY - with(currentDensity) { 18.dp.toPx() }).toInt()
                            )
                        }
                        .size(36.dp)
                        .clickable {
                            selectedListing = if (isSelected) null else listing
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        // Focus visual ring indicator
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(categoryColor.copy(alpha = 0.25f), CircleShape)
                                .border(2.dp, categoryColor, CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (isSelected) categoryColor else categoryColor.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                            .border(2.dp, Color.White, CircleShape),
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
                                else -> Icons.Default.SwapHoriz
                            },
                            contentDescription = "Category icon marker",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // 4. FLOATING TOP CONTROLS (ZOOM SLIDERS, RECENTER compass)
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
                    mapOffsetX = 0f
                    mapOffsetY = 0f
                    mapZoomScale = 45000f
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("map_recenter_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Zoom & Recenter Map"
                )
            }

            // Zoom In Button (+)
            SmallFloatingActionButton(
                onClick = {
                    if (mapZoomScale < 150000f) {
                        mapZoomScale = (mapZoomScale * 1.35f).coerceAtMost(150000f)
                    }
                },
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
                onClick = {
                    if (mapZoomScale > 10000f) {
                        mapZoomScale = (mapZoomScale / 1.35f).coerceAtLeast(10000f)
                    }
                },
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

        // 5. SLIDE-UP DETAIL DRAWER OVERLAY CARD (FOR SELECTED PIN)
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
