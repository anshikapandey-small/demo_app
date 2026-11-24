package com.wonderart.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.wonderart.app.AppContainer
import com.wonderart.app.data.model.CommentItem
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val container: AppContainer,
    private val postId: String,
    private val currentUserId: String
) : ViewModel() {

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError.asStateFlow()

    val comments: StateFlow<UiState<List<CommentItem>>> =
        container.communityRepository.observeComments(postId)
            .map<List<CommentItem>, UiState<List<CommentItem>>> { UiState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w("PostDetailVM", "Error observing comments", root)
                emit(UiState.Error(root.message ?: "Failed to load comments"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val isLikedByMe: StateFlow<Boolean> =
        container.communityRepository.observeMyLike(postId, currentUserId)
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(false)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = false
            )

    fun submitComment(post: CommunityPost, authorName: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = container.communityRepository.addComment(post, authorName, content)
            if (result.isSuccess) {
                _commentError.value = null
                onSuccess()
            } else {
                _commentError.value = result.exceptionOrNull()?.message ?: "Unable to post comment"
            }
        }
    }

    fun deleteCommentAdmin(commentId: String) {
        viewModelScope.launch {
            container.communityRepository.deleteCommentAdmin(postId, commentId)
        }
    }
}
