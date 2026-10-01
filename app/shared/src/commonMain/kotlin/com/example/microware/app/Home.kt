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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.icons.drawCommunityHomeIcon
import com.example.microware.ui.icons.drawCreateIcon
import com.example.microware.ui.icons.drawHomePlayIcon
import com.example.microware.ui.layout.insets

enum class HomeNavigation { Editor }

@Composable
fun Home(navigate: (HomeNavigation) -> Unit) {
    Column(
        modifier = Modifier.padding(
            start = insets.leftDp(),
            end = insets.rightDp(),
            top = insets.topDp(),
            bottom = insets.bottomDp(),
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