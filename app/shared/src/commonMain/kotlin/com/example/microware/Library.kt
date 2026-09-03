package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
fun LibraryTarget(foreground: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        val strokeWidth = 2.dp.toPx()
        val bookWidth = size.width * .18f
        val bottom = size.height * .88f
        val cornerRadius = CornerRadius(strokeWidth * 1.5f)

        fun drawBook(centerX: Float, top: Float, rotation: Float = 0f) {
            rotate(rotation, pivot = Offset(centerX, bottom)) {
                drawRoundRect(
                    color = foreground,
                    topLeft = Offset(centerX - bookWidth / 2f, top),
                    size = Size(bookWidth, bottom - top),
                    cornerRadius = cornerRadius,
                    style = Stroke(strokeWidth),
                )
            }
        }

        drawBook(size.width * .86f, size.height * .23f, rotation = -10f)
        drawBook(size.width * .18f, size.height * .16f)
        drawBook(size.width * .38f, size.height * .22f)
        drawBook(size.width * .58f, size.height * .14f)
    }
}
