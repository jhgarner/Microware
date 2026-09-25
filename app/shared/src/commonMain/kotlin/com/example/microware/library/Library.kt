package com.example.microware.library
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Obj
import com.example.microware.game.model.Trigger
import com.example.microware.game.sprite.Palette
import com.example.microware.game.sprite.Sprite
import com.example.microware.game.sprite.SpriteCodec
import com.example.microware.game.sprite.StoredSprite
import com.example.microware.platform.AppStorage
import com.example.microware.util.Identifiable
import com.example.microware.util.toPx

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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val ObjLibraryStorageKey = "microware.library.obj.v1"
private const val LogicLibraryStorageKey = "microware.library.logic.v1"

data class Library<T : Identifiable<T>>(
    val items: List<T> = emptyList(),
) {
    fun add(item: T): Library<T> = copy(items = items + item.newId())
}

@Serializable
private data class ObjStoredLibrary(
    val objects: List<StoredObj> = emptyList(),
)

@Serializable
private data class LogicStoredLibrary(
    val logics: List<StoredLogic> = emptyList(),
)


@Serializable
private data class StoredObj(
    val offsetX: Float,
    val offsetY: Float,
    val sprite: StoredSprite? = null,
    val width: Int? = null,
    val height: Int? = null,
    val pixels: List<Int>? = null,
)

@Serializable
private data class StoredLogic(
    val trigger: StoredTrigger,
    val actions: List<StoredAction>,
)

@Serializable
private data class StoredTrigger(
    val type: String,
    val firstTag: Int? = null,
    val secondTag: Int? = null,
)

@Serializable
private data class StoredAction(
    val type: String,
    val moverTag: Int? = null,
    val targetTag: Int? = null,
    val direction: String? = null,
    val speed: Int? = null,
)

private val libraryJson = Json { ignoreUnknownKeys = true }

fun loadObjLibrary(): Library<Obj> {
    val encoded = AppStorage.read(ObjLibraryStorageKey) ?: return Library()
    return runCatching {
        val stored = libraryJson.decodeFromString<ObjStoredLibrary>(encoded)
        Library(
            items = stored.objects.map { it.toObj() },
        )
    }.getOrDefault(Library())
}

fun loadLogicLibrary(): Library<Logic> {
    val encoded = AppStorage.read(LogicLibraryStorageKey) ?: return Library()
    return runCatching {
        val stored = libraryJson.decodeFromString<LogicStoredLibrary>(encoded)
        Library(
            items = stored.logics.map { it.toLogic() },
        )
    }.getOrDefault(Library())
}

fun saveObjLibrary(library: Library<Obj>) {
    val stored = ObjStoredLibrary(
        objects = library.items.map { it.toStoredObj() },
    )
    AppStorage.write(ObjLibraryStorageKey, libraryJson.encodeToString(stored))
}

fun saveLogicLibrary(library: Library<Logic>) {
    val stored = LogicStoredLibrary(
        logics = library.items.map { it.toStoredLogic() },
    )
    AppStorage.write(LogicLibraryStorageKey, libraryJson.encodeToString(stored))
}

private fun Obj.toStoredObj(): StoredObj = StoredObj(
    offsetX = offset.x,
    offsetY = offset.y,
    sprite = SpriteCodec.encode(sprite),
)

private fun StoredObj.toObj(): Obj {
    val decodedSprite = sprite?.let(SpriteCodec::decode) ?: decodeLegacySprite()
    return Obj(
        offset = Offset(offsetX, offsetY),
        sprite = decodedSprite,
    )
}

private fun StoredObj.decodeLegacySprite(): Sprite {
    val legacyWidth = width ?: error("Missing sprite width")
    val legacyHeight = height ?: error("Missing sprite height")
    val legacyPixels = pixels ?: error("Missing sprite pixels")
    require(legacyWidth > 0 && legacyHeight > 0)
    require(legacyPixels.size == legacyWidth * legacyHeight)
    val codes = ByteArray(legacyPixels.size) { index ->
        val code = legacyPixels[index]
        Palette.fromCode(code)
        code.toByte()
    }
    return Sprite.fromPaletteCodes(legacyWidth, legacyHeight, codes)
}

private fun Logic.toStoredLogic(): StoredLogic = StoredLogic(
    trigger = trigger.toStoredTrigger(),
    actions = actions.map { it.toStoredAction() },
)

private fun Trigger.toStoredTrigger(): StoredTrigger = when (this) {
    Trigger.Start -> StoredTrigger(type = "start")
    is Trigger.Tap -> StoredTrigger(type = "tap", firstTag = target.tag)
    is Trigger.Collision -> StoredTrigger(
        type = "collision",
        firstTag = firstTarget.tag,
        secondTag = secondTarget.tag,
    )
}

private fun Action.toStoredAction(): StoredAction = when (this) {
    is Action.Move -> StoredAction(
        type = "move",
        moverTag = mover.tag,
        direction = target.name,
        speed = speed,
    )

    is Action.MoveTo -> StoredAction(
        type = "moveTo",
        moverTag = mover.tag,
        targetTag = target.tag,
        speed = speed,
    )

    Action.Win -> StoredAction(type = "win")
    Action.Lose -> StoredAction(type = "lose")
}

private fun StoredLogic.toLogic(): Logic = Logic(
    trigger = trigger.toTrigger(),
    actions = actions.map { it.toAction() },
)

private fun StoredTrigger.toTrigger(): Trigger = when (type) {
    "start" -> Trigger.Start
    "tap" -> Trigger.Tap(LogicTag(requireNotNull(firstTag)))
    "collision" -> Trigger.Collision(
        firstTarget = LogicTag(requireNotNull(firstTag)),
        secondTarget = LogicTag(requireNotNull(secondTag)),
    )

    else -> error("Unknown trigger type: $type")
}

private fun StoredAction.toAction(): Action = when (type) {
    "move" -> Action.Move(
        mover = LogicTag(requireNotNull(moverTag)),
        target = Direction.valueOf(requireNotNull(direction)),
        speed = requireNotNull(speed),
    )

    "moveTo" -> Action.MoveTo(
        mover = LogicTag(requireNotNull(moverTag)),
        target = LogicTag(requireNotNull(targetTag)),
        speed = speed,
    )

    "win" -> Action.Win
    "lose" -> Action.Lose
    else -> error("Unknown action type: $type")
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
