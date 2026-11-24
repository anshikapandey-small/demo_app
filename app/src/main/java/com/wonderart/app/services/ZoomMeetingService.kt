package com.wonderart.app.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ZoomSignatureRequest(
    val meetingNumber: String,
    val role: Int, // 0 = participant, 1 = host
    val classId: String,
    val courseId: String
)

@JsonClass(generateAdapter = true)
data class ZoomSignatureResponse(
    val signature: String,
    val sdkKey: String,
    val meetingNumber: String,
    val joinUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateZoomMeetingRequest(
    val topic: String,
    val startTimeIso: String,
    val durationMinutes: Int,
    val courseId: String
)

@JsonClass(generateAdapter = true)
data class CreateZoomMeetingResponse(
    val meetingId: String,
    val joinUrl: String,
    val startUrl: String? = null
)

interface ZoomBackendApi {
    @POST("api/zoom/signature")
    suspend fun generateMeetingSignature(
        @Header("Authorization") firebaseBearerToken: String,
        @Body request: ZoomSignatureRequest
    ): ZoomSignatureResponse

    @POST("api/zoom/meetings")
    suspend fun createMeeting(
        @Header("Authorization") firebaseBearerToken: String,
        @Body request: CreateZoomMeetingRequest
    ): CreateZoomMeetingResponse
}

data class ZoomConfigurationState(
    val isSdkKeyConfigured: Boolean,
    val isBackendConfigured: Boolean
) {
    val isFullyConfigured: Boolean get() = isSdkKeyConfigured && isBackendConfigured
}

class ZoomMeetingService {

    fun getConfigurationState(): ZoomConfigurationState = ZoomConfigurationState(
        isSdkKeyConfigured = BuildConfig.ZOOM_SDK_APP_KEY.isNotBlank(),
        isBackendConfigured = BuildConfig.ZOOM_BACKEND_URL.isNotBlank()
    )

    private fun createBackendApi(): ZoomBackendApi? {
        val baseUrl = BuildConfig.ZOOM_BACKEND_URL.trim()
        if (baseUrl.isBlank()) return null
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ZoomBackendApi::class.java)
    }

    suspend fun requestMeetingAuthorization(
        firebaseIdToken: String,
        meetingNumber: String,
        isHost: Boolean,
        classId: String,
        courseId: String
    ): Result<ZoomSignatureResponse> {
        val api = createBackendApi()
            ?: return Result.failure(
                IllegalStateException(
                    "Zoom backend URL is not configured. Set ZOOM_BACKEND_URL in Secrets/BuildConfig to generate server-signed SDK JWT tokens."
                )
            )
        return runCatching {
            api.generateMeetingSignature(
                firebaseBearerToken = "Bearer $firebaseIdToken",
                request = ZoomSignatureRequest(
                    meetingNumber = meetingNumber,
                    role = if (isHost) 1 else 0,
                    classId = classId,
                    courseId = courseId
                )
            )
        }
    }

    fun launchExternalZoomLink(context: Context, joinUrl: String): Result<Unit> = runCatching {
        val uri = Uri.parse(joinUrl.trim())
        require(uri.scheme == "https" && (uri.host?.contains("zoom.us") == true)) {
            "Only verified https://zoom.us meeting URLs are allowed."
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
