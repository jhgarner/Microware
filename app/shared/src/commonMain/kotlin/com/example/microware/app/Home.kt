package com.example.microware.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.microware.game.sprite.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.ChevronDirection
import com.example.microware.ui.icons.drawCommunityHomeIcon
import com.example.microware.ui.icons.drawCreateIcon
import com.example.microware.ui.icons.drawChevron
import com.example.microware.ui.icons.drawHomePlayIcon
import com.example.microware.ui.layout.insets
import com.example.microware.util.toDp
import com.example.microware.util.toPx

private const val BannerWidth = 128
private const val BannerHeight = 32
private val BannerButtonWidth = 36.dp

enum class HomeNavigation { Editor }

val memberBanners: List<Sprite> = listOf(
    Sprite.pattern(128, 32) {
        when (it % 3) {
            0 -> Palette.Green
            1 -> Palette.Forest
            2 -> Palette.Cyan
            else -> Palette.Tan
        }
    },
    Sprite.solid(128, 32, Palette.Forest),
)

@Composable
fun Home(navigate: (HomeNavigation) -> Unit) {
    Banners()

    Column(
        modifier = Modifier.padding(
            start = insets.leftDp(),
            end = insets.rightDp(),
        )
            .fillMaxSize()
            .aspectRatio(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val loop = rememberInfiniteTransition()
        val t =
            loop.animateFloat(
                0f,
                3f,
                infiniteRepeatable(animation = tween(durationMillis = 9000, easing = LinearEasing)),
            )

        IconButton(
            modifier = Modifier.weight(0.5f),
            aspectRatio = 2f,
            onClick = { navigate(HomeNavigation.Editor) },
        ) {
            drawCreateIcon(t.value.coerceIn(0f, 1f))
        }
        Row(modifier = Modifier.weight(0.5f)) {
            IconButton(
                modifier = Modifier.weight(0.5f),
                onClick = { navigate(HomeNavigation.Editor) },
            ) {
                drawHomePlayIcon(t.value.coerceIn(1f, 2f) - 1f)
            }
            IconButton(
                modifier = Modifier.weight(0.5f),
                onClick = { navigate(HomeNavigation.Editor) },
            ) {
                drawCommunityHomeIcon(t.value.coerceIn(2f, 3f) - 2f)
            }
        }
    }
}

@Composable
private fun Banners() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = insets.leftDp(),
                end = insets.rightDp(),
                top = insets.topDp() + 32.dp,
            ),
    ) {
        var bannerIndex by remember { mutableStateOf(0) }
        val scale = minOf(
            ((maxWidth - BannerButtonWidth * 2).toPx() / BannerWidth).toInt(),
            (maxHeight.toPx() / BannerHeight).toInt(),
        )
        val canvasSize = IntSize(BannerWidth * scale, BannerHeight * scale)
        val canvasWidth = canvasSize.width.toDp()
        val canvasHeight = canvasSize.height.toDp()
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { bannerIndex-- },
                enabled = bannerIndex > 0,
                modifier = Modifier.requiredSize(BannerButtonWidth, canvasHeight),
                padding = 0.dp,
            ) { _ -> drawChevron(ChevronDirection.Left) }
            Canvas(modifier = Modifier.requiredSize(canvasWidth, canvasHeight)) {
                drawImage(
                    image = memberBanners[bannerIndex].bitmap,
                    dstSize = canvasSize,
                    filterQuality = FilterQuality.None,
                )
            }
            IconButton(
                onClick = { bannerIndex++ },
                enabled = bannerIndex < memberBanners.lastIndex,
                modifier = Modifier.requiredSize(BannerButtonWidth, canvasHeight),
                padding = 0.dp,
            ) { _ -> drawChevron(ChevronDirection.Right) }
        }
    }
}
