package com.wonderart.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressionUtil {

    private const val MAX_DIMENSION_PX = 800
    private const val TARGET_MAX_BYTES = 180_000

    suspend fun compressUriToDataUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Unable to open selected image.")
                val original = input.use { BitmapFactory.decodeStream(it) }
                    ?: error("Unsupported image format.")
                encodeBitmapToDataUri(original)
            }
        }

    suspend fun compressBitmapToDataUri(bitmap: Bitmap): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching { encodeBitmapToDataUri(bitmap) }
        }

    private fun encodeBitmapToDataUri(source: Bitmap): String {
        val scaled = scaleDownIfNeeded(source, MAX_DIMENSION_PX)
        var quality = 84
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 10
        } while (bytes.size > TARGET_MAX_BYTES && quality >= 35)

        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encoded"
    }

    fun decodeDataUriToBitmap(dataUri: String?): Bitmap? {
        if (dataUri.isNullOrBlank() || !dataUri.startsWith("data:image")) return null
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex == -1) return null
        return try {
            val base64Part = dataUri.substring(commaIndex + 1)
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val newW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
