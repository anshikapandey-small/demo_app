package com.wonderart.app.ui.courses

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wonderart.app.data.model.Course
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.LiveClass
import com.wonderart.app.data.model.LiveClassStatus
import com.wonderart.app.ui.components.ArtworkImage
import com.wonderart.app.ui.components.CircleBackButton
import com.wonderart.app.ui.theme.WonderAmberSoft
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
fun CourseDetailScreen(
    course: Course?,
    enrollment: Enrollment?,
    courseLiveClasses: List<LiveClass>,
    onBack: () -> Unit,
    onDemoEnroll: (Course) -> Unit,
    onUpdateProgress: (courseId: String, completedSessions: Long, totalSessions: Long) -> Unit,
    onOpenLiveClassRoom: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (course == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Course not found", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back to Courses") }
            }
        }
        return
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Image & Top Bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    ArtworkImage(
                        imageUrl = course.thumbnailUrl,
                        drawableKey = course.thumbnailKey,
                        contentDescription = course.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    CircleBackButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        shape = RoundedCornerShape(50),
                        color = WonderPurple
                    ) {
                        Text(
                            text = "₹${course.price}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Title & Meta
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = RoundedCornerShape(50), color = WonderPurpleSoft) {
                            Text(
                                text = course.difficulty,
                                style = MaterialTheme.typography.labelMedium,
                                color = WonderPurple,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(50), color = WonderCoralSoft) {
                            Text(
                                text = "${course.duration} • ${course.totalSessions} lessons",
                                style = MaterialTheme.typography.labelMedium,
                                color = WonderCoral,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Taught live by ${course.instructor}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Enrollment / Progress Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        if (enrollment == null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = WonderPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Demonstration Enrollment • No real payment is processed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WonderPurple
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Course Price: ₹${course.price}. Payment gateway integration is prepared for production release; use Demo Enroll to unlock course tracking & live classes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onDemoEnroll(course) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("demo_enroll_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WonderPurple)
                            ) {
                                Text(
                                    text = "Enroll (Demonstration Access)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = WonderMint
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Enrolled (${enrollment.enrollmentType.replace("_", " ")})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "${enrollment.progress}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WonderPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (enrollment.progress / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (enrollment.completionStatus == "completed") WonderMint else WonderPurple,
                                trackColor = WonderPurpleSoft
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completed lessons: ${enrollment.completedSessions} / ${course.totalSessions}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            onUpdateProgress(
                                                course.id,
                                                (enrollment.completedSessions - 1).coerceAtLeast(0),
                                                course.totalSessions
                                            )
                                        },
                                        enabled = enrollment.completedSessions > 0
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease completed lessons")
                                    }
                                    IconButton(
                                        onClick = {
                                            onUpdateProgress(
                                                course.id,
                                                (enrollment.completedSessions + 1).coerceAtMost(course.totalSessions),
                                                course.totalSessions
                                            )
                                        },
                                        enabled = enrollment.completedSessions < course.totalSessions
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Complete next lesson")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Learning Objectives
            if (course.learningObjectives.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Learning Objectives", style = MaterialTheme.typography.titleMedium)
                            course.learningObjectives.forEach { obj ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = WonderMint,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(obj, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Curriculum
            if (course.curriculum.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Course Curriculum", style = MaterialTheme.typography.titleMedium)
                            course.curriculum.forEachIndexed { idx, item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = WonderPurpleSoft
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = WonderPurple,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Text(item, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Course-specific Live Classes
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Live Classes for This Course", style = MaterialTheme.typography.titleMedium)
                    if (courseLiveClasses.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = WonderAmberSoft
                        ) {
                            Text(
                                text = "Nikhil schedules interactive live Zoom sessions for this course regularly. Check back soon!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        courseLiveClasses.forEach { liveClass ->
                            val status = LiveClassStatus.fromRaw(liveClass.status)
                            val timeStr = SimpleDateFormat("EEE, dd MMM • h:mm a", Locale.getDefault())
                                .format(Date(liveClass.scheduledAtMillis))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Videocam,
                                        contentDescription = null,
                                        tint = WonderPurple
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(liveClass.title, style = MaterialTheme.typography.titleSmall)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "$timeStr • ${status.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { onOpenLiveClassRoom(liveClass.id) },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text("Open")
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
