package com.example.microware

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.roundToInt

@Immutable
enum class HandleEdge(val innerAlignment: Alignment) {
    Left(innerAlignment = Alignment.CenterEnd),
    Right(innerAlignment = Alignment.BottomStart),
    Bottom(innerAlignment = Alignment.TopCenter),
}

enum class HandlePresentation {
    EdgeAttached,
    FloatingAction,
}

internal const val HandleMorphDurationMillis = 180

enum class HandleType(
    val foreground: Palette,
    val highlight: Palette,
    val edge: HandleEdge,
    val content: @Composable (Color) -> Unit,
) {
    Trash(
        foreground = Palette.Maroon,
        highlight = Palette.Red,
        edge = HandleEdge.Left,
        content = ::TrashControls
    ),
    Create(
        foreground = Palette.Navy,
        highlight = Palette.Cyan,
        edge = HandleEdge.Bottom,
        content = { CreateTarget(it, false) },
    ),
    Edit(
        foreground = Palette.Navy,
        highlight = Palette.Cyan,
        edge = HandleEdge.Bottom,
        content = { CreateTarget(it, true) },
    ),
    Library(
        foreground = Palette.Forest,
        highlight = Palette.Green,
        edge = HandleEdge.Right,
        content = ::LibraryTarget,
    ),
}

// Rect bounds are relative to the edge alignment, so they remain reusable across parent pages.
private fun restingRectForEdge(
    edge: HandleEdge,
    floatingAction: Boolean,
    insets: Insets,
    handleSize: Float,
): Rect {
    val size = when (edge) {
        HandleEdge.Left -> Size(handleSize + insets.left(), handleSize)
        HandleEdge.Right -> Size(handleSize + insets.right(), handleSize)
        HandleEdge.Bottom if floatingAction -> Size(handleSize, handleSize)
        HandleEdge.Bottom -> Size(handleSize, handleSize + insets.bottom())
    }
    val topOffset = when {
        edge != HandleEdge.Bottom -> -insets.bottom().toFloat()
        floatingAction -> -insets.bottom().toFloat()
        else -> 0f
    }
    return Rect(Offset(0f, topOffset), size)
}

private fun HandleEdge.alignment() = when (this) {
    HandleEdge.Left -> Alignment.BottomStart
    HandleEdge.Right -> Alignment.BottomEnd
    HandleEdge.Bottom -> Alignment.BottomCenter
}

private fun fullyHiddenRect(rect: Rect, edge: HandleEdge) = when (edge) {
    HandleEdge.Left -> rect.translate(-rect.width, 0f)
    HandleEdge.Right -> rect.translate(rect.width, 0f)
    HandleEdge.Bottom -> Rect(0f, rect.height, rect.width, rect.height * 2f)
}

sealed interface DrawerButtonState {
    val onClick: (() -> Unit)? get() = null

    data class Waiting(override val onClick: () -> Unit) : DrawerButtonState
    data object Dragging : DrawerButtonState
    data class Expanded(override val onClick: () -> Unit) : DrawerButtonState
}

val morphProgression = mutableMapOf<HandleEdge, Animatable<Float, AnimationVector1D>>()
val rectProgression = mutableMapOf<HandleEdge, Animatable<Rect, AnimationVector4D>>()

