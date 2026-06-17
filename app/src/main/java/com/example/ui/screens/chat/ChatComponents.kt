package com.example.ui.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ListingEntity
import com.example.ui.viewmodel.BarterViewModel
import java.text.SimpleDateFormat
import java.util.*
@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    isMe: Boolean,
    isOnline: Boolean? = null,
    isDndMode: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { formatter.format(Date(message.timestamp)) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.messageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isMe && isOnline != null) {
                    val dotColor = if (isDndMode == true) {
                        Color(0xFFFFB300)
                    } else if (isOnline) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFF9E9E9E)
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Text(
                    text = if (isMe) "You" else message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "• $timeString",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignContractDialog(
    listing: ListingEntity,
    walletBalance: Int,
    onDismiss: () -> Unit,
    onSign: (finalValuation: Int, yourVal: Int, theirVal: Int, shortfall: Int, signature: String) -> Unit
) {
    var yourValStr by remember { mutableStateOf("1500") }
    var theirValStr by remember { mutableStateOf("1500") }
    var signatureInput by remember { mutableStateOf("") }
    
    val yourVal = yourValStr.toIntOrNull() ?: 0
    val theirVal = theirValStr.toIntOrNull() ?: 0
    val shortfall = theirVal - yourVal
    val hasShortfall = shortfall > 0
    val requiresWalletPay = shortfall > 0
    val isInsufficientBalance = requiresWalletPay && walletBalance < shortfall
    
    // Final dynamic escrow value (higher of the two or a standardized contract value)
    val finalEscrowValuation = maxOf(yourVal, theirVal)
    
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Balance,
                        contentDescription = "Balance icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Fair-Swap Agreement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    "Calibrate the cooperative credit valuations of your services to resolve any value imbalances fairly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                // Input fields side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yourValStr,
                        onValueChange = { yourValStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Your Work Value") },
                        placeholder = { Text("credits") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contract_your_val_input"),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    
                    OutlinedTextField(
                        value = theirValStr,
                        onValueChange = { theirValStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Their Work Value") },
                        placeholder = { Text("credits") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contract_their_val_input"),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                // IMMENSE DYNAMIC SHORTFALL CALCULATOR CARD
                val cardBgColor = if (isInsufficientBalance) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                } else if (shortfall == 0) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "VALUE SHORTFALL CALCULATOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isInsufficientBalance) MaterialTheme.colorScheme.error 
                                        else MaterialTheme.colorScheme.primary
                            )
                            
                            val statusLabel = when {
                                shortfall == 0 -> "Balanced"
                                shortfall > 0 -> "Your Shortfall"
                                else -> "Their Shortfall"
                            }
                            val badgeColor = when {
                                shortfall == 0 -> Color(0xFF2E7D32)
                                shortfall > 0 && isInsufficientBalance -> MaterialTheme.colorScheme.error
                                shortfall > 0 -> Color(0xFFFFB300)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            
                            Surface(
                                color = badgeColor,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Your Service (${listing.needItem}):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$yourVal credits", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Their Service (${listing.haveItem}):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$theirVal credits", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Value Difference:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("${Math.abs(shortfall)} credits", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Actionable explanation
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (isInsufficientBalance) Icons.Default.Warning else Icons.Default.Info,
                                contentDescription = "status detail",
                                tint = if (isInsufficientBalance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val analysisText = when {
                                shortfall == 0 -> "Perfect Cashless Exchange: Mutual service valuations match exactly. No shortfall balances to resolve."
                                shortfall > 0 -> "You have a shortfall of $shortfall credits. You will deliver your service and pay their escrow $shortfall credits upon trade fulfillment."
                                else -> "${listing.ownerName} has a shortfall of ${-shortfall} credits. They will deliver their service and pay your escrow ${-shortfall} credits upon trade fulfillment."
                            }
                            Text(
                                analysisText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (shortfall > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = "wallet status",
                                    tint = if (isInsufficientBalance) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Your wallet balance: $walletBalance credits.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInsufficientBalance) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                )
                            }
                            
                            if (isInsufficientBalance) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "⚠️ You need ${shortfall - walletBalance} more credits to cover this swap imbalance. You can buy credit packs inside the Profile store.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // General rules notice
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "By signing this digital contract, you lock $finalEscrowValuation credits in matching escrow. Balances will transfer automatically when both milestone deliveries are checked.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OutlinedTextField(
                    value = signatureInput,
                    onValueChange = { signatureInput = it },
                    label = { Text("Type Full Name to Sign Contract") },
                    placeholder = { Text("e.g. Alex Mercer") },
                    modifier = Modifier.fillMaxWidth().testTag("contract_signature_input"),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("contract_discard_btn")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (signatureInput.isNotBlank() && !isInsufficientBalance) {
                                onSign(finalEscrowValuation, yourVal, theirVal, shortfall, signatureInput)
                            }
                        },
                        enabled = signatureInput.isNotBlank() && !isInsufficientBalance,
                        modifier = Modifier.testTag("contract_sign_btn")
                    ) {
                        Text("Sign & Lock Escrow")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillTradeDialog(
    onDismiss: () -> Unit,
    onFulfill: (credits: Int) -> Unit
) {
    var confirmedValuation by remember { mutableStateOf("1500") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Fulfill & Release Escrow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                
                Text(
                    "Both parties have satisfied their barter agreements. By clicking complete, the platform will release the matching escrow and transfer credits.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                
                OutlinedTextField(
                    value = confirmedValuation,
                    onValueChange = { confirmedValuation = it.filter { char -> char.isDigit() } },
                    label = { Text("Fulfill Credit Release Amount") },
                    modifier = Modifier.fillMaxWidth().testTag("fulfill_valuation_input"),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("fulfill_cancel_btn")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val credits = confirmedValuation.toIntOrNull() ?: 1500
                            onFulfill(credits)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.testTag("fulfill_confirm_btn")
                    ) {
                        Text("Confirm Completion")
                    }
                }
            }
        }
    }
}

@Composable
fun MilestoneRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f) 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag("checkbox_${title.substringBefore(" ").replace(".", "")}")
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
            color = if (isChecked) MaterialTheme.colorScheme.onSurface 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CelebrationAnimationOverlay(
    isFulfillment: Boolean,
    onDismiss: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnim = true
        kotlinx.coroutines.delay(2800)
        onDismiss()
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val rotateDegrees by animateFloatAsState(
        targetValue = if (startAnim) 360f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "rotate"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val shimOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f * alpha))
            .clickable { onDismiss() } // Tap to dismiss early
            .testTag("celebration_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Falling icons/particles simulator
        Box(modifier = Modifier.fillMaxSize()) {
            listOf(
                Triple("🤝", 0.1f, 150),
                Triple("✨", 0.25f, 320),
                Triple("💰", 0.4f, 480),
                Triple("🤝", 0.6f, 220),
                Triple("✨", 0.75f, 500),
                Triple("💰", 0.9f, 180),
                Triple("☘️", 0.15f, 600),
                Triple("⭐", 0.82f, 400),
                Triple("💎", 0.52f, 300)
            ).forEachIndexed { i, particle ->
                val emoji = particle.first
                val xFraction = particle.second
                val speed = particle.third
                
                var itemY by remember { mutableStateOf(-50f) }
                LaunchedEffect(startAnim) {
                    if (startAnim) {
                        val duration = (900000 / speed).toLong()
                        while (true) {
                            itemY = -50f
                            // Smoothly drop item down
                            val steps = 100
                            val stepDelay = duration / steps
                            for (step in 0..steps) {
                                itemY = (step.toFloat() / steps) * 1200f
                                kotlinx.coroutines.delay(stepDelay)
                            }
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .offset(
                            xFraction.coerceAtLeast(0f).coerceAtMost(1f).times(300).dp,
                            itemY.dp
                        )
                ) {
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Center card with bouncy entry
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main rotating emblem
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer(rotationZ = rotateDegrees)
                        .background(
                            color = if (isFulfillment) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFulfillment) Icons.Default.Handshake else Icons.Default.CheckCircle,
                        contentDescription = "Trade completion icon",
                        tint = if (isFulfillment) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Text(
                    text = if (isFulfillment) "Swap Fulfilled!" else "Agreement Signed!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isFulfillment) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Text(
                    text = if (isFulfillment) 
                        "The mutual services have been delivered, and matching escrows have been released to both wallets!"
                        else "You have digitally signed and committed to the physical/digital trade proposal, locking credit values in secure escrow.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFulfillment) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Superb Swap!")
                }
            }
        }
    }
}
