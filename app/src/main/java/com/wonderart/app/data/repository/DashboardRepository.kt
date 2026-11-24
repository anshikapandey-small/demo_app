package com.wonderart.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.wonderart.app.data.firebase.OperationType
import com.wonderart.app.data.firebase.handleFirestoreError
import com.wonderart.app.data.model.Announcement
import com.wonderart.app.data.model.NotificationItem
import com.wonderart.app.data.model.WonderUser
import com.wonderart.app.data.seed.SampleCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DashboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        requireUserId()
        emitAll(
            db.collection("announcements")
                .whereEqualTo("authorName", "Nikhil")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "announcements")
                    }
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "users/$userId/notifications")
                    }
                    throw error
                }
        )
    }

    fun observeAllUsersAdmin(): Flow<List<WonderUser>> = flow {
        requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WonderUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "users")
                    }
                    throw error
                }
        )
    }

    suspend fun createAnnouncementAdmin(title: String, message: String, courseId: String? = null): Result<Unit> {
        val annId = "ann_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            requireUserId()
            require(title.trim().length in 2..120) { "Announcement title must be 2-120 characters." }
            require(message.trim().length in 2..1000) { "Message must be 2-1000 characters." }
            val payload = mutableMapOf<String, Any>(
                "title" to title.trim(),
                "message" to message.trim(),
                "authorName" to "Nikhil",
                "createdAt" to FieldValue.serverTimestamp()
            )
            courseId?.takeIf { it.isNotBlank() }?.let { payload["courseId"] = it }
            db.collection("announcements").document(annId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "announcements/$annId")
            Result.failure(e)
        }
    }

    suspend fun createReminderNotification(
        userId: String,
        title: String,
        message: String,
        type: String = "class_reminder"
    ): Result<Unit> {
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user notification write." }
            db.collection("users").document(uid).collection("notifications").document(notifId).set(
                mapOf(
                    "userId" to uid,
                    "title" to title.take(120),
                    "message" to message.take(500),
                    "type" to type,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId/notifications/$notifId")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user notification update." }
        db.collection("users").document(uid).collection("notifications").document(notificationId)
            .update(mapOf("read" to true))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/notifications/$notificationId")
        Result.failure(e)
    }

    suspend fun seedSampleDataIfNeeded(forceSeed: Boolean = false): Result<Unit> = try {
        requireUserId()
        val existingCourses = db.collection("courses").limit(1).get().await()
        if (forceSeed || existingCourses.isEmpty) {
            val batch = db.batch()
            SampleCatalog.sampleCourseSeeds.forEach { (id, fields) ->
                val ref = db.collection("courses").document(id)
                val payload = fields.toMutableMap().apply {
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(ref, payload)
            }
            SampleCatalog.demoCommunitySeeds.forEach { (id, fields) ->
                val ref = db.collection("communityPosts").document(id)
                val payload = fields.toMutableMap().apply {
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(ref, payload)
            }
            val now = System.currentTimeMillis()
            batch.set(
                db.collection("liveClasses").document("live_sketching_1"),
                mapOf(
                    "courseId" to SampleCatalog.COURSE_PENCIL_ID,
                    "courseTitle" to "Pencil Sketching Fundamentals",
                    "title" to "Light, Shadow & Sphere Shading Workshop",
                    "description" to "Interactive Zoom session with Nikhil covering cross-hatching and tonal gradients.",
                    "instructor" to "Nikhil",
                    "scheduledAtMillis" to (now + 2 * 60 * 60 * 1000L),
                    "durationMinutes" to 60L,
                    "zoomMeetingId" to "84219304512",
                    "status" to "upcoming",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("liveClasses").document("live_watercolor_1"),
                mapOf(
                    "courseId" to SampleCatalog.COURSE_WATERCOLOR_ID,
                    "courseTitle" to "Watercolor Painting Masterclass",
                    "title" to "Wet-on-Wet Misty Landscapes",
                    "description" to "Paint atmospheric mountain washes live with step-by-step guidance from Nikhil.",
                    "instructor" to "Nikhil",
                    "scheduledAtMillis" to (now + 26 * 60 * 60 * 1000L),
                    "durationMinutes" to 75L,
                    "zoomMeetingId" to "91744028193",
                    "status" to "upcoming",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("announcements").document("ann_welcome_nikhil"),
                mapOf(
                    "title" to "Welcome to Wonderart Studio!",
                    "message" to "Hi artists! Explore our 4 foundational courses, sketch freely in Creative Canvas, and share your pieces in the Community gallery.",
                    "authorName" to "Nikhil",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "seedSampleData")
        Result.failure(e)
    }
}
