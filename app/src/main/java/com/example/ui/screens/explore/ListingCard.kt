package com.example.ui.screens.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ListingStatus
import com.example.data.categoryPinColor
import com.example.data.model.ListingEntity
import com.example.ui.components.BarterCard
import com.example.ui.theme.BarterSuccess

@Composable
fun ListingCard(
    listing: ListingEntity,
    distanceText: String,
    onSaveToggle: () -> Unit,
    onChatClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    isOnline: Boolean? = null,
    isDnd: Boolean? = null,
    showStatus: Boolean = false,
) {
    val accentColor = categoryPinColor(listing.categoryHave)

    BarterCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, accentColor, CircleShape)
                    .clickable { onProfileClick() },
            ) {
                AsyncImage(
                    model = listing.ownerAvatar,
                    contentDescription = "${listing.ownerName} avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (isOnline != null) {
                    val statusColor = when {
                        isDnd == true -> Color(0xFFFFB300)
                        isOnline -> BarterSuccess
                        else -> Color(0xFF9E9E9E)
                    }
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(1.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileClick() },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = listing.ownerName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (listing.isOwnerVerified) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Verified", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${listing.ownerRating} · ${listing.ownerRatingCount} swaps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onSaveToggle, modifier = Modifier.testTag("bookmark_button_${listing.id}")) {
                Icon(
                    imageVector = if (listing.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save offer",
                    tint = if (listing.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (showStatus && listing.listingStatus != ListingStatus.APPROVED) {
            Spacer(modifier = Modifier.height(8.dp))
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        when (listing.listingStatus) {
                            ListingStatus.PENDING_REVIEW -> "Under review"
                            ListingStatus.REJECTED -> "Needs changes"
                            else -> listing.listingStatus
                        },
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OfferColumn(
                modifier = Modifier.weight(1f),
                label = "Offers",
                value = listing.haveItem,
                category = listing.categoryHave,
            )
            OfferColumn(
                modifier = Modifier.weight(1f),
                label = "Wants",
                value = listing.needItem,
                category = listing.categoryNeed,
                isWant = true,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = listing.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (listing.photoUri != null) {
            Spacer(modifier = Modifier.height(8.dp))
            AsyncImage(
                model = listing.photoUri,
                contentDescription = "Offer photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(distanceText, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (listing.deliveryMode.contains("Physical")) "In person" else "Online",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onChatClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_barter_btn_${listing.id}"),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Message to swap")
        }
    }
}

@Composable
private fun RowScope.OfferColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    category: String,
    isWant: Boolean = false,
) {
    Column(
        modifier = modifier
            .background(
                if (isWant) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                RoundedCornerShape(8.dp),
            )
            .padding(10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
