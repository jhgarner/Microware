package com.example.microware.editor.world
import com.example.microware.editor.ExpandedSide
import com.example.microware.editor.Toolbar
import com.example.microware.editor.sprite.EditableCanvas
import com.example.microware.game.events.WorldEvent
import com.example.microware.game.model.Obj
import com.example.microware.game.model.World
import com.example.microware.library.Library
import com.example.microware.library.loadObjLibrary
import com.example.microware.library.saveObjLibrary
import com.example.microware.ui.components.HandleEdge
import com.example.microware.ui.components.Trash
import com.example.microware.ui.gestures.DragSource
import com.example.microware.ui.gestures.DragTarget
import com.example.microware.ui.gestures.dragSource

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

private var library by mutableStateOf(loadObjLibrary())

private sealed interface Expanded {
    fun edge(): ExpandedSide<ObjUpdater>

    data object Trash : Expanded {
        override fun edge() = ExpandedSide.Trash<ObjUpdater>()
    }

    data class Editor(val obj: Obj, val setExpanded: (Expanded) -> Unit) : Expanded {
        override fun edge() = ExpandedSide.Editor(ObjUpdater(obj, setExpanded))
    }

    data object Library : Expanded {
        override fun edge() = ExpandedSide.Library<ObjUpdater>()
    }

    companion object {
        fun fromEdge(edge: HandleEdge?, setExpanded: (Expanded) -> Unit): Expanded? = when (edge) {
            null -> null
            HandleEdge.Left -> Trash
            HandleEdge.Bottom -> Editor(Obj(20, 20), setExpanded)
            HandleEdge.Right -> Library
        }
    }
}

private data class ObjUpdater(val obj: Obj, val setExpanded: (Expanded) -> Unit)

@Composable
fun BoxWithConstraintsScope.WorldEditToolbar(
    dragTarget: DragTarget<Obj>,
    dragSource: DragSource<Obj>?,
    world: World,
    handler: (WorldEvent) -> Unit,
    willBeDeleted: MutableState<Boolean>,
) {
    Toolbar(
        dragTarget = dragTarget,
        dragSource = dragSource,
        willBeDeleted = { willBeDeleted.value = it },
        onLibraryDrop = { library = library.add(it); saveObjLibrary(library) },
        fromEdge = Expanded::fromEdge,
        dropOnEditor = Expanded::Editor,
        expandToEdge = { it.edge() },
        handler = { action -> handler { world -> world.copy(objects = action(world.objects)) } },
        garbagePanel = { source, garbage ->
            ObjectTable(
                items = garbage,
                dragSource = source,
                dragTarget = dragTarget
            )
        },
        editorPanel = { objUpdater ->
            val obj = objUpdater.obj
            val setExpanded = objUpdater.setExpanded
            EditableCanvas(
                sprite = obj.sprite,
                commit = { committedSprite ->
                    val newObj = obj.copy(sprite = committedSprite)
                    setExpanded(Expanded.Editor(newObj, setExpanded))
                    handler { world -> world.upsertObject(newObj) }
                },
                resize = { bounds ->
                    val newObj = obj.copy(
                        offset = obj.offset + Offset(
                            bounds.left.toFloat(),
                            bounds.top.toFloat(),
                        ),
                        sprite = obj.sprite.resized(bounds),
                    )
                    setExpanded(Expanded.Editor(world.clamped(newObj), setExpanded))
                    handler { world -> world.updateObject(newObj) }
                },
                logicTag = world.logicTagFor(obj.id),
                onLogicTagChange = { logicTag ->
                    handler { world -> world.assignLogicTag(obj.id, logicTag) }
                },
                editorKey = obj.id,
            )
        },
        libraryPanel = {
            ObjectTable(
                items = library.items,
                dragSource = it,
                dragTarget = dragTarget
            )
        }
    )
}
