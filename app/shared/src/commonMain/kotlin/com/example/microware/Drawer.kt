package com.example.microware

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlin.math.roundToInt

val HandleSize = 56.dp
val OverlayShadowElevation = 8.dp

enum class Side { Left, Right }

data class Placement(val side: Side, val offset: Float)

@Composable
fun Drawer(
    placement: Placement,
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val handleWidth = with(density) { HandleSize.toPx() }
        val drawerWidth = (with(density) { maxWidth.toPx() } - handleWidth).coerceAtLeast(0f)
        val panelShape = RoundedCornerShape(0.dp)
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val fraction = if (drawerWidth < 1f) {
            0f
        } else {
            when (placement.side) {
                Side.Left -> ((availableWidthPx - placement.offset) / drawerWidth).coerceIn(0f, 1f)
                Side.Right -> (placement.offset / drawerWidth).coerceIn(0f, 1f)
            }
        }
        val scrimAlpha = fraction * 0.32f
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
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

        Box(
            modifier = Modifier
                .offset {
                    Offset(
                        placement.run { if (side == Side.Left) offset else offset - drawerWidth },
                        0f
                    ).round()
                }
                .requiredSize(
                    with(density) { drawerWidth.toDp() },
                    maxHeight
                )
                .shadow(OverlayShadowElevation, panelShape)
                .background(Palette.OffWhite.color, panelShape)
                .padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun BottomDrawer(
    isOpen: Boolean,
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fullHeight = with(density) { maxHeight.toPx() }
        val handleHeight = with(density) { HandleSize.toPx() }
        val drawerHeight = (fullHeight - handleHeight).coerceAtLeast(0f)
        val drawerOffset by animateFloatAsState(
            targetValue = if (isOpen) handleHeight else fullHeight,
            animationSpec = tween(durationMillis = 180),
            label = "bottomDrawerOffset"
        )
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isOpen) 0.32f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "bottomDrawerScrim"
        )
        val panelShape = RoundedCornerShape(0.dp)

        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
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

        Box(
            modifier = Modifier
                .offset { IntOffset(0, drawerOffset.roundToInt()) }
                .requiredSize(
                    maxWidth,
                    with(density) { drawerHeight.toDp() }
                )
                .shadow(OverlayShadowElevation, panelShape)
                .background(Palette.OffWhite.color, panelShape)
                .padding(12.dp),
        ) {
            content()
        }
    }
}
