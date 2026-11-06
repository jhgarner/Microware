package com.example.microware.editor.sprite

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.ui.components.IconButton
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun SpriteHistoryControls(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    Row {
        IconButton(
            description = "Undo",
            modifier = Modifier.size(56.dp),
            enabled = canUndo,
            onClick = onUndo,
        ) { progress -> drawHistoryArrow(redo = false, progress = progress) }
        IconButton(
            description = "Redo",
            modifier = Modifier.size(56.dp),
            enabled = canRedo,
            onClick = onRedo,
        ) { progress -> drawHistoryArrow(redo = true, progress = progress) }
    }
}

private fun DrawScope.drawHistoryArrow(redo: Boolean, progress: Float) {
    scale(if (redo) -1f else 1f, 1f) {
        translate(left = -sin(progress * PI).toFloat() * size.width * .1f) {
            val curve = Path().apply {
                moveTo(size.width * .82f, size.height * .78f)
                cubicTo(
                    size.width * 1.02f, size.height * .22f,
                    size.width * .48f, size.height * .16f,
                    size.width * .18f, size.height * .4f,
                )
            }
            val strokeWidth = 3.dp.toPx()
            drawPath(
                curve,
                Palette.Navy.color,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            val head = Path().apply {
                moveTo(size.width * .2f, size.height * .12f)
                lineTo(size.width * .18f, size.height * .4f)
                lineTo(size.width * .47f, size.height * .43f)
            }
            drawPath(
                head,
                Palette.Navy.color,
                style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Preview
@Composable
fun SpriteHistoryControlsPreview() {
    SpriteHistoryControls(canUndo = true, canRedo = false, onUndo = {}, onRedo = {})
}
