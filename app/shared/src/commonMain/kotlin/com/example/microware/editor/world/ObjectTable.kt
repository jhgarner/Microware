package com.example.microware.editor.world

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.microware.game.model.Obj
import com.example.microware.ui.components.Grid
import com.example.microware.ui.gestures.DragSource
import com.example.microware.ui.gestures.DragTarget
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ObjectTable(
    items: List<Obj>,
    dragSource: DragSource<Obj>,
    dragTarget: DragTarget<Obj>,
    sourceId: Any,
) {
    Grid(
        items = items,
        itemSize = DpSize(64.dp, 64.dp),
        dragSource = dragSource,
        dragTarget = dragTarget,
        sourceId = sourceId,
        onMove = { _, _ -> }
    ) {
        ItemPreview(obj = it)
    }
}

@Composable
private fun ItemPreview(obj: Obj) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(4.dp)
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
