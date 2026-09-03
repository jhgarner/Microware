package com.example.microware

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.round

@Composable
fun BoxWithConstraintsScope.DrawerWithHandle(
    buttonModifier: Modifier = Modifier,
    side: HandleEdge,
    expandedSide: MutableState<HandleEdge?>,
    foreground: Palette,
    highlightForeground: Palette,
    hiddenSlide: Int = 16,
    offered: Offer?,
    onGiven: (Offer) -> Unit,
    extraOnDismiss: () -> Unit = {},
    buttonContent: @Composable (Color) -> Unit,
    drawerContent: @Composable () -> Unit,
) {
    var expandedSide by expandedSide
    val isExpanded = expandedSide == side
    var isHovered by remember { mutableStateOf(false) }
    val foregroundColor = if (isHovered) highlightForeground else foreground

    val fleeingTarget = when (expandedSide) {
        HandleEdge.Right -> Offset(-maxWidth.toPx(), 0f)
        HandleEdge.Left -> Offset(maxWidth.toPx(), 0f)
        HandleEdge.Bottom -> Offset(0f, -maxHeight.toPx() - HandleSize.toPx())
        null -> Offset.Zero
    }
    val fleeingAnimation = animateOffsetAsState(
        targetValue = if (!isExpanded) fleeingTarget else Offset.Zero,
        animationSpec = tween(durationMillis = 180)
    )

    Drawer(
        placement = side,
        isExpanded = isExpanded,
        onDismissRequest = { expandedSide = null; extraOnDismiss() },
    ) {
        drawerContent()
    }

    ButtonHandle(
        foreground = foregroundColor.color,
        openEdge = side,
        onClick = {
            expandedSide = if (expandedSide == side) {
                extraOnDismiss()
                null
            } else side
        },
        isExpanded = isExpanded,
        isDragging = offered != null,
        hiddenSlide = hiddenSlide,
        modifier = buttonModifier
            .offset { fleeingAnimation.value.round() }
            .dropTarget(
                offered = offered,
                onEnter = { isHovered = true },
                onExit = { isHovered = false },
                onGiven = onGiven,
                onEnd = { isHovered = false }),
    ) {
        buttonContent(foregroundColor.color)
    }
}
