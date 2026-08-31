package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ObjectTable(
    items: List<Obj>,
    settableOffer: MutableState<Offer?>,
    from: Any = Unit,
) {
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
                settableOffer = settableOffer,
                from = from
            )
        }
    }
}

@Composable
private fun ItemPreview(
    obj: Obj,
    settableOffer: MutableState<Offer?>,
    from: Any,
    zIndex: Float,
) {
    var wiggleCount by remember { mutableStateOf(0) }
    val wiggle = remember { Animatable(0f) }
    val wiggleDistance = with(LocalDensity.current) { 32.dp.toPx() }
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
            .dropSource(settableOffer, from, obj) {
                sequence {
                    expectTap()
                    wiggleCount += 1
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
