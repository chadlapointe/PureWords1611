package com.purewords1611.android.study.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.purewords1611.android.study.data.DrawingPath

@Composable
fun MarginaliaDrawingView(
    initialPaths: List<DrawingPath>,
    onPathsChanged: (List<DrawingPath>) -> Unit,
    modifier: Modifier = Modifier,
    currentColor: Color = Color(0xFF722F37), // Historical Red/Wine
    currentStrokeWidth: Float = 4f,
) {
    val paths = remember { mutableStateListOf<DrawingPath>().apply { addAll(initialPaths) } }
    val currentPoints = remember { mutableStateListOf<Offset>() }

    // Synchronize with initialPaths if they change (e.g. chapter change)
    LaunchedEffect(initialPaths) {
        paths.clear()
        paths.addAll(initialPaths)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints.add(offset)
                    },
                    onDrag = { change, _ ->
                        currentPoints.add(change.position)
                    },
                    onDragEnd = {
                        if (currentPoints.isNotEmpty()) {
                            val newPath = DrawingPath(
                                points = currentPoints.map { it.x to it.y },
                                color = currentColor.toArgb(),
                                strokeWidth = currentStrokeWidth
                            )
                            paths.add(newPath)
                            currentPoints.clear()
                            onPathsChanged(paths.toList())
                        }
                    }
                )
            }
    ) {
        // Draw existing paths
        paths.forEach { drawingPath ->
            val path = Path().apply {
                drawingPath.points.forEachIndexed { index, (x, y) ->
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = Color(drawingPath.color),
                style = Stroke(
                    width = drawingPath.strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        // Draw current path
        if (currentPoints.isNotEmpty()) {
            val path = Path().apply {
                currentPoints.forEachIndexed { index, offset ->
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
            }
            drawPath(
                path = path,
                color = currentColor,
                style = Stroke(
                    width = currentStrokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

private fun Color.toArgb(): Int {
    return ((alpha * 255.0f) + 0.5f).toInt() shl 24 or
            ((red * 255.0f) + 0.5f).toInt() shl 16 or
            ((green * 255.0f) + 0.5f).toInt() shl 8 or
            ((blue * 255.0f) + 0.5f).toInt()
}
