package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

val TrashSize = 56.dp
val TrashGap = 8.dp

@Composable
fun TrashTarget(isActive: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 8.dp,
        bottomEnd = 8.dp,
        bottomStart = 0.dp
    )
    val background = if (isActive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (isActive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(TrashSize)
            .background(background, shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val inset = strokeWidth / 2f
            val right = size.width - inset
            val bottom = size.height - inset
            val radius = 8.dp.toPx().coerceAtMost(size.height / 2f)
            val borderPath = Path().apply {
                moveTo(0f, inset)
                lineTo(right - radius, inset)
                quadraticTo(right, inset, right, inset + radius)
                lineTo(right, bottom - radius)
                quadraticTo(right, bottom, right - radius, bottom)
                lineTo(0f, bottom)
            }
            drawPath(
                path = borderPath,
                color = foreground,
                style = Stroke(strokeWidth)
            )
        }
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

    fun discard(garbage: Obj): Trash = copy(garbage = this.garbage.adding(garbage))

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
    trash: Trash,
    onCreate: () -> Unit,
    onTrashBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TrashGap),
        horizontalArrangement = Arrangement.spacedBy(TrashGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hiddenTrashSlide = -500f
        val visibleTrashSlide = -16f
        val trashSlideX = remember {
            Animatable(hiddenTrashSlide)
        }
        val (lastTrashState, setLastTrashState) = remember { mutableStateOf(trash.state) }
        LaunchedEffect(trash.state) {
            when (trash.state) {
                is TrashState.Open -> {
                    if (lastTrashState is TrashState.Closed) {
                        val start = if (trash.isEmpty()) hiddenTrashSlide else visibleTrashSlide
                        trashSlideX.snapTo(start)
                        trashSlideX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 180)
                        )
                    }
                }

                is TrashState.Closed -> if (trash.isEmpty()) {
                    trashSlideX.animateTo(
                        targetValue = hiddenTrashSlide,
                        animationSpec = tween(durationMillis = 180)
                    )
                } else {
                    trashSlideX.animateTo(
                        targetValue = visibleTrashSlide,
                        animationSpec = tween(durationMillis = 180)
                    )
                }

                is TrashState.Hovered -> {}
            }
        }
        setLastTrashState(trash.state)
        TrashTarget(
            isActive = trash.state is TrashState.Hovered,
            modifier = Modifier
                .offset { IntOffset(trashSlideX.value.roundToInt(), 0) }
                .onGloballyPositioned { coordinates ->
                    onTrashBoundsChanged(coordinates.boundsInRoot())
                }
        )
        Spacer(
            modifier = Modifier.width(
                16.dp
            )
        )
        Button(onClick = onCreate) {
            Text("Create")
        }
    }
}
