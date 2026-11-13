package com.example.microware.game.runtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.WorldHeight
import com.example.microware.game.model.WorldWidth
import com.example.microware.game.sprite.scaled
import com.example.microware.ui.layout.insets
import kotlin.math.min
import kotlin.random.Random

class Player(
    val logics: List<Logic>,
    val objects: List<ObjectState> = emptyList(),
    var timeMs: Long = 0L,
    var winState: WinState = WinState.Playing,
    private val random: Random = Random.Default,
) {
    private val taggedObjects = objects.filter { it.tags.isNotEmpty() }
    private var pointerLocation: Offset? = null
    private var touchedObjects: Set<ObjectState> = emptySet()
    private var overlaps: Set<Overlap> = emptySet()
    private var fractionalElapsedMs = 0.0
    private var hasDeliveredResult = false
    private var cachedScale = Float.NaN
    private var cachedBackground: ImageBitmap? = null
    private var scaledBitmaps: List<ImageBitmap> = emptyList()

    constructor(game: Game, random: Random = Random.Default) : this(
        logics = game.logics,
        objects = game.world.objects.map { obj ->
            ObjectState(
                bitmap = obj.sprite.bitmap,
                offset = obj.offset,
                tags = game.world.logicTagsFor(obj.id),
            )
        },
        random = random,
    )

    init {
        logics.filter { it.trigger is Trigger.Start }.forEach(::runActions)
    }

    val result: GameResult?
        get() = if (timeMs < GameDurationMillis) {
            null
        } else if (winState == WinState.Won) {
            GameResult.Won
        } else {
            GameResult.Lost
        }

    fun step(delta: Long) {
        if (delta <= 0L || timeMs >= GameDurationMillis) return

        val elapsed = delta.coerceAtMost(GameDurationMillis - timeMs)
        var remaining = elapsed
        while (remaining > 0L) {
            val substep = remaining.coerceAtMost(MaxSimulationStepMillis)
            move(substep)
            resolveCollisions()
            resolvePointerContacts()
            timeMs += substep
            remaining -= substep
        }
    }

    fun advance(delta: Long, speed: Float = 1f) {
        val scaledDelta = delta * speed.toDouble() + fractionalElapsedMs
        val wholeMilliseconds = scaledDelta.toLong()
        fractionalElapsedMs = scaledDelta - wholeMilliseconds
        step(wholeMilliseconds)
    }

    fun press(location: Offset) = movePointer(location)

    fun release() = movePointer(null)

    fun movePointer(location: Offset?) {
        if (timeMs >= GameDurationMillis) return
        pointerLocation = location
        resolvePointerContacts()
    }

    private fun resolvePointerContacts() {
        val location = pointerLocation
        val touching = if (location == null) emptySet() else taggedObjects
            .filter { it.rect().contains(location) }
            .toSet()
        val entered = touching - touchedObjects
        val exited = touchedObjects - touching
        touchedObjects = touching

        for (logic in logics) {
            val triggered = when (val trigger = logic.trigger) {
                is Trigger.Press -> entered.any { it.matches(trigger.target) }
                is Trigger.Release -> exited.any { it.matches(trigger.target) }
                else -> false
            }
            if (triggered) runActions(logic)
        }
    }

    private fun move(delta: Long) {
        val seconds = delta / 1000f
        for (objectState in taggedObjects) {
            val destination = objectState.destination
            if (destination == null) {
                objectState.offset += objectState.velocity * seconds
                continue
            }

            val distance = (destination - objectState.offset).getDistance()
            if (distance <= objectState.velocity.getDistance() * seconds) {
                objectState.offset = destination
                objectState.velocity = Offset.Zero
                objectState.destination = null
            } else {
                objectState.offset += objectState.velocity * seconds
            }
        }
    }

    private fun runActions(logic: Logic) = logic.actions.forEach(::runAction)

    private fun runAction(action: Action) {
        when (action) {
            Action.Win -> winState = WinState.Won
            Action.Lose -> winState = WinState.Lost
            is Action.Move -> for (mover in matching(action.mover)) {
                mover.velocity = dirVec(action.target) * action.speed.toFloat()
                mover.destination = null
            }

            is Action.MoveTo -> forEachMoverAndTarget(action.mover, action.target) { mover, target ->
                moveTo(mover, target.offset, action.speed)
            }

            is Action.MoveWithin -> forEachMoverAndTarget(action.mover, action.target) { mover, target ->
                moveWithin(mover, target.rect(), action.speed)
            }
        }
    }

    private fun matching(filter: TagFilter): List<ObjectState> =
        taggedObjects.filter { it.matches(filter) }

    private fun forEachMoverAndTarget(
        mover: TagFilter,
        target: TagFilter,
        move: (mover: ObjectState, target: ObjectState) -> Unit,
    ) {
        val targets = matching(target)
        if (targets.isEmpty()) return
        for (matchingMover in matching(mover)) move(matchingMover, targets.random(random))
    }

    private fun moveWithin(mover: ObjectState, bounds: Rect, speed: Int?) {
        val availableWidth = bounds.width - mover.bitmap.width
        val availableHeight = bounds.height - mover.bitmap.height
        if (availableWidth < 0f || availableHeight < 0f) return

        val destination = Offset(
            x = bounds.left + random.nextFloat() * availableWidth,
            y = bounds.top + random.nextFloat() * availableHeight,
        )
        moveTo(mover, destination, speed)
    }

    private fun moveTo(mover: ObjectState, destination: Offset, speed: Int?) {
        val difference = destination - mover.offset
        val distance = difference.getDistance()

        if (speed == null) {
            mover.offset = destination
            mover.velocity = Offset.Zero
            mover.destination = null
        } else if (distance == 0f) {
            mover.velocity = Offset.Zero
            mover.destination = null
        } else {
            mover.velocity = difference / distance * speed.toFloat()
            mover.destination = destination
        }
    }

    private fun resolveCollisions() {
        val currentOverlaps = buildSet {
            for (firstIndex in taggedObjects.indices) {
                val first = taggedObjects[firstIndex]
                for (secondIndex in firstIndex + 1 until taggedObjects.size) {
                    val second = taggedObjects[secondIndex]
                    if (first.rect().overlaps(second.rect())) add(Overlap(first, second))
                }
            }
        }
        val previousOverlaps = overlaps
        overlaps = currentOverlaps

        for (logic in logics) {
            val trigger = logic.trigger as? Trigger.Contact ?: continue
            val wereInContact = previousOverlaps.targetsInContact(trigger)
            val areInContact = currentOverlaps.targetsInContact(trigger)
            val triggeredTargets = when (trigger) {
                is Trigger.Collision -> areInContact - wereInContact
                is Trigger.Separation -> wereInContact - areInContact
            }
            repeat(triggeredTargets.size) { runActions(logic) }
        }
    }

    private fun Set<Overlap>.targetsInContact(trigger: Trigger.Contact): Set<ObjectState> =
        matching(trigger.secondTarget).filterTo(mutableSetOf()) { target ->
            count { it.joins(target, trigger.firstTarget) } >= trigger.firstTargetCount
        }

    @Composable
    fun PlayGame(
        active: Boolean = true,
        speed: Float = 1f,
        modifier: Modifier = Modifier,
        applyInsets: Boolean = true,
        onFinished: (GameResult) -> Unit = {},
    ) {
        var frameTime by remember(this@Player) { mutableStateOf(0L) }
        var canvasSize by remember(this@Player) { mutableStateOf(IntSize.Zero) }
        val latestOnFinished by rememberUpdatedState(onFinished)

        LaunchedEffect(this@Player, active, speed) {
            if (!active || hasDeliveredResult) return@LaunchedEffect

            var previousFrame = withFrameMillis { it }
            frameTime = previousFrame

            while (result == null) {
                val currentFrame = withFrameMillis { it }
                val delta = (currentFrame - previousFrame).coerceAtLeast(0L)
                advance(delta, speed)
                previousFrame = currentFrame
                frameTime = currentFrame

                result?.let { finishedResult ->
                    hasDeliveredResult = true
                    latestOnFinished(finishedResult)
                }
            }
        }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (applyInsets) {
                        Modifier.padding(
                            start = insets.leftDp(),
                            top = insets.topDp(),
                            end = insets.rightDp(),
                            bottom = insets.bottomDp(),
                        )
                    } else {
                        Modifier
                    }
                )
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(this@Player, canvasSize, active) {
                    if (active) {
                        try {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                movePointer(gameLocation(down.position, canvasSize))
                                do {
                                    val event = awaitPointerEvent()
                                    val pointer = event.changes.first { it.id == down.id }
                                    if (pointer.pressed) {
                                        movePointer(gameLocation(pointer.position, canvasSize))
                                    } else {
                                        release()
                                    }
                                    pointer.consume()
                                } while (pointer.pressed)
                            }
                        } finally {
                            release()
                        }
                    }
                },
        ) {
            val elapsed = if (frameTime >= 0L) timeMs else 0L
            draw()
            drawTimerBorder(
                progress = (elapsed.toFloat() / GameDurationMillis).coerceIn(0f, 1f),
                color = when (winState) {
                    WinState.Playing if timeMs >= GameDurationMillis -> {
                        Palette.Red.color
                    }

                    WinState.Playing -> Palette.Yellow.color
                    WinState.Won -> Palette.Green.color
                    WinState.Lost -> Palette.Red.color
                },
            )
        }
    }

    fun DrawScope.draw() {
        val frame = gameFrame(size)
        if (frame.scale <= 0f) return

        updateBitmapCache(frame.scale)
        cachedBackground?.let { drawImage(it, topLeft = frame.origin) }
        clipRect(
            left = frame.rect.left,
            top = frame.rect.top,
            right = frame.rect.right,
            bottom = frame.rect.bottom,
        ) {
            objects.forEachIndexed { index, objectState ->
                drawImage(
                    scaledBitmaps[index],
                    topLeft = frame.origin + objectState.offset * frame.scale,
                )
            }
        }
    }

    private fun gameLocation(location: Offset, canvasSize: IntSize): Offset? {
        val frame = gameFrame(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()))
        if (frame.scale <= 0f || !frame.rect.contains(location)) return null
        return (location - frame.origin) / frame.scale
    }

    private fun updateBitmapCache(scale: Float) {
        if (cachedScale == scale) return
        cachedScale = scale
        scaledBitmaps = objects.map { scaledBitmap(it.bitmap, scale) }
    }

    private fun scaledBitmap(bitmap: ImageBitmap, scale: Float): ImageBitmap {
        return scaled(
            bitmap = bitmap,
            width = (bitmap.width * scale).toInt().coerceAtLeast(1),
            height = (bitmap.height * scale).toInt().coerceAtLeast(1),
        )
    }

    companion object {
        internal const val GameDurationMillis = 3_000L
        private const val MaxSimulationStepMillis = 4L
    }
}

