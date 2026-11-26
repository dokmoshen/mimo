package app.mimo.notes.ui.editor

import app.mimo.notes.ui.components.MimoIcons
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.AutoFixOff
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.mimo.notes.R
import app.mimo.notes.data.DrawStroke
import app.mimo.notes.data.Drawing
import app.mimo.notes.ui.LocalMimoHaptics
import app.mimo.notes.ui.components.DrawingPreview
import android.graphics.PointF
import java.util.UUID

@Composable
fun DrawingEditorDialog(
    initial: Drawing?,
    noteColor: Color,
    contentColor: Color,
    onDismiss: () -> Unit,
    onSave: (Drawing?) -> Unit,
) {
    val strokes = remember { mutableStateListOf<DrawStroke>().apply { addAll(initial?.strokes.orEmpty()) } }
    val undoStack = remember { mutableStateListOf<List<DrawStroke>>() }
    val redoStack = remember { mutableStateListOf<List<DrawStroke>>() }
    var currentStroke by remember { mutableStateOf<DrawStroke?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedColor by remember { mutableLongStateOf(0L) }
    var brushWidth by remember { mutableStateOf(0.012f) }
    var eraser by remember { mutableStateOf(false) }
    var colorPicker by remember { mutableStateOf(false) }
    val haptics = LocalMimoHaptics.current

    fun commit(stroke: DrawStroke) {
        undoStack.add(strokes.toList())
        redoStack.clear()
        strokes.add(stroke)
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(strokes.toList())
        val previous = undoStack.removeAt(undoStack.lastIndex)
        strokes.clear()
        strokes.addAll(previous)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(strokes.toList())
        val next = redoStack.removeAt(redoStack.lastIndex)
        strokes.clear()
        strokes.addAll(next)
    }

    fun clear() {
        if (strokes.isEmpty()) return
        undoStack.add(strokes.toList())
        redoStack.clear()
        strokes.clear()
    }

    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = noteColor,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = contentColor.copy(alpha = 0.08f),
                        modifier = Modifier.size(44.dp),
                    ) {
                        IconButton(onClick = {
                            haptics.tap()
                            onDismiss()
                        }) {
                            Icon(
                                MimoIcons.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = contentColor,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.drawing_tools_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = contentColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            haptics.confirm()
                            onSave(Drawing(strokes.toList()))
                        },
                        shape = CircleShape,
                        color = contentColor.copy(alpha = 0.12f),
                        contentColor = contentColor,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(MimoIcons.Check, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .background(contentColor.copy(alpha = 0.03f), RoundedCornerShape(28.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(selectedColor, brushWidth, eraser, canvasSize) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures
                                    val norm = Offset(
                                        (offset.x / canvasSize.width).coerceIn(0f, 1f),
                                        (offset.y / canvasSize.height).coerceIn(0f, 1f),
                                    )
                                    currentStroke = DrawStroke(
                                        points = listOf(PointF(norm.x, norm.y)),
                                        color = selectedColor,
                                        width = brushWidth,
                                        eraser = eraser,
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val current = currentStroke ?: return@detectDragGestures
                                    val norm = Offset(
                                        (change.position.x / canvasSize.width).coerceIn(0f, 1f),
                                        (change.position.y / canvasSize.height).coerceIn(0f, 1f),
                                    )
                                    val points = current.points + PointF(norm.x, norm.y)
                                    currentStroke = current.copy(points = points)
                                },
                                onDragEnd = {
                                    currentStroke?.let { stroke ->
                                        if (stroke.points.isNotEmpty()) commit(stroke)
                                    }
                                    currentStroke = null
                                },
                                onDragCancel = { currentStroke = null },
                            )
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val step = 36.dp.toPx()
                        var x = step / 2
                        while (x < size.width) {
                            var y = step / 2
                            while (y < size.height) {
                                drawCircle(
                                    color = contentColor.copy(alpha = 0.055f),
                                    radius = 1.1.dp.toPx(),
                                    center = Offset(x, y),
                                )
                                y += step
                            }
                            x += step
                        }
                    }
                    val previewDrawing = Drawing(
                        strokes = strokes.toList() + listOfNotNull(currentStroke)
                    )
                    DrawingPreview(
                        drawing = previewDrawing,
                        backgroundColor = noteColor,
                        contentColor = contentColor,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (strokes.isEmpty() && currentStroke == null) {
                        Text(
                            stringResource(R.string.drawing_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.4f),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = contentColor.copy(alpha = 0.07f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val colors = listOf(
                                    0L to contentColor,
                                    (Color(0xFFE4572E).toArgb().toLong() and 0xFFFFFFFFL) to Color(0xFFE4572E),
                                    (Color(0xFFF2A93B).toArgb().toLong() and 0xFFFFFFFFL) to Color(0xFFF2A93B),
                                    (Color(0xFF3C8D5A).toArgb().toLong() and 0xFFFFFFFFL) to Color(0xFF3C8D5A),
                                    (Color(0xFF3D7EBF).toArgb().toLong() and 0xFFFFFFFFL) to Color(0xFF3D7EBF),
                                    (Color(0xFF7B5BA6).toArgb().toLong() and 0xFFFFFFFFL) to Color(0xFF7B5BA6),
                                )
                                colors.forEach { (stored, color) ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(color, CircleShape)
                                            .border(
                                                if (!eraser && selectedColor == stored) 2.5.dp else 1.dp,
                                                if (!eraser && selectedColor == stored) contentColor else contentColor.copy(alpha = 0.2f),
                                                CircleShape,
                                            )
                                            .clickable {
                                                haptics.tick()
                                                selectedColor = stored
                                                eraser = false
                                            },
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(contentColor.copy(alpha = 0.08f), CircleShape)
                                        .border(
                                            if (eraser) 2.dp else 1.dp,
                                            if (eraser) contentColor else contentColor.copy(alpha = 0.2f),
                                            CircleShape,
                                        )
                                        .clickable {
                                            haptics.toggle(!eraser)
                                            eraser = true
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        MimoIcons.AutoFixOff,
                                        contentDescription = stringResource(R.string.eraser),
                                        tint = contentColor,
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                listOf(0.006f to 6, 0.012f to 11, 0.026f to 18).forEach { (width, size) ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .border(
                                                if (brushWidth == width) 1.8.dp else 0.dp,
                                                contentColor.copy(alpha = 0.6f),
                                                CircleShape,
                                            )
                                            .clickable {
                                                haptics.tap()
                                                brushWidth = width
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Box(
                                            Modifier
                                                .size(size.dp)
                                                .background(contentColor, CircleShape)
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        haptics.tap()
                                        undo()
                                    },
                                    enabled = undoStack.isNotEmpty(),
                                ) {
                                    Icon(
                                        MimoIcons.Undo,
                                        contentDescription = stringResource(R.string.undo),
                                        tint = contentColor.copy(alpha = if (undoStack.isNotEmpty()) 0.8f else 0.3f),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptics.tap()
                                        redo()
                                    },
                                    enabled = redoStack.isNotEmpty(),
                                ) {
                                    Icon(
                                        MimoIcons.Redo,
                                        contentDescription = stringResource(R.string.redo),
                                        tint = contentColor.copy(alpha = if (redoStack.isNotEmpty()) 0.8f else 0.3f),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptics.reject()
                                        clear()
                                    },
                                    enabled = strokes.isNotEmpty(),
                                ) {
                                    Icon(
                                        MimoIcons.DeleteSweep,
                                        contentDescription = stringResource(R.string.clear_drawing),
                                        tint = contentColor.copy(alpha = if (strokes.isNotEmpty()) 0.8f else 0.3f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
