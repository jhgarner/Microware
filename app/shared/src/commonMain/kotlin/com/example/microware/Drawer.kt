package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlin.math.min
import kotlin.math.roundToInt

val HandleSize = 56.dp
val OverlayShadowElevation = 8.dp

enum class Side { Left, Right }

data class Placement(val side: Side, val offset: Float)

@Composable
fun Drawer(
    items: List<Obj>,
    placement: Placement,
    onItemDragStart: (Obj, Offset) -> Unit,
    onItemDragMove: (Offset) -> Unit,
    onItemDragEnd: (Offset) -> Unit,
    emptyText: String? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val handleWidth = with(density) { HandleSize.toPx() }
        val drawerWidth = (with(density) { maxWidth.toPx() } - handleWidth).coerceAtLeast(0f)
        val panelShape = RoundedCornerShape(0.dp)

        Box(
            modifier = Modifier
                .offset {
                    Offset(
                        placement.run { if (side == Side.Left) offset else offset - drawerWidth },
                        0f
                    ).round()
                }
                .requiredSize(
                    with(density) { drawerWidth.toDp() },
                    maxHeight
                )
                .shadow(OverlayShadowElevation, panelShape)
                .background(MaterialTheme.colorScheme.surface, panelShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, panelShape)
                .padding(12.dp)
        ) {
            if (items.isEmpty() && emptyText != null) {
                Text(emptyText)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items, key = { _, obj -> obj.id }) { index, obj ->
                        ItemPreview(
                            obj = obj,
                            zIndex = -index.toFloat(),
                            onDragStart = { pointerPosition ->
                                onItemDragStart(obj, pointerPosition)
                            },
                            onDragMove = onItemDragMove,
                            onDragEnd = { pointerPosition ->
                                onItemDragEnd(pointerPosition)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemPreview(
    obj: Obj,
    zIndex: Float,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit
) {
    var itemRootPosition by remember { mutableStateOf(Offset.Zero) }
    var wiggleCount by remember { mutableStateOf(0) }
    val wiggle = remember { Animatable(0f) }
    val wiggleDistance = with(LocalDensity.current) { 32.dp.toPx() }
    val currentOnDragStart = rememberUpdatedState(onDragStart)
    val currentOnDragMove = rememberUpdatedState(onDragMove)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    LaunchedEffect(wiggleCount) {
        if (wiggleCount == 0) return@LaunchedEffect
        wiggle.snapTo(0f)
        wiggle.animateTo(wiggleDistance, tween(durationMillis = 70))
        wiggle.animateTo(0f, tween(durationMillis = 70))
        wiggle.animateTo(wiggleDistance / 2f, tween(durationMillis = 70))
        wiggle.animateTo(0f, tween(durationMillis = 70))
    }
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .zIndex(zIndex)
            .graphicsLayer {
                translationX = wiggle.value
            }
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(4.dp)
            .onGloballyPositioned { coordinates ->
                itemRootPosition = coordinates.localToRoot(Offset.Zero)
            }
            .pointerInput(obj.id) {
                racing {
                    sequence {
                        expectTap()
                        wiggleCount += 1
                    }
                    sequence {
                        var started = false
                        val pointer = followHorizontalDrag { change ->
                            val pointerRootPosition = itemRootPosition + change.position
                            if (!started) {
                                currentOnDragStart.value(pointerRootPosition)
                                started = true
                            }
                            currentOnDragMove.value(pointerRootPosition)
                        }
                        if (started) {
                            currentOnDragEnd.value(itemRootPosition + pointer.position)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = min(
                size.width / obj.sprite.width,
                size.height / obj.sprite.height
            )
            val width = (obj.sprite.width * scale).roundToInt().coerceAtLeast(1)
            val height = (obj.sprite.height * scale).roundToInt().coerceAtLeast(1)
            drawImage(
                image = obj.sprite.bitmap,
                dstOffset = IntOffset(
                    ((size.width - width) / 2f).roundToInt(),
                    ((size.height - height) / 2f).roundToInt()
                ),
                dstSize = IntSize(width, height),
                filterQuality = FilterQuality.None
            )
        }
    }
}
