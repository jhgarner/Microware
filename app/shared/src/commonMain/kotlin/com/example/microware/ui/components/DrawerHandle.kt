package com.example.microware.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.round
import com.example.microware.ui.gestures.DragTarget
import com.example.microware.ui.gestures.Offer
import com.example.microware.ui.gestures.dropTarget
import com.example.microware.util.runMaybe
import com.example.microware.util.toPx

sealed interface DrawerHandleState<in T, out Data> {
    val data: Data? get() = null

    data class Waiting<Data>(
        val extraOnExpand: () -> Unit = {},
        val extraOnDismiss: () -> Unit = {},
    ) : DrawerHandleState<Any, Data>

    data class Dragging<T, Data>(
        val dragTarget: DragTarget<T>,
        val onGiven: (Offer<T>) -> Unit,
        val onEnter: () -> Unit = {},
        val onExit: () -> Unit = {},
        val onEnd: () -> Unit = {},
    ) : DrawerHandleState<T, Data>

    data class Expanded<Data>(
        val side: HandleEdge,
        override val data: Data?,
    ) : DrawerHandleState<Any, Data> {
        companion object {
            operator fun invoke(side: HandleEdge): Expanded<Unit> = Expanded(side, Unit)
        }
    }
}

@Composable
fun <T, Data> BoxWithConstraintsScope.DrawerWithHandle(
    buttonModifier: Modifier = Modifier,
    handleType: HandleType,
    hiddenSlide: Int = 16,
    state: DrawerHandleState<T, Data>?,
    setExpanded: (HandleEdge?) -> Unit,
    drawerContent: @Composable (Data) -> Unit,
) {
    val drawerState = when (state) {
        null -> null
        is DrawerHandleState.Expanded if state.data != null -> DrawerButtonState.Expanded {
            setExpanded(null)
        }

        is DrawerHandleState.Dragging<*, *> -> DrawerButtonState.Dragging
        is DrawerHandleState.Expanded -> DrawerButtonState.Waiting {}
        is DrawerHandleState.Waiting -> DrawerButtonState.Waiting {
            state.extraOnExpand()
            setExpanded(handleType.edge)
        }
    }
    var isHovered by remember { mutableStateOf(false) }

    val fleeingTarget = when ((state as? DrawerHandleState.Expanded)?.side) {
        HandleEdge.Right -> Offset(-maxWidth.toPx(), 0f)
        HandleEdge.Left -> Offset(maxWidth.toPx(), 0f)
        HandleEdge.Bottom -> Offset(0f, -maxHeight.toPx() - HandleSize.toPx())
        null -> Offset.Zero
    }
    val fleeingAnimation = animateOffsetAsState(
        targetValue = if (drawerState !is DrawerButtonState.Expanded) fleeingTarget else Offset.Zero,
        animationSpec = tween(durationMillis = 180)
    )

    Drawer(
        placement = handleType.edge,
        state = if (state is DrawerHandleState.Expanded && state.data != null) {
            DrawerState(state.data)
        } else null,
    ) {
        drawerContent(it)
    }

    ButtonHandle(
        type = handleType,
        highlight = isHovered,
        hiddenSlide = hiddenSlide,
        state = drawerState,
        modifier = buttonModifier
            .offset { fleeingAnimation.value.round() }
            .runMaybe(state as? DrawerHandleState.Dragging) {
                dropTarget(
                    dragController = it.dragTarget,
                    onEnter = {
                        isHovered = true
                        it.onEnter()
                    },
                    onExit = {
                        isHovered = false
                        it.onExit()
                    },
                    onGiven = it.onGiven,
                    onEnd = {
                        isHovered = false
                        it.onEnd()
                    })
            }
    )
}
