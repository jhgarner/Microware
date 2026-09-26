package com.example.microware.editor.world

import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.ui.components.IntegerSliderControl
import com.example.microware.ui.components.IntegerSliderDimension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
internal fun ObjectPositionEditor(
    world: World,
    obj: Obj,
    onPositionChange: (IntOffset) -> Unit,
) {
    val maxX = (world.background.width - obj.sprite.width).coerceAtLeast(0)
    val maxY = (world.background.height - obj.sprite.height).coerceAtLeast(0)
    var position by remember(obj.id) {
        mutableStateOf(obj.positionWithin(maxX, maxY))
    }

    LaunchedEffect(obj.id, obj.offset, maxX, maxY) {
        position = obj.positionWithin(maxX, maxY)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        IntegerSliderControl(
            dimension = IntegerSliderDimension.X,
            value = position.x,
            minValue = 0,
            maxValue = maxX,
            onValueChange = { value ->
                position = position.copy(x = checkNotNull(value))
                onPositionChange(position)
            },
        )
        IntegerSliderControl(
            dimension = IntegerSliderDimension.Y,
            value = position.y,
            minValue = 0,
            maxValue = maxY,
            onValueChange = { value ->
                position = position.copy(y = checkNotNull(value))
                onPositionChange(position)
            },
        )
        ObjectPositionPreviewCanvas(
            world = world,
            selectedObject = obj,
            position = position,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

private fun Obj.positionWithin(maxX: Int, maxY: Int) = IntOffset(
    x = offset.x.roundToInt().coerceIn(0, maxX),
    y = offset.y.roundToInt().coerceIn(0, maxY),
)