private data class Overlap(val first: ObjectState, val second: ObjectState) {
    fun joins(target: ObjectState, other: TagFilter): Boolean =
        first === target && second.matches(other) || second === target && first.matches(other)
}

private data class GameFrame(
    val origin: Offset,
    val size: Size,
    val scale: Float,
) {
    val rect: Rect get() = Rect(origin, size)
}

private fun gameFrame(canvasSize: Size): GameFrame {
    val scale = min(
        canvasSize.width / WorldWidth,
        canvasSize.height / WorldHeight,
    )
    val renderedSize = Size(WorldWidth * scale, WorldHeight * scale)
    return GameFrame(
        origin = Offset(
            (canvasSize.width - renderedSize.width) / 2f,
            (canvasSize.height - renderedSize.height) / 2f,
        ),
        size = renderedSize,
        scale = scale,
    )
}

private fun DrawScope.drawTimerBorder(progress: Float, color: Color) {
    val frame = gameFrame(size)
    if (frame.scale <= 0f) return

    val strokeWidth = 4.dp.toPx()
    val left = frame.rect.left + strokeWidth / 2f
    val top = frame.rect.top + strokeWidth / 2f
    val right = frame.rect.right - strokeWidth / 2f
    val bottom = frame.rect.bottom - strokeWidth / 2f
    val points = listOf(
        Offset((left + right) / 2f, top),
        Offset(left, top),
        Offset(left, bottom),
        Offset(right, bottom),
        Offset(right, top),
        Offset((left + right) / 2f, top),
    )
    val lengths = points.zipWithNext { start, end -> (end - start).getDistance() }

    for ((start, end) in points.zipWithNext()) {
        drawLine(
            color = Palette.Black.color,
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
        )
    }

    var remaining = lengths.sum() * progress
    for (index in lengths.indices) {
        if (remaining <= 0f) break
        val start = points[index]
        val end = points[index + 1]
        val length = lengths[index]
        val segmentProgress = (remaining / length).coerceAtMost(1f)
        drawLine(
            color = color,
            start = start,
            end = start + (end - start) * segmentProgress,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
        )
        remaining -= length
    }
}

private fun dirVec(direction: Direction): Offset = when (direction) {
    Direction.N -> Offset(0f, -1f)
    Direction.E -> Offset(1f, 0f)
    Direction.S -> Offset(0f, 1f)
    Direction.W -> Offset(-1f, 0f)
}

class ObjectState(
    val bitmap: ImageBitmap,
    var offset: Offset,
    val tags: Set<LogicTag> = emptySet(),
    var velocity: Offset = Offset.Zero,
    var destination: Offset? = null,
) {
    fun rect(): Rect = Rect(offset, Size(bitmap.width.toFloat(), bitmap.height.toFloat()))

    fun matches(filter: TagFilter): Boolean = filter.matches(tags)
}

enum class WinState { Won, Playing, Lost }
