package com.wonderart.app.ui.canvas

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wonderart.app.data.local.CanvasActionDto
import com.wonderart.app.data.local.CanvasPointDto
import com.wonderart.app.ui.components.CategoryPillRow
import com.wonderart.app.ui.theme.WonderCoral
import com.wonderart.app.ui.theme.WonderPurple
import com.wonderart.app.ui.theme.WonderPurpleSoft
import com.wonderart.app.viewmodel.CanvasLayerState
import com.wonderart.app.viewmodel.CanvasShapeType
import com.wonderart.app.viewmodel.CanvasTool
import com.wonderart.app.viewmodel.CanvasViewModel
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

private val artistSwatches = listOf(
    0xFF151627L,
    0xFF4F3FF0L,
    0xFFF96D4BL,
    0xFFEF4444L,
    0xFFF59E0BL,
    0xFF16A34AL,
    0xFF06B6D4L,
    0xFFEC4899L,
    0xFF8B5CF6L,
    0xFFFFFFFFL
)

private val paperColors = listOf(
    0xFFFFFFFFL to "Pure White",
    0xFFFFFBF2L to "Warm Cream",
    0xFFF3F0FFL to "Soft Lavender",
    0xFFEBFDF5L to "Mint Paper",
    0xFF1C1F33L to "Dark Studio"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreativeCanvasScreen(
    canvasViewModel: CanvasViewModel,
    isPublishing: Boolean,
    onBack: () -> Unit,
    onOpenSavedDrafts: () -> Unit,
    onPublishBitmap: (Bitmap, String, String, String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by canvasViewModel.state.collectAsStateWithLifecycle()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasPixelSize by remember { mutableStateOf(IntSize(1080, 1440)) }

    val activeStrokePoints = remember { mutableStateListOf<CanvasPointDto>() }
    var shapeStart by remember { mutableStateOf<CanvasPointDto?>(null) }
    var shapeCurrent by remember { mutableStateOf<CanvasPointDto?>(null) }

    var showLayersSheet by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showBgColorDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var showPublishPreviewDialog by remember { mutableStateOf(false) }
    var previewBitmapForPublish by remember { mutableStateOf<Bitmap?>(null) }

    fun handleAttemptExit() {
        if (state.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    BackHandler { handleAttemptExit() }

    LaunchedEffect(state.statusMessage) {
        if (state.statusMessage != null) {
            kotlinx.coroutines.delay(2500)
            canvasViewModel.clearStatusMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar (matching Figma 13 Drawing Canvas)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { handleAttemptExit() },
                    modifier = Modifier.testTag("canvas_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showTitleDialog = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Rename artwork",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (state.hasUnsavedChanges) "Unsaved changes" else "Saved locally",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.hasUnsavedChanges) WonderCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { canvasViewModel.undo() },
                    enabled = state.canUndo,
                    modifier = Modifier.testTag("canvas_undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
                IconButton(
                    onClick = { canvasViewModel.redo() },
                    enabled = state.canRedo,
                    modifier = Modifier.testTag("canvas_redo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                }

                Button(
                    onClick = {
                        val bmp = renderCanvasToBitmap(
                            width = canvasPixelSize.width.coerceAtLeast(800),
                            height = canvasPixelSize.height.coerceAtLeast(1000),
                            backgroundColorArgb = state.backgroundColorArgb,
                            layers = state.layers
                        )
                        canvasViewModel.saveDraftLocally(bmp)
                    },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WonderPurple),
                    modifier = Modifier.testTag("canvas_save_button")
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLarge)
                }

                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("canvas_more_button")
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Canvas menu")
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Publish to Community") },
                            leadingIcon = { Icon(Icons.Filled.CloudUpload, null, tint = WonderPurple) },
                            onClick = {
                                showOverflowMenu = false
                                previewBitmapForPublish = renderCanvasToBitmap(
                                    width = canvasPixelSize.width.coerceAtLeast(800),
                                    height = canvasPixelSize.height.coerceAtLeast(1000),
                                    backgroundColorArgb = state.backgroundColorArgb,
                                    layers = state.layers
                                )
                                showPublishPreviewDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export & Share PNG") },
                            leadingIcon = { Icon(Icons.Filled.Download, null) },
                            onClick = {
                                showOverflowMenu = false
                                val bmp = renderCanvasToBitmap(
                                    width = canvasPixelSize.width.coerceAtLeast(1080),
                                    height = canvasPixelSize.height.coerceAtLeast(1350),
                                    backgroundColorArgb = state.backgroundColorArgb,
                                    layers = state.layers
                                )
                                canvasViewModel.exportPng(bmp) { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share PNG Artwork"))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Canvas Background Color") },
                            leadingIcon = { Icon(Icons.Filled.ColorLens, null) },
                            onClick = {
                                showOverflowMenu = false
                                showBgColorDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("My Saved Drawings") },
                            leadingIcon = { Icon(Icons.Filled.FolderOpen, null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenSavedDrafts()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Blank Canvas") },
                            leadingIcon = { Icon(Icons.Filled.NoteAdd, null) },
                            onClick = {
                                showOverflowMenu = false
                                scale = 1f
                                offset = Offset.Zero
                                canvasViewModel.newBlankCanvas()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Clear Canvas", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showOverflowMenu = false
                                showClearDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Drawing Viewport
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val containerW = constraints.maxWidth.coerceAtLeast(600)
            val containerH = constraints.maxHeight.coerceAtLeast(800)
            canvasPixelSize = IntSize(containerW, containerH)

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = Color(state.backgroundColorArgb)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(state.isPanZoomMode) {
                            if (state.isPanZoomMode) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(0.6f, 4.0f)
                                    scale = newScale
                                    offset += pan
                                }
                            }
                        }
                        .pointerInput(
                            state.isPanZoomMode,
                            state.activeTool,
                            state.activeShape,
                            state.selectedColorArgb,
                            state.brushSize,
                            state.opacity,
                            state.activeLayerIndex
                        ) {
                            if (state.isPanZoomMode) return@pointerInput

                            if (state.activeTool == CanvasTool.FILL) {
                                detectTapGestures {
                                    canvasViewModel.commitAction(
                                        CanvasActionDto(
                                            tool = "FILL",
                                            colorArgb = state.selectedColorArgb,
                                            strokeWidth = 0f,
                                            alpha = state.opacity,
                                            points = emptyList()
                                        )
                                    )
                                }
                            } else {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        val normPt = CanvasPointDto(
                                            x = ((start.x - offset.x) / scale) / size.width.coerceAtLeast(1),
                                            y = ((start.y - offset.y) / scale) / size.height.coerceAtLeast(1)
                                        )
                                        if (state.activeTool == CanvasTool.SHAPE) {
                                            shapeStart = normPt
                                            shapeCurrent = normPt
                                        } else {
                                            activeStrokePoints.clear()
                                            activeStrokePoints.add(normPt)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val normPt = CanvasPointDto(
                                            x = ((change.position.x - offset.x) / scale) / size.width.coerceAtLeast(1),
                                            y = ((change.position.y - offset.y) / scale) / size.height.coerceAtLeast(1)
                                        )
                                        if (state.activeTool == CanvasTool.SHAPE) {
                                            shapeCurrent = normPt
                                        } else {
                                            activeStrokePoints.add(normPt)
                                        }
                                    },
                                    onDragEnd = {
                                        if (state.activeTool == CanvasTool.SHAPE) {
                                            val startPt = shapeStart
                                            val endPt = shapeCurrent
                                            if (startPt != null && endPt != null) {
                                                canvasViewModel.commitAction(
                                                    CanvasActionDto(
                                                        tool = "SHAPE",
                                                        shapeType = state.activeShape.name,
                                                        colorArgb = state.selectedColorArgb,
                                                        strokeWidth = state.brushSize,
                                                        alpha = state.opacity,
                                                        points = listOf(startPt, endPt)
                                                    )
                                                )
                                            }
                                            shapeStart = null
                                            shapeCurrent = null
                                        } else if (activeStrokePoints.isNotEmpty()) {
                                            canvasViewModel.commitAction(
                                                CanvasActionDto(
                                                    tool = state.activeTool.name,
                                                    colorArgb = state.selectedColorArgb,
                                                    strokeWidth = state.brushSize,
                                                    alpha = state.opacity,
                                                    points = activeStrokePoints.toList()
                                                )
                                            )
                                            activeStrokePoints.clear()
                                        }
                                    },
                                    onDragCancel = {
                                        activeStrokePoints.clear()
                                        shapeStart = null
                                        shapeCurrent = null
                                    }
                                )
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                                compositingStrategy = CompositingStrategy.Offscreen
                            )
                    ) {
                        drawRect(color = Color(state.backgroundColorArgb))

                        state.layers.forEachIndexed { layerIdx, layer ->
                            if (layer.visible) {
                                layer.actions.forEach { action ->
                                    drawCanvasAction(action)
                                }
                                if (layerIdx == state.activeLayerIndex) {
                                    if (activeStrokePoints.isNotEmpty()) {
                                        drawCanvasAction(
                                            CanvasActionDto(
                                                tool = state.activeTool.name,
                                                colorArgb = state.selectedColorArgb,
                                                strokeWidth = state.brushSize,
                                                alpha = state.opacity,
                                                points = activeStrokePoints.toList()
                                            )
                                        )
                                    }
                                    val sStart = shapeStart
                                    val sEnd = shapeCurrent
                                    if (sStart != null && sEnd != null) {
                                        drawCanvasAction(
                                            CanvasActionDto(
                                                tool = "SHAPE",
                                                shapeType = state.activeShape.name,
                                                colorArgb = state.selectedColorArgb,
                                                strokeWidth = state.brushSize,
                                                alpha = state.opacity,
                                                points = listOf(sStart, sEnd)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Layer Indicator Pill (matching Figma 13)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { showLayersSheet = true }
                            .testTag("canvas_layer_pill"),
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1D1F33).copy(alpha = 0.86f)
                    ) {
                        Text(
                            text = "Layer ${state.activeLayerIndex + 1} of ${state.layers.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Pan/Zoom & Reset Chip
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (scale != 1f || offset != Offset.Zero) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF1D1F33).copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "${(scale * 100).toInt()}% • Reset",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { canvasViewModel.togglePanZoomMode() }
                                .testTag("canvas_pan_zoom_toggle"),
                            shape = RoundedCornerShape(50),
                            color = if (state.isPanZoomMode) WonderPurple else Color.White.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PanTool,
                                    contentDescription = "Pan and Zoom",
                                    tint = if (state.isPanZoomMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (state.isPanZoomMode) "Pan/Zoom" else "Draw",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.isPanZoomMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    state.statusMessage?.let { msg ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF151627).copy(alpha = 0.88f)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Tool Dock (matching Figma 13)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tool Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ToolButton(Icons.Filled.Create, "Pencil", state.activeTool == CanvasTool.PENCIL && !state.isPanZoomMode) {
                        canvasViewModel.selectTool(CanvasTool.PENCIL)
                    }
                    ToolButton(Icons.Filled.Brush, "Brush", state.activeTool == CanvasTool.BRUSH && !state.isPanZoomMode) {
                        canvasViewModel.selectTool(CanvasTool.BRUSH)
                    }
                    ToolButton(Icons.Filled.AutoFixHigh, "Marker", state.activeTool == CanvasTool.MARKER && !state.isPanZoomMode) {
                        canvasViewModel.selectTool(CanvasTool.MARKER)
                    }
                    ToolButton(Icons.Filled.Close, "Eraser", state.activeTool == CanvasTool.ERASER && !state.isPanZoomMode) {
                        canvasViewModel.selectTool(CanvasTool.ERASER)
                    }
                    ToolButton(Icons.Filled.FormatColorFill, "Fill", state.activeTool == CanvasTool.FILL && !state.isPanZoomMode) {
                        canvasViewModel.selectTool(CanvasTool.FILL)
                    }
                    ToolButton(Icons.Filled.Category, "Shapes", state.activeTool == CanvasTool.SHAPE && !state.isPanZoomMode) {
                        canvasViewModel.selectTool(CanvasTool.SHAPE)
                    }
                }

                // Shape sub-selector when Shapes active
                if (state.activeTool == CanvasTool.SHAPE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasShapeType.entries.forEach { shape ->
                            val selected = shape == state.activeShape
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { canvasViewModel.selectShape(shape) },
                                shape = RoundedCornerShape(50),
                                color = if (selected) WonderPurple else MaterialTheme.colorScheme.background
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shape.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Color Swatches + Custom Color + Layers button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(artistSwatches) { argb ->
                            val selected = state.selectedColorArgb == argb
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 30.dp else 26.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) WonderPurple else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable { canvasViewModel.selectColor(argb) }
                            )
                        }
                        item {
                            Surface(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable { showColorPickerDialog = true },
                                shape = CircleShape,
                                color = WonderPurpleSoft
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.ColorLens,
                                        contentDescription = "Custom color picker",
                                        tint = WonderPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showLayersSheet = true },
                        shape = RoundedCornerShape(50),
                        color = WonderPurpleSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Layers,
                                contentDescription = "Layers",
                                tint = WonderPurple,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Layers",
                                style = MaterialTheme.typography.labelMedium,
                                color = WonderPurple
                            )
                        }
                    }
                }

                // Size & Opacity Sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )
                    Slider(
                        value = state.brushSize,
                        onValueChange = { canvasViewModel.setBrushSize(it) },
                        valueRange = 2f..48f,
                        colors = SliderDefaults.colors(thumbColor = WonderPurple, activeTrackColor = WonderPurple),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${state.brushSize.toInt()} px",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(42.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Alpha",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = state.opacity,
                        onValueChange = { canvasViewModel.setOpacity(it) },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = WonderCoral, activeTrackColor = WonderCoral),
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }

    // Layer Manager Sheet
    if (showLayersSheet) {
        ModalBottomSheet(onDismissRequest = { showLayersSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Layers (${state.layers.size}/8)", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = { canvasViewModel.addLayer() },
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Layer")
                    }
                }
                state.layers.forEachIndexed { idx, layer ->
                    val active = idx == state.activeLayerIndex
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { canvasViewModel.selectLayer(idx) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) WonderPurpleSoft else MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, if (active) WonderPurple else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { canvasViewModel.toggleLayerVisibility(idx) }) {
                                Icon(
                                    imageVector = if (layer.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Toggle layer visibility",
                                    tint = if (layer.visible) WonderPurple else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(layer.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${layer.actions.size} strokes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.layers.size > 1) {
                                IconButton(onClick = { canvasViewModel.deleteLayer(idx) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete layer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Custom HSV Color Picker Dialog
    if (showColorPickerDialog) {
        val hsv = remember {
            val arr = FloatArray(3)
            android.graphics.Color.colorToHSV(state.selectedColorArgb.toInt(), arr)
            arr
        }
        var hue by remember { mutableFloatStateOf(hsv[0]) }
        var sat by remember { mutableFloatStateOf(hsv[1].coerceAtLeast(0.2f)) }
        var value by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(0.2f)) }
        val previewArgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)).toLong() and 0xFFFFFFFFL

        AlertDialog(
            onDismissRequest = { showColorPickerDialog = false },
            title = { Text("Custom Color Picker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(previewArgb))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    )
                    Text("Hue", style = MaterialTheme.typography.labelMedium)
                    Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                    Text("Saturation", style = MaterialTheme.typography.labelMedium)
                    Slider(value = sat, onValueChange = { sat = it }, valueRange = 0f..1f)
                    Text("Brightness", style = MaterialTheme.typography.labelMedium)
                    Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    canvasViewModel.selectColor(previewArgb)
                    showColorPickerDialog = false
                }) { Text("Use Color") }
            },
            dismissButton = {
                TextButton(onClick = { showColorPickerDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Background Paper Color Dialog
    if (showBgColorDialog) {
        AlertDialog(
            onDismissRequest = { showBgColorDialog = false },
            title = { Text("Canvas Background") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    paperColors.forEach { (argb, label) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    canvasViewModel.setBackgroundColor(argb)
                                    showBgColorDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(1.dp, Color.Gray, CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBgColorDialog = false }) { Text("Close") }
            }
        )
    }

    // Rename Dialog
    if (showTitleDialog) {
        var tempTitle by remember { mutableStateOf(state.title) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("Artwork Title") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        canvasViewModel.setTitle(tempTitle.ifBlank { "Untitled Artwork" })
                        showTitleDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Clear Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Canvas?") },
            text = { Text("This clears all layers on the current canvas. You can still Undo immediately after clearing.") },
            confirmButton = {
                TextButton(onClick = {
                    canvasViewModel.clearCanvasConfirmed()
                    showClearDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Discard Unsaved Work Confirmation Dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Save before leaving?") },
            text = { Text("You have unsaved changes on '${state.title}'. Save locally before closing?") },
            confirmButton = {
                TextButton(onClick = {
                    val bmp = renderCanvasToBitmap(
                        width = canvasPixelSize.width.coerceAtLeast(800),
                        height = canvasPixelSize.height.coerceAtLeast(1000),
                        backgroundColorArgb = state.backgroundColorArgb,
                        layers = state.layers
                    )
                    canvasViewModel.saveDraftLocally(bmp) {
                        showDiscardDialog = false
                        onBack()
                    }
                }) { Text("Save & Exit") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        onBack()
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Drawing") }
                }
            }
        )
    }

    // Preview Before Publishing to Community Dialog
    if (showPublishPreviewDialog && previewBitmapForPublish != null) {
        var pubTitle by remember { mutableStateOf(state.title) }
        var pubDesc by remember { mutableStateOf("Created in Wonderart Creative Canvas.") }
        var pubCat by remember { mutableStateOf("Digital Canvas") }
        val cats = listOf("Digital Canvas", "Pencil Sketching", "Watercolor", "Acrylic Painting", "Mandala Art")
        val previewBmp = previewBitmapForPublish!!

        AlertDialog(
            onDismissRequest = { if (!isPublishing) showPublishPreviewDialog = false },
            title = { Text("Preview & Publish Artwork") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        bitmap = previewBmp.asImageBitmap(),
                        contentDescription = "Canvas preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                    OutlinedTextField(
                        value = pubTitle,
                        onValueChange = { pubTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pubDesc,
                        onValueChange = { pubDesc = it },
                        label = { Text("Description") },
                        minLines = 2
                    )
                    CategoryPillRow(
                        categories = cats,
                        selectedCategory = pubCat,
                        onSelectCategory = { pubCat = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPublishBitmap(previewBmp, pubTitle, pubDesc, pubCat) {
                            showPublishPreviewDialog = false
                        }
                    },
                    enabled = !isPublishing && pubTitle.isNotBlank() && pubDesc.isNotBlank()
                ) {
                    Text(if (isPublishing) "Publishing..." else "Publish")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPublishPreviewDialog = false },
                    enabled = !isPublishing
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) WonderPurple else MaterialTheme.colorScheme.background)
                .border(
                    width = 1.dp,
                    color = if (selected) WonderPurple else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) WonderPurple else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun DrawScope.drawCanvasAction(action: CanvasActionDto) {
    val w = size.width
    val h = size.height
    val baseColor = Color(action.colorArgb)

    when (action.tool) {
        "FILL" -> {
            drawRect(color = baseColor.copy(alpha = action.alpha))
        }
        "SHAPE" -> {
            if (action.points.size < 2) return
            val p1 = Offset(action.points[0].x * w, action.points[0].y * h)
            val p2 = Offset(action.points[1].x * w, action.points[1].y * h)
            val stroke = Stroke(width = action.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val color = baseColor.copy(alpha = action.alpha)
            when (action.shapeType) {
                CanvasShapeType.LINE.name -> drawLine(color = color, start = p1, end = p2, strokeWidth = action.strokeWidth, cap = StrokeCap.Round)
                CanvasShapeType.RECTANGLE.name -> {
                    val left = min(p1.x, p2.x)
                    val top = min(p1.y, p2.y)
                    drawRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(abs(p2.x - p1.x), abs(p2.y - p1.y)),
                        style = stroke
                    )
                }
                CanvasShapeType.CIRCLE.name -> {
                    val radius = hypot(p2.x - p1.x, p2.y - p1.y)
                    drawCircle(color = color, radius = radius, center = p1, style = stroke)
                }
                CanvasShapeType.TRIANGLE.name -> {
                    val left = min(p1.x, p2.x)
                    val right = maxOf(p1.x, p2.x)
                    val top = min(p1.y, p2.y)
                    val bottom = maxOf(p1.y, p2.y)
                    val path = Path().apply {
                        moveTo((left + right) / 2f, top)
                        lineTo(right, bottom)
                        lineTo(left, bottom)
                        close()
                    }
                    drawPath(path = path, color = color, style = stroke)
                }
            }
        }
        else -> {
            if (action.points.isEmpty()) return
            val path = Path().apply {
                val first = action.points.first()
                moveTo(first.x * w, first.y * h)
                if (action.points.size == 1) {
                    lineTo(first.x * w + 0.5f, first.y * h + 0.5f)
                } else {
                    for (i in 1 until action.points.size) {
                        val prev = action.points[i - 1]
                        val curr = action.points[i]
                        val midX = ((prev.x + curr.x) / 2f) * w
                        val midY = ((prev.y + curr.y) / 2f) * h
                        quadraticTo(prev.x * w, prev.y * h, midX, midY)
                    }
                    val last = action.points.last()
                    lineTo(last.x * w, last.y * h)
                }
            }
            val effectiveWidth = when (action.tool) {
                CanvasTool.PENCIL.name -> (action.strokeWidth * 0.55f).coerceAtLeast(1.5f)
                CanvasTool.MARKER.name -> action.strokeWidth * 1.65f
                CanvasTool.ERASER.name -> action.strokeWidth * 1.4f
                else -> action.strokeWidth
            }
            val effectiveAlpha = when (action.tool) {
                CanvasTool.MARKER.name -> (action.alpha * 0.45f).coerceIn(0.15f, 0.7f)
                else -> action.alpha
            }
            val blendMode = if (action.tool == CanvasTool.ERASER.name) BlendMode.Clear else BlendMode.SrcOver
            drawPath(
                path = path,
                color = if (action.tool == CanvasTool.ERASER.name) Color.Transparent else baseColor.copy(alpha = effectiveAlpha),
                style = Stroke(width = effectiveWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = blendMode
            )
        }
    }
}

fun renderCanvasToBitmap(
    width: Int,
    height: Int,
    backgroundColorArgb: Long,
    layers: List<CanvasLayerState>
): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    canvas.drawColor(Color(backgroundColorArgb).toArgb())

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    layers.forEach { layer ->
        if (!layer.visible) return@forEach
        val layerSave = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        layer.actions.forEach { action ->
            val colorInt = Color(action.colorArgb).toArgb()
            when (action.tool) {
                "FILL" -> {
                    paint.reset()
                    paint.isAntiAlias = true
                    paint.style = Paint.Style.FILL
                    paint.color = colorInt
                    paint.alpha = (action.alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                }
                "SHAPE" -> {
                    if (action.points.size >= 2) {
                        val x1 = action.points[0].x * width
                        val y1 = action.points[0].y * height
                        val x2 = action.points[1].x * width
                        val y2 = action.points[1].y * height
                        paint.reset()
                        paint.isAntiAlias = true
                        paint.style = Paint.Style.STROKE
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeJoin = Paint.Join.ROUND
                        paint.strokeWidth = action.strokeWidth
                        paint.color = colorInt
                        paint.alpha = (action.alpha * 255).toInt().coerceIn(0, 255)
                        when (action.shapeType) {
                            CanvasShapeType.LINE.name -> canvas.drawLine(x1, y1, x2, y2, paint)
                            CanvasShapeType.RECTANGLE.name -> canvas.drawRect(min(x1, x2), min(y1, y2), maxOf(x1, x2), maxOf(y1, y2), paint)
                            CanvasShapeType.CIRCLE.name -> canvas.drawCircle(x1, y1, hypot(x2 - x1, y2 - y1), paint)
                            CanvasShapeType.TRIANGLE.name -> {
                                val p = AndroidPath().apply {
                                    moveTo((x1 + x2) / 2f, min(y1, y2))
                                    lineTo(maxOf(x1, x2), maxOf(y1, y2))
                                    lineTo(min(x1, x2), maxOf(y1, y2))
                                    close()
                                }
                                canvas.drawPath(p, paint)
                            }
                        }
                    }
                }
                else -> {
                    if (action.points.isNotEmpty()) {
                        val path = AndroidPath().apply {
                            val first = action.points.first()
                            moveTo(first.x * width, first.y * height)
                            if (action.points.size == 1) {
                                lineTo(first.x * width + 0.5f, first.y * height + 0.5f)
                            } else {
                                for (i in 1 until action.points.size) {
                                    val prev = action.points[i - 1]
                                    val curr = action.points[i]
                                    val midX = ((prev.x + curr.x) / 2f) * width
                                    val midY = ((prev.y + curr.y) / 2f) * height
                                    quadTo(prev.x * width, prev.y * height, midX, midY)
                                }
                                val last = action.points.last()
                                lineTo(last.x * width, last.y * height)
                            }
                        }
                        paint.reset()
                        paint.isAntiAlias = true
                        paint.style = Paint.Style.STROKE
                        paint.strokeCap = Paint.Cap.ROUND
                        paint.strokeJoin = Paint.Join.ROUND
                        paint.strokeWidth = when (action.tool) {
                            CanvasTool.PENCIL.name -> (action.strokeWidth * 0.55f).coerceAtLeast(1.5f)
                            CanvasTool.MARKER.name -> action.strokeWidth * 1.65f
                            CanvasTool.ERASER.name -> action.strokeWidth * 1.4f
                            else -> action.strokeWidth
                        }
                        if (action.tool == CanvasTool.ERASER.name) {
                            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                        } else {
                            paint.color = colorInt
                            val effAlpha = if (action.tool == CanvasTool.MARKER.name) (action.alpha * 0.45f) else action.alpha
                            paint.alpha = (effAlpha * 255).toInt().coerceIn(0, 255)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }
        canvas.restoreToCount(layerSave)
    }
    return bmp
}
