package com.wonderart.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.wonderart.app.AppContainer
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.Course
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.LiveClass
import com.wonderart.app.data.model.ReportItem
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.model.WonderUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _adminFeedback = MutableStateFlow<String?>(null)
    val adminFeedback: StateFlow<String?> = _adminFeedback.asStateFlow()

    val allUsers: StateFlow<UiState<List<WonderUser>>> =
        container.dashboardRepository.observeAllUsersAdmin().toUiStateFlow("users")

    val allCourses: StateFlow<UiState<List<Course>>> =
        container.courseRepository.observeAllCoursesAdmin().toUiStateFlow("courses")

    val allEnrollments: StateFlow<UiState<List<Enrollment>>> =
        container.courseRepository.observeAllEnrollmentsAdmin().toUiStateFlow("enrollments")

    val allPosts: StateFlow<UiState<List<CommunityPost>>> =
        container.communityRepository.observeAllPostsAdmin().toUiStateFlow("posts")

    val allReports: StateFlow<UiState<List<ReportItem>>> =
        container.communityRepository.observeReportsAdmin().toUiStateFlow("reports")

    val allLiveClasses: StateFlow<UiState<List<LiveClass>>> =
        container.liveClassRepository.observeAllLiveClassesAdmin().toUiStateFlow("liveClasses")

    fun clearFeedback() {
        _adminFeedback.value = null
    }

    fun seedSampleCatalogAndPosts() {
        viewModelScope.launch {
            val result = container.dashboardRepository.seedSampleDataIfNeeded(forceSeed = true)
            _adminFeedback.value = if (result.isSuccess) {
                "Seeded 4 sample courses, 4 demo posts, live classes & announcement!"
            } else {
                result.exceptionOrNull()?.message ?: "Seed failed"
            }
        }
    }

    fun saveCourse(
        courseId: String?,
        title: String,
        description: String,
        category: String,
        price: Long,
        difficulty: String,
        duration: String,
        totalSessions: Long,
        curriculumRaw: String,
        thumbnailKey: String,
        published: Boolean
    ) {
        viewModelScope.launch {
            val curriculum = curriculumRaw.lines().map { it.trim() }.filter { it.isNotBlank() }
            val result = container.courseRepository.saveCourseAdmin(
                courseId = courseId,
                title = title,
                description = description,
                instructor = "Nikhil",
                category = category,
                price = price,
                difficulty = difficulty,
                duration = duration,
                totalSessions = totalSessions,
                curriculum = curriculum,
                learningObjectives = listOf("Master core techniques with Nikhil"),
                thumbnailKey = thumbnailKey,
                thumbnailUrl = null,
                published = published
            )
            _adminFeedback.value = if (result.isSuccess) {
                if (courseId == null) "Course created" else "Course updated"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to save course"
            }
        }
    }

    fun toggleCoursePublished(course: Course) {
        viewModelScope.launch {
            val next = !course.published
            val result = container.courseRepository.toggleCoursePublishedAdmin(course.id, next)
            _adminFeedback.value = if (result.isSuccess) {
                if (next) "Course published" else "Course archived"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to update course status"
            }
        }
    }

    fun grantEnrollment(student: WonderUser, course: Course) {
        viewModelScope.launch {
            val result = container.courseRepository.grantStudentEnrollmentAdmin(student.uid, student.name, course)
            _adminFeedback.value = if (result.isSuccess) {
                "Granted ${student.name} access to ${course.title}"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to grant enrollment"
            }
        }
    }

    fun revokeEnrollment(enrollmentId: String) {
        viewModelScope.launch {
            val result = container.courseRepository.revokeEnrollmentAdmin(enrollmentId)
            _adminFeedback.value = if (result.isSuccess) "Enrollment removed" else "Failed to remove enrollment"
        }
    }

    fun saveLiveClass(
        classId: String?,
        course: Course,
        title: String,
        description: String,
        scheduledAtMillis: Long,
        durationMinutes: Long,
        zoomMeetingId: String,
        zoomJoinUrl: String?
    ) {
        viewModelScope.launch {
            val result = container.liveClassRepository.saveLiveClassAdmin(
                classId = classId,
                courseId = course.id,
                courseTitle = course.title,
                title = title,
                description = description,
                scheduledAtMillis = scheduledAtMillis,
                durationMinutes = durationMinutes,
                zoomMeetingId = zoomMeetingId,
                zoomJoinUrl = zoomJoinUrl
            )
            _adminFeedback.value = if (result.isSuccess) {
                if (classId == null) "Live class scheduled" else "Live class updated"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to save live class"
            }
        }
    }

    fun updateLiveClassStatus(classId: String, status: String) {
        viewModelScope.launch {
            val result = container.liveClassRepository.updateLiveClassStatusAdmin(classId, status)
            _adminFeedback.value = if (result.isSuccess) {
                "Live class marked as $status"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to update class status"
            }
        }
    }

    fun deleteLiveClass(classId: String) {
        viewModelScope.launch {
            val result = container.liveClassRepository.deleteLiveClassAdmin(classId)
            _adminFeedback.value = if (result.isSuccess) "Live class removed" else "Failed to remove class"
        }
    }

    fun togglePostHidden(post: CommunityPost) {
        viewModelScope.launch {
            val result = container.communityRepository.setPostHiddenAdmin(post.id, !post.hidden)
            _adminFeedback.value = if (result.isSuccess) {
                if (!post.hidden) "Post hidden from community" else "Post restored to community"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to moderate post"
            }
        }
    }

    fun deletePostAdmin(postId: String) {
        viewModelScope.launch {
            val result = container.communityRepository.deletePost(postId)
            _adminFeedback.value = if (result.isSuccess) "Post deleted" else "Failed to delete post"
        }
    }

    fun resolveReport(reportId: String, status: String) {
        viewModelScope.launch {
            val result = container.communityRepository.updateReportStatusAdmin(reportId, status)
            _adminFeedback.value = if (result.isSuccess) "Report marked $status" else "Failed to update report"
        }
    }

    fun publishAnnouncement(title: String, message: String) {
        viewModelScope.launch {
            val result = container.dashboardRepository.createAnnouncementAdmin(title, message)
            _adminFeedback.value = if (result.isSuccess) {
                "Announcement published to all students"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to publish announcement"
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w("AdminVM", "Error observing $label", root)
                emit(UiState.Error(root.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )
}
