package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.PartyValuation
import com.example.data.ValueShortfallCalculator
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ListingEntity
import com.example.ui.viewmodel.BarterViewModel
import com.example.ui.screens.chat.CelebrationAnimationOverlay
import com.example.ui.screens.chat.ChatBubble
import com.example.ui.screens.chat.FulfillTradeDialog
import com.example.ui.screens.chat.MilestoneRow
import com.example.ui.screens.chat.ReportTradeDialog
import com.example.ui.screens.chat.SignContractDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: BarterViewModel,
    listingId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val messagesFlow = remember(listingId) { viewModel.getMessagesForListing(listingId) }
    val messages by messagesFlow.collectAsState(initial = emptyList())
    val myProfile by viewModel.myProfile.collectAsState()

    var activeListing by remember { mutableStateOf<ListingEntity?>(null) }

    val partnerId = activeListing?.ownerId ?: ""
    val partnerProfileFlow = remember(partnerId) { if (partnerId.isNotEmpty()) viewModel.getProfileFlowById(partnerId) else kotlinx.coroutines.flow.flowOf(null) }
    val partnerProfile by partnerProfileFlow.collectAsState(initial = null)
    var showSignContractDialog by remember { mutableStateOf(false) }
    var showFulfillDialog by remember { mutableStateOf(false) }
    var showSignSuccessAnimation by remember { mutableStateOf(false) }
    var showFulfillSuccessAnimation by remember { mutableStateOf(false) }

    // Privacy & safety actions
    val blockedUserIds by viewModel.blockedUserIds.collectAsState()
    val isBlocked = partnerId.isNotEmpty() && blockedUserIds.contains(partnerId)
    var showSafetyMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showCancelConfirm by remember { mutableStateOf(false) }
    
    var milestoneHaveDelivered by remember(listingId) { mutableStateOf(false) }
    var milestoneNeedDelivered by remember(listingId) { mutableStateOf(false) }

    // Per-side credit valuations captured when the Fair-Swap agreement is signed.
    // Reused at fulfillment time to settle the value shortfall against the wallet.
    var signedYourVal by remember(listingId) { mutableStateOf(0) }
    var signedTheirVal by remember(listingId) { mutableStateOf(0) }
    
    // Fetch associated listing details to show context at the top
    LaunchedEffect(listingId) {
        val lst = viewModel.allListings.value.firstOrNull { it.id == listingId }
        activeListing = lst
    }

    val listState = rememberLazyListState()
    
    // Auto scroll down to the newest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .clickable(enabled = partnerId.isNotEmpty()) { onNavigateToProfile(partnerId) }
                            .testTag("chat_view_profile")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeListing?.ownerName ?: "Barter Talk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            partnerProfile?.let { partner ->
                                val statusColor = if (partner.isDndMode) {
                                    Color(0xFFFFB300) // Yellow/Amber for Do Not Disturb
                                } else if (partner.isOnline) {
                                    Color(0xFF4CAF50) // Green for Online
                                } else {
                                    Color(0xFF9E9E9E) // Gray for Offline
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                            }
                        }

                        val statusSubText = partnerProfile?.let { partner ->
                            if (partner.isDndMode) {
                                "Busy • Auto-responder active"
                            } else if (partner.isOnline) {
                                "Online • Active Now"
                            } else {
                                "Offline • Last seen recently"
                            }
                        } ?: "Secured Escrow Chat"

                        Text(
                            text = statusSubText,
                            style = MaterialTheme.typography.labelSmall,
                            color = partnerProfile?.let { partner ->
                                if (partner.isOnline && !partner.isDndMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            } ?: MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Safe trade verification banner
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "ratings security status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Privacy & safety overflow menu
                    Box {
                        IconButton(
                            onClick = { showSafetyMenu = true },
                            modifier = Modifier.testTag("chat_safety_menu_btn")
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Privacy and safety options"
                            )
                        }
                        DropdownMenu(
                            expanded = showSafetyMenu,
                            onDismissRequest = { showSafetyMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View profile & reputation") },
                                onClick = {
                                    showSafetyMenu = false
                                    if (partnerId.isNotEmpty()) onNavigateToProfile(partnerId)
                                },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.testTag("menu_view_profile")
                            )
                            DropdownMenuItem(
                                text = { Text("Report bad swap") },
                                onClick = {
                                    showSafetyMenu = false
                                    showReportDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                modifier = Modifier.testTag("menu_report_swap")
                            )
                            DropdownMenuItem(
                                text = { Text(if (isBlocked) "Unblock user" else "Block user") },
                                onClick = {
                                    showSafetyMenu = false
                                    if (isBlocked) {
                                        if (partnerId.isNotEmpty()) viewModel.unblockUser(partnerId)
                                    } else {
                                        showBlockConfirm = true
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                modifier = Modifier.testTag("menu_block_user")
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel trade") },
                                onClick = {
                                    showSafetyMenu = false
                                    showCancelConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.Cancel, contentDescription = null) },
                                modifier = Modifier.testTag("menu_cancel_trade")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Consume the Scaffold insets so the input bar's imePadding() below
                // doesn't re-apply the navigation-bar inset on top of the keyboard
                // inset (the previous double-count left a blank gap above the keyboard).
                .consumeWindowInsets(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Pinned listing context header
            activeListing?.let { listing ->
                val tradeStates by viewModel.tradeLifecycleStates.collectAsState()
                val currentTradeState = tradeStates[listing.id] ?: BarterViewModel.TradeState.NEGOTIATING

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("chat_listing_context"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = partnerId.isNotEmpty()) { onNavigateToProfile(partnerId) }
                                .padding(12.dp)
                                .testTag("swap_tile_view_profile"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Handshake,
                                contentDescription = "Exchange badge",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "PROPOSED SWAP DETAILS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Have: ${listing.haveItem}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Need: ${listing.needItem}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                            
                            // Visual Trade State Badge
                            Surface(
                                color = when (currentTradeState) {
                                    BarterViewModel.TradeState.NEGOTIATING -> MaterialTheme.colorScheme.primaryContainer
                                    BarterViewModel.TradeState.AGREEMENT_SIGNED -> Color(0xFFE8F5E9)
                                    BarterViewModel.TradeState.IN_PROGRESS -> Color(0xFFFFF3E0)
                                    BarterViewModel.TradeState.UNDER_REVIEW -> Color(0xFFEDE7F6)
                                    BarterViewModel.TradeState.COMPLETED -> Color(0xFFE3F2FD)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = currentTradeState.name.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentTradeState) {
                                        BarterViewModel.TradeState.NEGOTIATING -> MaterialTheme.colorScheme.onPrimaryContainer
                                        BarterViewModel.TradeState.AGREEMENT_SIGNED -> Color(0xFF2E7D32)
                                        BarterViewModel.TradeState.IN_PROGRESS -> Color(0xFFE65100)
                                        BarterViewModel.TradeState.UNDER_REVIEW -> Color(0xFF5E35B1)
                                        BarterViewModel.TradeState.COMPLETED -> Color(0xFF1565C0)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // Interactive Milestones Panel
                        if (currentTradeState != BarterViewModel.TradeState.NEGOTIATING) {
                            var isMilestonesExpanded by remember { mutableStateOf(true) }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isMilestonesExpanded = !isMilestonesExpanded },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val checkedCount = if (currentTradeState == BarterViewModel.TradeState.COMPLETED) 3 else {
                                        1 + (if (milestoneHaveDelivered) 1 else 0) + (if (milestoneNeedDelivered) 1 else 0)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Milestones Tracker icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ESCROW TRADE MILESTONES ($checkedCount/3)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = if (isMilestonesExpanded) "Collapse ▲" else "Expand ▼",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                AnimatedVisibility(visible = isMilestonesExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Milestone 1 (System Escrow - Locked)
                                        MilestoneRow(
                                            title = "1. Financial Credit Escrow Locked",
                                            isChecked = true,
                                            onCheckedChange = {},
                                            enabled = false
                                        )
                                        
                                        // Milestone 2
                                        MilestoneRow(
                                            title = "2. ${listing.ownerName} delivers '${listing.haveItem}'",
                                            isChecked = if (currentTradeState == BarterViewModel.TradeState.COMPLETED) true else milestoneHaveDelivered,
                                            onCheckedChange = { 
                                                if (currentTradeState != BarterViewModel.TradeState.COMPLETED) {
                                                    milestoneHaveDelivered = it
                                                    viewModel.sendChatMessage(
                                                        listingId = listing.id,
                                                        recipientName = listing.ownerName,
                                                        messageText = if (it) "✅ Milestone Met: '${listing.haveItem}' marked as DELIVERED!" else "⚠️ Milestone Reverted: '${listing.haveItem}' marked as PENDING."
                                                    )
                                                }
                                            },
                                            enabled = currentTradeState != BarterViewModel.TradeState.COMPLETED
                                        )
                                        
                                        // Milestone 3
                                        MilestoneRow(
                                            title = "3. You deliver '${listing.needItem}'",
                                            isChecked = if (currentTradeState == BarterViewModel.TradeState.COMPLETED) true else milestoneNeedDelivered,
                                            onCheckedChange = { 
                                                if (currentTradeState != BarterViewModel.TradeState.COMPLETED) {
                                                    milestoneNeedDelivered = it
                                                    viewModel.sendChatMessage(
                                                        listingId = listing.id,
                                                        recipientName = listing.ownerName,
                                                        messageText = if (it) "✅ Milestone Met: '${listing.needItem}' marked as DELIVERED!" else "⚠️ Milestone Reverted: '${listing.needItem}' marked as PENDING."
                                                    )
                                                }
                                            },
                                            enabled = currentTradeState != BarterViewModel.TradeState.COMPLETED
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                        
                        // Lifecycle interactive action row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (currentTradeState) {
                                BarterViewModel.TradeState.NEGOTIATING -> {
                                    Text(
                                        "Draft a Service Exchange Agreement to lock exchange rules in escrow.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = { showSignContractDialog = true },
                                        modifier = Modifier.testTag("draft_agreement_btn")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Sign Agreement", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Draft Agreement")
                                    }
                                }
                                BarterViewModel.TradeState.AGREEMENT_SIGNED -> {
                                    Text(
                                        "Contract signed securely. Set the exchange status to In Progress to begin work.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.updateTradeState(listing.id, BarterViewModel.TradeState.IN_PROGRESS)
                                            viewModel.sendChatMessage(
                                                listingId = listing.id,
                                                recipientName = listing.ownerName,
                                                messageText = "⚡ Status Update: Agreement active! Swap has officially transitioned to IN PROGRESS."
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("start_work_btn")
                                    ) {
                                        Text("Start Work")
                                    }
                                }
                                BarterViewModel.TradeState.IN_PROGRESS -> {
                                    Text(
                                        "Fulfill milestones, then release credit escrow to complete.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { showFulfillDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.testTag("fulfill_swap_btn")
                                    ) {
                                        Text("Complete Swap")
                                    }
                                }
                                BarterViewModel.TradeState.UNDER_REVIEW -> {
                                    Text(
                                        "Swap is under final review for trust verifications.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                BarterViewModel.TradeState.COMPLETED -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Barter Swap fully completed & verified!",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showSignContractDialog) {
                    val walletBalance by viewModel.walletBalance.collectAsState()
                    SignContractDialog(
                        listing = listing,
                        walletBalance = walletBalance,
                        onDismiss = { showSignContractDialog = false },
                        onSign = { finalVal, yourVal, theirVal, shortfall, sign ->
                            viewModel.updateTradeState(
                                listing.id,
                                BarterViewModel.TradeState.AGREEMENT_SIGNED,
                                signedSelfValue = yourVal,
                                signedCounterpartyValue = theirVal
                            )
                            signedYourVal = yourVal
                            signedTheirVal = theirVal
                            
                            val shortfallMsg = if (shortfall > 0) {
                                "⚠️ Value Imbalance Shortfall: You will pay an extra $shortfall credits to ${listing.ownerName} upon final fulfillment."
                            } else if (shortfall < 0) {
                                "✨ Value Imbalance Shortfall: ${listing.ownerName} will pay an extra ${-shortfall} credits to you upon final fulfillment."
                            } else {
                                "🤝 Perfectly balanced cashless exchange!"
                            }
                            
                            val fullContractMsg = """
                                📄 SERVICE EXCHANGE AGREEMENT SECURED
                                Digital Stamp: [$sign]
                                • Your Service [${listing.needItem}]: $yourVal credits
                                • Their Service [${listing.haveItem}]: $theirVal credits
                                • Locked Escrow Valuation: $finalVal credits
                                $shortfallMsg
                            """.trimIndent()
                            
                            viewModel.sendChatMessage(
                                listingId = listing.id,
                                recipientName = listing.ownerName,
                                messageText = fullContractMsg
                            )
                            showSignContractDialog = false
                            showSignSuccessAnimation = true
                        }
                    )
                }

                if (showFulfillDialog) {
                    val mePartyId = myProfile?.userId ?: "me"
                    val persistedValuations by viewModel.tradeValuations.collectAsState()
                    // Persisted valuations are the source of truth (survive a restart between
                    // signing and fulfillment); fall back to in-memory state if absent.
                    val (persistedSelf, persistedCounterparty) = persistedValuations[listing.id]
                        ?: (0 to 0)
                    val effectiveSelfVal = if (persistedSelf > 0) persistedSelf else signedYourVal
                    val effectiveCounterpartyVal = if (persistedCounterparty > 0) persistedCounterparty else signedTheirVal
                    val shortfallResult = remember(effectiveSelfVal, effectiveCounterpartyVal, mePartyId, listing.ownerId) {
                        ValueShortfallCalculator.calculate(
                            PartyValuation(mePartyId, "You", effectiveSelfVal),
                            PartyValuation(listing.ownerId, listing.ownerName, effectiveCounterpartyVal)
                        )
                    }
                    FulfillTradeDialog(
                        counterpartyName = listing.ownerName,
                        result = shortfallResult,
                        mePartyId = mePartyId,
                        onDismiss = { showFulfillDialog = false },
                        onFulfill = {
                            viewModel.settleShortfall(shortfallResult, mePartyId, listing.ownerName)
                            viewModel.updateTradeState(listing.id, BarterViewModel.TradeState.COMPLETED)
                            val settlementMsg = when {
                                shortfallResult.shortfall <= 0 ->
                                    "🎉 Swap fully completed! Balanced cashless exchange — no credits changed hands."
                                ValueShortfallCalculator.isPayer(shortfallResult, mePartyId) ->
                                    "🎉 Swap fully completed! You settled a ${shortfallResult.shortfall} credit shortfall to ${listing.ownerName}."
                                else ->
                                    "🎉 Swap fully completed! ${listing.ownerName} settled a ${shortfallResult.shortfall} credit shortfall to you."
                            }
                            viewModel.sendChatMessage(
                                listingId = listing.id,
                                recipientName = listing.ownerName,
                                messageText = settlementMsg
                            )
                            showFulfillDialog = false
                            showFulfillSuccessAnimation = true
                        }
                    )
                }
            }

            // Messages feed
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No messages yet.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Express interest professionally and initiate exchange terms!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { message ->
                        val isFromMe = message.senderId == "me"
                        ChatBubble(
                            message = message,
                            isMe = isFromMe,
                            isOnline = if (isFromMe) null else partnerProfile?.isOnline,
                            isDndMode = if (isFromMe) null else partnerProfile?.isDndMode,
                            modifier = Modifier.testTag("chat_msg_${message.id}")
                        )
                    }
                }
            }

            // Secure bottom typing bar
            if (isBlocked) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .testTag("chat_blocked_banner"),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "You blocked this user. Messaging is disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Propose barter details securely...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendChatMessage(
                                    listingId = listingId,
                                    recipientName = activeListing?.ownerName ?: "Barter Member",
                                    messageText = messageText.trim()
                                )
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("send_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send secure chat message",
                            tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            }
        }
    }

    if (showSignSuccessAnimation) {
        CelebrationAnimationOverlay(
            isFulfillment = false,
            onDismiss = { showSignSuccessAnimation = false }
        )
    }

    if (showFulfillSuccessAnimation) {
        CelebrationAnimationOverlay(
            isFulfillment = true,
            onDismiss = { showFulfillSuccessAnimation = false }
        )
    }

    val counterpartyName = activeListing?.ownerName ?: partnerProfile?.name ?: "this member"

    if (showReportDialog) {
        ReportTradeDialog(
            counterpartyName = counterpartyName,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason ->
                if (partnerId.isNotEmpty()) {
                    viewModel.reportTrade(
                        listingId = listingId,
                        reportedUserId = partnerId,
                        reportedUserName = counterpartyName,
                        reason = reason
                    )
                }
                showReportDialog = false
            }
        )
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block $counterpartyName?") },
            text = { Text("Their listings will be hidden from your matches and feed, and you won't be able to message each other. You can unblock them later from this menu.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (partnerId.isNotEmpty()) {
                            viewModel.blockUser(partnerId, counterpartyName, listingId)
                        }
                        showBlockConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_block_btn")
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel") }
            },
            modifier = Modifier.testTag("block_user_dialog")
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel this trade?") },
            text = { Text("The current agreement will be withdrawn and the swap reset to negotiating. A note will be posted in the chat. This can't be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelTrade(listingId, counterpartyName)
                        showCancelConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_cancel_trade_btn")
                ) {
                    Text("Cancel trade")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Keep trade") }
            },
            modifier = Modifier.testTag("cancel_trade_dialog")
        )
    }
}


