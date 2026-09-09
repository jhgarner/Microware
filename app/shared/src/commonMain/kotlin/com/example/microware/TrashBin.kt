package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

val TrashGap = 8.dp

data class Trash(
    val garbage: PersistentList<Obj>,
    val logicGarbage: PersistentList<Logic>,
    val state: TrashState,
) {
    constructor() : this(persistentListOf(), persistentListOf(), TrashState.Closed)

    fun discard(garbage: Obj): Trash =
        copy(garbage = this.garbage.adding(garbage.copy(id = nextId())))

    fun discard(logic: Logic): Trash =
        copy(
            logicGarbage = logicGarbage.adding(
                logic.copy(id = (logicGarbage.maxOfOrNull { it.id } ?: 0) + 1)
            )
        )

    fun nextId(): Int = (garbage.maxOfOrNull { it.id } ?: -1) + 1

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
fun TrashControls(foreground: Color) {
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
