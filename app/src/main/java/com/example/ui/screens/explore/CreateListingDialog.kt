package com.example.ui.screens.explore

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.data.PhotoStorageHelper
import com.example.data.model.ListingEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.glassmorphic
import kotlinx.coroutines.launch
import java.io.File
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingDialog(
    profileCountry: String,
    onDismiss: () -> Unit,
    onSubmit: (have: String, need: String, categoryHave: String, categoryNeed: String, desc: String, haveType: String, needType: String, deliveryMode: String, photoUri: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var capturedPhotoUri by remember { mutableStateOf<String?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    // Launcher for taking picture via camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                val savedUriString = PhotoStorageHelper.saveTempFileToOfflineStorage(context, file)
                capturedPhotoUri = savedUriString
            }
        }
    }

    // Launcher for camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = PhotoStorageHelper.createTempImageFile(context)
                tempPhotoFile = tempFile
                val uri = PhotoStorageHelper.getUriForFile(context, tempFile)
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var haveText by remember { mutableStateOf("") }
    var needText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }
    
    // Offering & Needed Types: "Service" or "Product"
    var haveType by remember { mutableStateOf("Service") }
    var needType by remember { mutableStateOf("Service") }

    // Delivery Mode: "Online" or "Physical In-Person Handover"
    var deliveryMode by remember { mutableStateOf("Online") }

    val serviceCats = listOf("Design & Creative", "Tech Support & Coding", "Education & Tutoring", "Music Lessons", "Organizing & Cleaning", "Photography & Video")
    val productCats = listOf("Musical Instruments", "Handcrafted Art & Goods", "Home Decor", "Books & Study Materials", "Electronics")

    val catsHave = if (haveType == "Service") serviceCats else productCats
    val catsNeed = if (needType == "Service") serviceCats else productCats

    var selectedCatHave by remember(haveType) { mutableStateOf(catsHave[0]) }
    var selectedCatNeed by remember(needType) { mutableStateOf(catsNeed[0]) }

    val isHandoverRestricted = deliveryMode == "Physical In-Person Handover" && profileCountry != "USA"
    val isFormValid = haveText.isNotBlank() && needText.isNotBlank() && !isHandoverRestricted

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 640.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Safe Swap verify",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Propose SFW Swap",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Configure your safe physical products or services to swap. Handover modes are dynamically regulated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // 1. WHAT ARE YOU OFFERING? (HAVE)
                item {
                    Text(
                        "Step 1: Your Offering",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    // Type selector: Service vs Product
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Service", "Product").forEach { type ->
                            val isSelected = haveType == type
                            Surface(
                                onClick = { haveType = type },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                    Text(
                                        text = "Offering $type",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = haveText,
                        onValueChange = { haveText = it },
                        label = { Text("What specific ${haveType.lowercase()} are you offering?") },
                        placeholder = { Text(if (haveType == "Service") "e.g. 1-Hour Guitar Coaching Class" else "e.g. Classical Acoustic Guitar (Warm wood)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_have_input"),
                        singleLine = true
                    )
                }

                item {
                    Text("Offering SFW Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(catsHave) { c ->
                            FilterChip(
                                selected = selectedCatHave == c,
                                onClick = { selectedCatHave = c },
                                label = { Text(c) }
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }

                // ATTACH ITEM PHOTO (WITH REAL CAMERA CAPTURE + SECURE OFFLINE LOCAL RETRIEVAL)
                item {
                    Text(
                        "Step 1b: item visual verification (Camera & Offline storage)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (capturedPhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = capturedPhotoUri,
                                    contentDescription = "Captured trade item photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Clear photo button
                                IconButton(
                                    onClick = { capturedPhotoUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove captured photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    Text(
                                        "OFFLINE PHOTO SECURED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Camera attachment overview",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Please snap a photo of the item to proceed safely",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        // Capture triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Trigger real Camera capture
                                    val permission = android.Manifest.permission.CAMERA
                                    val isPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context, permission
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (isPermissionGranted) {
                                        try {
                                            val tempFile = PhotoStorageHelper.createTempImageFile(context)
                                            tempPhotoFile = tempFile
                                            val uri = PhotoStorageHelper.getUriForFile(context, tempFile)
                                            takePictureLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(permission)
                                    }
                                },
                                modifier = Modifier.weight(1.1f).testTag("dialog_camera_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Camera launch icon",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Snap Photo", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            // Simulation trigger for quick UI presentation
                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val mockUri = PhotoStorageHelper.saveSimulatedItemPhoto(
                                            context,
                                            haveText.ifEmpty { "Swap Item Preview" },
                                            selectedCatHave
                                        )
                                        capturedPhotoUri = mockUri
                                    }
                                },
                                modifier = Modifier.weight(0.9f).testTag("dialog_camera_sim_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Simulate camera photo launch",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulate", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }

                // 2. WHAT DO YOU NEED? (NEED)
                item {
                    Text(
                        "Step 2: What You Need",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    // Type selector: Service vs Product
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Service", "Product").forEach { type ->
                            val isSelected = needType == type
                            Surface(
                                onClick = { needType = type },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                    Text(
                                        text = "Seeking $type",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = needText,
                        onValueChange = { needText = it },
                        label = { Text("What specific ${needType.lowercase()} do you need?") },
                        placeholder = { Text(if (needType == "Service") "e.g. Website development support" else "e.g. Studio recording microphone") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_need_input"),
                        singleLine = true
                    )
                }

                item {
                    Text("Needed SFW Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(catsNeed) { c ->
                            FilterChip(
                                selected = selectedCatNeed == c,
                                onClick = { selectedCatNeed = c },
                                label = { Text(c) }
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }

                // 3. DELIVERY & USA RESTRICTIONS
                item {
                    Text(
                        "Step 3: Handover Mode & Delivery",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Online", "Physical In-Person Handover").forEach { mode ->
                            val isSelected = deliveryMode == mode
                            Surface(
                                onClick = { deliveryMode = mode },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                    Text(
                                        text = mode.replace("Physical ", ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // GEOPRUDENTIAL WARNING BANNER FOR PHYSICAL HANDOVERS OUTSIDE USA
                if (deliveryMode == "Physical In-Person Handover") {
                    item {
                        val isUSA = profileCountry.replace(" ", "").equals("USA", ignoreCase = true) || profileCountry.contains("USA", ignoreCase = true) || profileCountry.contains("NY", ignoreCase = true)
                        val bannerColor = if (isUSA) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        val bannerText = if (isUSA) {
                            "✅ Verified US location record: physical handovers permitted under platform mutual safety compliance logs."
                        } else {
                            "⚠️ RESTRICTED ACCESS: Physical handovers are strictly locked to USA regional cooperatives due to cross-border logistical & security regulations. Non-US residents must use Online Swap mode."
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = bannerColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isUSA) Icons.Default.Info else Icons.Default.Warning,
                                    contentDescription = "Handover status icon",
                                    tint = if (isUSA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUSA) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // SHIPPING/TRACKING placeholder
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "shipping track",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "📦 International freight labels, courier tracking, and import duties checklists are coming in the next Pro release.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = descText,
                        onValueChange = { descText = it },
                        label = { Text("Details & Safe-Exchange conditions") },
                        placeholder = { Text("Outline safety boundaries, timing, or delivery specifications...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("dialog_desc_input"),
                        maxLines = 4
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_cancel")) {
                            Text("Discard")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isFormValid) {
                                    onSubmit(haveText, needText, selectedCatHave, selectedCatNeed, descText, haveType, needType, deliveryMode, capturedPhotoUri)
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier.testTag("dialog_submit")
                        ) {
                            Text("Broadcast SFW Proposit")
                        }
                    }
                }
            }
        }
    }
}
