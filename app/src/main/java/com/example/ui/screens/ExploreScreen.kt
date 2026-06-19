package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.BARTER_CATEGORIES
import com.example.data.model.ListingEntity
import com.example.ui.viewmodel.BarterViewModel
import com.example.ui.components.SectionHeader
import com.example.ui.screens.explore.AISmartMatchesSection
import com.example.ui.screens.explore.CreateListingWizard
import com.example.ui.screens.explore.ListingCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: BarterViewModel,
    onNavigateToChat: (Int) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listings by viewModel.filteredListings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val maxDistance by viewModel.maxDistanceFilter.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    val otherProfiles by viewModel.otherProfiles.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val smartMatches by viewModel.smartMatches.collectAsState()
    val listingSubmitState by viewModel.listingSubmitState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    var isFilterExpanded by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }

    val allConnectedProfiles = remember(myProfile, otherProfiles) {
        (otherProfiles + listOfNotNull(myProfile)).associateBy { it.userId }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Permanent Search Bar with Collapsible Geo Filter Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Highly visible & elegant Keyword.Category Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search offers, skills, people…") },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.primary
                    ) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search query")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Geolocation Filter Toggle Button (indicated if custom distance/geolocation limit is active)
            IconButton(
                onClick = { isFilterExpanded = !isFilterExpanded },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (maxDistance != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .testTag("filter_toggle_button")
            ) {
                BadgedBox(
                    badge = {
                        if (maxDistance != null) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFilterExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                        contentDescription = "Toggle geolocation filter map and range",
                        tint = if (maxDistance != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Expandable Geolocation filter panel
        AnimatedVisibility(
            visible = isFilterExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader(
                        title = "Distance filter",
                        subtitle = "Near ${myProfile?.locationName ?: "your location"}",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        if (maxDistance != null) {
                            TextButton(onClick = { viewModel.setMaxDistance(null) }) {
                                Text("Show anywhere")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (maxDistance != null) "Within ${maxDistance!!.toInt()} miles" else "Any distance",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = maxDistance ?: 20f,
                        onValueChange = { viewModel.setMaxDistance(if (it >= 19.5f) null else it.coerceAtLeast(1f)) },
                        valueRange = 1f..20f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("distance_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 mi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text("10 mi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text("Anywhere", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Category slider styled as Segmented Controls from HTML layout
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp,
                        enabled = true,
                        selected = selectedCategory == null
                    ),
                    modifier = Modifier.testTag("category_all")
                )
            }
            items(BARTER_CATEGORIES) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.setCategoryFilter(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp,
                        enabled = true,
                        selected = selectedCategory == category
                    ),
                    modifier = Modifier.testTag("category_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Toggle view button row (Feed vs Map View) for Geolocation
        TabRow(
            selectedTabIndex = if (isMapView) 1 else 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            indicator = {},
            divider = {}
        ) {
            Tab(
                selected = !isMapView,
                onClick = { isMapView = false },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (!isMapView) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .testTag("toggle_list_view"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Show list feed of deals",
                            tint = if (!isMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "List",
                            fontWeight = FontWeight.Medium,
                            color = if (!isMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            Tab(
                selected = isMapView,
                onClick = { isMapView = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isMapView) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .testTag("toggle_map_view"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Show interactive neighborhood map",
                            tint = if (isMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Map",
                            fontWeight = FontWeight.Medium,
                            color = if (isMapView) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isMapView) {
            NearbyMatchesMapView(
                viewModel = viewModel,
                listings = listings,
                onNavigateToChat = onNavigateToChat,
                onNavigateToProfile = onNavigateToProfile,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            // Listings feed
            if (listings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.SwapCalls,
                            contentDescription = "Empty trades",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No offers in this area",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Try widening the distance filter or changing your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (smartMatches.isNotEmpty()) {
                        item {
                            AISmartMatchesSection(
                                matches = smartMatches,
                                isFallback = false,
                                distanceCalc = { viewModel.getDistanceTo(it) },
                                onChatClick = { onNavigateToChat(it.id) },
                                onProfileClick = { onNavigateToProfile(it.ownerId) },
                                isDarkTheme = isDarkMode
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        val trending = listings.filter { it.isOwnerVerified && it.ownerId != (myProfile?.userId ?: "") }.take(3)
                        if (trending.isNotEmpty()) {
                            item {
                                AISmartMatchesSection(
                                    matches = trending,
                                    isFallback = true,
                                    distanceCalc = { viewModel.getDistanceTo(it) },
                                    onChatClick = { onNavigateToChat(it.id) },
                                    onProfileClick = { onNavigateToProfile(it.ownerId) },
                                    isDarkTheme = isDarkMode
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    items(listings, key = { it.id }) { listing ->
                        val ownerProfile = allConnectedProfiles[listing.ownerId]
                        ListingCard(
                            listing = listing,
                            distanceText = viewModel.getDistanceTo(listing),
                            onSaveToggle = { viewModel.toggleSaveListing(listing) },
                            onChatClick = { onNavigateToChat(listing.id) },
                            onProfileClick = { onNavigateToProfile(listing.ownerId) },
                            modifier = Modifier.testTag("listing_card_${listing.id}"),
                            isDarkTheme = isDarkMode,
                            isOnline = ownerProfile?.isOnline,
                            isDnd = ownerProfile?.isDndMode
                        )
                    }
                }
            }
        }
    }

        ExtendedFloatingActionButton(
            onClick = { showCreateDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = "Post offer") },
            text = { Text("Post offer") },
            expanded = true,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("create_listing_fab")
        )
    }

    if (showCreateDialog) {
        CreateListingWizard(
            profileCountry = myProfile?.country ?: "USA",
            submitState = listingSubmitState,
            onDismiss = {
                viewModel.clearListingSubmitState()
                showCreateDialog = false
            },
            onClearSubmitState = { viewModel.clearListingSubmitState() },
            onSubmit = { have, need, catHave, catNeed, desc, haveType, needType, deliveryMode, photo ->
                viewModel.submitNewListing(
                    have = have,
                    need = need,
                    categoryHave = catHave,
                    categoryNeed = catNeed,
                    desc = desc,
                    haveType = haveType,
                    needType = needType,
                    deliveryMode = deliveryMode,
                    photoUri = photo,
                )
            },
        )
    }
}

