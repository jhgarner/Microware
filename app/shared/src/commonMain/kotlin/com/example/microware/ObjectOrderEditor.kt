package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

private val ObjectOrderCardHeight = 88.dp
private val ObjectOrderCardShape = RoundedCornerShape(8.dp)

@Composable
internal fun ObjectOrderEditor(
    world: World,
    handler: (WorldEvent) -> Unit,
    state: WorldEditorState,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val topFirstObjects = world.objects.asReversed()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = insets.leftDp(),
                top = insets.topDp(),
                end = insets.rightDp(),
            ),
    ) {
        GhostingLazyColumn(
            items = topFirstObjects,
            dragController = state.dragController,
            modifier = Modifier.fillMaxSize(),
            listModifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            belowList = {
                Spacer(
                    modifier = Modifier.height(
                        HandleSize + TrashGap + insets.bottomDp(),
                    ),
                )
            },
            onDrop = { droppedOffer, targetIndex ->
                val obj = droppedOffer.item
                handler { world -> world.reorderObjectFromTop(obj.id, targetIndex) }
                obj
            },
            ghostContent = { obj, itemModifier ->
                ObjectOrderCard(obj, world, itemModifier)
            },
            itemContent = { obj, itemModifier ->
                ObjectOrderCard(
                    obj = obj,
                    world = world,
                    modifier = itemModifier
                        .fillMaxWidth()
                        .height(ObjectOrderCardHeight),
                )
            },
            draggedItemAlpha = { 0f },
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            insertionSlotHeight = ObjectOrderCardHeight,
        )
    }
}

@Composable
private fun ObjectOrderCard(
    obj: Obj,
    world: World,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(ObjectOrderCardShape)
            .background(Palette.OffWhite.color)
            .border(2.dp, Palette.DarkGray.color, ObjectOrderCardShape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "Reorder object" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            world.logicTagFor(obj.id)?.let { tag ->
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    with(tag) { draw() }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.size(72.dp)) {
            drawSpriteFit(obj.sprite)
        }
    }
}

private fun DrawScope.drawSpriteFit(sprite: Sprite) {
    val scale = min(
        size.width / sprite.width,
        size.height / sprite.height,
    )
    val width = (sprite.width * scale).roundToInt().coerceAtLeast(1)
    val height = (sprite.height * scale).roundToInt().coerceAtLeast(1)
    drawImage(
        image = sprite.bitmap,
        dstOffset = IntOffset(
            ((size.width - width) / 2f).roundToInt(),
            ((size.height - height) / 2f).roundToInt(),
        ),
        dstSize = IntSize(width, height),
        filterQuality = FilterQuality.None,
    )
}

internal fun DrawScope.drawObjectOrderIcon() {
    val stroke = 2.dp.toPx()
    val layers = listOf(
        Palette.Cyan.color to .24f,
        Palette.Orange.color to .44f,
        Palette.Green.color to .64f,
    )
    layers.forEach { (color, top) ->
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * .2f, size.height * top),
            size = androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 2f),
        )
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(size.width * .2f, size.height * top),
            size = androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .16f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 2f),
            style = Stroke(stroke),
        )
    }
}
