package com.example.microware.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.layout.insets
import com.example.microware.util.toDp
import com.example.microware.util.toPx

const val BannerWidth = 128
const val BannerHeight = 32

data class Banner(val sprite: Sprite) {
    init {
        require(sprite.width == BannerWidth && sprite.height == BannerHeight)
    }
}

@Composable
fun DrawBanner(banner: Banner) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = insets.leftDp(),
                end = insets.rightDp(),
                top = insets.topDp() + 32.dp,
            ),
    ) {
        val scale = minOf(
            (maxWidth.toPx() / BannerWidth).toInt(),
            (maxHeight.toPx() / BannerHeight).toInt(),
        )
        val canvasSize = IntSize(BannerWidth * scale, BannerHeight * scale)
        val canvasWidth = canvasSize.width.toDp()
        val canvasHeight = canvasSize.height.toDp()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.requiredSize(canvasWidth, canvasHeight)) {
                drawImage(
                    image = banner.sprite.bitmap,
                    dstSize = canvasSize,
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }
}
