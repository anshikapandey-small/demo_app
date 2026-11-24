package com.wonderart.app.data.repository

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
import com.wonderart.app.data.model.LiveClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class LiveClassRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeStudentLiveClasses(): Flow<List<LiveClass>> = flow {
        requireUserId()
        emitAll(
            db.collection("liveClasses")
                .whereIn("status", listOf("upcoming", "live_now", "completed"))
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LiveClass::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.scheduledAtMillis }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "liveClasses")
                    }
                    throw error
                }
        )
    }

    fun observeAllLiveClassesAdmin(): Flow<List<LiveClass>> = flow {
        requireUserId()
        emitAll(
            db.collection("liveClasses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LiveClass::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.scheduledAtMillis }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "liveClasses")
                    }
                    throw error
                }
        )
    }

    suspend fun saveLiveClassAdmin(
        classId: String?,
        courseId: String,
        courseTitle: String,
        title: String,
        description: String,
        scheduledAtMillis: Long,
        durationMinutes: Long,
        zoomMeetingId: String,
        zoomJoinUrl: String?,
        status: String = "upcoming"
    ): Result<Unit> {
        val targetId = classId?.takeIf { it.isNotBlank() } ?: "class_${UUID.randomUUID().toString().take(8)}"
        return try {
            requireUserId()
            require(title.trim().length in 2..140) { "Class title must be 2-140 characters." }
            require(description.trim().length in 2..1000) { "Description must be 2-1000 characters." }
            require(zoomMeetingId.trim().isNotBlank()) { "Zoom Meeting ID is required." }
            val ref = db.collection("liveClasses").document(targetId)
            val baseMap = mutableMapOf<String, Any>(
                "courseId" to courseId,
                "courseTitle" to courseTitle.take(120),
                "title" to title.trim(),
                "description" to description.trim(),
                "instructor" to "Nikhil",
                "scheduledAtMillis" to scheduledAtMillis,
                "durationMinutes" to durationMinutes.coerceIn(10L, 480L),
                "zoomMeetingId" to zoomMeetingId.trim().take(64),
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            zoomJoinUrl?.trim()?.takeIf { it.isNotBlank() }?.let { baseMap["zoomJoinUrl"] = it.take(500) }

            if (classId.isNullOrBlank()) {
                baseMap["createdAt"] = FieldValue.serverTimestamp()
                ref.set(baseMap).await()
            } else {
                ref.update(baseMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "liveClasses/$targetId")
            Result.failure(e)
        }
    }

    suspend fun updateLiveClassStatusAdmin(classId: String, status: String): Result<Unit> = try {
        requireUserId()
        db.collection("liveClasses").document(classId).update(
            mapOf(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "liveClasses/$classId")
        Result.failure(e)
    }

    suspend fun deleteLiveClassAdmin(classId: String): Result<Unit> = try {
        requireUserId()
        db.collection("liveClasses").document(classId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "liveClasses/$classId")
        Result.failure(e)
    }
}
