package com.wonderart.app.ui.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.wonderart.app.data.model.CommentItem
import com.wonderart.app.data.model.CommunityPost
import com.wonderart.app.data.model.UiState
import com.wonderart.app.ui.components.ArtworkImage
import com.wonderart.app.ui.components.AvatarCircle
import com.wonderart.app.ui.components.CircleBackButton
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderCoralSoft
import com.wonderart.app.ui.theme.WonderPurple
import com.wonderart.app.ui.theme.WonderPurpleSoft

@Composable
fun PostDetailScreen(
    post: CommunityPost?,
    allPosts: List<CommunityPost>,
    currentUserId: String,
    currentUserName: String,
    isAdmin: Boolean,
    commentsState: UiState<List<CommentItem>>,
    isLikedByMe: Boolean,
    onBack: () -> Unit,
    onToggleLike: (CommunityPost, Boolean) -> Unit,
    onAddComment: (CommunityPost, String, String, () -> Unit) -> Unit,
    onUpdatePost: (String, String, String, String) -> Unit,
    onDeletePost: (String) -> Unit,
    onReportPost: (CommunityPost, String) -> Unit,
    onDeleteCommentAdmin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (post == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Artwork not found", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onBack) { Text("Return to Gallery") }
            }
        }
        return
    }

    var commentText by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showArtistDialog by remember { mutableStateOf(false) }

    val isAuthorOrAdmin = post.userId == currentUserId || isAdmin
    val comments = (commentsState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add a thoughtful comment...") },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedBorderColor = WonderPurple,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input")
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(enabled = commentText.isNotBlank()) {
                                onAddComment(post, currentUserName, commentText) {
                                    commentText = ""
                                }
                            }
                            .testTag("send_comment_button"),
                        shape = CircleShape,
                        color = if (commentText.isNotBlank()) WonderPurple else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send comment",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleBackButton(onClick = onBack)
                        Text("Artwork", style = MaterialTheme.typography.titleMedium)
                        Box {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreHoriz,
                                        contentDescription = "Artwork options"
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (isAuthorOrAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("Edit artwork details") },
                                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                                        onClick = {
                                            menuExpanded = false
                                            showEditDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete artwork") },
                                        leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                        onClick = {
                                            menuExpanded = false
                                            onDeletePost(post.id)
                                        }
                                    )
                                }
                                if (post.userId != currentUserId) {
                                    DropdownMenuItem(
                                        text = { Text("Report inappropriate content") },
                                        leadingIcon = { Icon(Icons.Filled.Flag, null) },
                                        onClick = {
                                            menuExpanded = false
                                            showReportDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Artwork Image
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        ArtworkImage(
                            imageUrl = post.imageUrl,
                            drawableKey = post.drawableKey,
                            contentDescription = post.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.15f)
                                .clip(RoundedCornerShape(22.dp))
                        )
                    }
                }

                // Title, Artist, Like Pill, Description
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(50), color = WonderPurpleSoft) {
                                        Text(
                                            text = post.category,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = WonderPurple,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (post.demo) {
                                        Surface(shape = RoundedCornerShape(50), color = WonderCoralSoft) {
                                            Text(
                                                text = "Demo Artwork",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = WonderCoral,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onToggleLike(post, isLikedByMe) }
                                        .testTag("like_artwork_button"),
                                    shape = RoundedCornerShape(50),
                                    color = if (isLikedByMe) WonderCoralSoft else MaterialTheme.colorScheme.background
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = if (isLikedByMe) "Unlike" else "Like",
                                            tint = WonderCoral,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "${post.likesCount}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = WonderCoral
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(post.title, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showArtistDialog = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarCircle(name = post.authorName, size = 34.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = post.authorName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Tap to view artist gallery",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WonderPurple
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = post.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Comments Header
                item {
                    Text(
                        text = "Comments (${comments.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (comments.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "No comments yet. Share encouraging feedback with ${post.authorName}!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AvatarCircle(name = comment.authorName, size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = comment.content,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (isAdmin) {
                                    IconButton(onClick = { onDeleteCommentAdmin(comment.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove comment",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(post.title) }
        var editDesc by remember { mutableStateOf(post.description) }
        var editCat by remember { mutableStateOf(post.category) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Artwork") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editCat,
                        onValueChange = { editCat = it },
                        label = { Text("Category") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdatePost(post.id, editTitle, editDesc, editCat)
                        showEditDialog = false
                    },
                    enabled = editTitle.isNotBlank() && editDesc.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Artwork") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Help keep Wonderart safe for students of all ages. Explain why this post should be reviewed by Nikhil:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReportPost(post, reason)
                        showReportDialog = false
                    },
                    enabled = reason.trim().length >= 2
                ) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showArtistDialog) {
        val artistPosts = allPosts.filter { it.userId == post.userId }
        val totalLikes = artistPosts.sumOf { it.likesCount }
        AlertDialog(
            onDismissRequest = { showArtistDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCircle(name = post.authorName, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(post.authorName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${artistPosts.size} artworks • $totalLikes likes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    artistPosts.take(4).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("♥ ${item.likesCount}", style = MaterialTheme.typography.labelMedium, color = WonderCoral)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArtistDialog = false }) { Text("Close") }
            }
        )
    }
}
