# Wonderart

**Create. Learn. Inspire.**  
Native Android online art-learning platform founded by **Nikhil**, built with Kotlin, Jetpack Compose, Material Design 3, and Firebase.

## Architecture & Modules

- **UI & Design System (`com.wonderart.app.ui.theme`, `ui/components`)**: Centralized Material 3 theme (`Color.kt`, `Theme.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`) featuring Wonderart's purple (`#4F3FF0`), coral (`#F96D4B`), and soft lavender (`#F7F6FB`) palette with Plus Jakarta Sans & Inter typography.
- **Navigation (`com.wonderart.app.navigation`)**: Jetpack Navigation Compose with auth-state gating, per-user UID ViewModel scoping (`key(uid)`), and adaptive `WonderartBottomBar` / `WonderartNavigationRail` layouts.
- **Student Experience**:
  - **Dashboard**: Welcome header, XP/Level/Streak card, Enrolled/Ongoing/Completed KPIs, recently accessed courses, upcoming live classes, teacher announcements, featured community artworks, and Creative Canvas shortcut.
  - **Courses Marketplace & Detail**: Search, category filters, Explore / My Courses toggle, curriculum breakdown, learning objectives, demonstration enrollment ("No real payment is processed"), session progress updates, and course live classes.
  - **Art Community**: Two-column gallery, category pills, Camera (`CAMERA` permission) and Android Photo Picker (`PickVisualMedia`) artwork uploads, JPEG compression, likes, comments, edit/delete for own posts, artist gallery modal, and moderation reporting.
  - **Creative Canvas Studio (`ui/canvas`)**: Works fully offline for all students with Pencil, Brush, Marker, Eraser, Fill, Shapes (Line, Rectangle, Circle, Triangle), Color Swatches & HSV picker, Brush Size/Opacity sliders, Undo/Redo, Layer visibility/selection/deletion, Pan & Zoom, Canvas background selector, local offline JSON + PNG draft storage (`CanvasDraftStorage`), PNG export/share via `FileProvider`, and Publish Preview directly to Wonderart Community.
  - **Live Online Classes (`ui/liveclasses`)**: Upcoming, Live Now, Completed, and Cancelled states, enrollment-gated access, local & Firestore class reminders (`POST_NOTIFICATIONS`), and Zoom integration readiness.
- **Teacher Admin Console (`ui/admin`, `AdminViewModel`)**: Accessible exclusively to Nikhil (`admins/{uid}` or verified admin account) for Course CRUD/publishing, Student enrollment granting/revoking, Live Class scheduling & status control, Announcements, and Community moderation/reports.

## Firebase Setup

This project is provisioned with Firebase Authentication (Google Sign-In via Jetpack Credential Manager) and Cloud Firestore (`R.string.firestore_database_id`).

### Optional Firebase Console Upgrades (Manual)
1. **Firebase Cloud Storage**: To store full-resolution artwork files in Cloud Storage instead of compressed inline data URIs, enable Storage in the Firebase Console, add `firebase-storage` to `app/build.gradle.kts`, and swap `ArtworkMediaService` in `WonderartApplication.kt`.
2. **Firebase Cloud Messaging (FCM)**: Enable FCM in the Firebase Console, add `firebase-messaging`, and deploy a Cloud Function triggered on `/announcements/{id}` and `/liveClasses/{id}` writes.
3. **Email/Password Authentication**: Enable the Email/Password provider in Firebase Authentication if password sign-in is desired alongside Google Sign-In.

### Security Rules & Testing
- Rules are defined in `firestore.rules` and schema documented in `firebase-blueprint.json`.
- Run emulator rules tests with:
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```

## Live Classroom (Zoom SDK & Backend JWT)

`ZoomMeetingService` and `LiveClassRoomScreen` enforce enrollment-gated access and never embed Zoom SDK secrets in the Android APK:
1. Configure `ZOOM_BACKEND_URL` (your HTTPS Cloud Function or backend server generating Zoom Meeting SDK JWT signatures via `POST /api/zoom/signature` and creating meetings via `POST /api/zoom/meetings`) and `ZOOM_SDK_APP_KEY` in Secrets / `BuildConfig`.
2. Add the official Zoom Meeting SDK for Android AAR/Maven dependency when packaging with a Zoom Developer account, and initialize it with the backend-signed JWT returned by `ZoomMeetingService.requestMeetingAuthorization(...)`.
3. Verified `https://zoom.us/...` meeting join URLs can also be launched directly from `LiveClassRoomScreen`.

## Building the APK

```bash
gradle assembleDebug
```
Debug APK output is generated under `app/build/outputs/apk/debug/`.
