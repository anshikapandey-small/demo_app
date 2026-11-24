package com.wonderart.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@JsonClass(generateAdapter = true)
data class CanvasPointDto(
    val x: Float,
    val y: Float
)

@JsonClass(generateAdapter = true)
data class CanvasActionDto(
    val tool: String, // PENCIL, BRUSH, MARKER, ERASER, FILL, SHAPE
    val shapeType: String? = null, // LINE, RECTANGLE, CIRCLE, TRIANGLE
    val colorArgb: Long,
    val strokeWidth: Float,
    val alpha: Float,
    val points: List<CanvasPointDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CanvasLayerDto(
    val id: String,
    val name: String,
    val visible: Boolean = true,
    val actions: List<CanvasActionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CanvasDraftDocument(
    val draftId: String,
    val title: String,
    val backgroundColorArgb: Long = 0xFFFFFFFF,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val layers: List<CanvasLayerDto> = emptyList()
)

data class SavedDraftSummary(
    val draft: CanvasDraftDocument,
    val previewFile: File?
)

class CanvasDraftStorage(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(CanvasDraftDocument::class.java)

    private val draftsDir: File
        get() = File(context.filesDir, "canvas_drafts").apply { mkdirs() }

    private val exportsDir: File
        get() = File(context.filesDir, "canvas_exports").apply { mkdirs() }

    suspend fun saveDraft(draft: CanvasDraftDocument, previewBitmap: Bitmap): Result<SavedDraftSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeId = draft.draftId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                val jsonFile = File(draftsDir, "$safeId.json")
                val pngFile = File(draftsDir, "$safeId.png")

                val updated = draft.copy(
                    draftId = safeId,
                    updatedAtMillis = System.currentTimeMillis()
                )
                jsonFile.writeText(adapter.toJson(updated))
                FileOutputStream(pngFile).use { out ->
                    previewBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                SavedDraftSummary(updated, pngFile)
            }
        }

    suspend fun listDrafts(): List<SavedDraftSummary> = withContext(Dispatchers.IO) {
        val files = draftsDir.listFiles { file -> file.extension == "json" } ?: return@withContext emptyList()
        files.mapNotNull { jsonFile ->
            try {
                val doc = adapter.fromJson(jsonFile.readText()) ?: return@mapNotNull null
                val pngFile = File(draftsDir, "${doc.draftId}.png").takeIf { it.exists() }
                SavedDraftSummary(doc, pngFile)
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.draft.updatedAtMillis }
    }

    suspend fun loadDraft(draftId: String): CanvasDraftDocument? = withContext(Dispatchers.IO) {
        val safeId = draftId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val jsonFile = File(draftsDir, "$safeId.json")
        if (!jsonFile.exists()) return@withContext null
        try {
            adapter.fromJson(jsonFile.readText())
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteDraft(draftId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val safeId = draftId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            File(draftsDir, "$safeId.json").delete()
            File(draftsDir, "$safeId.png").delete()
            Unit
        }
    }

    suspend fun exportPng(title: String, bitmap: Bitmap): Result<Pair<File, Uri>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanName = title.lowercase()
                    .replace(Regex("[^a-z0-9]+"), "_")
                    .trim('_')
                    .ifBlank { "wonderart_sketch" }
                val file = File(exportsDir, "${cleanName}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                file to uri
            }
        }
}
