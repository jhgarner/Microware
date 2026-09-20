package com.example.microware

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

private var library by mutableStateOf(loadObjLibrary())

private sealed interface Expanded {
    val edge: HandleEdge

    data object Trash : Expanded {
        override val edge = HandleEdge.Left
    }

    data class Editor(val obj: Obj) : Expanded {
        override val edge = HandleEdge.Bottom
    }

    data object Library : Expanded {
        override val edge = HandleEdge.Right
    }

    companion object {
        fun fromEdge(edge: HandleEdge?): Expanded? = when (edge) {
            null -> null
            HandleEdge.Left -> Trash
            HandleEdge.Bottom -> Editor(Obj(20, 20))
            HandleEdge.Right -> Library
        }
    }
}

@Composable
fun BoxWithConstraintsScope.WorldEditToolbar(
    dragTarget: DragTarget<Obj>,
    dragSource: DragSource<Obj>?,
    world: World,
    handler: (WorldEvent) -> Unit,
    willBeDeleted: MutableState<Boolean>,
) {
    var trash by remember { mutableStateOf(Trash()) }
    val offer = dragTarget.offer

    val (expandedSide, setExpanded) = remember { mutableStateOf<Expanded?>(null) }.apply {
        if (offer != null) value = null
    }

    val trashState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragTarget,
            onEnter = { willBeDeleted.value = true },
            onExit = { willBeDeleted.value = false },
            onEnd = { willBeDeleted.value = false },
            onGiven = { offer ->
                trash = trash.discard(offer.item)
                handler { world -> world.deleteObject(offer.item.id) }
            }
        )

        expandedSide is Expanded.Trash && dragSource != null -> DrawerHandleState.Expanded(
            expandedSide.edge,
            dragSource
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        dragSource != null -> DrawerHandleState.Waiting()
        else -> null
    }
    DrawerWithHandle(
        state = trashState,
        setExpanded = { setExpanded(Expanded.fromEdge(it)) },
        side = HandleEdge.Left,
        foreground = Palette.Maroon,
        highlightForeground = Palette.Red,
        hiddenSlide = if (trash.garbage.isNotEmpty()) 16 else 500,
        buttonContent = { TrashControls(it) },
        drawerContent = {
            ObjectTable(
                items = trash.garbage,
                dragSource = it,
                dragTarget = dragTarget
            )
        },
    )

    val createState = when {
        offer != null -> DrawerHandleState.Dragging(dragTarget, onGiven = { offer ->
            setExpanded(Expanded.Editor(offer.item))
        })

        expandedSide is Expanded.Editor -> DrawerHandleState.Expanded(
            expandedSide.edge,
            expandedSide.obj
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        state = createState,
        side = HandleEdge.Bottom,
        setExpanded = { setExpanded(Expanded.fromEdge(it)) },
        foreground = Palette.Navy,
        highlightForeground = Palette.Cyan,
        buttonContent = { CreateTarget(foreground = it, isEditMode = dragTarget.offer != null) }
    ) { obj ->
        Column(modifier = Modifier.fillMaxSize()) {
            EditableCanvas(
                sprite = obj.sprite,
                commit = { committedSprite ->
                    val newObj = obj.copy(sprite = committedSprite)
                    setExpanded(Expanded.Editor(newObj))
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
                    setExpanded(Expanded.Editor(world.clamped(newObj)))
                    handler { world -> world.updateObject(newObj) }
                },
                logicTag = world.logicTagFor(obj.id),
                onLogicTagChange = { logicTag ->
                    handler { world -> world.assignLogicTag(obj.id, logicTag) }
                },
                modifier = Modifier.weight(1f),
                editorKey = obj.id,
            )
        }
    }

    val libraryState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragTarget,
            onEnter = { willBeDeleted.value = true },
            onExit = { willBeDeleted.value = false },
            onEnd = { willBeDeleted.value = false },
            onGiven = { offer ->
                library = library.add(offer.obj)
                saveObjLibrary(library)
            },
        )

        expandedSide is Expanded.Library && dragSource != null -> DrawerHandleState.Expanded(
            expandedSide.edge,
            dragSource
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        dragSource != null -> DrawerHandleState.Waiting()
        else -> null
    }
    DrawerWithHandle(
        state = libraryState,
        setExpanded = { setExpanded(Expanded.fromEdge(it)) },
        side = HandleEdge.Right,
        foreground = Palette.Forest,
        highlightForeground = Palette.Green,
        buttonContent = { LibraryTarget(it) },
        drawerContent = {
            ObjectTable(
                items = library.items,
                dragSource = it,
                dragTarget = dragTarget
            )
        },
    )
}
