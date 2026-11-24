package com.wonderart.app.services

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.wonderart.app.utils.ImageCompressionUtil

/**
 * Abstraction for artwork image storage.
 * Currently uses compressed JPEG payloads stored via Firestore so community publishing
 * works immediately without requiring manual Firebase Cloud Storage bucket provisioning.
 * Swap [uploadArtworkBitmap] / [uploadArtworkUri] with FirebaseStorage when Storage is enabled.
 */
interface ArtworkMediaService {
    suspend fun prepareArtworkForPost(context: Context, uri: Uri): Result<String>
    suspend fun prepareArtworkBitmapForPost(bitmap: Bitmap): Result<String>
}

class CompressedFirestoreMediaService : ArtworkMediaService {
    override suspend fun prepareArtworkForPost(context: Context, uri: Uri): Result<String> =
        ImageCompressionUtil.compressUriToDataUri(context, uri)

    override suspend fun prepareArtworkBitmapForPost(bitmap: Bitmap): Result<String> =
        ImageCompressionUtil.compressBitmapToDataUri(bitmap)
}
