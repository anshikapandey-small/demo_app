package com.wonderart.app.ui.liveclasses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.wonderart.app.data.model.LiveClass
import com.wonderart.app.data.model.LiveClassStatus
import com.wonderart.app.services.ZoomMeetingService
import com.wonderart.app.ui.components.CircleBackButton
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderDarkBg
import com.wonderart.app.ui.theme.WonderDarkBorder
import com.wonderart.app.ui.theme.WonderDarkSurface
import com.wonderart.app.ui.theme.WonderMint
import com.wonderart.app.ui.theme.WonderPurple
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveClassRoomScreen(
    liveClass: LiveClass?,
    isEnrolled: Boolean,
    isAdmin: Boolean,
    zoomMeetingService: ZoomMeetingService,
    onBack: () -> Unit,
    onOpenCourseDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (liveClass == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Live class not found", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        }
        return
    }

    val zoomConfig = remember { zoomMeetingService.getConfigurationState() }
    val status = LiveClassStatus.fromRaw(liveClass.status)
    val authorized = isEnrolled || isAdmin
    var sdkStatusNote by remember { mutableStateOf<String?>(null) }

    val timeStr = remember(liveClass.scheduledAtMillis) {
        SimpleDateFormat("EEE, dd MMM yyyy • h:mm a", Locale.getDefault())
            .format(Date(liveClass.scheduledAtMillis))
    }

    // Dark Studio Theme inspired by Figma 14 Live Class
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WonderDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleBackButton(
                    onClick = onBack,
                    tint = Color.White,
                    containerColor = WonderDarkSurface
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (status == LiveClassStatus.LIVE_NOW) WonderCoral else WonderDarkSurface,
                    border = BorderStroke(1.dp, WonderDarkBorder)
                ) {
                    Text(
                        text = status.label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Studio Stage Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = WonderDarkSurface,
                border = BorderStroke(1.dp, WonderDarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(WonderPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = liveClass.courseTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFB5B8CE)
                            )
                            Text(
                                text = liveClass.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = liveClass.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD6D8EA)
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WonderDarkBg
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Instructor: ${liveClass.instructor}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                            Text(
                                text = "Schedule: $timeStr (${liveClass.durationMinutes} min)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB5B8CE)
                            )
                            Text(
                                text = "Zoom Meeting ID: ${liveClass.zoomMeetingId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB5B8CE)
                            )
                        }
                    }
                }
            }

            // Access Verification Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = WonderDarkSurface,
                border = BorderStroke(1.dp, WonderDarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (authorized) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = if (authorized) WonderMint else WonderCoral
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (authorized) {
                                if (isAdmin) "Host Access Verified (Nikhil)" else "Course Enrollment Verified"
                            } else {
                                "Enrollment Required to Join"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                    }

                    if (!authorized) {
                        Text(
                            text = "Only students enrolled in ${liveClass.courseTitle} can join this live Zoom session.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB5B8CE)
                        )
                        Button(
                            onClick = { onOpenCourseDetail(liveClass.courseId) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WonderPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to Course & Enroll")
                        }
                    } else {
                        if (!zoomConfig.isFullyConfigured) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = WonderDarkBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = WonderCoral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Zoom Meeting SDK credentials (ZOOM_SDK_APP_KEY, ZOOM_BACKEND_URL) are not configured yet. No secrets are stored in this app. Add the Zoom Android SDK AAR and Firebase Cloud Function signature endpoint to join natively.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD6D8EA)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val token = try {
                                        Firebase.auth.currentUser?.getIdToken(false)?.await()?.token ?: ""
                                    } catch (_: Exception) {
                                        ""
                                    }
                                    val res = zoomMeetingService.requestMeetingAuthorization(
                                        firebaseIdToken = token,
                                        meetingNumber = liveClass.zoomMeetingId,
                                        isHost = isAdmin,
                                        classId = liveClass.id,
                                        courseId = liveClass.courseId
                                    )
                                    sdkStatusNote = res.fold(
                                        onSuccess = {
                                            "Backend JWT signature received for meeting ${it.meetingNumber}. Attach the Zoom Meeting SDK AAR to launch the embedded room."
                                        },
                                        onFailure = { err ->
                                            err.message ?: "Zoom backend is not configured."
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("join_zoom_sdk_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WonderPurple)
                        ) {
                            Icon(Icons.Filled.Videocam, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isAdmin) "Start via Zoom SDK" else "Join via Zoom SDK")
                        }

                        if (!liveClass.zoomJoinUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val launchRes = zoomMeetingService.launchExternalZoomLink(
                                        context,
                                        liveClass.zoomJoinUrl
                                    )
                                    if (launchRes.isFailure) {
                                        sdkStatusNote = launchRes.exceptionOrNull()?.message
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, WonderDarkBorder)
                            ) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Verified Zoom Link", color = Color.White)
                            }
                        }

                        sdkStatusNote?.let { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = WonderCoral
                            )
                        }
                    }
                }
            }
        }
    }
}
