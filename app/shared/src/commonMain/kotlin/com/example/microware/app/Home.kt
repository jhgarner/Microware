package com.example.microware.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.microware.Palette
import com.example.microware.community.Banner
import com.example.microware.community.DrawBanner
import com.example.microware.game.sprite.Sprite
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawCommunityHomeIcon
import com.example.microware.ui.icons.drawCreateIcon
import com.example.microware.ui.icons.drawHomePlayIcon
import com.example.microware.ui.layout.insets
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

enum class HomeNavigation { Editor, Play, Community }

val memberBanners: List<Banner> = listOf(
    Banner(
        Sprite.pattern(128, 32) {
            when (it % 3) {
                0 -> Palette.Green
                1 -> Palette.Forest
                2 -> Palette.Cyan
                else -> Palette.Tan
            }
        },
    ),
    Banner(Sprite.solid(128, 32, Palette.Forest)),
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
                onClick = { navigate(HomeNavigation.Play) },
            ) {
                drawHomePlayIcon(t.value.coerceIn(1f, 2f) - 1f)
            }
            IconButton(
                modifier = Modifier.weight(0.5f),
                onClick = { navigate(HomeNavigation.Community) },
            ) {
                drawCommunityHomeIcon(t.value.coerceIn(2f, 3f) - 2f)
            }
        }
    }
}

@Composable
private fun Banners() {
    val pagerState = rememberPagerState(initialPage = 500) { memberBanners.size * 1000 }
    var interacted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        pagerState.interactionSource.interactions.collect { interacted = true }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3.seconds)
            if (interacted) break
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
    HorizontalPager(pagerState) {
        DrawBanner(memberBanners[((it - 500) % memberBanners.size + memberBanners.size) % memberBanners.size])
    }
}
