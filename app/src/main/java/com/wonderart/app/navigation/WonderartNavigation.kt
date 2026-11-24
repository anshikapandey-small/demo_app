package com.wonderart.app.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.wonderart.app.AppContainer
import com.wonderart.app.data.model.UiState
import com.wonderart.app.data.seed.SampleCatalog
import com.wonderart.app.ui.admin.AdminDashboardScreen
import com.wonderart.app.ui.authentication.AuthScreen
import com.wonderart.app.ui.authentication.SplashScreen
import com.wonderart.app.ui.canvas.CreativeCanvasScreen
import com.wonderart.app.ui.canvas.SavedDrawingsScreen
import com.wonderart.app.ui.community.CommunityScreen
import com.wonderart.app.ui.community.PostDetailScreen
import com.wonderart.app.ui.community.UploadArtworkScreen
import com.wonderart.app.ui.components.CreateMenuBottomSheet
import com.wonderart.app.ui.components.MainTab
import com.wonderart.app.ui.components.WonderartBottomBar
import com.wonderart.app.ui.components.WonderartNavigationRail
import com.wonderart.app.ui.courses.CourseDetailScreen
import com.wonderart.app.ui.courses.CoursesScreen
import com.wonderart.app.ui.dashboard.DashboardScreen
import com.wonderart.app.ui.liveclasses.LiveClassRoomScreen
import com.wonderart.app.ui.liveclasses.LiveClassesScreen
import com.wonderart.app.ui.profile.NotificationsScreen
import com.wonderart.app.ui.profile.ProfileScreen
import com.wonderart.app.ui.profile.ProgressBadgesScreen
import com.wonderart.app.ui.theme.WonderartTheme
import com.wonderart.app.viewmodel.AdminViewModel
import com.wonderart.app.viewmodel.CanvasViewModel
import com.wonderart.app.viewmodel.MainViewModel
import com.wonderart.app.viewmodel.PostDetailViewModel
import kotlinx.coroutines.launch

object WonderartRoutes {
    const val HOME = "home"
    const val COURSES = "courses"
    const val MY_COURSES = "my_courses"
    const val COURSE_DETAIL = "course_detail/{courseId}"
    const val COMMUNITY = "community"
    const val POST_DETAIL = "post_detail/{postId}"
    const val UPLOAD_ARTWORK = "upload_artwork"
    const val CREATIVE_CANVAS = "creative_canvas"
    const val SAVED_DRAWINGS = "saved_drawings"
    const val LIVE_CLASSES = "live_classes"
    const val LIVE_CLASS_ROOM = "live_class_room/{classId}"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val PROGRESS_BADGES = "progress_badges"
    const val ADMIN_DASHBOARD = "admin_dashboard"

    fun courseDetail(courseId: String): String = "course_detail/${Uri.encode(courseId)}"
    fun postDetail(postId: String): String = "post_detail/${Uri.encode(postId)}"
    fun liveClassRoom(classId: String): String = "live_class_room/${Uri.encode(classId)}"
}

