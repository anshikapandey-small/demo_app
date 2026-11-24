package com.wonderart.app.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wonderart.app.AppContainer
import com.wonderart.app.data.local.CanvasActionDto
import com.wonderart.app.data.local.CanvasDraftDocument
import com.wonderart.app.data.local.CanvasLayerDto
import com.wonderart.app.data.local.SavedDraftSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class CanvasTool(val label: String) {
    PENCIL("Pencil"),
    BRUSH("Brush"),
    MARKER("Marker"),
    ERASER("Eraser"),
    FILL("Fill"),
    SHAPE("Shapes")
}

enum class CanvasShapeType(val label: String) {
    LINE("Line"),
    RECTANGLE("Rect"),
    CIRCLE("Circle"),
    TRIANGLE("Triangle")
}

data class CanvasLayerState(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String,
    val visible: Boolean = true,
    val actions: List<CanvasActionDto> = emptyList()
)

private data class CanvasSnapshot(
    val layers: List<CanvasLayerState>,
    val activeLayerIndex: Int,
    val backgroundColorArgb: Long
)

data class CreativeCanvasUiState(
    val draftId: String = "draft_${UUID.randomUUID().toString().take(8)}",
    val title: String = "Untitled Artwork",
    val backgroundColorArgb: Long = 0xFFFFFFFF,
    val layers: List<CanvasLayerState> = listOf(
        CanvasLayerState(id = "layer_1", name = "Layer 1", visible = true),
        CanvasLayerState(id = "layer_2", name = "Layer 2", visible = true)
    ),
    val activeLayerIndex: Int = 0,
    val activeTool: CanvasTool = CanvasTool.BRUSH,
    val activeShape: CanvasShapeType = CanvasShapeType.CIRCLE,
    val brushSize: Float = 10f,
    val opacity: Float = 1.0f,
    val selectedColorArgb: Long = 0xFF4F3FF0,
    val isPanZoomMode: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val savedDrafts: List<SavedDraftSummary> = emptyList(),
    val statusMessage: String? = null
)

class CanvasViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _state = MutableStateFlow(CreativeCanvasUiState())
    val state: StateFlow<CreativeCanvasUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<CanvasSnapshot>()
    private val redoStack = ArrayDeque<CanvasSnapshot>()

    init {
        refreshSavedDrafts()
    }

    fun refreshSavedDrafts() {
        viewModelScope.launch {
            val drafts = container.canvasDraftStorage.listDrafts()
            _state.update { it.copy(savedDrafts = drafts) }
        }
    }

    fun clearStatusMessage() {
        _state.update { it.copy(statusMessage = null) }
    }

    fun setTitle(newTitle: String) {
        _state.update { it.copy(title = newTitle.take(80), hasUnsavedChanges = true) }
    }

    fun selectTool(tool: CanvasTool) {
        _state.update { it.copy(activeTool = tool, isPanZoomMode = false) }
    }

    fun selectShape(shape: CanvasShapeType) {
        _state.update { it.copy(activeShape = shape, activeTool = CanvasTool.SHAPE, isPanZoomMode = false) }
    }

    fun setBrushSize(size: Float) {
        _state.update { it.copy(brushSize = size.coerceIn(2f, 48f)) }
    }

    fun setOpacity(opacity: Float) {
        _state.update { it.copy(opacity = opacity.coerceIn(0.1f, 1.0f)) }
    }

    fun selectColor(argb: Long) {
        _state.update { it.copy(selectedColorArgb = argb) }
    }

    fun togglePanZoomMode() {
        _state.update { it.copy(isPanZoomMode = !it.isPanZoomMode) }
    }

    private fun pushUndoSnapshot() {
        val current = _state.value
        undoStack.addLast(
            CanvasSnapshot(
                layers = current.layers,
                activeLayerIndex = current.activeLayerIndex,
                backgroundColorArgb = current.backgroundColorArgb
            )
        )
        if (undoStack.size > 40) undoStack.removeFirst()
        redoStack.clear()
    }

    fun commitAction(action: CanvasActionDto) {
        pushUndoSnapshot()
        _state.update { s ->
            val idx = s.activeLayerIndex.coerceIn(0, s.layers.lastIndex)
            val updatedLayers = s.layers.mapIndexed { i, layer ->
                if (i == idx) layer.copy(actions = layer.actions + action) else layer
            }
            s.copy(
                layers = updatedLayers,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun setBackgroundColor(argb: Long) {
        pushUndoSnapshot()
        _state.update {
            it.copy(
                backgroundColorArgb = argb,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _state.value
        redoStack.addLast(
            CanvasSnapshot(
                layers = current.layers,
                activeLayerIndex = current.activeLayerIndex,
                backgroundColorArgb = current.backgroundColorArgb
            )
        )
        val prev = undoStack.removeLast()
        _state.update {
            it.copy(
                layers = prev.layers,
                activeLayerIndex = prev.activeLayerIndex,
                backgroundColorArgb = prev.backgroundColorArgb,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _state.value
        undoStack.addLast(
            CanvasSnapshot(
                layers = current.layers,
                activeLayerIndex = current.activeLayerIndex,
                backgroundColorArgb = current.backgroundColorArgb
            )
        )
        val next = redoStack.removeLast()
        _state.update {
            it.copy(
                layers = next.layers,
                activeLayerIndex = next.activeLayerIndex,
                backgroundColorArgb = next.backgroundColorArgb,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }

    fun clearCanvasConfirmed() {
        pushUndoSnapshot()
        _state.update { s ->
            val cleared = s.layers.map { it.copy(actions = emptyList()) }
            s.copy(
                layers = cleared,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                statusMessage = "Canvas cleared"
            )
        }
    }

    fun addLayer() {
        pushUndoSnapshot()
        _state.update { s ->
            if (s.layers.size >= 8) {
                return@update s.copy(statusMessage = "Maximum 8 layers supported")
            }
            val newLayer = CanvasLayerState(name = "Layer ${s.layers.size + 1}", visible = true)
            val newList = s.layers + newLayer
            s.copy(
                layers = newList,
                activeLayerIndex = newList.lastIndex,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun selectLayer(index: Int) {
        _state.update { s ->
            s.copy(activeLayerIndex = index.coerceIn(0, s.layers.lastIndex))
        }
    }

    fun toggleLayerVisibility(index: Int) {
        pushUndoSnapshot()
        _state.update { s ->
            val updated = s.layers.mapIndexed { i, layer ->
                if (i == index) layer.copy(visible = !layer.visible) else layer
            }
            s.copy(
                layers = updated,
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun deleteLayer(index: Int) {
        if (_state.value.layers.size <= 1) {
            _state.update { it.copy(statusMessage = "At least one layer is required") }
            return
        }
        pushUndoSnapshot()
        _state.update { s ->
            val updated = s.layers.filterIndexed { i, _ -> i != index }
            s.copy(
                layers = updated,
                activeLayerIndex = s.activeLayerIndex.coerceIn(0, updated.lastIndex),
                hasUnsavedChanges = true,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun newBlankCanvas() {
        undoStack.clear()
        redoStack.clear()
        _state.value = CreativeCanvasUiState(
            savedDrafts = _state.value.savedDrafts
        )
    }

    fun loadDraft(draftId: String) {
        viewModelScope.launch {
            val doc = container.canvasDraftStorage.loadDraft(draftId)
            if (doc != null) {
                undoStack.clear()
                redoStack.clear()
                val loadedLayers = if (doc.layers.isEmpty()) {
                    listOf(CanvasLayerState(name = "Layer 1"))
                } else {
                    doc.layers.map {
                        CanvasLayerState(
                            id = it.id,
                            name = it.name,
                            visible = it.visible,
                            actions = it.actions
                        )
                    }
                }
                _state.update {
                    it.copy(
                        draftId = doc.draftId,
                        title = doc.title,
                        backgroundColorArgb = doc.backgroundColorArgb,
                        layers = loadedLayers,
                        activeLayerIndex = 0,
                        hasUnsavedChanges = false,
                        canUndo = false,
                        canRedo = false,
                        statusMessage = "Loaded '${doc.title}'"
                    )
                }
            } else {
                _state.update { it.copy(statusMessage = "Could not load draft") }
            }
        }
    }

    fun saveDraftLocally(renderedBitmap: Bitmap, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val current = _state.value
            val doc = CanvasDraftDocument(
                draftId = current.draftId,
                title = current.title.ifBlank { "Untitled Artwork" },
                backgroundColorArgb = current.backgroundColorArgb,
                layers = current.layers.map {
                    CanvasLayerDto(
                        id = it.id,
                        name = it.name,
                        visible = it.visible,
                        actions = it.actions
                    )
                }
            )
            val result = container.canvasDraftStorage.saveDraft(doc, renderedBitmap)
            if (result.isSuccess) {
                refreshSavedDrafts()
                _state.update {
                    it.copy(
                        hasUnsavedChanges = false,
                        statusMessage = "Saved offline to My Drawings"
                    )
                }
                onSaved()
            } else {
                _state.update {
                    it.copy(statusMessage = result.exceptionOrNull()?.message ?: "Save failed")
                }
            }
        }
    }

    fun deleteDraft(draftId: String) {
        viewModelScope.launch {
            container.canvasDraftStorage.deleteDraft(draftId)
            refreshSavedDrafts()
            _state.update { it.copy(statusMessage = "Draft removed") }
        }
    }

    fun exportPng(renderedBitmap: Bitmap, onExported: (Uri) -> Unit) {
        viewModelScope.launch {
            val result = container.canvasDraftStorage.exportPng(_state.value.title, renderedBitmap)
            result.onSuccess { (file, uri) ->
                _state.update { it.copy(statusMessage = "Exported PNG: ${file.name}") }
                onExported(uri)
            }.onFailure { err ->
                _state.update { it.copy(statusMessage = err.message ?: "Export failed") }
            }
        }
    }
}
