package com.wonderart.app.ui.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.NotificationItem
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.model.WonderUser
import com.wonderart.app.ui.components.AvatarCircle
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderCoralSoft
import com.wonderart.app.ui.theme.WonderErrorRed
import com.wonderart.app.ui.theme.WonderPurple
import com.wonderart.app.ui.theme.WonderPurpleSoft

@Composable
fun ProfileScreen(
    userState: UiState<WonderUser?>,
    isAdmin: Boolean,
    enrollmentsState: UiState<List<Enrollment>>,
    postsState: UiState<List<CommunityPost>>,
    notificationsState: UiState<List<NotificationItem>>,
    savedDraftsCount: Int,
    onUpdateDisplayName: (String) -> Unit,
    onOpenMyCourses: () -> Unit,
    onOpenSavedDrawings: () -> Unit,
    onOpenProgressBadges: () -> Unit,
    onOpenLiveClasses: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = (userState as? UiState.Success)?.data
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: "Wonderart Artist"
    val email = user?.email ?: ""
    val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()

    val myPostsCount = remember(posts, user) {
        if (user == null) 0 else posts.count { it.userId == user.uid }
    }
    val completedCourses = remember(enrollments) {
        enrollments.count { it.completionStatus == "completed" }
    }
    val unreadNotifications = remember(notifications) {
        notifications.count { !it.read }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember(displayName) { mutableStateOf(displayName) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("profile_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Header Card matching Figma 11
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AvatarCircle(
                            name = displayName,
                            size = 76.dp,
                            containerColor = WonderPurple,
                            textColor = Color.White
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    editedName = displayName
                                    showEditNameDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("profile_edit_name_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit display name",
                                    tint = WonderPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (email.isNotBlank()) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isAdmin) WonderCoralSoft else WonderPurpleSoft
                        ) {
                            Text(
                                text = if (isAdmin) "Teacher • Administrator (Nikhil)" else "Student • Wonderart Studio",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isAdmin) WonderCoral else WonderPurple,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatColumn(
                                value = "${enrollments.size}",
                                label = "Courses"
                            )
                            ProfileStatColumn(
                                value = "$myPostsCount",
                                label = "Published"
                            )
                            ProfileStatColumn(
                                value = "$completedCourses",
                                label = "Completed"
                            )
                        }
                    }
                }
            }

            if (isAdmin) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenAdminDashboard)
                            .testTag("profile_admin_dashboard_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = WonderPurpleSoft,
                        border = BorderStroke(1.dp, WonderPurple.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = null,
                                tint = WonderPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Teacher Admin Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WonderPurple
                                )
                                Text(
                                    text = "Manage courses, students, live Zoom classes & moderation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = WonderPurple
                            )
                        }
                    }
                }
            }

            // Menu List Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Filled.MenuBook,
                            title = "My Courses",
                            subtitle = "${enrollments.size} enrolled courses",
                            testTag = "profile_menu_my_courses",
                            onClick = onOpenMyCourses
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileMenuRow(
                            icon = Icons.Filled.Brush,
                            title = "Saved Drawings (Offline)",
                            subtitle = "$savedDraftsCount local drafts on device",
                            testTag = "profile_menu_saved_drawings",
                            onClick = onOpenSavedDrawings
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileMenuRow(
                            icon = Icons.Filled.EmojiEvents,
                            title = "Progress & Badges",
                            subtitle = "${user?.xp ?: 0} XP • ${user?.streakDays ?: 1}-day streak",
                            testTag = "profile_menu_progress",
                            onClick = onOpenProgressBadges
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileMenuRow(
                            icon = Icons.Filled.Videocam,
                            title = "Live Classes",
                            subtitle = "Upcoming Zoom workshops with Nikhil",
                            testTag = "profile_menu_live_classes",
                            onClick = onOpenLiveClasses
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileMenuRow(
                            icon = Icons.Filled.Notifications,
                            title = "Notifications",
                            subtitle = if (unreadNotifications > 0) "$unreadNotifications unread updates" else "All caught up",
                            badgeCount = unreadNotifications,
                            testTag = "profile_menu_notifications",
                            onClick = onOpenNotifications
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileMenuRow(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Log out",
                            subtitle = "Sign out of your Google account",
                            tint = WonderErrorRed,
                            showChevron = false,
                            testTag = "profile_logout_button",
                            onClick = onSignOut
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Update Profile Name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            onUpdateDisplayName(editedName.trim())
                            showEditNameDialog = false
                        }
                    },
                    modifier = Modifier.testTag("profile_name_save_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileStatColumn(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    badgeCount: Int = 0,
    showChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (tint == WonderErrorRed) WonderErrorRed else WonderPurple,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = tint
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (badgeCount > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = WonderCoral
            ) {
                Text(
                    text = "$badgeCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
