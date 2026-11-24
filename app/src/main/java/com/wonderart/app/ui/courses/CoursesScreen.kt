package com.wonderart.app.ui.courses

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wonderart.app.data.model.Course
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.seed.SampleCatalog
import com.wonderart.app.ui.components.ArtworkImage
import com.wonderart.app.ui.components.CategoryPillRow
import com.wonderart.app.ui.components.EmptyStateCard
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderCoralSoft
import com.wonderart.app.ui.theme.WonderMint
import com.wonderart.app.ui.theme.WonderMintSoft
import com.wonderart.app.ui.theme.WonderPurple
import com.wonderart.app.ui.theme.WonderPurpleSoft

@Composable
fun CoursesScreen(
    coursesState: UiState<List<Course>>,
    enrollmentsState: UiState<List<Enrollment>>,
    initialMyCoursesOnly: Boolean = false,
    onOpenCourseDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var myCoursesOnly by remember { mutableStateOf(initialMyCoursesOnly) }

    val cloudCourses = (coursesState as? UiState.Success)?.data ?: emptyList()
    val allCourses = cloudCourses.ifEmpty { SampleCatalog.previewFallbackCourses }
    val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
    val enrollmentByCourseId = remember(enrollments) { enrollments.associateBy { it.courseId } }

    val categories = listOf("All", "Pencil Sketching", "Watercolor", "Portrait", "Acrylic Painting")

    val filtered = remember(allCourses, searchQuery, selectedCategory, myCoursesOnly, enrollmentByCourseId) {
        allCourses.filter { course ->
            val matchesSearch = searchQuery.isBlank() ||
                course.title.contains(searchQuery, ignoreCase = true) ||
                course.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" ||
                course.category.equals(selectedCategory, ignoreCase = true)
            val matchesMyCourses = !myCoursesOnly || enrollmentByCourseId.containsKey(course.id)
            matchesSearch && matchesCategory && matchesMyCourses
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Courses",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                text = "Curated art classes taught live by Nikhil",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Toggle All vs My Courses
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                FilterSwitchPill(
                                    label = "Explore",
                                    selected = !myCoursesOnly,
                                    onClick = { myCoursesOnly = false }
                                )
                                FilterSwitchPill(
                                    label = "My (${enrollments.size})",
                                    selected = myCoursesOnly,
                                    onClick = { myCoursesOnly = true }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search sketching, watercolor, portrait...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = WonderPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("courses_search_input")
                    )
                }
            }

            item {
                CategoryPillRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it }
                )
            }

            if (cloudCourses.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = WonderPurpleSoft
                    ) {
                        Text(
                            text = "Showing the 4 foundational Wonderart courses. Nikhil can sync or customize courses anytime from Teacher Admin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WonderPurple,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        EmptyStateCard(
                            icon = Icons.Filled.MenuBook,
                            title = if (myCoursesOnly) "No enrolled courses yet" else "No matching courses",
                            message = if (myCoursesOnly) {
                                "Explore Wonderart courses and enroll with demonstration access to track progress and join live classes."
                            } else {
                                "Try clearing your search or choosing another art category."
                            },
                            actionLabel = if (myCoursesOnly) "Explore Courses" else "Reset Filter",
                            onAction = {
                                myCoursesOnly = false
                                selectedCategory = "All"
                                searchQuery = ""
                            }
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { course ->
                    val enrollment = enrollmentByCourseId[course.id]
                    CourseMarketplaceCard(
                        course = course,
                        enrollment = enrollment,
                        onClick = { onOpenCourseDetail(course.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSwitchPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) WonderPurple else Color.Transparent
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun CourseMarketplaceCard(
    course: Course,
    enrollment: Enrollment?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("course_card_${course.id}"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                ArtworkImage(
                    imageUrl = course.thumbnailUrl,
                    drawableKey = course.thumbnailKey,
                    contentDescription = course.title,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = WonderCoralSoft
                        ) {
                            Text(
                                text = course.difficulty,
                                style = MaterialTheme.typography.labelSmall,
                                color = WonderCoral,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = WonderPurpleSoft
                        ) {
                            Text(
                                text = "₹${course.price}",
                                style = MaterialTheme.typography.labelMedium,
                                color = WonderPurple,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Instructor: ${course.instructor}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = course.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${course.duration} • ${course.totalSessions} live lessons",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (enrollment != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = WonderMintSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = WonderMint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${enrollment.progress}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = WonderMint,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (enrollment != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (enrollment.progress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (enrollment.completionStatus == "completed") WonderMint else WonderPurple,
                    trackColor = WonderPurpleSoft
                )
            }
        }
    }
}
