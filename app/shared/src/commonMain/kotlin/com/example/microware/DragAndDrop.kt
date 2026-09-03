package com.example.microware

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

data class Offer(val offset: Offset, val localOffset: Offset, val obj: Obj, val from: Any)

class DragController {
    private val offerState = mutableStateOf<Offer?>(null)
    private var rootCoordinates: LayoutCoordinates? = null

    internal var activePointerId: PointerId? = null
        private set

    val offer: Offer?
        get() = offerState.value

    internal fun setRootCoordinates(coordinates: LayoutCoordinates) {
        rootCoordinates = coordinates
    }

    internal fun startOrUpdate(
        pointerId: PointerId,
        offset: Offset,
        localOffset: Offset,
        obj: Obj,
        from: Any,
    ) {
        when (activePointerId) {
            null -> {
                activePointerId = pointerId
                offerState.value = Offer(offset, localOffset, obj, from)
            }

            pointerId -> {
                offerState.value = offerState.value?.copy(offset = offset)
            }
        }
    }

    internal fun update(pointerId: PointerId, offset: Offset) {
        if (activePointerId == pointerId) {
            offerState.value = offerState.value?.copy(offset = offset)
        }
    }

    internal fun finish(pointerId: PointerId) {
        if (activePointerId == pointerId) {
            activePointerId = null
            offerState.value = null
        }
    }

    internal fun cancel(pointerId: PointerId? = null) {
        if (pointerId == null || activePointerId == pointerId) {
            activePointerId = null
            offerState.value = null
        }
    }

    internal fun toWindow(position: Offset): Offset =
        rootCoordinates?.localToWindow(position) ?: position
}

@Composable
fun Modifier.dragSource(
    dragController: DragController,
    from: Any,
    obj: Obj,
    raceBefore: suspend context(PointerRacer) () -> Unit = { },
): Modifier {
    val layout = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentObj = rememberUpdatedState(obj)
    val currentFrom = rememberUpdatedState(from)
    return onGloballyPositioned { layout.value = it }
        .pointerInput(obj.id) {
            racing {
                raceBefore()
                sequence {
                    val localToWindow = layout.value?.let { it::localToWindow } ?: { it }
                    val end = followDrag { change ->
                        dragController.startOrUpdate(
                            pointerId = change.id,
                            offset = localToWindow(change.position),
                            localOffset = change.position,
                            obj = currentObj.value,
                            from = currentFrom.value,
                        )
                    }
                    dragController.finish(end.id)
                }
            }
        }
}

fun Modifier.dragObserver(dragController: DragController): Modifier =
    pointerInput(dragController) {
        try {
            awaitEachGesture {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointerId = dragController.activePointerId
                    if (pointerId != null) {
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null || !change.pressed) {
                            dragController.cancel(pointerId)
                            return@awaitEachGesture
                        }

                        change.consume()
                        dragController.update(
                            pointerId,
                            dragController.toWindow(change.position),
                        )
                    }

                    if (event.changes.all { !it.pressed }) {
                        return@awaitEachGesture
                    }
                }
            }
        } finally {
            dragController.cancel()
        }
    }

@Composable
fun Modifier.dropTarget(
    offered: Offer?,
    onStart: () -> Unit = {},
    onEnter: () -> Unit = {},
    onExit: () -> Unit = {},
    onGiven: (Offer) -> Unit = {},
    onEnd: () -> Unit = {},
): Modifier {
    var layout by remember { mutableStateOf<LayoutCoordinates?>(null) }
    layout?.apply {
        val rect = boundsInWindow()
        val lastOffer = rememberLast(offered)
        val wasContained = lastOffer?.let { rect.contains(it.offset) } ?: false
        val contained = offered?.let { rect.contains(it.offset) }
        if (lastOffer == null && offered != null) {
            onStart()
        } else {
            if (!wasContained && contained == true) {
                onEnter()
            } else if (wasContained && contained == null) {
                onGiven(lastOffer)
            } else if (wasContained && contained == false) {
                onExit()
            }
            if (lastOffer != null && offered == null) {
                onEnd()
            }
        }
    }

    return onGloballyPositioned { layout = it }
}
