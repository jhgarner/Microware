package com.example.microware.ui.gestures

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.example.microware.game.model.Obj
import com.example.microware.util.modify
import kotlin.math.roundToInt

data class Offer<T>(
    val offset: Offset,
    val size: IntSize,
    val localOffset: Offset,
    val item: T,
    val from: Any,
)

val Offer<Obj>.obj: Obj
    get() = item

data class DropTargetCallbacks<T>(
    val onStart: () -> Unit = {},
    val onEnter: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onGiven: (Offer<T>) -> Unit = {},
    val onEnd: () -> Unit = {},
)

class DropTarget<T>(val to: Any) {
    var bounds: Rect = Rect.Zero
    var callbacks by mutableStateOf(DropTargetCallbacks<T>())
}

interface DragTarget<T> {
    val offer: Offer<T>?
    val hoveredDropTarget: DropTarget<T>?

    fun registerDropTarget(target: DropTarget<T>)
    fun unregisterDropTarget(target: DropTarget<T>)
    fun updateDropTargetBounds(target: DropTarget<T>, bounds: Rect)
}

class Source<T>(
    fromState: MutableState<Any>,
    boundsState: MutableState<Rect>,
    itemState: MutableState<T>,
) {
    var from by fromState
    var bounds by boundsState
    var item by itemState
}

@Composable
fun <T> rememberSource(from: Any, item: T): Source<T> = remember {
    Source(
        mutableStateOf(from),
        mutableStateOf(Rect.Zero), mutableStateOf(item),
    )
}.also {
    it.from = from
    it.item = item
}

interface DragSource<T> {
    fun registerDragSource(source: Source<T>)
    fun unregisterDragSource(source: Source<T>)
}

class DragController<T> : DragTarget<T>, DragSource<T> {
    var offerState: MutableState<MutableState<Offer<T>>?> = mutableStateOf(null)
    var rootCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)
    val dropTargets = mutableListOf<DropTarget<T>>()
    private val dragSources = mutableListOf<Source<T>>()
    override var hoveredDropTarget: DropTarget<T>? = null

    override val offer: Offer<T>?
        get() = offerState.value?.value

    internal fun setRootCoordinates(coordinates: LayoutCoordinates) {
        rootCoordinates.value = coordinates
    }

    override fun registerDropTarget(target: DropTarget<T>) {
        dropTargets += target
        offerState.value?.value?.offset?.let { updateHoveredDropTarget(it) }
    }

    override fun unregisterDropTarget(target: DropTarget<T>) {
        dropTargets.remove(target)
        offerState.value?.value?.offset?.let { updateHoveredDropTarget(it) }
    }

    override fun updateDropTargetBounds(target: DropTarget<T>, bounds: Rect) {
        target.bounds = bounds
        offerState.value?.value?.offset?.let { updateHoveredDropTarget(it) }
    }

    override fun registerDragSource(source: Source<T>) {
        dragSources += source
    }

    internal fun topmostDragSourceAt(offset: Offset): Source<T>? =
        dragSources.lastOrNull { it.bounds.contains(offset) }

    override fun unregisterDragSource(source: Source<T>) {
        dragSources.remove(source)
    }

    fun updateHoveredDropTarget(offset: Offset) {
        val newTarget = dropTargets
            .filter { it.bounds.contains(offset) }
            .minByOrNull { it.bounds.width * it.bounds.height }
        if (newTarget === hoveredDropTarget) return

        hoveredDropTarget?.callbacks?.onExit()
        hoveredDropTarget = newTarget
        newTarget?.callbacks?.onEnter()
    }
}

fun <T> Modifier.dragSource(
    dragController: DragSource<T>,
    from: Any,
    item: T,
): Modifier = composed {
    val source = rememberSource(from, item)
    DisposableEffect(dragController, source) {
        dragController.registerDragSource(source)
        onDispose { dragController.unregisterDragSource(source) }
    }
    onGloballyPositioned { source.bounds = it.boundsInWindow() }
}

fun <T> Modifier.dragObserver(dragController: DragController<T>): Modifier = composed {
    val layout = dragController.rootCoordinates
    pointerInput(dragController) {
        racing {
            sequence {
                val localToWindow = layout.value?.let { it::localToWindow } ?: { it }
                var started = false
                try {
                    val lastChange = followDrag { change ->
                        change.consume()
                        val offset = localToWindow(change.position)
                        if (!started) {
                            started = true
                            val source = dragController.topmostDragSourceAt(offset)
                            if (source != null) {
                                dragController.offerState.value = mutableStateOf(
                                    Offer(
                                        offset = offset,
                                        size = IntSize(
                                            source.bounds.width.roundToInt(),
                                            source.bounds.height.roundToInt()
                                        ),
                                        localOffset = offset - source.bounds.topLeft,
                                        item = source.item,
                                        from = source.from,
                                    )
                                )
                                dragController.dropTargets.forEach { it.callbacks.onStart() }
                            }
                        } else {
                            dragController.offerState.value?.modify { it.copy(offset = offset) }
                        }
                        dragController.updateHoveredDropTarget(offset)
                    }
                    val lastOffset = localToWindow(lastChange.position)
                    dragController.offerState.value?.modify { it.copy(offset = lastOffset) }
                    dragController.updateHoveredDropTarget(lastOffset)
                    dragController.offerState.value?.let { (offer, _) ->
                        dragController.hoveredDropTarget?.callbacks?.onGiven(offer)
                    }
                } finally {
                    dragController.dropTargets.forEach { it.callbacks.onEnd() }
                    dragController.offerState.value = null
                }
            }
        }
    }.onGloballyPositioned { dragController.setRootCoordinates(it) }
}

fun <T> Modifier.dropTarget(
    dragController: DragTarget<T>,
    to: Any = Unit,
    onStart: () -> Unit = {},
    onEnter: () -> Unit = {},
    onExit: () -> Unit = {},
    onGiven: (Offer<T>) -> Unit = { },
    onEnd: () -> Unit = {},
): Modifier = composed {
    val target = remember { DropTarget<T>(to) }

    target.callbacks = DropTargetCallbacks(
        onStart = onStart,
        onEnter = onEnter,
        onExit = onExit,
        onGiven = onGiven,
        onEnd = onEnd,
    )

    DisposableEffect(dragController, target) {
        dragController.registerDropTarget(target)
        onDispose { dragController.unregisterDropTarget(target) }
    }

    this@composed.onGloballyPositioned {
        dragController.updateDropTargetBounds(target, it.boundsInWindow())
    }
}
