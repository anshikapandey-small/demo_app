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
import com.wonderart.app.data.model.Course
import com.wonderart.app.data.model.Enrollment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CourseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observePublishedCourses(): Flow<List<Course>> = flow {
        requireUserId()
        emitAll(
            db.collection("courses")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.price }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "courses")
                    }
                    throw error
                }
        )
    }

    fun observeAllCoursesAdmin(): Flow<List<Course>> = flow {
        requireUserId()
        emitAll(
            db.collection("courses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Course::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "courses")
                    }
                    throw error
                }
        )
    }

    fun observeStudentEnrollments(studentId: String): Flow<List<Enrollment>> = flow {
        if (studentId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("enrollments")
                .whereEqualTo("studentId", studentId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Enrollment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == studentId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "enrollments")
                    }
                    throw error
                }
        )
    }

    fun observeAllEnrollmentsAdmin(): Flow<List<Enrollment>> = flow {
        requireUserId()
        emitAll(
            db.collection("enrollments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Enrollment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "enrollments")
                    }
                    throw error
                }
        )
    }

    suspend fun enrollInCourseDemo(course: Course, studentName: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        val enrollmentId = "${uid ?: "unauth"}_${course.id}"
        return try {
            val signedUid = requireUserId()
            require(course.id.isNotBlank()) { "Course ID is required." }
            val docId = "${signedUid}_${course.id}"
            val payload = mapOf(
                "studentId" to signedUid,
                "studentName" to studentName.trim().ifBlank { "Student" }.take(80),
                "courseId" to course.id,
                "courseTitle" to course.title.take(120),
                "progress" to 0L,
                "completedSessions" to 0L,
                "completionStatus" to "ongoing",
                "enrollmentType" to "demo",
                "enrolledAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("enrollments").document(docId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "enrollments/$enrollmentId")
            Result.failure(e)
        }
    }

    suspend fun updateEnrollmentProgress(
        courseId: String,
        completedSessions: Long,
        totalSessions: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val docId = "${uid ?: "unauth"}_$courseId"
        return try {
            val signedUid = requireUserId()
            val validDocId = "${signedUid}_$courseId"
            val clampedSessions = completedSessions.coerceIn(0L, totalSessions.coerceAtLeast(1L))
            val pct = ((clampedSessions * 100L) / totalSessions.coerceAtLeast(1L)).coerceIn(0L, 100L)
            val status = if (pct >= 100L) "completed" else "ongoing"
            db.collection("enrollments").document(validDocId).update(
                mapOf(
                    "completedSessions" to clampedSessions,
                    "progress" to pct,
                    "completionStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "enrollments/$docId")
            Result.failure(e)
        }
    }

    suspend fun saveCourseAdmin(
        courseId: String?,
        title: String,
        description: String,
        instructor: String,
        category: String,
        price: Long,
        difficulty: String,
        duration: String,
        totalSessions: Long,
        curriculum: List<String>,
        learningObjectives: List<String>,
        thumbnailKey: String,
        thumbnailUrl: String?,
        published: Boolean
    ): Result<Unit> {
        val targetId = courseId?.takeIf { it.isNotBlank() } ?: "course_${UUID.randomUUID().toString().take(8)}"
        return try {
            requireUserId()
            require(title.trim().length in 2..120) { "Course title must be 2-120 characters." }
            require(description.trim().length in 4..2000) { "Course description must be 4-2000 characters." }
            val ref = db.collection("courses").document(targetId)
            if (courseId.isNullOrBlank()) {
                val payload = mutableMapOf<String, Any>(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "instructor" to instructor.trim().ifBlank { "Nikhil" },
                    "category" to category.trim(),
                    "price" to price.coerceAtLeast(0L),
                    "difficulty" to difficulty.trim(),
                    "duration" to duration.trim(),
                    "totalSessions" to totalSessions.coerceIn(1L, 500L),
                    "curriculum" to curriculum.take(40),
                    "learningObjectives" to learningObjectives.take(25),
                    "thumbnailKey" to thumbnailKey,
                    "published" to published,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                thumbnailUrl?.takeIf { it.isNotBlank() }?.let { payload["thumbnailUrl"] = it }
                ref.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "title" to title.trim(),
                    "description" to description.trim(),
                    "instructor" to instructor.trim().ifBlank { "Nikhil" },
                    "category" to category.trim(),
                    "price" to price.coerceAtLeast(0L),
                    "difficulty" to difficulty.trim(),
                    "duration" to duration.trim(),
                    "totalSessions" to totalSessions.coerceIn(1L, 500L),
                    "curriculum" to curriculum.take(40),
                    "learningObjectives" to learningObjectives.take(25),
                    "thumbnailKey" to thumbnailKey,
                    "published" to published,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                thumbnailUrl?.takeIf { it.isNotBlank() }?.let { updates["thumbnailUrl"] = it }
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "courses/$targetId")
            Result.failure(e)
        }
    }

    suspend fun toggleCoursePublishedAdmin(courseId: String, published: Boolean): Result<Unit> = try {
        requireUserId()
        db.collection("courses").document(courseId).update(
            mapOf(
                "published" to published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "courses/$courseId")
        Result.failure(e)
    }

    suspend fun grantStudentEnrollmentAdmin(studentId: String, studentName: String, course: Course): Result<Unit> {
        val docId = "${studentId}_${course.id}"
        return try {
            requireUserId()
            db.collection("enrollments").document(docId).set(
                mapOf(
                    "studentId" to studentId,
                    "studentName" to studentName.take(80),
                    "courseId" to course.id,
                    "courseTitle" to course.title.take(120),
                    "progress" to 0L,
                    "completedSessions" to 0L,
                    "completionStatus" to "ongoing",
                    "enrollmentType" to "admin_granted",
                    "enrolledAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "enrollments/$docId")
            Result.failure(e)
        }
    }

    suspend fun revokeEnrollmentAdmin(enrollmentId: String): Result<Unit> = try {
        requireUserId()
        db.collection("enrollments").document(enrollmentId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "enrollments/$enrollmentId")
        Result.failure(e)
    }
}
