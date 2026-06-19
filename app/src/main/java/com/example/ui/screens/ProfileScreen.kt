package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.model.ProfileEntity
import com.example.data.model.RatingEntity
import com.example.data.model.CompletedTradeEntity
import com.example.ui.viewmodel.BarterViewModel
import com.example.ui.components.glassmorphic
import com.example.ui.components.GlassCard
import com.example.ui.screens.profile.CompletedTradeCard
import com.example.ui.screens.profile.RatingCard
import com.example.ui.screens.profile.SubmitRatingDialog
import com.example.ui.screens.profile.WithdrawDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: BarterViewModel,
    userId: String,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var ratingDialogListingId by remember { mutableStateOf<Int?>(null) }
    var showRatingSubmission by remember { mutableStateOf(false) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isProUser by viewModel.isProUser.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    var showWithdrawDialog by remember { mutableStateOf(false) }
    val locationMessage by viewModel.locationMessage.collectAsState()
    val socialVerificationMessage by viewModel.socialVerificationMessage.collectAsState()
    var socialProfileUrl by remember { mutableStateOf("") }

    // Collect profile
    val profileFlow = remember(userId) {
        if (userId == "me") viewModel.myProfile else viewModel.getProfileFlowById(userId)
    }
    val profile by profileFlow.collectAsState(initial = null)

    // Collect profile trust reviews
    val reviewsFlow = remember(userId) { viewModel.getReviewsForUser(userId) }
    val reviews by reviewsFlow.collectAsState(initial = emptyList())

    // Collect profile completed trades
    val completedTradesFlow = remember(userId) { viewModel.getCompletedTradesForUser(userId) }
    val completedTrades by completedTradesFlow.collectAsState(initial = emptyList())
    val myListings by viewModel.myListings.collectAsState()

    // Fields for editing profile
    var nameField by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var roleField by remember(profile) { mutableStateOf(profile?.role ?: "") }
    var haveField by remember(profile) { mutableStateOf(profile?.skillsOffered ?: "") }
    var needField by remember(profile) { mutableStateOf(profile?.skillsNeeded ?: "") }
    var locField by remember(profile) { mutableStateOf(profile?.locationName ?: "") }
    var isOnlineField by remember(profile) { mutableStateOf(profile?.isOnline ?: true) }
    var isDndModeField by remember(profile) { mutableStateOf(profile?.isDndMode ?: false) }
    var autoReplyField by remember(profile) { mutableStateOf(profile?.autoReplyMessage ?: "Hey! I am currently away from my trust card. Leave a message and I will get back to you.") }
    var preferredCategoryField by remember(profile) { mutableStateOf(profile?.preferredCategory ?: "All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (userId == "me") "Profile" else profile?.name ?: "Profile") },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_btn")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (userId == "me" && !isEditing) {
                        IconButton(
                            onClick = { isEditing = true }, 
                            modifier = Modifier.testTag("profile_edit_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                        
                        // Let the auditor logout to view the login screen and onboarding anytime!
                        IconButton(
                            onClick = { viewModel.signOutUser() }, 
                            modifier = Modifier.testTag("profile_logout_btn")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out / Switch Account")
                        }
                    }
                    
                    // Share Profile socially
                    IconButton(
                        onClick = {
                            profile?.let { p ->
                                val shareText = """
                                    Check out ${p.name} on Barter-me!
                                    Offering: ${p.skillsOffered}
                                    Looking for: ${p.skillsNeeded}
                                    Rating: ${p.rating} (${p.ratingCount} reviews)
                                """.trimIndent()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }
                        },
                        modifier = Modifier.testTag("profile_share_btn")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share Profile")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        profile?.let { p ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bio card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphic(cornerRadius = 24.dp, borderWidth = 1.dp, isDarkTheme = isDarkMode),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Portrait photo",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (userId == "me" && isProUser) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFFFB300),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "PRO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (p.verifyStatus == "VERIFIED") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified Identity",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                val (statusLabel, statusColor, textColor) = if (p.isDndMode) {
                                    Triple("DND", Color(0xFFFFB300), Color.Black)
                                } else if (p.isOnline) {
                                    Triple("Online", Color(0xFF4CAF50), Color.White)
                                } else {
                                    Triple("Offline", Color(0xFF9E9E9E), Color.White)
                                }
                                Surface(
                                    color = statusColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = p.role,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Trust Score layout
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "cumulative star rating",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${p.rating} Peer Rating",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD68F00),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${p.ratingCount} completed swaps)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Trust Achievements Row / Gamification (User retention) — key stats
                if (!isEditing) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("achievements_section")
                                .glassmorphic(cornerRadius = 16.dp, borderWidth = 1.dp, isDarkTheme = isDarkMode),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Achievements",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TRUST LEVEL STATS & TROPHIES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Tier Badge based on completed swaps
                                    val tierTitle = when {
                                        p.ratingCount >= 5 -> "Local Legend 👑"
                                        p.ratingCount >= 2 -> "Cooperative Guru 🚀"
                                        p.ratingCount >= 1 -> "Active Swapper 🌱"
                                        else -> "Novice Trader 🗺️"
                                    }
                                    
                                    val tierColor = when {
                                        p.ratingCount >= 5 -> Color(0xFFC5A028) // Gold
                                        p.ratingCount >= 2 -> Color(0xFF1976D2) // Guru Blue
                                        p.ratingCount >= 1 -> Color(0xFF388E3C) // Growth Green
                                        else -> Color(0xFF455A64) // Slate
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(tierColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = tierTitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = tierColor
                                        )
                                        Text(
                                            text = "Reputation Tier",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    // Agreement signature count badge
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${p.ratingCount} Agreements",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Contracts Audited",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Edit Profile Dialog/Fields Integration
                if (isEditing && userId == "me") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("edit_profile_fields"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Edit Identity & Custom Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                
                                OutlinedTextField(
                                    value = nameField,
                                    onValueChange = { nameField = it },
                                    label = { Text("Display Name") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = roleField,
                                    onValueChange = { roleField = it },
                                    label = { Text("Profession / Background") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_role_input"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = haveField,
                                    onValueChange = { haveField = it },
                                    label = { Text("Trading Offer Skills (comma-separated)") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_offered_input")
                                )

                                OutlinedTextField(
                                    value = needField,
                                    onValueChange = { needField = it },
                                    label = { Text("Trading Needs / Looking For (comma-separated)") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_needed_input")
                                )

                                OutlinedTextField(
                                    value = locField,
                                    onValueChange = { locField = it },
                                    label = { Text("Neighborhood Location") },
                                    modifier = Modifier.fillMaxWidth().testTag("edit_loc_input"),
                                    singleLine = true
                                )

                                OutlinedButton(
                                    onClick = { viewModel.refreshLocationFromGps() },
                                    modifier = Modifier.fillMaxWidth().testTag("use_gps_location_btn")
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Use my current GPS location")
                                }

                                locationMessage?.let { message ->
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                                
                                Text("Presence & Auto-Responder Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Display Online", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Let others see you are active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    }
                                    Switch(
                                        checked = isOnlineField,
                                        onCheckedChange = { isOnlineField = it },
                                        modifier = Modifier.testTag("setting_online_switch")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Do Not Disturb (DND)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Instantly reply with custom away message", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    }
                                    Switch(
                                        checked = isDndModeField,
                                        onCheckedChange = { isDndModeField = it },
                                        modifier = Modifier.testTag("setting_dnd_switch")
                                    )
                                }

                                OutlinedTextField(
                                    value = autoReplyField,
                                    onValueChange = { autoReplyField = it },
                                    label = { Text("Automated Custom Reply Text") },
                                    placeholder = { Text("Leave a message if offline / busy...") },
                                    modifier = Modifier.fillMaxWidth().testTag("setting_autoreply_input")
                                )

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Primary Trading Interest", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("All", "Photography", "Cleaning", "Design").forEach { cat ->
                                            val isSel = preferredCategoryField == cat
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { preferredCategoryField = cat },
                                                label = { Text(cat) },
                                                modifier = Modifier.testTag("setting_category_chip_$cat")
                                            )
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { isEditing = false }) {
                                        Text("Go Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.updateMyProfile(
                                                name = nameField,
                                                role = roleField,
                                                skillsHave = haveField,
                                                skillsWant = needField,
                                                locName = locField,
                                                isOnline = isOnlineField,
                                                isDndMode = isDndModeField,
                                                autoReply = autoReplyField,
                                                prefCategory = preferredCategoryField
                                            )
                                            isEditing = false
                                        },
                                        modifier = Modifier.testTag("profile_save_changes_btn")
                                    ) {
                                        Text("Update Profile")
                                    }
                                }
                            }
                        }
                    }
                }

                // Offered & Looking For skills
                if (!isEditing) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "OFFERS & ABILITIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = p.skillsOffered,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = "WANTS TO TRADE FOR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = p.skillsNeeded,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = "ACTIVE IN AREA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Active Loc", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = p.locationName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // My posted offers (listings)
                    if (userId == "me" && myListings.isNotEmpty()) {
                        item {
                            Text(
                                "My offers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(myListings, key = { it.id }) { listing ->
                            com.example.ui.screens.explore.ListingCard(
                                listing = listing,
                                distanceText = listing.locationName,
                                onSaveToggle = { viewModel.toggleSaveListing(listing) },
                                onChatClick = { },
                                onProfileClick = { },
                                showStatus = true,
                                isDarkTheme = isDarkMode,
                            )
                        }
                    }

                    // Verified Completed Trade History Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VERIFIED COMPLETED TRADES",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${completedTrades.size} swaps",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (completedTrades.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassmorphic(cornerRadius = 16.dp, borderWidth = 1.dp, isDarkTheme = isDarkMode),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = "No trades completed icon",
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "No completed swaps recorded on public trade ledger yet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(completedTrades) { trade ->
                            CompletedTradeCard(trade = trade, isDarkMode = isDarkMode)
                        }
                    }

                    // Spacer divider
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Leave reviews or review summary
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMMUNITY TRUST HISTORY",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (userId != "me") {
                                Button(
                                    onClick = { showRatingSubmission = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("propose_rating_btn")
                                ) {
                                    Icon(Icons.Outlined.Feedback, contentDescription = "Rate", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify Completed Swap")
                                }
                            }
                        }
                    }

                    // Display Reviews List
                    if (reviews.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No review logs recorded yet. Trust builds with swaps!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    } else {
                        items(reviews) { review ->
                            RatingCard(review = review)
                        }
                    }

                    // Dynamic Credit Ledger Wallet Section (for "me" only, as proposed in SaaS blueprint)
                    if (userId == "me") {
                        item {
                            val ledgerTransactions by viewModel.ledgerTransactions.collectAsState()
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("credit_wallet_card"),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Wallet,
                                                contentDescription = "Wallet Balance Icon",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                "COOPERATIVE WALLET",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "WAL_1201",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "$walletBalance",
                                                style = MaterialTheme.typography.displayMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Cooperative Barter Credits",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        
                                        Button(
                                            onClick = { showWithdrawDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("withdraw_cooperative_credits_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = "Withdrawal icon",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Withdraw",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                    
                                    Text(
                                        "LEDGER TRANSACTIONS AUDIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        ledgerTransactions.forEach { tx ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = tx.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                    )
                                                    val df = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }
                                                    Text(
                                                        text = df.format(java.util.Date(tx.timestamp)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.width(8.dp))
                                                
                                                Text(
                                                    text = if (tx.type == "earned") "+${tx.amount}" else "-${tx.amount}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (tx.type == "earned") Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Support & Premium Upgrades Section (Option 1: One-time Pro & Option 3: Token packs/Micro-transactions)
                    if (userId == "me") {
                        item {
                            val isProUser by viewModel.isProUser.collectAsState()
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("premium_store_card")
                                    .glassmorphic(cornerRadius = 24.dp, borderWidth = 1.dp, isDarkTheme = isDarkMode),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Headline
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Premium Badge",
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "PREMIUM UPGRADES & TOKENS",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    // Option 1: Pro Tier (One-Time Payment)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            "🎯 LIFETIME PRO TIER",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        if (isProUser) {
                                                            Surface(
                                                                color = Color(0xFF2E7D32),
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    "ACTIVE",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        "Elevate trust instantly. Unlocks elite badges, priority search exposure, and zero transaction ledger limits.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            if (!isProUser) {
                                                Button(
                                                    onClick = { viewModel.upgradeToPro() },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .testTag("upgrade_pro_one_time_btn"),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text(
                                                        "Unlock Forever Pro for $19.99 (One-Time)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Success",
                                                        tint = Color(0xFF2E7D32),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        "Outstanding! Premium benefits are applied to your trust ledger profile.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF2E7D32)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Option 3: Cooperative Credits Packs Store (Microtransactions)
                                    Text(
                                        "💳 TOP UP COOPERATIVE BALANCE (OP-3)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Secure additional exchange tokens instantly. Direct contribution to cooperative barter matching fund.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Package 1
                                        Button(
                                            onClick = { viewModel.buyCredits(500, 4.99) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(60.dp)
                                                .testTag("buy_credits_pack_light"),
                                            contentPadding = PaddingValues(4.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("+500", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("$4.99", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        
                                        // Package 2
                                        Button(
                                            onClick = { viewModel.buyCredits(1500, 9.99) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(60.dp)
                                                .testTag("buy_credits_pack_standard"),
                                            contentPadding = PaddingValues(4.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("+1,500", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("$9.99", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        
                                        // Package 3
                                        Button(
                                            onClick = { viewModel.buyCredits(4000, 19.99) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(60.dp)
                                                .testTag("buy_credits_pack_pro"),
                                            contentPadding = PaddingValues(4.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("+4,000", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("$19.99", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Social Verification Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Verification Badge Link",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "SOCIAL MEDIA VERIFICATION",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Link verified handles to ensure a high community trust standard and eliminate scam accounts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                socialVerificationMessage?.let { message ->
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (BuildConfig.DEBUG && userId == "me") {
                                    Button(
                                        onClick = { viewModel.toggleSocialVerification() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (p.verifyStatus == "VERIFIED") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("verify_id_button")
                                    ) {
                                        Icon(
                                            imageVector = if (p.verifyStatus == "VERIFIED") Icons.Default.Check else Icons.Default.Link,
                                            contentDescription = "Simulated link"
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (p.verifyStatus == "VERIFIED") "LinkedIn: Linked & Approved" else "Associate LinkedIn ID")
                                    }
                                    } else if (userId == "me" && p.verifyStatus != "VERIFIED") {
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = socialProfileUrl,
                                                onValueChange = { socialProfileUrl = it },
                                                label = { Text("LinkedIn profile URL") },
                                                modifier = Modifier.fillMaxWidth().testTag("social_profile_url_input"),
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = {
                                                    viewModel.requestSocialVerification(
                                                        provider = "LINKEDIN",
                                                        profileUrl = socialProfileUrl
                                                    )
                                                },
                                                enabled = socialProfileUrl.isNotBlank(),
                                                modifier = Modifier.fillMaxWidth().testTag("request_social_verify_btn")
                                            ) {
                                                Icon(Icons.Default.Link, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Submit for team review")
                                            }
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("verify_id_pending"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (p.verifyStatus == "VERIFIED") {
                                                        "Identity verified by Barter-me trust review."
                                                    } else {
                                                        "Social verification is reviewed by our team. You'll be notified when approved."
                                                    },
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Privacy & data controls — account deletion lives here, not out in the open.
                    if (userId == "me") {
                        item {
                            var showDeleteConfirmation by remember { mutableStateOf(false) }
                            var privacyExpanded by remember { mutableStateOf(false) }

                            if (showDeleteConfirmation) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteConfirmation = false },
                                    title = { Text("Delete Account permanently?") },
                                    text = { Text("This will delete your social verification credentials, clear active barter matching services, and reset your user profile stats. This action cannot be undone.") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.deleteUserAccount()
                                                showDeleteConfirmation = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.testTag("confirm_delete_account_btn")
                                        ) {
                                            Text("Delete Permanently")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteConfirmation = false }) {
                                            Text("Cancel")
                                        }
                                    },
                                    modifier = Modifier.testTag("delete_account_dialog")
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("privacy_card")
                                    .glassmorphic(cornerRadius = 16.dp, borderWidth = 1.dp, isDarkTheme = isDarkMode),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { privacyExpanded = !privacyExpanded }
                                            .testTag("privacy_toggle"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.PrivacyTip,
                                                contentDescription = "Privacy",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "PRIVACY",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            imageVector = if (privacyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (privacyExpanded) "Collapse privacy options" else "Expand privacy options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (privacyExpanded) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Permanently request full data removal under GDPR/CCPA requirements. Wiping your profile resets all community swap statistics and matches.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { showDeleteConfirmation = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("delete_account_button"),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteForever,
                                                    contentDescription = "Confirm GDPR Deletion icon"
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Delete my account")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRatingSubmission) {
        SubmitRatingDialog(
            recipientName = profile?.name ?: "Barterer",
            onDismiss = { showRatingSubmission = false },
            onSubmit = { stars, comments, barterTitle ->
                profile?.let { p ->
                    viewModel.submitRating(
                        listingId = 999, // dynamic finished code index
                        toUserId = p.userId,
                        stars = stars,
                        comment = comments,
                        barterTitle = barterTitle
                    )
                }
                showRatingSubmission = false
            }
        )
     }

     if (showWithdrawDialog) {
        WithdrawDialog(
            walletBalance = walletBalance,
            onDismiss = { showWithdrawDialog = false },
            onWithdraw = { amount, method, details ->
                viewModel.withdrawCredits(amount, method, details)
                showWithdrawDialog = false
            }
        )
     }
}

