package com.wonderart.app.ui.liveclasses

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.LiveClass
import com.wonderart.app.data.model.LiveClassStatus
import com.wonderart.app.data.model.UiState
import com.wonderart.app.ui.components.CategoryPillRow
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
fun LiveClassesScreen(
    liveClassesState: UiState<List<LiveClass>>,
    enrollmentsState: UiState<List<Enrollment>>,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onOpenClassRoom: (String) -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    onSetReminder: (LiveClass) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("Upcoming & Live") }
    val filters = listOf("Upcoming & Live", "Live Now", "Completed", "All")

    val classes = (liveClassesState as? UiState.Success)?.data ?: emptyList()
    val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
    val enrolledCourseIds = remember(enrollments) { enrollments.map { it.courseId }.toSet() }

    val filtered = remember(classes, selectedFilter) {
        when (selectedFilter) {
            "Live Now" -> classes.filter { it.status == "live_now" }
            "Completed" -> classes.filter { it.status == "completed" }
            "All" -> classes
            else -> classes.filter { it.status == "upcoming" || it.status == "live_now" }
        }
    }

    var pendingReminderClass by remember { mutableStateOf<LiveClass?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            pendingReminderClass?.let { onSetReminder(it) }
            pendingReminderClass = null
        }
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleBackButton(onClick = onBack)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Live Online Classes", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Interactive Zoom workshops with Nikhil",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.size(44.dp))
                }
            }

            item {
                CategoryPillRow(
                    categories = filters,
                    selectedCategory = selectedFilter,
                    onSelectCategory = { selectedFilter = it }
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.Videocam,
                            title = "No live classes in this filter",
                            message = "Enroll in a course or check back when Nikhil schedules the next Zoom session."
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { liveClass ->
                    val isEnrolled = isAdmin || enrolledCourseIds.contains(liveClass.courseId)
                    val status = LiveClassStatus.fromRaw(liveClass.status)
                    val dateLabel = remember(liveClass.scheduledAtMillis) {
                        SimpleDateFormat("EEE, dd MMM • h:mm a", Locale.getDefault())
                            .format(Date(liveClass.scheduledAtMillis))
                    }

                    val (badgeBg, badgeText) = when (status) {
                        LiveClassStatus.LIVE_NOW -> WonderCoral to Color.White
                        LiveClassStatus.UPCOMING -> WonderPurpleSoft to WonderPurple
                        LiveClassStatus.COMPLETED -> WonderMintSoft to WonderMint
                        LiveClassStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenClassRoom(liveClass.id) }
                            .testTag("live_class_card_${liveClass.id}"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (status == LiveClassStatus.LIVE_NOW) WonderCoral else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = liveClass.courseTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WonderPurple
                                )
                                Surface(shape = RoundedCornerShape(50), color = badgeBg) {
                                    Text(
                                        text = status.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeText,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(liveClass.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = liveClass.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "$dateLabel • ${liveClass.durationMinutes} min • Meeting ${liveClass.zoomMeetingId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (status == LiveClassStatus.UPCOMING) {
                                    OutlinedButton(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {
                                                pendingReminderClass = liveClass
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                onSetReminder(liveClass)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Remind me")
                                    }
                                }

                                if (isEnrolled) {
                                    Button(
                                        onClick = { onOpenClassRoom(liveClass.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (status == LiveClassStatus.LIVE_NOW) WonderCoral else WonderPurple
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Videocam,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            when (status) {
                                                LiveClassStatus.LIVE_NOW -> "Join Live"
                                                LiveClassStatus.COMPLETED -> "Class Details"
                                                else -> "Open Classroom"
                                            }
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onOpenCourseDetail(liveClass.courseId) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Enroll to Join")
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
