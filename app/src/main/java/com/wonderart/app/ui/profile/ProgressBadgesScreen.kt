package com.wonderart.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.model.WonderUser
import com.wonderart.app.ui.components.CircleBackButton
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

private data class ArtBadge(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val softBg: Color,
    val unlocked: Boolean
)

@Composable
fun ProgressBadgesScreen(
    userState: UiState<WonderUser?>,
    enrollmentsState: UiState<List<Enrollment>>,
    postsState: UiState<List<CommunityPost>>,
    savedDraftsCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = (userState as? UiState.Success)?.data
    val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()

    val xp = user?.xp ?: 50L
    val level = (xp / 250L) + 1L
    val xpIntoLevel = xp % 250L
    val streak = (user?.streakDays ?: 1L).coerceAtLeast(1L)

    val myPublishedCount = remember(posts, user) {
        if (user == null) 0 else posts.count { it.userId == user.uid }
    }
    val completedCount = remember(enrollments) {
        enrollments.count { it.completionStatus == "completed" }
    }

    val levelTitle = when {
        level >= 6 -> "Studio Maestro"
        level >= 4 -> "Color Explorer"
        level >= 2 -> "Creative Sketcher"
        else -> "Rising Artist"
    }

    val badges = remember(enrollments.size, myPublishedCount, savedDraftsCount, completedCount) {
        listOf(
            ArtBadge(
                title = "First Course",
                subtitle = "Enroll in a course",
                icon = Icons.Filled.School,
                tint = WonderPurple,
                softBg = WonderPurpleSoft,
                unlocked = enrollments.isNotEmpty()
            ),
            ArtBadge(
                title = "Creative Sketcher",
                subtitle = "Save a canvas draft",
                icon = Icons.Filled.Brush,
                tint = WonderCoral,
                softBg = WonderCoralSoft,
                unlocked = savedDraftsCount > 0
            ),
            ArtBadge(
                title = "Gallery Artist",
                subtitle = "Share to Community",
                icon = Icons.Filled.Collections,
                tint = WonderAmber,
                softBg = WonderAmberSoft,
                unlocked = myPublishedCount > 0
            ),
            ArtBadge(
                title = "Course Finisher",
                subtitle = "Complete 1 course",
                icon = Icons.Filled.Star,
                tint = WonderMint,
                softBg = WonderMintSoft,
                unlocked = completedCount > 0
            )
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleBackButton(onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "My Progress",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            // Level & XP Gradient Card matching Figma 19
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(WonderPurple, WonderPurpleDark)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "LEVEL $level",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = levelTitle,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { (xpIntoLevel / 250f).coerceIn(0.05f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = WonderCoral,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$xp total XP • ${250L - xpIntoLevel} XP to Level ${level + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // Streak Card matching Figma 19
            item {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val activeDays = streak.toInt().coerceIn(1, 7)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = WonderCoral,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$streak-day creative streak",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEachIndexed { index, day ->
                                val completed = index < activeDays
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (completed) WonderMint else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (completed) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Badges Section
            item {
                Text(
                    text = "Badges",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    badges.take(2).forEach { badge ->
                        BadgeTile(badge = badge, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    badges.drop(2).forEach { badge ->
                        BadgeTile(badge = badge, modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Text(
                    text = "Course Completion",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (enrollments.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.School,
                        title = "No courses started yet",
                        message = "Enroll in a course from the Courses tab to track your learning progress."
                    )
                }
            } else {
                items(enrollments, key = { it.id }) { enrollment ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = enrollment.courseTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${enrollment.progress}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WonderPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
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
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun BadgeTile(
    badge: ArtBadge,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (badge.unlocked) badge.tint.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (badge.unlocked) badge.softBg else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badge.icon,
                    contentDescription = badge.title,
                    tint = if (badge.unlocked) badge.tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = badge.title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (badge.unlocked) "Unlocked" else badge.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (badge.unlocked) badge.tint else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
