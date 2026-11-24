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
import com.wonderart.app.data.model.CommentItem
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.ReportItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CommunityRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeVisiblePosts(): Flow<List<CommunityPost>> = flow {
        requireUserId()
        emitAll(
            db.collection("communityPosts")
                .whereEqualTo("hidden", false)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "communityPosts")
                    }
                    throw error
                }
        )
    }

    fun observeAllPostsAdmin(): Flow<List<CommunityPost>> = flow {
        requireUserId()
        emitAll(
            db.collection("communityPosts")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "communityPosts")
                    }
                    throw error
                }
        )
    }

    fun observeComments(postId: String): Flow<List<CommentItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("communityPosts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CommentItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "communityPosts/$postId/comments")
                    }
                    throw error
                }
        )
    }

    fun observeMyLike(postId: String, userId: String): Flow<Boolean> = flow {
        if (userId.isBlank()) {
            emit(false)
            return@flow
        }
        emitAll(
            db.collection("communityPosts").document(postId)
                .collection("likes").document(userId)
                .snapshots()
                .map { it.exists() }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, "communityPosts/$postId/likes/$userId")
                    }
                    throw error
                }
        )
    }

    suspend fun publishArtwork(
        authorName: String,
        title: String,
        description: String,
        category: String,
        imageDataUri: String?
    ): Result<String> {
        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        return try {
            val uid = requireUserId()
            require(title.trim().length in 1..120) { "Title must be 1-120 characters." }
            require(description.trim().length in 1..1000) { "Description must be 1-1000 characters." }
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "authorName" to authorName.trim().ifBlank { "Student" }.take(80),
                "title" to title.trim(),
                "description" to description.trim(),
                "category" to category.trim().ifBlank { "Watercolor" },
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "demo" to false,
                "hidden" to false,
                "flagged" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            imageDataUri?.takeIf { it.isNotBlank() }?.let { payload["imageUrl"] = it }
            db.collection("communityPosts").document(postId).set(payload).await()
            Result.success(postId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "communityPosts/$postId")
            Result.failure(e)
        }
    }

    suspend fun toggleLike(post: CommunityPost, currentlyLiked: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        val postRef = db.collection("communityPosts").document(post.id)
        val likeRef = postRef.collection("likes").document(uid)
        val batch = db.batch()
        if (currentlyLiked) {
            batch.delete(likeRef)
            val nextCount = (post.likesCount - 1L).coerceAtLeast(0L)
            batch.update(postRef, mapOf("likesCount" to nextCount))
        } else {
            batch.set(
                likeRef,
                mapOf(
                    "postId" to post.id,
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(postRef, mapOf("likesCount" to post.likesCount + 1L))
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "communityPosts/${post.id}")
        Result.failure(e)
    }

    suspend fun addComment(post: CommunityPost, authorName: String, content: String): Result<Unit> {
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        return try {
            val uid = requireUserId()
            val trimmed = content.trim()
            require(trimmed.length in 1..500) { "Comment must be 1-500 characters." }
            val postRef = db.collection("communityPosts").document(post.id)
            val commentRef = postRef.collection("comments").document(commentId)
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to post.id,
                    "userId" to uid,
                    "authorName" to authorName.trim().ifBlank { "Student" }.take(80),
                    "content" to trimmed,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(postRef, mapOf("commentsCount" to post.commentsCount + 1L))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "communityPosts/${post.id}/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, title: String, description: String, category: String): Result<Unit> = try {
        requireUserId()
        require(title.trim().length in 1..120) { "Title must be 1-120 characters." }
        require(description.trim().length in 1..1000) { "Description must be 1-1000 characters." }
        db.collection("communityPosts").document(postId).update(
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "category" to category.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "communityPosts/$postId")
        Result.failure(e)
    }

    suspend fun deletePost(postId: String): Result<Unit> = try {
        requireUserId()
        db.collection("communityPosts").document(postId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "communityPosts/$postId")
        Result.failure(e)
    }

    suspend fun reportPost(post: CommunityPost, reason: String): Result<Unit> {
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        return try {
            val uid = requireUserId()
            val cleanReason = reason.trim()
            require(cleanReason.length in 2..500) { "Please provide a brief reason (2-500 chars)." }
            val batch = db.batch()
            batch.set(
                db.collection("reports").document(reportId),
                mapOf(
                    "postId" to post.id,
                    "postTitle" to post.title.take(120),
                    "reporterId" to uid,
                    "reason" to cleanReason,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("communityPosts").document(post.id),
                mapOf(
                    "flagged" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports/$reportId")
            Result.failure(e)
        }
    }

    fun observeReportsAdmin(): Flow<List<ReportItem>> = flow {
        requireUserId()
        emitAll(
            db.collection("reports")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ReportItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser != null) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, "reports")
                    }
                    throw error
                }
        )
    }

    suspend fun setPostHiddenAdmin(postId: String, hidden: Boolean): Result<Unit> = try {
        requireUserId()
        db.collection("communityPosts").document(postId).update(
            mapOf(
                "hidden" to hidden,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "communityPosts/$postId")
        Result.failure(e)
    }

    suspend fun deleteCommentAdmin(postId: String, commentId: String): Result<Unit> = try {
        requireUserId()
        db.collection("communityPosts").document(postId)
            .collection("comments").document(commentId)
            .delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "communityPosts/$postId/comments/$commentId")
        Result.failure(e)
    }

    suspend fun updateReportStatusAdmin(reportId: String, status: String): Result<Unit> = try {
        requireUserId()
        db.collection("reports").document(reportId).update(
            mapOf(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
        Result.failure(e)
    }
}
