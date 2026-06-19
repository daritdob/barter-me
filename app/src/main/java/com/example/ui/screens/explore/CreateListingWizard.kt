package com.example.ui.screens.explore

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.BARTER_CATEGORIES
import com.example.data.PhotoStorageHelper
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.viewmodel.ListingSubmitState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingWizard(
    profileCountry: String,
    submitState: ListingSubmitState,
    onDismiss: () -> Unit,
    onClearSubmitState: () -> Unit,
    onSubmit: (
        have: String, need: String, categoryHave: String, categoryNeed: String,
        desc: String, haveType: String, needType: String, deliveryMode: String, photoUri: String?
    ) -> Unit,
    initialHave: String = "",
    initialNeed: String = "",
    initialCategoryHave: String = BARTER_CATEGORIES.first(),
    initialCategoryNeed: String = BARTER_CATEGORIES.first(),
    initialDesc: String = "",
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var haveText by remember { mutableStateOf(initialHave) }
    var needText by remember { mutableStateOf(initialNeed) }
    var categoryHave by remember { mutableStateOf(initialCategoryHave) }
    var categoryNeed by remember { mutableStateOf(initialCategoryNeed) }
    var description by remember { mutableStateOf(initialDesc) }
    var haveType by remember { mutableStateOf("Service") }
    var needType by remember { mutableStateOf("Service") }
    var deliveryMode by remember { mutableStateOf("Online") }
    var guidelinesAccepted by remember { mutableStateOf(false) }
    var capturedPhotoUri by remember { mutableStateOf<String?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let { file ->
                capturedPhotoUri = PhotoStorageHelper.saveTempFileToOfflineStorage(context, file)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val tempFile = PhotoStorageHelper.createTempImageFile(context)
                tempPhotoFile = tempFile
                takePictureLauncher.launch(PhotoStorageHelper.getUriForFile(context, tempFile))
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(submitState) {
        when (submitState) {
            ListingSubmitState.Success -> {
                onClearSubmitState()
                onDismiss()
            }
            else -> Unit
        }
    }

    val isChecking = submitState is ListingSubmitState.Checking
    val failedReasons = (submitState as? ListingSubmitState.Failed)?.reasons.orEmpty()

    Dialog(
        onDismissRequest = { if (!isChecking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("create_listing_wizard"),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Post an offer") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isChecking) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                )

                LinearProgressIndicator(
                    progress = { (step + 1) / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isChecking -> {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Checking your offer…",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "Making sure it follows community guidelines",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when (step) {
                                0 -> StepOffer(
                                    title = "What are you offering?",
                                    text = haveText,
                                    onTextChange = { haveText = it },
                                    category = categoryHave,
                                    onCategoryChange = { categoryHave = it },
                                    itemType = haveType,
                                    onTypeChange = { haveType = it },
                                )
                                1 -> StepOffer(
                                    title = "What do you want in return?",
                                    text = needText,
                                    onTextChange = { needText = it },
                                    category = categoryNeed,
                                    onCategoryChange = { categoryNeed = it },
                                    itemType = needType,
                                    onTypeChange = { needType = it },
                                )
                                2 -> StepDetails(
                                    description = description,
                                    onDescriptionChange = { description = it },
                                    deliveryMode = deliveryMode,
                                    onDeliveryModeChange = { deliveryMode = it },
                                    photoUri = capturedPhotoUri,
                                    onTakePhoto = {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    },
                                    profileCountry = profileCountry,
                                )
                                3 -> StepGuidelines(
                                    accepted = guidelinesAccepted,
                                    onAcceptedChange = { guidelinesAccepted = it },
                                    failedReasons = failedReasons,
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (step > 0) {
                                SecondaryButton(
                                    text = "Back",
                                    onClick = { step -= 1 },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (step < 3) {
                                PrimaryButton(
                                    text = "Continue",
                                    onClick = { step += 1 },
                                    modifier = Modifier.weight(1f),
                                    enabled = stepValid(step, haveText, needText, description, guidelinesAccepted),
                                )
                            } else {
                                PrimaryButton(
                                    text = "Submit for review",
                                    onClick = {
                                        onClearSubmitState()
                                        onSubmit(
                                            haveText.trim(), needText.trim(),
                                            categoryHave, categoryNeed,
                                            description.trim(), haveType, needType,
                                            deliveryMode, capturedPhotoUri,
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = guidelinesAccepted &&
                                        stepValid(step, haveText, needText, description, guidelinesAccepted),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun stepValid(
    step: Int,
    have: String,
    need: String,
    desc: String,
    guidelines: Boolean,
): Boolean = when (step) {
    0 -> have.trim().length >= 3
    1 -> need.trim().length >= 3
    2 -> desc.trim().length >= 10
    3 -> guidelines
    else -> true
}

@Composable
private fun StepOffer(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    itemType: String,
    onTypeChange: (String) -> Unit,
) {
    SectionHeader(title = title, subtitle = "Be specific so others know what to expect.")
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Short title") },
        singleLine = true,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = itemType == "Service",
            onClick = { onTypeChange("Service") },
            label = { Text("Service") },
        )
        FilterChip(
            selected = itemType == "Product",
            onClick = { onTypeChange("Product") },
            label = { Text("Product") },
        )
    }
    Text("Category", style = MaterialTheme.typography.labelLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BARTER_CATEGORIES) { cat ->
            FilterChip(
                selected = category == cat,
                onClick = { onCategoryChange(cat) },
                label = { Text(cat) },
            )
        }
    }
}

@Composable
private fun StepDetails(
    description: String,
    onDescriptionChange: (String) -> Unit,
    deliveryMode: String,
    onDeliveryModeChange: (String) -> Unit,
    photoUri: String?,
    onTakePhoto: () -> Unit,
    profileCountry: String,
) {
    SectionHeader(
        title = "Add details",
        subtitle = "Help people understand scope, timing, and how the swap works.",
    )
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        label = { Text("Description") },
        minLines = 4,
    )
    Text("How will you swap?", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = deliveryMode == "Online",
            onClick = { onDeliveryModeChange("Online") },
            label = { Text("Online") },
        )
        FilterChip(
            selected = deliveryMode.contains("Physical"),
            onClick = { onDeliveryModeChange("Physical In-Person Handover") },
            label = { Text("In person") },
            enabled = profileCountry == "USA",
        )
    }
    if (profileCountry != "USA") {
        Text(
            "In-person swaps are only available in the USA.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FilledTonalButton(onClick = onTakePhoto) {
        Icon(Icons.Default.CameraAlt, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (photoUri == null) "Add photo (optional)" else "Retake photo")
    }
    if (photoUri != null) {
        AsyncImage(
            model = photoUri,
            contentDescription = "Offer photo",
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun StepGuidelines(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    failedReasons: List<String>,
) {
    SectionHeader(
        title = "Community guidelines",
        subtitle = "Offers are checked before they appear in the feed.",
    )
    val rules = listOf(
        "Swaps only — no cash sales, crypto, or prohibited items",
        "Be honest about what you offer and what you expect",
        "In-person item swaps need a photo",
        "Keep descriptions respectful and accurate",
    )
    rules.forEach { rule ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(rule, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (failedReasons.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "Please fix these issues:",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                failedReasons.forEach { reason ->
                    Text("• $reason", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Checkbox(checked = accepted, onCheckedChange = onAcceptedChange)
        Text(
            "I confirm this is a good-faith swap offer",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
