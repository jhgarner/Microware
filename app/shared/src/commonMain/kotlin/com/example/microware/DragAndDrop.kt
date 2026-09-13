package com.example.microware

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

data class Offer<T>(
    val offset: Offset,
    val size: IntSize,
    val localOffset: Offset,
    val item: T,
    val from: Any,
    val released: Boolean = false,
)

val Offer<Obj>.obj: Obj
    get() = item

data class DropTargetCallbacks<T>(
    val onStart: () -> Unit = {},
    val onEnter: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onGiven: (Offer<T>, DropCompletion) -> Unit = { _, _ -> },
    val onEnd: () -> Unit = {},
)

class DropTarget<T> {
    var bounds: Rect? = null
    var callbacks = DropTargetCallbacks<T>()
}

class DropCompletion(
    private val onComplete: () -> Unit,
) {
    private var deferred = false
    private var completed = false

    fun defer() {
        if (!completed) {
            deferred = true
        }
    }

    fun complete() {
        if (!completed) {
            completed = true
            onComplete()
        }
    }

    internal fun completeIfNotDeferred() {
        if (!deferred) {
            complete()
        }
    }
}

data class DropRelease<T>(
    val offer: Offer<T>,
    val completion: DropCompletion,
)

interface DragTarget<T> {
    val offer: Offer<T>?

    fun registerDropTarget(target: DropTarget<T>)
    fun unregisterDropTarget(target: DropTarget<T>)
    fun releaseFor(target: DropTarget<T>): DropRelease<T>?
    fun updateDropTargetBounds(target: DropTarget<T>, bounds: Rect)
}

interface DragSource<T> {
    fun startOrUpdate(
        pointerId: PointerId,
        offset: Offset,
        size: IntSize,
        localOffset: Offset,
        item: T,
        from: Any,
    )
}

class DragController<T> : DragTarget<T>, DragSource<T> {
    private val offerState = mutableStateOf<Offer<T>?>(null)
    private var rootCoordinates: LayoutCoordinates? = null
    private val dropTargets = mutableListOf<DropTarget<T>>()
    private var hoveredDropTarget: DropTarget<T>? = null
    private var releaseTarget: DropTarget<T>? = null
    private var releaseCompletion: DropCompletion? = null
    private var releaseDispatched = false

    internal var activePointerId: PointerId? = null
        private set

    override val offer: Offer<T>?
        get() = offerState.value

    internal fun setRootCoordinates(coordinates: LayoutCoordinates) {
        rootCoordinates = coordinates
    }

    override fun startOrUpdate(
        pointerId: PointerId,
        offset: Offset,
        size: IntSize,
        localOffset: Offset,
        item: T,
        from: Any,
    ) {
        when (activePointerId) {
            null -> {
                activePointerId = pointerId
                offerState.value = Offer(
                    offset = offset,
                    size = size,
                    localOffset = localOffset,
                    item = item,
                    from = from,
                )
                dropTargets.forEach { it.callbacks.onStart() }
                updateHoveredDropTarget(offset)
            }

            pointerId -> {
                offerState.value = offerState.value?.copy(offset = offset)
                updateHoveredDropTarget(offset)
            }
        }
    }

    internal fun update(pointerId: PointerId, offset: Offset) {
        if (activePointerId == pointerId) {
            offerState.value = offerState.value?.copy(offset = offset)
            updateHoveredDropTarget(offset)
        }
    }

    internal fun finish(pointerId: PointerId, offset: Offset) {
        if (activePointerId == pointerId) {
            offerState.value = offerState.value?.copy(offset = offset)
            updateHoveredDropTarget(offset)
            releaseTarget = hoveredDropTarget
            releaseDispatched = false
            releaseCompletion = releaseTarget?.let { target ->
                lateinit var completion: DropCompletion
                completion = DropCompletion {
                    completeRelease(completion)
                }
                completion
            }
            activePointerId = null
            offerState.value = offerState.value?.copy(released = true)
        }
    }

    internal fun cancel(pointerId: PointerId? = null) {
        if (pointerId != null && activePointerId != pointerId) return
        clearHoveredDropTarget()
        activePointerId = null
        releaseTarget = null
        releaseCompletion = null
        releaseDispatched = false
        offerState.value = null
    }

    internal fun clearReleased() {
        if (offerState.value?.released == true) {
            releaseTarget = null
            releaseCompletion = null
            releaseDispatched = false
            offerState.value = null
        }
    }

    internal fun hasReleaseTarget(): Boolean = releaseTarget != null

    private fun completeRelease(completion: DropCompletion) {
        if (offerState.value?.released == true && releaseCompletion === completion) {
            releaseTarget = null
            releaseCompletion = null
            releaseDispatched = false
            offerState.value = null
        }
    }

