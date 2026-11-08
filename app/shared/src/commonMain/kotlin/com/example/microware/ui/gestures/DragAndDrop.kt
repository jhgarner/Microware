package com.example.microware.ui.gestures

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.round
import com.example.microware.game.model.Obj

/**
 * An item being dragged. [pointer] is in window coordinates and [grip] is the point that was
 * pressed as a fraction of the source's size, so any ghost can keep the same spot under the
 * finger whatever size it is drawn at.
 */
data class Offer<T>(
    val item: T,
    val from: Any,
    val pointer: Offset,
    val grip: Offset,
) {
    fun topLeftFor(size: Size): Offset =
        pointer - Offset(grip.x * size.width, grip.y * size.height)
}

val Offer<Obj>.obj: Obj
    get() = item

class Press<T>(
    val pointer: PointerId,
    val item: T,
    val from: Any,
    val grip: Offset,
)

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

interface DragSource<T> {
    fun press(press: Press<T>)
}

class DragController<T> : DragTarget<T>, DragSource<T> {
    override var offer by mutableStateOf<Offer<T>?>(null)
        private set
    override var hoveredDropTarget: DropTarget<T>? = null
        private set
    private val dropTargets = mutableListOf<DropTarget<T>>()
    private var lastPress: Press<T>? = null

    override fun press(press: Press<T>) {
        lastPress = press
    }

    internal fun takePress(pointer: PointerId): Press<T>? =
        lastPress?.takeIf { it.pointer == pointer }.also { lastPress = null }

    override fun registerDropTarget(target: DropTarget<T>) {
        dropTargets += target
        refreshHover()
    }

    override fun unregisterDropTarget(target: DropTarget<T>) {
        dropTargets.remove(target)
        refreshHover()
    }

    override fun updateDropTargetBounds(target: DropTarget<T>, bounds: Rect) {
        target.bounds = bounds
        refreshHover()
    }

    internal fun dragTo(press: Press<T>, pointer: Offset) {
        val current = offer
        offer = current?.copy(pointer = pointer)
            ?: Offer(press.item, press.from, pointer, press.grip)
        if (current == null) dropTargets.forEach { it.callbacks.onStart() }
        refreshHover()
    }

    internal fun drop(press: Press<T>, pointer: Offset) {
        dragTo(press, pointer)
        offer?.let { offer -> hoveredDropTarget?.callbacks?.onGiven(offer) }
    }

    internal fun endDrag() {
        if (offer == null) return
        dropTargets.forEach { it.callbacks.onEnd() }
        hoveredDropTarget = null
        offer = null
    }

    private fun refreshHover() {
        val pointer = offer?.pointer ?: return
        val newTarget = dropTargets
            .filter { it.bounds.contains(pointer) }
            .minByOrNull { it.bounds.width * it.bounds.height }
        if (newTarget === hoveredDropTarget) return

        hoveredDropTarget?.callbacks?.onExit()
        hoveredDropTarget = newTarget
        newTarget?.callbacks?.onEnter()
    }
}

/**
 * Marks this element as draggable. Compose's hit testing decides whether a press reaches it, so
 * anything drawn on top that handles input (drawers, the scrim, buttons) shields it.
 */
fun <T> Modifier.dragSource(
    source: DragSource<T>,
    from: Any,
    item: T,
): Modifier = pointerInput(source, from, item) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes
                .filter { it.changedToDownIgnoreConsumed() }
                .forEach { down ->
                    val grip = Offset(down.position.x / size.width, down.position.y / size.height)
                    source.press(Press(down.id, item, from, grip))
                }
        }
    }
}

/**
 * Runs drags for the [dragSource]s inside this element. Sources are always descendants, so they
 * see each press first and this only has to pick up the one they reported.
 */
fun <T> Modifier.dragObserver(dragController: DragController<T>): Modifier = composed {
    val layout = remember { LayoutHolder() }
    onGloballyPositioned { layout.coordinates = it }.pointerInput(dragController) {
        fun Offset.inWindow() = layout.coordinates.localToWindow(this)

        racing {
            sequence {
                val down = expectDown()
                val press = dragController.takePress(down.id) ?: unexpected(down)
                try {
                    val up = followDrag(down) { change ->
                        change.consume()
                        dragController.dragTo(press, change.position.inWindow())
                    }
                    dragController.drop(press, up.position.inWindow())
                } finally {
                    dragController.endDrag()
                }
            }
        }
    }
}

private class LayoutHolder {
    lateinit var coordinates: LayoutCoordinates
}

/** Places this element so the offer's grip point sits under the pointer, wherever its parent is. */
fun Modifier.followPointer(offer: Offer<*>): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        val size = Size(placeable.width.toFloat(), placeable.height.toFloat())
        coordinates?.let { placeable.place(it.windowToLocal(offer.topLeftFor(size)).round()) }
    }
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
