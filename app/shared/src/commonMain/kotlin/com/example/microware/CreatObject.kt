package com.example.microware

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun BoxWithConstraintsScope.CreateTarget(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    isEditMode: Boolean,
    isActive: Boolean,
    onBoundsChanged: ((Rect) -> Unit),
) {
    val morphProgress by animateFloatAsState(
        targetValue = if (isEditMode) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "createEditMorph"
    )
    val targetColor = when {
        isEditMode && isActive -> Palette.Cyan.color
        else -> Palette.Navy.color
    }
    val foreground by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 180),
        label = "createEditColor"
    )

    ButtonHandle(
        foreground = foreground,
        openEdge = HandleEdge.Bottom,
        onClick = onClick,
        isExpanded = isExpanded,
        isDragging = isEditMode,
        onBoundsChanged = onBoundsChanged,
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 2.dp.toPx()
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)
            val halfLength = size.minDimension * 0.34f

            val plusHStart = Offset(center.x - halfLength, center.y)
            val plusHEnd = Offset(center.x + halfLength, center.y)
            val plusVStart = Offset(center.x, center.y - halfLength)
            val plusVEnd = Offset(center.x, center.y + halfLength)

            val tip = Offset(w * 0.22f, h * 0.84f)
            val eraser = Offset(w * 0.78f, h * 0.16f)
            val dirRaw = eraser - tip
            val dirLen = dirRaw.getDistance()
            val dir = if (dirLen > 0f) dirRaw / dirLen else Offset(0.7f, -0.7f)
            val perp = Offset(-dir.y, dir.x)
            val halfW = size.minDimension * 0.10f
            val tipLen = size.minDimension * 0.22f
            val tipBase = tip + dir * tipLen
            val p1 = eraser + perp * halfW
            val p2 = eraser - perp * halfW
            val p3 = tipBase - perp * halfW
            val p4 = tipBase + perp * halfW

            fun lerp(a: Offset, b: Offset, t: Float) = Offset(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t
            )

            val t = morphProgress
            val hStart = lerp(plusHStart, tip, t)
            val hEnd = lerp(plusHEnd, p3, t)
            val vStart = lerp(plusVStart, tip, t)
            val vEnd = lerp(plusVEnd, p4, t)

            drawLine(
                color = foreground,
                start = hStart,
                end = hEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = foreground,
                start = vStart,
                end = vEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            if (t > 0.01f) {
                val tipAlpha = t
                val tipColor = foreground.copy(alpha = tipAlpha)
                drawLine(
                    color = tipColor,
                    start = p2,
                    end = p3,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tipColor,
                    start = p1,
                    end = p4,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tipColor,
                    start = p1,
                    end = p2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tipColor,
                    start = p3,
                    end = p4,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