@Composable
fun WonderartAppRoot(container: AppContainer) {
    WonderartTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }

        val currentUser by container.authRepository.authStateFlow()
            .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

        var showAuthScreen by rememberSaveable { mutableStateOf(false) }
        var autoSignInAttempted by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!autoSignInAttempted) {
                autoSignInAttempted = true
                container.authRepository.attemptAutoSignIn(
                    context = context,
                    credentialManager = credentialManager,
                    scope = scope,
                    onAuthSuccess = { showAuthScreen = false },
                    onUnauthenticated = { /* Keep user on SplashScreen */ }
                )
            }
        }

        val activeUser = currentUser
        if (activeUser == null) {
            if (showAuthScreen) {
                BackHandler { showAuthScreen = false }
                AuthScreen(
                    authRepository = container.authRepository,
                    onBack = { showAuthScreen = false },
                    onAuthSuccess = { showAuthScreen = false }
                )
            } else {
                SplashScreen(
                    onGetStarted = { showAuthScreen = true },
                    onExistingAccount = { showAuthScreen = true }
                )
            }
        } else {
            key(activeUser.uid) {
                AuthenticatedWonderartShell(
                    container = container,
                    uid = activeUser.uid,
                    onSignOut = {
                        showAuthScreen = true
                        scope.launch {
                            container.authRepository.signOut(credentialManager)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedWonderartShell(
    container: AppContainer,
    uid: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val mainViewModel: MainViewModel = viewModel(key = uid) {
        MainViewModel(container, uid)
    }
    val canvasViewModel: CanvasViewModel = viewModel(key = "${uid}_canvas") {
        CanvasViewModel(container)
    }

    val userState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val isAdmin by mainViewModel.isAdmin.collectAsStateWithLifecycle()
    val coursesState by mainViewModel.courses.collectAsStateWithLifecycle()
    val enrollmentsState by mainViewModel.enrollments.collectAsStateWithLifecycle()
    val postsState by mainViewModel.posts.collectAsStateWithLifecycle()
    val liveClassesState by mainViewModel.liveClasses.collectAsStateWithLifecycle()
    val announcementsState by mainViewModel.announcements.collectAsStateWithLifecycle()
    val notificationsState by mainViewModel.notifications.collectAsStateWithLifecycle()
    val statusBanner by mainViewModel.statusBanner.collectAsStateWithLifecycle()
    val isPublishing by mainViewModel.isPublishing.collectAsStateWithLifecycle()
    val canvasState by canvasViewModel.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateMenu by remember { mutableStateOf(false) }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { message ->
            snackbarHostState.showSnackbar(message)
            mainViewModel.clearStatusBanner()
        }
    }

    val topLevelRoutes = remember {
        setOf(
            WonderartRoutes.HOME,
            WonderartRoutes.COURSES,
            WonderartRoutes.COMMUNITY,
            WonderartRoutes.PROFILE
        )
    }
    val showPrimaryNav = currentRoute in topLevelRoutes

    val navigateTab: (MainTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && showPrimaryNav) {
                WonderartNavigationRail(
                    currentRoute = currentRoute,
                    onNavigateTab = navigateTab,
                    onCreateClick = { showCreateMenu = true }
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail && showPrimaryNav) {
                        WonderartBottomBar(
                            currentRoute = currentRoute,
                            onNavigateTab = navigateTab,
                            onCreateClick = { showCreateMenu = true }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = WonderartRoutes.HOME
                    ) {
                        composable(WonderartRoutes.HOME) {
                            DashboardScreen(
                                userState = userState,
                                isAdmin = isAdmin,
                                coursesState = coursesState,
                                enrollmentsState = enrollmentsState,
                                liveClassesState = liveClassesState,
                                announcementsState = announcementsState,
                                notificationsState = notificationsState,
                                postsState = postsState,
                                onOpenNotifications = { navController.navigate(WonderartRoutes.NOTIFICATIONS) },
                                onOpenProgress = { navController.navigate(WonderartRoutes.PROGRESS_BADGES) },
                                onOpenCourses = { navigateTab(MainTab.COURSES) },
                                onOpenCourseDetail = { id -> navController.navigate(WonderartRoutes.courseDetail(id)) },
                                onOpenLiveClasses = { navController.navigate(WonderartRoutes.LIVE_CLASSES) },
                                onOpenLiveClassRoom = { id -> navController.navigate(WonderartRoutes.liveClassRoom(id)) },
                                onOpenCreativeCanvas = { navController.navigate(WonderartRoutes.CREATIVE_CANVAS) },
                                onOpenPostDetail = { id -> navController.navigate(WonderartRoutes.postDetail(id)) },
                                onOpenCommunity = { navigateTab(MainTab.COMMUNITY) },
                                onOpenAdminDashboard = {
                                    if (isAdmin) navController.navigate(WonderartRoutes.ADMIN_DASHBOARD)
                                }
                            )
                        }

                        composable(WonderartRoutes.COURSES) {
                            CoursesScreen(
                                coursesState = coursesState,
                                enrollmentsState = enrollmentsState,
                                initialMyCoursesOnly = false,
                                onOpenCourseDetail = { id -> navController.navigate(WonderartRoutes.courseDetail(id)) }
                            )
                        }

                        composable(WonderartRoutes.MY_COURSES) {
                            CoursesScreen(
                                coursesState = coursesState,
                                enrollmentsState = enrollmentsState,
                                initialMyCoursesOnly = true,
                                onOpenCourseDetail = { id -> navController.navigate(WonderartRoutes.courseDetail(id)) }
                            )
                        }

                        composable(
                            route = WonderartRoutes.COURSE_DETAIL,
                            arguments = listOf(navArgument("courseId") { type = NavType.StringType })
                        ) { entry ->
                            val courseId = entry.arguments?.getString("courseId").orEmpty()
                            val cloudCourses = (coursesState as? UiState.Success)?.data ?: emptyList()
                            val allCourses = cloudCourses.ifEmpty { SampleCatalog.previewFallbackCourses }
                            val course = allCourses.firstOrNull { it.id == courseId }
                            val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
                            val enrollment = enrollments.firstOrNull { it.courseId == courseId }
                            val classes = ((liveClassesState as? UiState.Success)?.data ?: emptyList())
                                .filter { it.courseId == courseId }

                            CourseDetailScreen(
                                course = course,
                                enrollment = enrollment,
                                courseLiveClasses = classes,
                                onBack = { navController.popBackStack() },
                                onDemoEnroll = { mainViewModel.enrollInCourseDemo(it) },
                                onUpdateProgress = { cid, completed, total ->
                                    mainViewModel.updateCourseProgress(cid, completed, total)
                                },
                                onOpenLiveClassRoom = { id ->
                                    navController.navigate(WonderartRoutes.liveClassRoom(id))
                                }
                            )
                        }

                        composable(WonderartRoutes.COMMUNITY) {
                            CommunityScreen(
                                postsState = postsState,
                                onOpenPostDetail = { id -> navController.navigate(WonderartRoutes.postDetail(id)) },
                                onOpenUploadArtwork = { navController.navigate(WonderartRoutes.UPLOAD_ARTWORK) }
                            )
                        }

                        composable(
                            route = WonderartRoutes.POST_DETAIL,
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { entry ->
                            val postId = entry.arguments?.getString("postId").orEmpty()
                            val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
                            val post = allPosts.firstOrNull { it.id == postId }
                            val currentUserName = (userState as? UiState.Success)?.data?.name ?: "Wonderart Artist"

                            val detailVm: PostDetailViewModel = viewModel(key = "${uid}_post_$postId") {
                                PostDetailViewModel(container, postId, uid)
                            }
                            val commentsState by detailVm.comments.collectAsStateWithLifecycle()
                            val isLikedByMe by detailVm.isLikedByMe.collectAsStateWithLifecycle()

                            PostDetailScreen(
                                post = post,
                                allPosts = allPosts,
                                currentUserId = uid,
                                currentUserName = currentUserName,
                                isAdmin = isAdmin,
                                commentsState = commentsState,
                                isLikedByMe = isLikedByMe,
                                onBack = { navController.popBackStack() },
                                onToggleLike = { p, liked -> mainViewModel.toggleLike(p, liked) },
                                onAddComment = { p, authorName, content, onDone ->
                                    detailVm.submitComment(p, authorName, content, onDone)
                                },
                                onUpdatePost = { id, t, d, c -> mainViewModel.updatePost(id, t, d, c) },
                                onDeletePost = { id ->
                                    mainViewModel.deletePost(id) {
                                        navController.popBackStack()
                                    }
                                },
                                onReportPost = { p, reason -> mainViewModel.reportPost(p, reason) },
                                onDeleteCommentAdmin = { cid -> detailVm.deleteCommentAdmin(cid) }
                            )
                        }

                        composable(WonderartRoutes.UPLOAD_ARTWORK) {
                            UploadArtworkScreen(
                                isPublishing = isPublishing,
                                onBack = { navController.popBackStack() },
                                onPublish = { uri, title, description, category ->
                                    mainViewModel.publishArtworkFromUri(
                                        context = context,
                                        imageUri = uri,
                                        title = title,
                                        description = description,
                                        category = category,
                                        onPublished = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            )
                        }

                        composable(WonderartRoutes.CREATIVE_CANVAS) {
                            CreativeCanvasScreen(
                                canvasViewModel = canvasViewModel,
                                isPublishing = isPublishing,
                                onBack = { navController.popBackStack() },
                                onOpenSavedDrafts = { navController.navigate(WonderartRoutes.SAVED_DRAWINGS) },
                                onPublishBitmap = { bitmap, title, desc, cat, onDone ->
                                    mainViewModel.publishArtworkFromBitmap(
                                        bitmap = bitmap,
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        onPublished = {
                                            onDone()
                                            navigateTab(MainTab.COMMUNITY)
                                        }
                                    )
                                }
                            )
                        }

                        composable(WonderartRoutes.SAVED_DRAWINGS) {
                            SavedDrawingsScreen(
                                canvasViewModel = canvasViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenCanvas = { navController.navigate(WonderartRoutes.CREATIVE_CANVAS) }
                            )
                        }

                        composable(WonderartRoutes.LIVE_CLASSES) {
                            LiveClassesScreen(
                                liveClassesState = liveClassesState,
                                enrollmentsState = enrollmentsState,
                                isAdmin = isAdmin,
                                onBack = { navController.popBackStack() },
                                onOpenClassRoom = { id -> navController.navigate(WonderartRoutes.liveClassRoom(id)) },
                                onOpenCourseDetail = { id -> navController.navigate(WonderartRoutes.courseDetail(id)) },
                                onSetReminder = { lc -> mainViewModel.scheduleClassReminder(context, lc) }
                            )
                        }

                        composable(
                            route = WonderartRoutes.LIVE_CLASS_ROOM,
                            arguments = listOf(navArgument("classId") { type = NavType.StringType })
                        ) { entry ->
                            val classId = entry.arguments?.getString("classId").orEmpty()
                            val classes = (liveClassesState as? UiState.Success)?.data ?: emptyList()
                            val liveClass = classes.firstOrNull { it.id == classId }
                            val enrollments = (enrollmentsState as? UiState.Success)?.data ?: emptyList()
                            val enrolled = liveClass != null && enrollments.any { it.courseId == liveClass.courseId }

                            LiveClassRoomScreen(
                                liveClass = liveClass,
                                isEnrolled = enrolled,
                                isAdmin = isAdmin,
                                zoomMeetingService = container.zoomMeetingService,
                                onBack = { navController.popBackStack() },
                                onOpenCourseDetail = { cid -> navController.navigate(WonderartRoutes.courseDetail(cid)) }
                            )
                        }

                        composable(WonderartRoutes.PROFILE) {
                            ProfileScreen(
                                userState = userState,
                                isAdmin = isAdmin,
                                enrollmentsState = enrollmentsState,
                                postsState = postsState,
                                notificationsState = notificationsState,
                                savedDraftsCount = canvasState.savedDrafts.size,
                                onUpdateDisplayName = { mainViewModel.updateDisplayName(it) },
                                onOpenMyCourses = { navController.navigate(WonderartRoutes.MY_COURSES) },
                                onOpenSavedDrawings = { navController.navigate(WonderartRoutes.SAVED_DRAWINGS) },
                                onOpenProgressBadges = { navController.navigate(WonderartRoutes.PROGRESS_BADGES) },
                                onOpenLiveClasses = { navController.navigate(WonderartRoutes.LIVE_CLASSES) },
                                onOpenNotifications = { navController.navigate(WonderartRoutes.NOTIFICATIONS) },
                                onOpenAdminDashboard = {
                                    if (isAdmin) navController.navigate(WonderartRoutes.ADMIN_DASHBOARD)
                                },
                                onSignOut = onSignOut
                            )
                        }

                        composable(WonderartRoutes.NOTIFICATIONS) {
                            NotificationsScreen(
                                notificationsState = notificationsState,
                                onBack = { navController.popBackStack() },
                                onMarkRead = { mainViewModel.markNotificationRead(it) }
                            )
                        }

                        composable(WonderartRoutes.PROGRESS_BADGES) {
                            ProgressBadgesScreen(
                                userState = userState,
                                enrollmentsState = enrollmentsState,
                                postsState = postsState,
                                savedDraftsCount = canvasState.savedDrafts.size,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(WonderartRoutes.ADMIN_DASHBOARD) {
                            if (isAdmin) {
                                val adminViewModel: AdminViewModel = viewModel(key = "${uid}_admin") {
                                    AdminViewModel(container)
                                }
                                val coroutineScope = rememberCoroutineScope()
                                AdminDashboardScreen(
                                    adminViewModel = adminViewModel,
                                    isAdmin = true,
                                    onBack = { navController.popBackStack() },
                                    onShowMessage = { msg ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            } else {
                                DashboardScreen(
                                    userState = userState,
                                    isAdmin = false,
                                    coursesState = coursesState,
                                    enrollmentsState = enrollmentsState,
                                    liveClassesState = liveClassesState,
                                    announcementsState = announcementsState,
                                    notificationsState = notificationsState,
                                    postsState = postsState,
                                    onOpenNotifications = { navController.navigate(WonderartRoutes.NOTIFICATIONS) },
                                    onOpenProgress = { navController.navigate(WonderartRoutes.PROGRESS_BADGES) },
                                    onOpenCourses = { navigateTab(MainTab.COURSES) },
                                    onOpenCourseDetail = { id -> navController.navigate(WonderartRoutes.courseDetail(id)) },
                                    onOpenLiveClasses = { navController.navigate(WonderartRoutes.LIVE_CLASSES) },
                                    onOpenLiveClassRoom = { id -> navController.navigate(WonderartRoutes.liveClassRoom(id)) },
                                    onOpenCreativeCanvas = { navController.navigate(WonderartRoutes.CREATIVE_CANVAS) },
                                    onOpenPostDetail = { id -> navController.navigate(WonderartRoutes.postDetail(id)) },
                                    onOpenCommunity = { navigateTab(MainTab.COMMUNITY) },
                                    onOpenAdminDashboard = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateMenu) {
        CreateMenuBottomSheet(
            onDismiss = { showCreateMenu = false },
            onCreateDrawing = { navController.navigate(WonderartRoutes.CREATIVE_CANVAS) },
            onUploadArtwork = { navController.navigate(WonderartRoutes.UPLOAD_ARTWORK) }
        )
    }
}
