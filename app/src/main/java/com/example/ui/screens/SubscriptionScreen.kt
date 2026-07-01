package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.BarterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: BarterViewModel,
    onNavigateBack: () -> Unit
) {
    val activity = LocalActivity.current ?: return
    val subscriptionType by viewModel.subscriptionType.collectAsState()
    val totalOffersCreated by viewModel.totalOffersCreated.collectAsState()
    val remainingOffers = viewModel.getRemainingFreeOffers()
    
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf("MONTHLY") }
    var purchaseInProgress by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription Plans") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            CurrentStatusCard(
                subscriptionType = subscriptionType,
                totalOffersCreated = totalOffersCreated,
                remainingOffers = remainingOffers
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (subscriptionType == "FREE") {
                SubscriptionPlanCard(
                    title = "Monthly Plan",
                    price = "$5",
                    period = "per month",
                    features = listOf(
                        "Unlimited swaps and offers",
                        "Priority support",
                        "Advanced matching",
                        "Cancel anytime"
                    ),
                    isPopular = false,
                    onSelect = {
                        selectedPlan = "MONTHLY"
                        showPurchaseDialog = true
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SubscriptionPlanCard(
                    title = "Lifetime Plan",
                    price = "$49",
                    period = "one-time payment",
                    features = listOf(
                        "Unlimited swaps and offers forever",
                        "Priority support",
                        "Advanced matching",
                        "Best value - save 91%!"
                    ),
                    isPopular = true,
                    onSelect = {
                        selectedPlan = "LIFETIME"
                        showPurchaseDialog = true
                    }
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "You're All Set!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = when (subscriptionType) {
                                "MONTHLY" -> "You have an active Monthly subscription"
                                "LIFETIME" -> "You have a Lifetime subscription"
                                else -> "Active subscription"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Enjoy unlimited swaps and offers!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { if (!purchaseInProgress) showPurchaseDialog = false },
            title = {
                Text(
                    text = "Confirm Purchase",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (selectedPlan == "MONTHLY") {
                            "Subscribe to the Monthly Plan for $5/month?\n\nYou'll get unlimited swaps and offers."
                        } else {
                            "Purchase the Lifetime Plan for $49 (one-time payment)?\n\nYou'll get unlimited swaps and offers forever!"
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        purchaseInProgress = true
                        viewModel.purchaseSubscription(
                            activity = activity,
                            isMonthly = selectedPlan == "MONTHLY"
                        ) { success, message ->
                            purchaseInProgress = false
                            if (success) {
                                showPurchaseDialog = false
                            }
                        }
                    },
                    enabled = !purchaseInProgress
                ) {
                    if (purchaseInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPurchaseDialog = false },
                    enabled = !purchaseInProgress
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CurrentStatusCard(
    subscriptionType: String,
    totalOffersCreated: Int,
    remainingOffers: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = when (subscriptionType) {
                            "MONTHLY" -> "Monthly"
                            "LIFETIME" -> "Lifetime"
                            else -> "Free"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    imageVector = when (subscriptionType) {
                        "MONTHLY", "LIFETIME" -> Icons.Default.Star
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = when (subscriptionType) {
                        "MONTHLY", "LIFETIME" -> Color(0xFFFFD700)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Offers Created",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalOffersCreated.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (subscriptionType == "FREE") remainingOffers.toString() else "Unlimited",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (subscriptionType == "FREE" && remainingOffers == 0) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    title: String,
    price: String,
    period: String,
    features: List<String>,
    isPopular: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPopular) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            if (isPopular) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFFFFA500)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BEST VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPopular) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = "Select Plan",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
