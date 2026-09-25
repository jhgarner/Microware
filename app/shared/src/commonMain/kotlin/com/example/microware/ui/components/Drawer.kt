package com.example.microware.ui.components
import com.example.microware.game.sprite.Palette
import com.example.microware.ui.layout.insets
import com.example.microware.util.runMaybe
import com.example.microware.util.toPx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

val HandleSize = 56.dp
val OverlayShadowElevation = 8.dp

data class DrawerState<Data>(val data: Data, val onDismiss: () -> Unit)

@Composable
fun <Data> Drawer(
    placement: HandleEdge,
    state: DrawerState<Data>?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(Data) -> Unit,
) {
    val fraction = animateFloatAsState(if (state != null) 1f else 0f, tween(durationMillis = 180))
    val scrimAlpha = fraction.value * 0.32f
    if (scrimAlpha > 0f) {
        Box(
            modifier = Modifier
                .zIndex(0.99f)
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .runMaybe(state?.onDismiss) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = it
                    )
                }
        )
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().zIndex(1f)) {
        val handleInset = when (placement) {
            HandleEdge.Left -> insets.rightDp()
            HandleEdge.Right -> insets.leftDp()
            HandleEdge.Bottom -> insets.topDp()
        }
        val handleSize = HandleSize + handleInset
        val handleSizePx = handleSize.toPx()
        val maxWidthPx = maxWidth.toPx()
        val maxHeightPx = maxHeight.toPx()
        val drawerSize = when (placement) {
            HandleEdge.Left, HandleEdge.Right -> DpSize(maxWidth - handleSize, maxHeight)
            HandleEdge.Bottom -> DpSize(maxWidth, maxHeight - handleSize)
        }
        val closedOffset = when (placement) {
            HandleEdge.Left -> Offset(-maxWidthPx - handleSizePx, 0f)
            HandleEdge.Right -> Offset(maxWidthPx, 0f)
            HandleEdge.Bottom -> Offset(0f, maxHeightPx)
        }
        val expandedOffset = when (placement) {
            HandleEdge.Left -> Offset(0f, 0f)
            HandleEdge.Right -> Offset(handleSizePx, 0f)
            HandleEdge.Bottom -> Offset(0f, handleSizePx)
        }
        val contentPadding = when (placement) {
            HandleEdge.Left -> PaddingValues(
                start = insets.leftDp(),
                top = insets.topDp(),
                bottom = insets.bottomDp()
            )

            HandleEdge.Right -> PaddingValues(
                end = insets.rightDp(),
                top = insets.topDp(),
                bottom = insets.bottomDp()
            )

            HandleEdge.Bottom -> PaddingValues(
                start = insets.leftDp(),
                end = insets.rightDp(),
                bottom = insets.bottomDp()
            )
        }
        val slideX = remember { Animatable(closedOffset.x) }
        val slideY = remember { Animatable(closedOffset.y) }
        LaunchedEffect(state == null) {
            coroutineScope {
                launch {
                    slideX.animateTo(
                        targetValue = when {
                            state != null -> expandedOffset.x
                            else -> closedOffset.x
                        },
                        animationSpec = tween(durationMillis = 180)
                    )
                }
                launch {
                    slideY.animateTo(
                        targetValue = when {
                            state != null -> expandedOffset.y
                            else -> closedOffset.y
                        },
                        animationSpec = tween(durationMillis = 180)
                    )
                }
            }
        }
        val panelShape = RoundedCornerShape(0.dp)

        Box(
            modifier = Modifier
                .offset { Offset(slideX.value, slideY.value).round() }
                .requiredSize(drawerSize)
                .shadow(OverlayShadowElevation, panelShape)
                .background(Palette.OffWhite.color, panelShape)
                .padding(12.dp)
        ) {
            Box(modifier = Modifier.padding(contentPadding)) {
                state?.let { content(it.data) }
            }
        }
    }
}
