package com.wonderart.app.ui.dashboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wonderart.app.data.model.Announcement
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.Course
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.LiveClass
import com.wonderart.app.data.model.LiveClassStatus
import com.wonderart.app.data.model.NotificationItem
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.model.WonderUser
import com.wonderart.app.data.seed.SampleCatalog
import com.wonderart.app.ui.components.ArtworkImage
import com.wonderart.app.ui.components.EmptyStateCard
import com.wonderart.app.ui.theme.WonderAmber
import com.wonderart.app.ui.theme.WonderAmberSoft
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderCoralSoft
import com.wonderart.app.ui.theme.WonderMint
import com.wonderart.app.ui.theme.WonderMintSoft
import com.wonderart.app.ui.theme.WonderPurple
import com.wonderart.app.ui.theme.WonderPurpleDark
import com.wonderart.app.ui.theme.WonderPurpleSoft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userState: UiState<WonderUser?>,
    isAdmin: Boolean,
    coursesState: UiState<List<Course>>,
    enrollmentsState: UiState<List<Enrollment>>,
    liveClassesState: UiState<List<LiveClass>>,
    announcementsState: UiState<List<Announcement>>,
    notificationsState: UiState<List<NotificationItem>>,
    postsState: UiState<List<CommunityPost>>,
    onOpenNotifications: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    onOpenLiveClasses: () -> Unit,
    onOpenLiveClassRoom: (String) -> Unit,
    onOpenCreativeCanvas: () -> Unit,
    onOpenPostDetail: (String) -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = (userState as? UiState.Success)?.data
    val displayName = user?.name?.takeIf { it.isNotBlank() } ?: "Artist"
    val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
    val courses = ((coursesState as? UiState.Success)?.data ?: emptyList())
        .ifEmpty { SampleCatalog.previewFallbackCourses }
    val liveClasses = (liveClassesState as? UiState.Success)?.data ?: emptyList()
    val announcements = (announcementsState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()

    val totalEnrolled = enrollments.size
    val completedCount = enrollments.count { it.completionStatus == "completed" }
    val ongoingCount = enrollments.count { it.completionStatus == "ongoing" }
    val overallProgress = if (enrollments.isEmpty()) 0 else enrollments.map { it.progress.toInt() }.average().toInt()
    val unreadNotifs = notifications.count { !it.read }

    var refreshing by remember { mutableStateOf(false) }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { refreshing = false },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isAdmin) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = onOpenAdminDashboard)
                                        .testTag("dashboard_admin_chip"),
                                    shape = RoundedCornerShape(50),
                                    color = WonderCoralSoft
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AdminPanelSettings,
                                            contentDescription = "Teacher Admin",
                                            tint = WonderCoral,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Admin",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WonderCoral,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dashboard_notifications_button"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                IconButton(onClick = onOpenNotifications) {
                                    Box {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = "Notifications",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (unreadNotifs > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(WonderCoral)
                                                    .align(Alignment.TopEnd)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Learning Progress Hero Card (Figma 19 inspired)
                item {
                    val xp = user?.xp ?: 50L
                    val level = (xp / 250L).toInt() + 1
                    val levelXp = (xp % 250L).toInt()
                    val levelTitle = when {
                        level >= 5 -> "Master Illustrator"
                        level >= 3 -> "Studio Painter"
                        else -> "Sketcher"
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(onClick = onOpenProgress)
                            .testTag("dashboard_progress_card"),
                        shape = RoundedCornerShape(22.dp),
                        color = WonderPurple
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(listOf(WonderPurple, WonderPurpleDark))
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "LEVEL $level • $levelTitle",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Overall Progress: $overallProgress%",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = WonderAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${user?.streakDays ?: 1}d streak",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            LinearProgressIndicator(
                                progress = { (levelXp / 250f).coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = WonderCoral,
                                trackColor = Color.White.copy(alpha = 0.22f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "$xp XP earned • Tap to view badges & progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    }
                }

                // 3-Stat Row (Enrolled / Ongoing / Completed)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardStatCard(
                            label = "Enrolled",
                            value = totalEnrolled.toString(),
                            accent = WonderPurple,
                            softBg = WonderPurpleSoft,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            label = "Ongoing",
                            value = ongoingCount.toString(),
                            accent = WonderCoral,
                            softBg = WonderCoralSoft,
                            modifier = Modifier.weight(1f)
                        )
                        DashboardStatCard(
                            label = "Completed",
                            value = completedCount.toString(),
                            accent = WonderMint,
                            softBg = WonderMintSoft,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Creative Canvas Quick Studio Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenCreativeCanvas)
                            .testTag("dashboard_canvas_banner"),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(WonderCoralSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Brush,
                                    contentDescription = "Creative Canvas",
                                    tint = WonderCoral,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Creative Canvas Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sketch, paint with layers & save drafts offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = onOpenCreativeCanvas,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WonderPurple)
                            ) {
                                Text("Draw", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Enrolled / Recently Accessed Courses
                item {
                    SectionHeader(
                        title = if (enrollments.isNotEmpty()) "Continue Learning" else "Featured Courses by Nikhil",
                        actionLabel = "All courses",
                        onAction = onOpenCourses
                    )
                    Spacer(Modifier.height(10.dp))
                    if (enrollments.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(enrollments, key = { it.id }) { enrollment ->
                                val matchedCourse = courses.firstOrNull { it.id == enrollment.courseId }
                                EnrolledCourseCard(
                                    enrollment = enrollment,
                                    thumbnailKey = matchedCourse?.thumbnailKey,
                                    onClick = { onOpenCourseDetail(enrollment.courseId) }
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(courses.take(4), key = { it.id }) { course ->
                                FeaturedCourseCard(
                                    course = course,
                                    onClick = { onOpenCourseDetail(course.id) }
                                )
                            }
                        }
                    }
                }

                // Upcoming Live Classes
                item {
                    SectionHeader(
                        title = "Upcoming Live Classes",
                        actionLabel = "Schedule",
                        onAction = onOpenLiveClasses
                    )
                    Spacer(Modifier.height(10.dp))
                    val upcoming = liveClasses.filter { it.status == "live_now" || it.status == "upcoming" }
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (upcoming.isEmpty()) {
                            EmptyStateCard(
                                icon = Icons.Filled.Videocam,
                                title = "No live classes scheduled right now",
                                message = "Check the Live Classes schedule or enroll in a course to get notified when Nikhil goes live.",
                                actionLabel = "View Live Schedule",
                                onAction = onOpenLiveClasses
                            )
                        } else {
                            upcoming.take(2).forEach { liveClass ->
                                LiveClassCompactCard(
                                    liveClass = liveClass,
                                    onClick = { onOpenLiveClassRoom(liveClass.id) }
                                )
                            }
                        }
                    }
                }

                // Teacher Announcements
                item {
                    SectionHeader(
                        title = "From Nikhil",
                        actionLabel = null,
                        onAction = null
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (announcements.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(WonderPurpleSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Campaign,
                                            contentDescription = null,
                                            tint = WonderPurple
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Welcome to Wonderart!",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Practice in Creative Canvas, join a live Zoom class, and share your sketches with the gallery.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            announcements.take(2).forEach { ann ->
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(WonderAmberSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Campaign,
                                                contentDescription = null,
                                                tint = WonderAmber
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = ann.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = ann.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Featured Community Artwork
                item {
                    SectionHeader(
                        title = "Community Spotlight",
                        actionLabel = "Gallery",
                        onAction = onOpenCommunity
                    )
                    Spacer(Modifier.height(10.dp))
                    if (posts.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(posts.take(4), key = { it.id }) { post ->
                                Surface(
                                    modifier = Modifier
                                        .width(172.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { onOpenPostDetail(post.id) },
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(126.dp)
                                        ) {
                                            ArtworkImage(
                                                imageUrl = post.imageUrl,
                                                drawableKey = post.drawableKey,
                                                contentDescription = post.title,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(8.dp),
                                                shape = RoundedCornerShape(50),
                                                color = Color.White.copy(alpha = 0.92f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Favorite,
                                                        contentDescription = null,
                                                        tint = WonderCoral,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "${post.likesCount}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = post.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = post.authorName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
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

@Composable
private fun DashboardStatCard(
    label: String,
    value: String,
    accent: Color,
    softBg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(softBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EnrolledCourseCard(
    enrollment: Enrollment,
    thumbnailKey: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(236.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtworkImage(
                    imageUrl = null,
                    drawableKey = thumbnailKey,
                    contentDescription = enrollment.courseTitle,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = enrollment.courseTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${enrollment.progress}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = WonderPurple
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (enrollment.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (enrollment.completionStatus == "completed") WonderMint else WonderPurple,
                trackColor = WonderPurpleSoft
            )
        }
    }
}

@Composable
private fun FeaturedCourseCard(
    course: Course,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            ArtworkImage(
                imageUrl = course.thumbnailUrl,
                drawableKey = course.thumbnailKey,
                contentDescription = course.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.instructor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = WonderPurpleSoft
                    ) {
                        Text(
                            text = "₹${course.price}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WonderPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveClassCompactCard(
    liveClass: LiveClass,
    onClick: () -> Unit
) {
    val statusEnum = LiveClassStatus.fromRaw(liveClass.status)
    val isLive = statusEnum == LiveClassStatus.LIVE_NOW
    val timeLabel = remember(liveClass.scheduledAtMillis) {
        SimpleDateFormat("EEE, dd MMM • h:mm a", Locale.getDefault())
            .format(Date(liveClass.scheduledAtMillis))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isLive) WonderCoral else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLive) WonderCoralSoft else WonderPurpleSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLive) Icons.Filled.PlayCircle else Icons.Filled.School,
                    contentDescription = null,
                    tint = if (isLive) WonderCoral else WonderPurple
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = liveClass.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = liveClass.courseTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$timeLabel • ${liveClass.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isLive) WonderCoral else WonderPurpleSoft
            ) {
                Text(
                    text = statusEnum.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLive) Color.White else WonderPurple,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}
