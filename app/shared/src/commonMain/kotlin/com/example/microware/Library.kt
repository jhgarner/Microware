package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
    val pixels: List<Int>
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
    isActive: Boolean,
    isDragging: Boolean,
    isOpen: Boolean,
    onClick: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TrashGap)
    ) {
        val density = LocalDensity.current
        val openSlide = -(
                with(density) { maxWidth.toPx() } - with(density) { HandleSize.toPx() }
                )
        val hiddenLibrarySlide = 16f
        val librarySlideX = remember { Animatable(hiddenLibrarySlide) }
        LaunchedEffect(isDragging, isOpen, openSlide) {
            librarySlideX.animateTo(
                targetValue = when {
                    isOpen -> openSlide
                    isDragging -> 0f
                    else -> hiddenLibrarySlide
                },
                animationSpec = tween(durationMillis = 180)
            )
        }
        LibraryTarget(
            isActive = isActive,
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(librarySlideX.value.roundToInt(), 0) }
                .onGloballyPositioned { coordinates ->
                    onBoundsChanged(coordinates.boundsInRoot())
                }
        )
    }
}

@Composable
private fun LibraryTarget(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(
        topStart = 8.dp,
        topEnd = 0.dp,
        bottomEnd = 0.dp,
        bottomStart = 8.dp
    )
    val foreground = if (isActive) {
        Palette.Green
    } else {
        Palette.Forest
    }.color

    Box(
        modifier = modifier
            .size(HandleSize)
            .shadow(OverlayShadowElevation, shape)
            .clickable(onClick = onClick)
            .background(Palette.OffWhite.color, shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val inset = strokeWidth / 2f
            val right = size.width - inset
            val bottom = size.height - inset
            val radius = 8.dp.toPx().coerceAtMost(size.height / 2f)
            val borderPath = Path().apply {
                moveTo(right, inset)
                lineTo(inset + radius, inset)
                quadraticTo(inset, inset, inset, inset + radius)
                lineTo(inset, bottom - radius)
                quadraticTo(inset, bottom, inset + radius, bottom)
                lineTo(right, bottom)
            }
            drawPath(
                path = borderPath,
                color = foreground,
                style = Stroke(strokeWidth)
            )
        }
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
