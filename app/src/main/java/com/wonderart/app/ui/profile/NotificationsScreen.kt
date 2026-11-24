package com.wonderart.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wonderart.app.data.model.NotificationItem
import com.wonderart.app.data.model.UiState
import com.wonderart.app.ui.components.CircleBackButton
import com.wonderart.app.ui.components.EmptyStateCard
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderCoralSoft
import com.wonderart.app.ui.theme.WonderMint
import com.wonderart.app.ui.theme.WonderMintSoft
import com.wonderart.app.ui.theme.WonderPurple
import com.wonderart.app.ui.theme.WonderPurpleSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    notificationsState: UiState<List<NotificationItem>>,
    onBack: () -> Unit,
    onMarkRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val unreadIds = notifications.filter { !it.read }.map { it.id }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircleBackButton(onClick = onBack)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    if (unreadIds.isNotEmpty()) {
                        TextButton(
                            onClick = { unreadIds.forEach(onMarkRead) },
                            modifier = Modifier.testTag("notifications_mark_all_read")
                        ) {
                            Text("Mark all read", color = WonderPurple)
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.NotificationsNone,
                        title = "No notifications yet",
                        message = "Class reminders, enrollment updates, and announcements from Nikhil will show up here."
                    )
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    NotificationRowCard(
                        item = item,
                        onClick = {
                            if (!item.read) {
                                onMarkRead(item.id)
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NotificationRowCard(
    item: NotificationItem,
    onClick: () -> Unit
) {
    val (icon, tint, softBg) = when (item.type) {
        "class_reminder" -> Triple(Icons.Filled.Videocam, WonderCoral, WonderCoralSoft)
        "announcement" -> Triple(Icons.Filled.Campaign, WonderPurple, WonderPurpleSoft)
        "enrollment" -> Triple(Icons.Filled.School, WonderMint, WonderMintSoft)
        "community" -> Triple(Icons.Filled.Favorite, WonderCoral, WonderCoralSoft)
        else -> Triple(Icons.Filled.Notifications, WonderPurple, WonderPurpleSoft)
    }

    val timeStr = item.createdAt?.toDate()?.let {
        SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()).format(Date(it.time))
    } ?: "Just now"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("notification_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (item.read) MaterialTheme.colorScheme.outlineVariant else WonderPurple.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(softBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!item.read) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(WonderPurple)
                )
            }
        }
    }
}