    override fun releaseFor(target: DropTarget<T>): DropRelease<T>? {
        if (
            offerState.value?.released != true ||
            releaseTarget !== target ||
            releaseDispatched
        ) {
            return null
        }

        val offer = offerState.value ?: return null
        val completion = releaseCompletion ?: return null
        releaseDispatched = true
        return DropRelease(
            offer = offer,
            completion = completion,
        )
    }

    override fun registerDropTarget(target: DropTarget<T>) {
        if (target !in dropTargets) {
            dropTargets += target
            if (activePointerId != null) {
                updateHoveredDropTarget(offerState.value?.offset)
            }
        }
    }

    override fun unregisterDropTarget(target: DropTarget<T>) {
        if (hoveredDropTarget === target) {
            target.callbacks.onExit()
            target.callbacks.onEnd()
            hoveredDropTarget = null
        }
        if (releaseTarget === target && !releaseDispatched) {
            releaseTarget = null
            releaseCompletion = null
            releaseDispatched = false
            offerState.value = null
        }
        dropTargets.remove(target)
    }

    override fun updateDropTargetBounds(target: DropTarget<T>, bounds: Rect) {
        target.bounds = bounds
        if (activePointerId != null) {
            updateHoveredDropTarget(offerState.value?.offset)
        }
    }

    private fun updateHoveredDropTarget(offset: Offset?) {
        val newTarget = offset?.let { position ->
            dropTargets.asReversed().firstOrNull { target ->
                target.bounds?.contains(position) == true
            }
        }
        if (newTarget === hoveredDropTarget) return

        hoveredDropTarget?.callbacks?.onExit()
        hoveredDropTarget = newTarget
        newTarget?.callbacks?.onEnter()
    }

    private fun clearHoveredDropTarget() {
        hoveredDropTarget?.callbacks?.onExit()
        hoveredDropTarget?.callbacks?.onEnd()
        hoveredDropTarget = null
    }

    internal fun toWindow(position: Offset): Offset =
        rootCoordinates?.localToWindow(position) ?: position
}

@Composable
fun <T> Modifier.dragSource(
    dragController: DragSource<T>,
    from: Any,
    item: T,
    raceBefore: suspend context(PointerRacer) () -> Unit = { },
): Modifier {
    val layout = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentItem = rememberUpdatedState(item)
    val currentFrom = rememberUpdatedState(from)
    val density = density
    return onGloballyPositioned { layout.value = it }
        .pointerInput(item, density) {
            racing {
                raceBefore()
                sequence {
                    val localToWindow = layout.value?.let { it::localToWindow } ?: { it }
                    followDrag { change ->
                        dragController.startOrUpdate(
                            pointerId = change.id,
                            offset = localToWindow(change.position),
                            size = layout.value?.size ?: IntSize.Zero,
                            localOffset = change.position,
                            item = currentItem.value,
                            from = currentFrom.value,
                        )
                    }
                }
            }
        }
}

fun <T> Modifier.dragObserver(dragController: DragController<T>): Modifier =
    pointerInput(dragController) {
        try {
            awaitEachGesture {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val pointerId = dragController.activePointerId
                    if (pointerId != null) {
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null) {
                            dragController.cancel(pointerId)
                            return@awaitEachGesture
                        }

                        if (!change.pressed) {
                            dragController.finish(
                                pointerId = pointerId,
                                offset = dragController.toWindow(change.position),
                            )
                            if (!dragController.hasReleaseTarget()) {
                                dragController.clearReleased()
                            }
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

fun <T> Modifier.dropTarget(
    dragController: DragTarget<T>,
    onStart: () -> Unit = {},
    onEnter: () -> Unit = {},
    onExit: () -> Unit = {},
    onGiven: (Offer<T>, DropCompletion) -> Unit = { _, _ -> },
    onEnd: () -> Unit = {},
): Modifier = composed {
    val target = remember { DropTarget<T>() }

    SideEffect {
        target.callbacks = DropTargetCallbacks(
            onStart = onStart,
            onEnter = onEnter,
            onExit = onExit,
            onGiven = onGiven,
            onEnd = onEnd,
        )
    }

    DisposableEffect(dragController, target) {
        dragController.registerDropTarget(target)
        onDispose { dragController.unregisterDropTarget(target) }
    }

    LaunchedEffect(dragController.offer?.released, target) {
        if (dragController.offer?.released == true) {
            withFrameNanos { }
            val release = dragController.releaseFor(target) ?: return@LaunchedEffect
            try {
                target.callbacks.onGiven(release.offer, release.completion)
            } finally {
                target.callbacks.onEnd()
                release.completion.completeIfNotDeferred()
            }
        }
    }

    this@composed.onGloballyPositioned {
        dragController.updateDropTargetBounds(target, it.boundsInWindow())
    }
}
