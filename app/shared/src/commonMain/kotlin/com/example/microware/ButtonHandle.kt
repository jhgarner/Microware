package com.example.microware

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class HandleEdge(val innerAlignment: Alignment) {
    Left(innerAlignment = Alignment.CenterEnd),
    Right(innerAlignment = Alignment.BottomStart),
    Bottom(innerAlignment = Alignment.TopCenter),
}

@Composable
private fun BoxScope.restingModsForEdge(edge: HandleEdge): Modifier = when (edge) {
    HandleEdge.Left -> Modifier
        .padding(bottom = insets.bottomDp())
        .size(DpSize(HandleSize + insets.leftDp(), HandleSize))
        .align(Alignment.BottomStart)

    HandleEdge.Right -> Modifier
        .padding(bottom = insets.bottomDp())
        .size(DpSize(HandleSize + insets.rightDp(), HandleSize))
        .align(Alignment.BottomEnd)

    HandleEdge.Bottom -> Modifier
        .size(DpSize(HandleSize, HandleSize + insets.bottomDp()))
        .align(Alignment.BottomCenter)
}

@Composable
fun BoxWithConstraintsScope.ButtonHandle(
    foreground: Color,
    openEdge: HandleEdge,
    isDragging: Boolean,
    isExpanded: Boolean,
    hiddenSlide: Int = 16,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = handleShape(openEdge)
    val insets = LocalInsets.current
    val openSlide = when (openEdge) {
        HandleEdge.Left ->
            IntOffset(
                (maxWidth.toPx() - HandleSize.toPx() - insets.right() - insets.left()).toInt(),
                0
            )

        HandleEdge.Right ->
            IntOffset(
                (-maxWidth.toPx() + HandleSize.toPx() + insets.left() + insets.right()).toInt(),
                0
            )

        HandleEdge.Bottom ->
            IntOffset(
                0,
                (-maxHeight.toPx() + HandleSize.toPx() + insets.top() + insets.bottom()).toInt()
            )
    }
    val hidingOffset = when (openEdge) {
        HandleEdge.Left ->
            IntOffset(-hiddenSlide, 0)

        HandleEdge.Right ->
            IntOffset(hiddenSlide, 0)

        HandleEdge.Bottom -> IntOffset(0, hiddenSlide)
    }
    val slideX = remember { Animatable(hidingOffset.x.toFloat()) }
    val slideY = remember { Animatable(hidingOffset.y.toFloat()) }
    LaunchedEffect(isDragging, isExpanded, hidingOffset, openSlide) {
        coroutineScope {
            launch {
                slideX.animateTo(
                    targetValue = when {
                        isExpanded -> openSlide.x.toFloat()
                        isDragging -> 0f
                        else -> hidingOffset.x.toFloat()
                    },
                    animationSpec = tween(durationMillis = 180)
                )
            }
            launch {
                slideY.animateTo(
                    targetValue = when {
                        isExpanded -> openSlide.y.toFloat()
                        isDragging -> 0f
                        else -> hidingOffset.y.toFloat()
                    },
                    animationSpec = tween(durationMillis = 180)
                )
            }
        }
    }
    var targetModifier = Modifier
        .zIndex(0.99f)
        .then(restingModsForEdge(openEdge))
        .offset { IntOffset(slideX.value.roundToInt(), slideY.value.roundToInt()) }
        .then(modifier)
        .shadow(OverlayShadowElevation, shape)
        .background(Palette.OffWhite.color, shape)

    if (onClick != null) {
        targetModifier = targetModifier.clickable(onClick = onClick)
    }

    Box(modifier = targetModifier) {
        HandleBorder(openEdge = openEdge, color = foreground)
        Box(modifier = Modifier.size(HandleSize).align(openEdge.innerAlignment)) {
            Crossfade(
                targetState = isExpanded,
                animationSpec = tween(durationMillis = 180),
                label = "handle icon"
            ) { expanded ->
                if (expanded) {
                    CloseChevron(edge = openEdge, color = foreground)
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
private fun CloseChevron(edge: HandleEdge, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val strokeWidth = 2.dp.toPx()
        val top = Offset(size.width * 0.5f, size.height * 0.15f)
        val right = Offset(size.width * 0.85f, size.height * 0.5f)
        val bottom = Offset(size.width * 0.5f, size.height * 0.85f)
        val left = Offset(size.width * 0.15f, size.height * 0.5f)

        when (edge) {
            HandleEdge.Left -> {
                drawLine(color, top, left, strokeWidth, StrokeCap.Round)
                drawLine(color, left, bottom, strokeWidth, StrokeCap.Round)
            }

            HandleEdge.Right -> {
                drawLine(color, top, right, strokeWidth, StrokeCap.Round)
                drawLine(color, right, bottom, strokeWidth, StrokeCap.Round)
            }

            HandleEdge.Bottom -> {
                drawLine(color, left, bottom, strokeWidth, StrokeCap.Round)
                drawLine(color, bottom, right, strokeWidth, StrokeCap.Round)
            }
        }
    }
}

private fun handleShape(openEdge: HandleEdge?) = when (openEdge) {
    HandleEdge.Left -> RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 8.dp,
        bottomEnd = 8.dp,
        bottomStart = 0.dp
    )

    HandleEdge.Right -> RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 0.dp,
        bottomEnd = 0.dp,
        bottomStart = 8.dp
    )

    HandleEdge.Bottom -> RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 8.dp,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )

    null -> RoundedCornerShape(8.dp)
}

@Composable
private fun HandleBorder(openEdge: HandleEdge?, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx()
        val inset = strokeWidth / 2f
        val right = size.width - inset
        val bottom = size.height - inset
        val radius = 8.dp.toPx().coerceAtMost(size.height / 2f)

        if (openEdge == null) {
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(strokeWidth)
            )
        } else {
            val borderPath = Path().apply {
                when (openEdge) {
                    HandleEdge.Left -> {
                        moveTo(0f, inset)
                        lineTo(right - radius, inset)
                        quadraticTo(right, inset, right, inset + radius)
                        lineTo(right, bottom - radius)
                        quadraticTo(right, bottom, right - radius, bottom)
                        lineTo(0f, bottom)
                    }

                    HandleEdge.Right -> {
                        moveTo(right, inset)
                        lineTo(inset + radius, inset)
                        quadraticTo(inset, inset, inset, inset + radius)
                        lineTo(inset, bottom - radius)
                        quadraticTo(inset, bottom, inset + radius, bottom)
                        lineTo(right, bottom)
                    }

                    HandleEdge.Bottom -> {
                        moveTo(0f, bottom)
                        lineTo(0f, inset + radius)
                        quadraticTo(0f, inset, inset + radius, inset)
                        lineTo(right - radius, inset)
                        quadraticTo(right, inset, right, inset + radius)
                        lineTo(right, bottom)
                    }
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
