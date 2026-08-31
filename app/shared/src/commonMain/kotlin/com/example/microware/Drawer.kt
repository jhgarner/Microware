package com.example.microware

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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

val HandleSize = 56.dp
val OverlayShadowElevation = 8.dp

data class Placement(val side: HandleEdge, val offset: Float)

@Composable
fun Drawer(
    placement: Placement,
    isExpanded: Boolean,
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val fraction = animateFloatAsState(if (isExpanded) 1f else 0f, tween(durationMillis = 180))
    val scrimAlpha = fraction.value * 0.32f
    if (scrimAlpha > 0f) {
        Box(
            modifier = Modifier
                .zIndex(0.4f)
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .then(
                    if (onDismissRequest != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismissRequest
                        )
                    } else Modifier
                )
        )
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize().zIndex(1f)) {
        val density = LocalDensity.current
        val handleInset = when (placement.side) {
            HandleEdge.Left -> insets.rightDp()
            HandleEdge.Right -> insets.leftDp()
            HandleEdge.Bottom -> insets.topDp()
        }
        val handleSize = HandleSize + handleInset
        val handleSizePx = with(density) { handleSize.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val drawerSize = when (placement.side) {
            HandleEdge.Left, HandleEdge.Right -> DpSize(maxWidth - handleSize, maxHeight)
            HandleEdge.Bottom -> DpSize(maxWidth, maxHeight - handleSize)
        }
        val closedOffset = when (placement.side) {
            HandleEdge.Left -> Offset(-maxWidthPx - handleSizePx, 0f)
            HandleEdge.Right -> Offset(maxWidthPx, 0f)
            HandleEdge.Bottom -> Offset(0f, maxHeightPx)
        }
        val expandedOffset = when (placement.side) {
            HandleEdge.Left -> Offset(0f, 0f)
            HandleEdge.Right -> Offset(handleSizePx, 0f)
            HandleEdge.Bottom -> Offset(0f, handleSizePx)
        }
        val contentPadding = when (placement.side) {
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
        LaunchedEffect(isExpanded) {
            coroutineScope {
                launch {
                    slideX.animateTo(
                        targetValue = when {
                            isExpanded -> expandedOffset.x
                            else -> closedOffset.x
                        },
                        animationSpec = tween(durationMillis = 180)
                    )
                }
                launch {
                    slideY.animateTo(
                        targetValue = when {
                            isExpanded -> expandedOffset.y
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
                content()
            }
        }
    }
}
