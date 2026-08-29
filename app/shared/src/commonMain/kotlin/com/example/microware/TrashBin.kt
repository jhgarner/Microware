package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

val TrashGap = 8.dp

@Composable
fun TrashTarget(
    isActive: Boolean,
    onClick: (() -> Unit)? = null,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val foreground = if (isActive) {
        Palette.Red
    } else {
        Palette.Maroon
    }.color

    ButtonHandle(
        foreground = foreground,
        openEdge = HandleEdge.Left,
        onClick = onClick,
        onBoundsChanged = onBoundsChanged,
        modifier = modifier,
    ) { foreground ->
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 2.dp.toPx()
            val canLeft = size.width * 0.28f
            val canTop = size.height * 0.25f
            val canWidth = size.width * 0.44f
            val canHeight = size.height * 0.58f
            drawRoundRect(
                color = foreground,
                topLeft = Offset(canLeft, canTop),
                size = Size(canWidth, canHeight),
                cornerRadius = CornerRadius(strokeWidth, strokeWidth),
                style = Stroke(strokeWidth)
            )
            drawLine(
                color = foreground,
                start = Offset(size.width * 0.2f, size.height * 0.18f),
                end = Offset(size.width * 0.8f, size.height * 0.18f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = foreground,
                start = Offset(size.width * 0.43f, size.height * 0.08f),
                end = Offset(size.width * 0.57f, size.height * 0.08f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

data class Trash(val garbage: PersistentList<Obj>, val state: TrashState) {
    constructor() : this(persistentListOf<Obj>(), TrashState.Closed)

    fun discard(garbage: Obj): Trash =
        copy(garbage = this.garbage.adding(garbage.copy(id = nextId())))

    fun retrieve(garbage: Obj): Trash {
        val index = this.garbage.indexOfFirst { it.id == garbage.id }
        return if (index == -1) this else copy(garbage = this.garbage.removingAt(index))
    }

    fun nextId(): Int = (garbage.maxOfOrNull { it.id } ?: -1) + 1

    fun isEmpty(): Boolean = garbage.isEmpty()

    fun open(): Trash = copy(state = TrashState.Open)

    fun close(): Trash = copy(state = TrashState.Closed)

    fun hover(): Trash = copy(state = TrashState.Hovered)
}

sealed interface TrashState {
    object Open : TrashState
    object Closed : TrashState
    object Hovered : TrashState
}

@Composable
fun TrashControls(
    trash: MutableState<Trash>,
    isExpanded: Boolean,
    availableWidth: Float,
    onTrashClick: () -> Unit,
    modifier: Modifier = Modifier,
    offered: Offer?,
    onGiven: (Offer) -> Unit,
    onBoundsChanged: ((Rect) -> Unit)? = null,
) {
    var trash by trash
    val isDragging = offered != null
    val density = LocalDensity.current
    val handleWidth = with(density) { HandleSize.toPx() }
    val drawerOpenSlide = availableWidth - handleWidth
    val hiddenTrashSlide = -500f
    val visibleTrashSlide = -16f
    val trashSlideX = remember {
        Animatable(hiddenTrashSlide)
    }
    LaunchedEffect(trash.isEmpty(), drawerOpenSlide, isDragging, isExpanded) {
        trashSlideX.animateTo(
            targetValue = when {
                isExpanded -> drawerOpenSlide
                isDragging -> 0f
                trash.isEmpty() -> hiddenTrashSlide
                else -> visibleTrashSlide
            },
            animationSpec = tween(durationMillis = 180)
        )
    }
    TrashTarget(
        isActive = trash.state is TrashState.Hovered,
        onClick = onTrashClick,
        onBoundsChanged = onBoundsChanged,
        modifier = Modifier
            .offset { IntOffset(trashSlideX.value.roundToInt(), 0) }
                then modifier
            .dropTarget(
                offered = offered,
                onStart = { trash = trash.open() },
                onEnter = { trash = trash.hover() },
                onExit = { trash = trash.open() },
                onGiven = { trash = trash.discard(it.obj); onGiven(it) },
                onEnd = { trash = trash.close() })
    )
}
