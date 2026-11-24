package com.wonderart.app.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.wonderart.app.AppContainer
import com.wonderart.app.data.model.Announcement
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.Course
import com.wonderart.app.data.model.Enrollment
import com.wonderart.app.data.model.LiveClass
import com.wonderart.app.data.model.NotificationItem
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.model.WonderUser
import com.wonderart.app.utils.LocalNotificationHelper
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val container: AppContainer,
    val currentUserId: String
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    val userProfile: StateFlow<UiState<WonderUser?>> =
        container.authRepository.observeUserProfile(currentUserId)
            .toUiStateFlow("userProfile")

    val courses: StateFlow<UiState<List<Course>>> =
        container.courseRepository.observePublishedCourses()
            .toUiStateFlow("courses")

    val enrollments: StateFlow<UiState<List<Enrollment>>> =
        container.courseRepository.observeStudentEnrollments(currentUserId)
            .toUiStateFlow("enrollments")

    val posts: StateFlow<UiState<List<CommunityPost>>> =
        container.communityRepository.observeVisiblePosts()
            .toUiStateFlow("communityPosts")

    val liveClasses: StateFlow<UiState<List<LiveClass>>> =
        container.liveClassRepository.observeStudentLiveClasses()
            .toUiStateFlow("liveClasses")

    val announcements: StateFlow<UiState<List<Announcement>>> =
        container.dashboardRepository.observeAnnouncements()
            .toUiStateFlow("announcements")

    val notifications: StateFlow<UiState<List<NotificationItem>>> =
        container.dashboardRepository.observeNotifications(currentUserId)
            .toUiStateFlow("notifications")

    init {
        viewModelScope.launch {
            val adminResult = container.authRepository.ensureUserProfileAndCheckAdmin()
            adminResult.onSuccess { adminFlag ->
                _isAdmin.value = adminFlag
                if (adminFlag) {
                    container.dashboardRepository.seedSampleDataIfNeeded(forceSeed = false)
                }
            }.onFailure { err ->
                Log.w(TAG, "Profile sync note: ${err.message}")
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun enrollInCourseDemo(course: Course) {
        viewModelScope.launch {
            val studentName = (userProfile.value as? UiState.Success)?.data?.name ?: "Student"
            val result = container.courseRepository.enrollInCourseDemo(course, studentName)
            if (result.isSuccess) {
                container.authRepository.addXp(currentUserId, 50L)
                container.dashboardRepository.createReminderNotification(
                    userId = currentUserId,
                    title = "Enrolled in ${course.title}",
                    message = "Demonstration enrollment active. You can now join live classes for this course!",
                    type = "enrollment"
                )
                _statusBanner.value = "Enrolled in ${course.title} (Demo Enrollment)"
            } else {
                _statusBanner.value = result.exceptionOrNull()?.message
                    ?: "Enrollment requires an active Firestore course record."
            }
        }
    }

    fun updateCourseProgress(courseId: String, completedSessions: Long, totalSessions: Long) {
        viewModelScope.launch {
            val result = container.courseRepository.updateEnrollmentProgress(
                courseId = courseId,
                completedSessions = completedSessions,
                totalSessions = totalSessions
            )
            if (result.isSuccess) {
                container.authRepository.addXp(currentUserId, 25L)
                _statusBanner.value = "Progress updated ($completedSessions / $totalSessions sessions)"
            } else {
                _statusBanner.value = result.exceptionOrNull()?.message ?: "Could not update progress"
            }
        }
    }

    fun toggleLike(post: CommunityPost, currentlyLiked: Boolean) {
        viewModelScope.launch {
            val result = container.communityRepository.toggleLike(post, currentlyLiked)
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.message ?: "Unable to update like"
            }
        }
    }

    fun publishArtworkFromUri(
        context: Context,
        imageUri: Uri,
        title: String,
        description: String,
        category: String,
        onPublished: () -> Unit
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val mediaResult = container.artworkMediaService.prepareArtworkForPost(context, imageUri)
            mediaResult.onSuccess { encoded ->
                val authorName = (userProfile.value as? UiState.Success)?.data?.name ?: "Wonderart Artist"
                val postResult = container.communityRepository.publishArtwork(
                    authorName = authorName,
                    title = title,
                    description = description,
                    category = category,
                    imageDataUri = encoded
                )
                postResult.onSuccess {
                    container.authRepository.addXp(currentUserId, 40L)
                    _statusBanner.value = "Artwork published to Wonderart Community!"
                    onPublished()
                }.onFailure { err ->
                    _statusBanner.value = err.message ?: "Failed to publish artwork"
                }
            }.onFailure { err ->
                _statusBanner.value = err.message ?: "Failed to process selected image"
            }
            _isPublishing.value = false
        }
    }

    fun publishArtworkFromBitmap(
        bitmap: Bitmap,
        title: String,
        description: String,
        category: String,
        onPublished: () -> Unit
    ) {
        viewModelScope.launch {
            _isPublishing.value = true
            val mediaResult = container.artworkMediaService.prepareArtworkBitmapForPost(bitmap)
            mediaResult.onSuccess { encoded ->
                val authorName = (userProfile.value as? UiState.Success)?.data?.name ?: "Wonderart Artist"
                val postResult = container.communityRepository.publishArtwork(
                    authorName = authorName,
                    title = title,
                    description = description,
                    category = category,
                    imageDataUri = encoded
                )
                postResult.onSuccess {
                    container.authRepository.addXp(currentUserId, 40L)
                    _statusBanner.value = "Canvas artwork published to Wonderart Community!"
                    onPublished()
                }.onFailure { err ->
                    _statusBanner.value = err.message ?: "Failed to publish canvas artwork"
                }
            }.onFailure { err ->
                _statusBanner.value = err.message ?: "Failed to prepare canvas image"
            }
            _isPublishing.value = false
        }
    }

    fun updatePost(postId: String, title: String, description: String, category: String) {
        viewModelScope.launch {
            val result = container.communityRepository.updatePost(postId, title, description, category)
            _statusBanner.value = if (result.isSuccess) {
                "Artwork updated"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to update artwork"
            }
        }
    }

    fun deletePost(postId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val result = container.communityRepository.deletePost(postId)
            if (result.isSuccess) {
                _statusBanner.value = "Artwork deleted"
                onDeleted()
            } else {
                _statusBanner.value = result.exceptionOrNull()?.message ?: "Failed to delete artwork"
            }
        }
    }

    fun reportPost(post: CommunityPost, reason: String) {
        viewModelScope.launch {
            val result = container.communityRepository.reportPost(post, reason)
            _statusBanner.value = if (result.isSuccess) {
                "Report submitted for Nikhil's moderation review"
            } else {
                result.exceptionOrNull()?.message ?: "Failed to submit report"
            }
        }
    }

    fun scheduleClassReminder(context: Context, liveClass: LiveClass) {
        viewModelScope.launch {
            val timeStr = SimpleDateFormat("EEE, dd MMM • h:mm a", Locale.getDefault())
                .format(Date(liveClass.scheduledAtMillis))
            container.dashboardRepository.createReminderNotification(
                userId = currentUserId,
                title = "Reminder: ${liveClass.title}",
                message = "Scheduled for $timeStr (${liveClass.courseTitle})",
                type = "class_reminder"
            )
            LocalNotificationHelper.showReminderNotification(
                context = context,
                title = "Class Reminder Set",
                message = "${liveClass.title} • $timeStr",
                id = liveClass.id.hashCode()
            )
            _statusBanner.value = "Reminder added for ${liveClass.title}"
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            container.dashboardRepository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            val result = container.authRepository.updateDisplayName(currentUserId, newName)
            _statusBanner.value = if (result.isSuccess) {
                "Profile name updated"
            } else {
                result.exceptionOrNull()?.message ?: "Could not update profile name"
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private companion object {
        const val TAG = "MainVM"
    }
}
