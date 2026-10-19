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
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.microware.community.Banner
import com.example.microware.community.DrawBanner
import com.example.microware.community.defaultBanner
import com.example.microware.game.sprite.Sprite.Companion.load
import com.example.microware.getBanners
import com.example.microware.getVersion
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawCommunityHomeIcon
import com.example.microware.ui.icons.drawCreateIcon
import com.example.microware.ui.icons.drawHomePlayIcon
import com.example.microware.ui.layout.insets
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

enum class HomeNavigation { Editor, Play, Community }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navigate: (HomeNavigation) -> Unit) {
    var importantMessage by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    LaunchedEffect(Unit) {
        val version = runCatching { getVersion() }.getOrElse { "" }
        if (version == "" || version.length > 12) {
            importantMessage = {
                Text("The server is not responding. The app will probably not work. $version")
            }
        } else if (version != "1.0.3") {
            importantMessage = {
                val uriHandler = LocalUriHandler.current
                Text("To continue, update your app by downloading the latest apk file. The app will likely crash without it.")
                Button(onClick = { uriHandler.openUri("https://github.com/jhgarner/Microware/releases") }) {
                    Text("Open Github to update")
                }
            }
        }
    }
    if (importantMessage != null) {
        BasicAlertDialog(onDismissRequest = { importantMessage = null }) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    importantMessage?.invoke()
                }
            }
        }
    }
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
    var banners by remember { mutableStateOf(listOf<Banner>()) }
    LaunchedEffect(Unit) {
        val sprites = runCatching { getBanners() }.getOrElse { listOf() }
        banners = sprites.map { runCatching { Banner(it.load()) }.getOrElse { defaultBanner() } }
    }
    val pagerState = rememberPagerState(initialPage = 500) { banners.size * 1000 }
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
        DrawBanner(banners[((it - 500) % banners.size + banners.size) % banners.size])
    }
}
