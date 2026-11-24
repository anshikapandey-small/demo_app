package com.wonderart.app.ui.admin

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.Course
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.LiveClass
import com.wonderart.app.data.model.LiveClassStatus
import com.wonderart.app.data.model.ReportItem
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.model.WonderUser
import com.wonderart.app.ui.components.CategoryPillRow
import com.wonderart.app.ui.components.CircleBackButton
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderCoralSoft
import com.wonderart.app.ui.theme.WonderMint
import com.wonderart.app.ui.theme.WonderMintSoft
import com.wonderart.app.ui.theme.WonderPurple
import com.wonderart.app.ui.theme.WonderPurpleSoft
import com.wonderart.app.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAdmin) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Teacher Administrator access required", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Return to Dashboard") }
            }
        }
        return
    }

    val usersState by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val coursesState by adminViewModel.allCourses.collectAsStateWithLifecycle()
    val enrollmentsState by adminViewModel.allEnrollments.collectAsStateWithLifecycle()
    val postsState by adminViewModel.allPosts.collectAsStateWithLifecycle()
    val reportsState by adminViewModel.allReports.collectAsStateWithLifecycle()
    val liveClassesState by adminViewModel.allLiveClasses.collectAsStateWithLifecycle()
    val feedback by adminViewModel.adminFeedback.collectAsStateWithLifecycle()

    LaunchedEffect(feedback) {
        feedback?.let {
            onShowMessage(it)
            adminViewModel.clearFeedback()
        }
    }

    val users = (usersState as? UiState.Success)?.data ?: emptyList()
    val courses = (coursesState as? UiState.Success)?.data ?: emptyList()
    val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
    val posts = (postsState as? UiState.Success)?.data ?: emptyList()
    val reports = (reportsState as? UiState.Success)?.data ?: emptyList()
    val liveClasses = (liveClassesState as? UiState.Success)?.data ?: emptyList()

    val tabs = listOf("Overview", "Courses", "Students", "Live Classes", "Moderation")
    var selectedTab by remember { mutableStateOf("Overview") }

    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var showCourseDialog by remember { mutableStateOf(false) }

    var showClassDialog by remember { mutableStateOf(false) }
    var editingLiveClass by remember { mutableStateOf<LiveClass?>(null) }

    var grantTargetStudent by remember { mutableStateOf<WonderUser?>(null) }

    var announcementTitle by remember { mutableStateOf("") }
    var announcementMessage by remember { mutableStateOf("") }

    var studentQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Column {
                            Text(
                                text = "Nikhil's Studio Admin",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Teacher Management Console",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                CategoryPillRow(
                    categories = tabs,
                    selectedCategory = selectedTab,
                    onSelectCategory = { selectedTab = it }
                )
            }

            when (selectedTab) {
                "Overview" -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard("Students", "${users.size}", WonderPurple, Modifier.weight(1f))
                            AdminStatCard("Courses", "${courses.size}", WonderCoral, Modifier.weight(1f))
                            AdminStatCard("Enrollments", "${enrollments.size}", WonderMint, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard("Live Classes", "${liveClasses.size}", WonderPurple, Modifier.weight(1f))
                            AdminStatCard("Posts", "${posts.size}", WonderCoral, Modifier.weight(1f))
                            AdminStatCard(
                                "Pending Reports",
                                "${reports.count { it.status == "pending" }}",
                                MaterialTheme.colorScheme.error,
                                Modifier.weight(1f)
                            )
                        }
                    }

                    // Sample catalog seed control
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Sample Catalog & Demo Posts", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Populate the 4 starter courses, 4 demo community artworks, live workshops, and welcome announcement.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { adminViewModel.seedSampleCatalogAndPosts() },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("admin_seed_sample_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Seed Sample Data")
                                }
                            }
                        }
                    }

                    // Announcement Composer
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = WonderPurple)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Publish Announcement", style = MaterialTheme.typography.titleMedium)
                                }
                                OutlinedTextField(
                                    value = announcementTitle,
                                    onValueChange = { announcementTitle = it },
                                    label = { Text("Announcement Title") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_announcement_title")
                                )
                                OutlinedTextField(
                                    value = announcementMessage,
                                    onValueChange = { announcementMessage = it },
                                    label = { Text("Message to Students") },
                                    minLines = 2,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_announcement_message")
                                )
                                Button(
                                    onClick = {
                                        if (announcementTitle.isNotBlank() && announcementMessage.isNotBlank()) {
                                            adminViewModel.publishAnnouncement(announcementTitle, announcementMessage)
                                            announcementTitle = ""
                                            announcementMessage = ""
                                        }
                                    },
                                    enabled = announcementTitle.isNotBlank() && announcementMessage.isNotBlank(),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = WonderPurple),
                                    modifier = Modifier.testTag("admin_publish_announcement_button")
                                ) {
                                    Text("Publish Announcement")
                                }
                            }
                        }
                    }
                }

                "Courses" -> {
                    item {
                        Button(
                            onClick = {
                                editingCourse = null
                                showCourseDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = WonderPurple),
                            modifier = Modifier.testTag("admin_create_course_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Create New Course")
                        }
                    }

                    items(courses, key = { it.id }) { course ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(course.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${course.category} • ₹${course.price} • ${course.totalSessions} sessions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (course.published) "Published" else "Archived",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (course.published) WonderMint else WonderCoral,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingCourse = course
                                        showCourseDialog = true
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit course")
                                }
                                Switch(
                                    checked = course.published,
                                    onCheckedChange = { adminViewModel.toggleCoursePublished(course) }
                                )
                            }
                        }
                    }
                }

                "Students" -> {
                    item {
                        OutlinedTextField(
                            value = studentQuery,
                            onValueChange = { studentQuery = it },
                            label = { Text("Search students by name or email") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val filteredUsers = users.filter {
                        studentQuery.isBlank() ||
                            it.name.contains(studentQuery, ignoreCase = true) ||
                            it.email.contains(studentQuery, ignoreCase = true)
                    }

                    items(filteredUsers, key = { it.uid }) { student ->
                        val studentEnrollments = enrollments.filter { it.studentId == student.uid }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(student.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = student.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Role: ${student.role} • ${studentEnrollments.size} enrolled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WonderPurple
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { grantTargetStudent = student },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text("Grant Course")
                                    }
                                }

                                if (studentEnrollments.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    studentEnrollments.forEach { enrollment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "• ${enrollment.courseTitle} (${enrollment.progress}%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = { adminViewModel.revokeEnrollment(enrollment.id) }
                                            ) {
                                                Text("Revoke", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Live Classes" -> {
                    item {
                        Button(
                            onClick = {
                                editingLiveClass = null
                                showClassDialog = true
                            },
                            enabled = courses.isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = WonderPurple),
                            modifier = Modifier.testTag("admin_schedule_class_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Schedule Live Class")
                        }
                    }

                    items(liveClasses, key = { it.id }) { liveClass ->
                        val dateStr = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
                            .format(Date(liveClass.scheduledAtMillis))
                        val status = LiveClassStatus.fromRaw(liveClass.status)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(liveClass.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${liveClass.courseTitle} • $dateStr",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Status: ${status.label} • Zoom ID: ${liveClass.zoomMeetingId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WonderPurple
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingLiveClass = liveClass
                                            showClassDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit live class")
                                    }
                                    IconButton(
                                        onClick = { adminViewModel.deleteLiveClass(liveClass.id) }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete live class",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        OutlinedButton(
                                            onClick = { adminViewModel.updateLiveClassStatus(liveClass.id, "live_now") },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Start Live")
                                        }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = { adminViewModel.updateLiveClassStatus(liveClass.id, "completed") },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Complete")
                                        }
                                    }
                                    item {
                                        OutlinedButton(
                                            onClick = { adminViewModel.updateLiveClassStatus(liveClass.id, "cancelled") },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "Moderation" -> {
                    item {
                        Text("Reported Content (${reports.size})", style = MaterialTheme.typography.titleLarge)
                    }

                    items(reports, key = { it.id }) { report ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(report.postTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "Reason: ${report.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Status: ${report.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WonderPurple
                                )
                                if (report.status == "pending") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { adminViewModel.resolveReport(report.id, "reviewed") }) {
                                            Text("Mark Reviewed")
                                        }
                                        TextButton(onClick = { adminViewModel.resolveReport(report.id, "dismissed") }) {
                                            Text("Dismiss")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("All Community Posts (${posts.size})", style = MaterialTheme.typography.titleLarge)
                    }

                    items(posts, key = { it.id }) { post ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(post.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "By ${post.authorName} • ${post.category} • ${if (post.hidden) "Hidden" else "Visible"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { adminViewModel.togglePostHidden(post) }) {
                                    Icon(
                                        imageVector = if (post.hidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (post.hidden) "Restore post" else "Hide post",
                                        tint = WonderPurple
                                    )
                                }
                                IconButton(onClick = { adminViewModel.deletePostAdmin(post.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete post",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showCourseDialog) {
        CourseEditorDialog(
            initial = editingCourse,
            onDismiss = { showCourseDialog = false },
            onSave = { title, desc, cat, price, diff, duration, sessions, curriculum, thumbKey, published ->
                adminViewModel.saveCourse(
                    courseId = editingCourse?.id,
                    title = title,
                    description = desc,
                    category = cat,
                    price = price,
                    difficulty = diff,
                    duration = duration,
                    totalSessions = sessions,
                    curriculumRaw = curriculum,
                    thumbnailKey = thumbKey,
                    published = published
                )
                showCourseDialog = false
            }
        )
    }

    if (showClassDialog && courses.isNotEmpty()) {
        LiveClassEditorDialog(
            courses = courses,
            initial = editingLiveClass,
            onDismiss = { showClassDialog = false },
            onSave = { course, title, desc, hoursFromNow, durationMin, meetingId, joinUrl ->
                val scheduledMillis = editingLiveClass?.scheduledAtMillis
                    ?: (System.currentTimeMillis() + hoursFromNow * 3_600_000L)
                adminViewModel.saveLiveClass(
                    classId = editingLiveClass?.id,
                    course = course,
                    title = title,
                    description = desc,
                    scheduledAtMillis = scheduledMillis,
                    durationMinutes = durationMin,
                    zoomMeetingId = meetingId,
                    zoomJoinUrl = joinUrl.ifBlank { null }
                )
                showClassDialog = false
            }
        )
    }

    grantTargetStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { grantTargetStudent = null },
            title = { Text("Grant Course to ${student.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    courses.forEach { course ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    adminViewModel.grantEnrollment(student, course)
                                    grantTargetStudent = null
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = WonderPurpleSoft
                        ) {
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = WonderPurple,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { grantTargetStudent = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CourseEditorDialog(
    initial: Course?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, String, String, Long, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "Watercolor") }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "499") }
    var difficulty by remember { mutableStateOf(initial?.difficulty ?: "Beginner") }
    var duration by remember { mutableStateOf(initial?.duration ?: "4 weeks") }
    var sessionsText by remember { mutableStateOf(initial?.totalSessions?.toString() ?: "12") }
    var curriculum by remember { mutableStateOf(initial?.curriculum?.joinToString("\n") ?: "") }
    var thumbKey by remember { mutableStateOf(initial?.thumbnailKey ?: "silent_mountains") }
    var published by remember { mutableStateOf(initial?.published ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Course" else "Edit Course") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter(Char::isDigit) },
                    label = { Text("Price (INR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = difficulty, onValueChange = { difficulty = it }, label = { Text("Difficulty") })
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration") })
                OutlinedTextField(
                    value = sessionsText,
                    onValueChange = { sessionsText = it.filter(Char::isDigit) },
                    label = { Text("Total Sessions") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = curriculum,
                    onValueChange = { curriculum = it },
                    label = { Text("Curriculum (one line per module)") },
                    minLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Published", modifier = Modifier.weight(1f))
                    Switch(checked = published, onCheckedChange = { published = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val price = priceText.toLongOrNull() ?: 499L
                    val sessions = (sessionsText.toLongOrNull() ?: 12L).coerceAtLeast(1L)
                    if (title.isNotBlank() && description.isNotBlank()) {
                        onSave(title, description, category, price, difficulty, duration, sessions, curriculum, thumbKey, published)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LiveClassEditorDialog(
    courses: List<Course>,
    initial: LiveClass?,
    onDismiss: () -> Unit,
    onSave: (Course, String, String, Int, Long, String, String) -> Unit
) {
    var selectedCourse by remember {
        mutableStateOf(courses.firstOrNull { it.id == initial?.courseId } ?: courses.first())
    }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var hoursText by remember { mutableStateOf("2") }
    var durationText by remember { mutableStateOf(initial?.durationMinutes?.toString() ?: "60") }
    var meetingId by remember { mutableStateOf(initial?.zoomMeetingId ?: "845 9012 3310") }
    var joinUrl by remember { mutableStateOf(initial?.zoomJoinUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Schedule Live Class" else "Edit Live Class") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Course", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(courses, key = { it.id }) { course ->
                        val active = course.id == selectedCourse.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedCourse = course },
                            shape = RoundedCornerShape(50),
                            color = if (active) WonderPurple else WonderPurpleSoft
                        ) {
                            Text(
                                text = course.title.take(18),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) Color.White else WonderPurple,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Session Title") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Session Description") })
                if (initial == null) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter(Char::isDigit) },
                        label = { Text("Starts in (hours from now)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter(Char::isDigit) },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = meetingId, onValueChange = { meetingId = it }, label = { Text("Zoom Meeting ID") })
                OutlinedTextField(
                    value = joinUrl,
                    onValueChange = { joinUrl = it },
                    label = { Text("Zoom Join URL (optional https://zoom.us/...)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hrs = (hoursText.toIntOrNull() ?: 2).coerceAtLeast(1)
                    val dur = (durationText.toLongOrNull() ?: 60L).coerceIn(10L, 480L)
                    if (title.isNotBlank() && description.isNotBlank() && meetingId.isNotBlank()) {
                        onSave(selectedCourse, title, description, hrs, dur, meetingId, joinUrl)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
