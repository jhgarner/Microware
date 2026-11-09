package com.example.microware.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.microware.ui.icons.drawBinoculars
import com.example.microware.ui.icons.drawDesert
import com.example.microware.util.Loadable
import com.example.microware.util.getOrElse

private val BinocularsSize = DpSize(128.dp, 112.dp)
private val DesertSize = DpSize(240.dp, 160.dp)
private const val ExitMillis = 400

/** Shows [content] with binoculars searching while [items] load and a desert once they're empty. */
@Composable
fun <T> LoadingContent(
    items: Loadable<List<T>>,
    modifier: Modifier = Modifier,
    content: @Composable (List<T>) -> Unit,
) {
    Box(modifier) {
        content(items.getOrElse(emptyList()))
        Searching(visible = items is Loadable.Loading)
        EmptyDesert(visible = items is Loadable.Loaded && items.value.isEmpty())
    }
}

@Composable
fun BoxScope.Searching(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(),
        exit = slideOutVertically(tween(ExitMillis)) { it } + fadeOut(tween(ExitMillis)),
    ) {
        val scan = rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 2400, easing = LinearEasing)),
        )
        Canvas(Modifier.size(BinocularsSize).semantics { contentDescription = "Searching" }) {
            drawBinoculars(scan.value)
        }
    }
}

@Composable
fun BoxScope.EmptyDesert(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.Center),
        enter = fadeIn(tween(delayMillis = ExitMillis / 2)),
        exit = fadeOut(),
    ) {
        val rise = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            rise.animateTo(1f, tween(durationMillis = 1200, delayMillis = ExitMillis / 2, easing = LinearEasing))
        }
        val roll = rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 5000, easing = LinearEasing)),
        )
        Canvas(Modifier.size(DesertSize).semantics { contentDescription = "Nothing here yet" }) {
            drawDesert(rise.value, roll.value)
        }
    }
}
