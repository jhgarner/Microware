package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private const val LibraryStorageKey = "microware.library.v1"

data class Library(val objects: List<Obj>) {
    constructor() : this(emptyList())

    fun add(obj: Obj): Library = copy(objects = objects + obj.copy(id = nextId()))

    fun remove(id: Int): Library = copy(objects = objects.filterNot { it.id == id })

    private fun nextId(): Int = (objects.maxOfOrNull { it.id } ?: -1) + 1
}

@Serializable
private data class StoredLibrary(val objects: List<StoredObj> = emptyList())

@Serializable
private data class StoredObj(
    val id: Int,
    val offsetX: Float,
    val offsetY: Float,
    val width: Int,
    val height: Int,
    val pixels: List<Int>,
)

private val libraryJson = Json { ignoreUnknownKeys = true }

fun loadLibrary(): Library {
    val encoded = AppStorage.read(LibraryStorageKey) ?: return Library()
    return runCatching {
        val stored = libraryJson.decodeFromString<StoredLibrary>(encoded)
        Library(stored.objects.map { it.toObj() })
    }.getOrDefault(Library())
}

fun saveLibrary(library: Library) {
    val stored = StoredLibrary(library.objects.map { it.toStoredObj() })
    AppStorage.write(LibraryStorageKey, libraryJson.encodeToString(stored))
}

private fun Obj.toStoredObj(): StoredObj = StoredObj(
    id = id,
    offsetX = offset.x,
    offsetY = offset.y,
    width = sprite.width,
    height = sprite.height,
    pixels = sprite.rows.flatMap { row -> row.map { it.ordinal } }
)

private fun StoredObj.toObj(): Obj {
    require(width > 0 && height > 0)
    require(pixels.size == width * height)
    val palette = Palette.entries
    val rows = pixels.chunked(width).map { row ->
        row.map { palette.getOrNull(it) ?: error("Unknown palette value: $it") }
            .toPersistentList()
    }.toPersistentList()
    return Obj(
        id = id,
        offset = Offset(offsetX, offsetY),
        sprite = Sprite(rows)
    )
}

@Composable
fun LibraryControls(
    offered: Offer?,
    onGiven: (Obj) -> Unit,
    isExpanded: Boolean,
    availableWidth: Float,
    onClick: () -> Unit,
    onBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDragging = offered != null
    val density = LocalDensity.current
    val openSlide = -(
            availableWidth - with(density) { HandleSize.toPx() }
            )
    val hiddenLibrarySlide = 16f
    val librarySlideX = remember { Animatable(hiddenLibrarySlide) }
    LaunchedEffect(isDragging, isExpanded, openSlide) {
        librarySlideX.animateTo(
            targetValue = when {
                isExpanded -> openSlide
                isDragging -> 0f
                else -> hiddenLibrarySlide
            },
            animationSpec = tween(durationMillis = 180)
        )
    }
    LibraryTarget(
        offered = offered,
        onGiven = onGiven,
        onClick = onClick,
        onBoundsChanged = onBounds,
        modifier = modifier
            .offset { IntOffset(librarySlideX.value.roundToInt(), 0) }
    )
}

@Composable
private fun LibraryTarget(
    offered: Offer?,
    onGiven: (Obj) -> Unit,
    onClick: () -> Unit,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var state: TrashState by remember { mutableStateOf(TrashState.Closed) }
    val foreground = if (state == TrashState.Hovered) {
        Palette.Green
    } else {
        Palette.Forest
    }.color

    ButtonHandle(
        foreground = foreground,
        openSide = Side.Right,
        onClick = onClick,
        onBoundsChanged = onBoundsChanged,
        modifier = modifier
            .dropTarget(
                offered,
                onStart = { state = TrashState.Open },
                onEnter = { state = TrashState.Hovered },
                onExit = { state = TrashState.Open },
                onGiven = { onGiven(it.obj) },
                onEnd = { state = TrashState.Closed })
    ) { foreground ->
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 2.dp.toPx()
            val left = size.width * 0.18f
            val right = size.width * 0.82f
            val top = size.height * 0.2f
            val middle = size.height * 0.5f
            val bottom = size.height * 0.8f
            drawLine(
                color = foreground,
                start = Offset(left, top),
                end = Offset(right, top),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = foreground,
                start = Offset(left, middle),
                end = Offset(right, middle),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = foreground,
                start = Offset(left, bottom),
                end = Offset(right, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = foreground,
                start = Offset(left, top),
                end = Offset(left, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = foreground,
                start = Offset(right, top),
                end = Offset(right, bottom),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