@Composable
fun BoxWithConstraintsScope.ButtonHandle(
    type: HandleType,
    highlight: Boolean = false,
    state: DrawerButtonState? = null,
    hiddenSlide: Int = 16,
    modifier: Modifier = Modifier,
    presentation: HandlePresentation = HandlePresentation.EdgeAttached,
) {
    val isFloatingAction = presentation == HandlePresentation.FloatingAction
    val foreground = if (highlight) type.highlight else type.foreground
    val insets = LocalInsets.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val handleSizePx = with(density) { HandleSize.toPx() }
    val restingRect = restingRectForEdge(
        type.edge,
        isFloatingAction,
        insets,
        handleSizePx,
    )
    val openSlide = when (type.edge) {
        HandleEdge.Left -> IntOffset(
            (maxWidth.toPx() - HandleSize.toPx() - insets.right() - insets.left()).toInt(),
            0,
        )

        HandleEdge.Right -> IntOffset(
            (-maxWidth.toPx() + HandleSize.toPx() + insets.left() + insets.right()).toInt(),
            0,
        )

        HandleEdge.Bottom -> IntOffset(
            0,
            (-maxHeight.toPx() + insets.top() + restingRect.height).toInt(),
        )
    }
    val hidingOffset = when (type.edge) {
        HandleEdge.Left -> IntOffset(-hiddenSlide, 0)
        HandleEdge.Right -> IntOffset(hiddenSlide, 0)
        HandleEdge.Bottom -> IntOffset(0, hiddenSlide)
    }
    val targetRect = when (state) {
        null -> fullyHiddenRect(restingRect, type.edge)
        is DrawerButtonState.Expanded -> restingRect.translate(openSlide.toOffset())
        DrawerButtonState.Dragging -> restingRect
        is DrawerButtonState.Waiting -> restingRect.translate(hidingOffset.toOffset())
    }
    val rect = rectProgression.getOrPut(type.edge) {
        Animatable(fullyHiddenRect(restingRect, type.edge), Rect.VectorConverter)
    }
    LaunchedEffect(targetRect) {
        rect.animateTo(targetRect, tween(durationMillis = HandleMorphDurationMillis))
    }
    val animatedRect = rect.value
    val animatedSize = with(density) {
        DpSize(animatedRect.width.toDp(), animatedRect.height.toDp())
    }
    val morph = remember(type.edge, animatedSize, density, layoutDirection) {
        val cornerRadius = with(density) { 8.dp.toPx() }
        Morph(
            start = handlePolygon(
                animatedRect.width,
                animatedRect.height,
                cornerRadius,
                openEdge = null,
                layoutDirection = layoutDirection,
            ),
            end = handlePolygon(
                animatedRect.width,
                animatedRect.height,
                cornerRadius,
                type.edge,
                layoutDirection,
            ),
        )
    }
    val morphProgress = morphProgression.getOrPut(type.edge) {
        Animatable(0f)
    }
    LaunchedEffect(isFloatingAction) {
        morphProgress.animateTo(
            if (isFloatingAction) 0f else 1f,
            tween(durationMillis = HandleMorphDurationMillis),
        )
    }
    val shape = MorphHandleShape(morph, morphProgress.value)
    var targetModifier = Modifier
        .zIndex(0.99f)
        .size(animatedSize)
        .align(type.edge.alignment())
        .offset {
            IntOffset(animatedRect.left.roundToInt(), animatedRect.top.roundToInt())
        }
        .then(modifier)
        .shadow(OverlayShadowElevation, shape)
        .background(Palette.OffWhite.color, shape)

    state?.onClick?.let {
        targetModifier = targetModifier.clickable(onClick = it)
    }

    Box(modifier = targetModifier) {
        HandleBorder(
            openEdge = type.edge,
            color = foreground.color,
            morphProgress = morphProgress.value,
        )
        Box(modifier = Modifier.size(HandleSize).align(type.edge.innerAlignment)) {
            Crossfade(
                targetState = state is DrawerButtonState.Expanded,
                animationSpec = tween(durationMillis = 180),
                label = "handle icon"
            ) { expanded ->
                if (expanded) {
                    CloseChevron(edge = type.edge, color = foreground.color)
                } else {
                    type.content(foreground.color)
                }
            }
        }
    }
}

private fun handlePolygon(
    width: Float,
    height: Float,
    cornerRadius: Float,
    openEdge: HandleEdge?,
    layoutDirection: LayoutDirection,
) = RoundedPolygon(
    vertices = floatArrayOf(
        0f, 0f,
        width, 0f,
        width, height,
        0f, height,
    ),
    perVertexRounding = when (openEdge) {
        null -> List(4) { CornerRounding(cornerRadius) }
        HandleEdge.Left -> if (layoutDirection == LayoutDirection.Ltr) {
            listOf(
                CornerRounding(0f),
                CornerRounding(cornerRadius),
                CornerRounding(cornerRadius),
                CornerRounding(0f),
            )
        } else {
            listOf(
                CornerRounding(cornerRadius),
                CornerRounding(0f),
                CornerRounding(0f),
                CornerRounding(cornerRadius),
            )
        }

        HandleEdge.Right -> if (layoutDirection == LayoutDirection.Ltr) {
            listOf(
                CornerRounding(cornerRadius),
                CornerRounding(0f),
                CornerRounding(0f),
                CornerRounding(cornerRadius),
            )
        } else {
            listOf(
                CornerRounding(0f),
                CornerRounding(cornerRadius),
                CornerRounding(cornerRadius),
                CornerRounding(0f),
            )
        }

        HandleEdge.Bottom -> listOf(
            CornerRounding(cornerRadius),
            CornerRounding(cornerRadius),
            CornerRounding(0f),
            CornerRounding(0f),
        )
    },
)

private class MorphHandleShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cubics = morph.asCubics(progress)
        val first = cubics.first()
        val path = Path().apply {
            moveTo(first.anchor0X, first.anchor0Y)
            cubics.forEach { cubic ->
                cubicTo(
                    cubic.control0X,
                    cubic.control0Y,
                    cubic.control1X,
                    cubic.control1Y,
                    cubic.anchor1X,
                    cubic.anchor1Y,
                )
            }
            close()
        }
        return Outline.Generic(path)
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

@Composable
private fun HandleBorder(
    openEdge: HandleEdge,
    color: Color,
    morphProgress: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 2.dp.toPx()
        val inset = strokeWidth / 2f
        val right = size.width - inset
        val bottom = size.height - inset
        val radius = 8.dp.toPx().coerceAtMost(size.height / 2f)

        drawRoundRect(
            color = color.copy(alpha = color.alpha * (1f - morphProgress)),
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(strokeWidth)
        )

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
            color = color.copy(alpha = color.alpha * morphProgress),
            style = Stroke(strokeWidth)
        )
    }
}
