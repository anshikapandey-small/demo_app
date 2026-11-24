package com.wonderart.app

import android.app.Application
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.wonderart.app.data.local.CanvasDraftStorage
import com.wonderart.app.data.repository.AuthRepository
import com.wonderart.app.data.repository.CommunityRepository
import com.wonderart.app.data.repository.CourseRepository
import com.wonderart.app.data.repository.DashboardRepository
import com.wonderart.app.data.repository.LiveClassRepository
import com.wonderart.app.services.ArtworkMediaService
import com.wonderart.app.services.CompressedFirestoreMediaService
import com.wonderart.app.services.ZoomMeetingService

class AppContainer(application: Application) {
    private val databaseId: String = application.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)

    val authRepository = AuthRepository(firestore)
    val courseRepository = CourseRepository(firestore)
    val communityRepository = CommunityRepository(firestore)
    val liveClassRepository = LiveClassRepository(firestore)
    val dashboardRepository = DashboardRepository(firestore)

    val canvasDraftStorage = CanvasDraftStorage(application.applicationContext)
    val artworkMediaService: ArtworkMediaService = CompressedFirestoreMediaService()
    val zoomMeetingService = ZoomMeetingService()
}

class WonderartApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
