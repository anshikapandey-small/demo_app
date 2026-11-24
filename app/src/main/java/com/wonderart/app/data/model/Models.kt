package com.wonderart.app.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class WonderUser(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val role: String = "student",
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Course(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val instructor: String = "Nikhil",
    val thumbnailKey: String = "",
    val thumbnailUrl: String? = null,
    val category: String = "",
    val price: Long = 0L,
    val difficulty: String = "Beginner",
    val duration: String = "",
    val totalSessions: Long = 1L,
    val curriculum: List<String> = emptyList(),
    val learningObjectives: List<String> = emptyList(),
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Enrollment(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val progress: Long = 0L,
    val completedSessions: Long = 0L,
    val completionStatus: String = "ongoing",
    val enrollmentType: String = "demo",
    @ServerTimestamp val enrolledAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "Watercolor",
    val imageUrl: String? = null,
    val drawableKey: String? = null,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val demo: Boolean = false,
    val hidden: Boolean = false,
    val flagged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommentItem(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReportItem(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postTitle: String = "",
    val reporterId: String = "",
    val reason: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LiveClass(
    @DocumentId val id: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val title: String = "",
    val description: String = "",
    val instructor: String = "Nikhil",
    val scheduledAtMillis: Long = 0L,
    val durationMinutes: Long = 60L,
    val zoomMeetingId: String = "",
    val zoomJoinUrl: String? = null,
    val status: String = "upcoming",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorName: String = "Nikhil",
    val courseId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "class_reminder",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class LiveClassStatus(val raw: String, val label: String) {
    UPCOMING("upcoming", "Upcoming"),
    LIVE_NOW("live_now", "Live Now"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled");

    companion object {
        fun fromRaw(raw: String): LiveClassStatus =
            entries.firstOrNull { it.raw == raw } ?: UPCOMING
    }
}
