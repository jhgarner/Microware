package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp

@Composable
fun ButtonHandle(
    foreground: Color,
    openSide: Side? = null,
    onClick: (() -> Unit)? = null,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val shape = handleShape(openSide)
    var targetModifier = Modifier
        .size(HandleSize)
        .then(modifier)
        .shadow(OverlayShadowElevation, shape)
        .background(Palette.OffWhite.color, shape)

    if (onClick != null) {
        targetModifier = targetModifier.clickable(onClick = onClick)
    }
    if (onBoundsChanged != null) {
        targetModifier = targetModifier.onGloballyPositioned {
            onBoundsChanged(it.boundsInParent())
        }
    }

    Box(modifier = targetModifier) {
        HandleBorder(openSide = openSide, color = foreground)
        content(foreground)
    }
}

private fun handleShape(openSide: Side?) = when (openSide) {
    Side.Left -> RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 8.dp,
        bottomEnd = 8.dp,
        bottomStart = 0.dp
    )

    Side.Right -> RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 0.dp,
        bottomEnd = 0.dp,
        bottomStart = 8.dp
    )

    null -> RoundedCornerShape(8.dp)
}

@Composable
private fun HandleBorder(openSide: Side?, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx()
        val inset = strokeWidth / 2f
        val right = size.width - inset
        val bottom = size.height - inset
        val radius = 8.dp.toPx().coerceAtMost(size.height / 2f)

        if (openSide == null) {
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(strokeWidth)
            )
        } else {
            val borderPath = Path().apply {
                if (openSide == Side.Left) {
                    moveTo(0f, inset)
                    lineTo(right - radius, inset)
                    quadraticTo(right, inset, right, inset + radius)
                    lineTo(right, bottom - radius)
                    quadraticTo(right, bottom, right - radius, bottom)
                    lineTo(0f, bottom)
                } else {
                    moveTo(right, inset)
                    lineTo(inset + radius, inset)
                    quadraticTo(inset, inset, inset, inset + radius)
                    lineTo(inset, bottom - radius)
                    quadraticTo(inset, bottom, inset + radius, bottom)
                    lineTo(right, bottom)
                }
            }
            drawPath(
                path = borderPath,
                color = color,
                style = Stroke(strokeWidth)
            )
        }
    }
}
