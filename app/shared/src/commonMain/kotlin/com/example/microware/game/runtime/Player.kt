package com.example.microware.game.runtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.World
import com.example.microware.game.model.WorldHeight
import com.example.microware.game.model.WorldWidth
import com.example.microware.game.sprite.scaled
import com.example.microware.ui.layout.insets
import kotlin.math.min

data class Player(
    val logics: List<Logic>,
    val objects: MutableMap<LogicTag, ObjectState> = mutableMapOf(),
    val scenery: List<ObjectState> = emptyList(),
    var timeMs: Long = 0L,
    var winState: WinState = WinState.Playing,
    var colliding: Set<Colliding> = setOf(),
) {
    private var cachedScale = Float.NaN
    private var cachedBackground: ImageBitmap? = null
    private val cachedObjects = mutableMapOf<LogicTag, CachedBitmap>()
    private val cachedScenery = mutableListOf<CachedBitmap>()

    constructor(game: Game) : this(
        logics = game.logics,
        objects = worldObj(game.world),
        scenery = worldScenery(game.world),
    )

    init {
        for (logic in logics) {
            if (logic.trigger is Trigger.Start) {
                logic.actions.forEach { runAction(it) }
            }
        }
    }

    fun step(delta: Long) {
        if (delta <= 0L || timeMs >= GameDurationMillis) return

        val elapsed = delta.coerceAtMost(GameDurationMillis - timeMs)
        move(elapsed)

        val newColliding = mutableSetOf<Colliding>()
        val objectEntries = objects.entries.toList()
        for (firstIndex in objectEntries.indices) {
            val (tag, obj) = objectEntries[firstIndex]
            for (secondIndex in firstIndex + 1 until objectEntries.size) {
                val (tag2, obj2) = objectEntries[secondIndex]
                if (obj.rect().overlaps(obj2.rect())) {
                    newColliding += Colliding.new(tag, tag2)
                }
            }
        }

        for (logic in logics) {
            if (logic.trigger is Trigger.Collision) {
                val trigger = Colliding.new(logic.trigger)
                if (trigger in newColliding && trigger !in colliding) {
                    logic.actions.forEach { runAction(it) }
                }
            }
        }

        colliding = newColliding
        timeMs += elapsed
    }

    fun tap(location: Offset) {
        if (timeMs >= GameDurationMillis) return

        for (logic in logics) {
            if (logic.trigger is Trigger.Tap) {
                objects[logic.trigger.target]?.let { obj ->
                    if (obj.rect().contains(location)) {
                        logic.actions.forEach { runAction(it) }
                    }
                }
            }
        }
    }

    private fun move(delta: Long) {
        val seconds = delta / 1000f
        for (objectState in objects.values) {
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

    private fun runAction(action: Action) {
        when (action) {
            Action.Win -> winState = WinState.Won
            Action.Lose -> winState = WinState.Lost
            is Action.Move -> {
                objects[action.mover]?.apply {
                    velocity = dirVec(action.target) * action.speed.toFloat()
                    destination = null
                }
            }

            is Action.MoveTo -> {
                val mover = objects[action.mover] ?: return
                val target = objects[action.target] ?: return
                val difference = target.offset - mover.offset
                val distance = difference.getDistance()

                if (action.speed == null) {
                    mover.offset = target.offset
                    mover.velocity = Offset.Zero
                    mover.destination = null
                } else if (distance == 0f) {
                    mover.velocity = Offset.Zero
                    mover.destination = null
                } else {
                    mover.velocity = difference / distance * action.speed.toFloat()
                    mover.destination = target.offset
                }
            }
        }
    }

    @Composable
    fun PlayGame(active: Boolean = true) {
        var frameTime by remember(this@Player) { mutableStateOf(0L) }
        var canvasSize by remember(this@Player) { mutableStateOf(IntSize.Zero) }

        LaunchedEffect(this@Player, active) {
            if (!active) return@LaunchedEffect

            var previousFrame = withFrameMillis { it }
            frameTime = previousFrame

            while (timeMs < GameDurationMillis) {
                val currentFrame = withFrameMillis { it }
                val delta = (currentFrame - previousFrame).coerceAtLeast(0L)
                step(delta)
                previousFrame = currentFrame
                frameTime = currentFrame
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.leftDp(),
                    top = insets.topDp(),
                    end = insets.rightDp(),
                    bottom = insets.bottomDp(),
                )
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(this@Player, canvasSize, active) {
                    if (active) {
                        detectTapGestures { location ->
                            gameLocation(location, canvasSize)?.let(::tap)
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
            for (objectState in scenery) {
                cachedScenery.firstOrNull { it.source === objectState.bitmap }?.bitmap?.let { bitmap ->
                    drawImage(
                        bitmap,
                        topLeft = frame.origin + objectState.offset * frame.scale,
                    )
                }
            }
            for ((tag, objectState) in objects) {
                cachedObjects[tag]?.bitmap?.let { bitmap ->
                    drawImage(
                        bitmap,
                        topLeft = frame.origin + objectState.offset * frame.scale,
                    )
                }
            }
        }
    }

    private fun gameLocation(location: Offset, canvasSize: IntSize): Offset? {
        val frame = gameFrame(Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()))
        if (frame.scale <= 0f || !frame.rect.contains(location)) return null
        return (location - frame.origin) / frame.scale
    }

    private fun updateBitmapCache(scale: Float) {
        if (cachedScale != scale) {
            cachedScale = scale
            cachedObjects.clear()
            cachedScenery.clear()
        }

        cachedObjects.keys.retainAll(objects.keys)
        for ((tag, objectState) in objects) {
            val cached = cachedObjects[tag]
            if (cached == null || cached.source !== objectState.bitmap) {
                cachedObjects[tag] = CachedBitmap(
                    source = objectState.bitmap,
                    bitmap = scaledBitmap(objectState.bitmap, scale),
                )
            }
        }

        for (objectState in scenery) {
            if (cachedScenery.none { it.source === objectState.bitmap }) {
                cachedScenery += CachedBitmap(
                    source = objectState.bitmap,
                    bitmap = scaledBitmap(objectState.bitmap, scale),
                )
            }
        }
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

        private fun worldObj(world: World): MutableMap<LogicTag, ObjectState> {
            val result = mutableMapOf<LogicTag, ObjectState>()
            val objById = world.objects.associate {
                it.id to ObjectState(
                    it.sprite.bitmap,
                    it.offset,
                    Offset.Zero,
                )
            }
            for ((tag, id) in world.logicTags) {
                objById[id]?.let { result[tag] = it }
            }
            return result
        }

        private fun worldScenery(world: World): List<ObjectState> {
            val taggedObjectIds = world.logicTags.values.toSet()
            return world.objects
                .filterNot { it.id in taggedObjectIds }
                .map { ObjectState(it.sprite.bitmap, it.offset, Offset.Zero) }
        }
    }
}

private data class CachedBitmap(val source: ImageBitmap, val bitmap: ImageBitmap)

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

data class ObjectState(
    val bitmap: ImageBitmap,
    var offset: Offset,
    var velocity: Offset,
    var destination: Offset? = null,
) {
    fun rect(): Rect = Rect(offset, Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
}

enum class WinState { Won, Playing, Lost }

@ConsistentCopyVisibility
data class Colliding private constructor(val a: LogicTag, val b: LogicTag) {
    fun matches(trigger: Trigger.Collision): Boolean =
        new(trigger.firstTarget, trigger.secondTarget) == this

    companion object {
        fun new(a: LogicTag, b: LogicTag): Colliding =
            if (a.tag < b.tag) Colliding(a, b) else Colliding(b, a)

        fun new(trigger: Trigger.Collision): Colliding =
            new(trigger.firstTarget, trigger.secondTarget)
    }
}
