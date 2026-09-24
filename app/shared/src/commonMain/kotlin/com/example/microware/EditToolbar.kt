package com.example.microware

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.PersistentList

sealed interface ExpandedSide<Data> {
    val edge: HandleEdge
    val data: Data? get() = null

    class Trash<Data> : ExpandedSide<Data> {
        override val edge = HandleEdge.Left
    }

    class Editor<Data>(override val data: Data) : ExpandedSide<Data> {
        override val edge = HandleEdge.Bottom
    }

    class Library<Data> : ExpandedSide<Data> {
        override val edge = HandleEdge.Right
    }
}

@Composable
fun <T : Identifiable<T>, Data, Expanded> BoxWithConstraintsScope.Toolbar(
    dragTarget: DragTarget<T>,
    dragSource: DragSource<T>?,
    willBeDeleted: (Boolean) -> Unit,
    onLibraryDrop: (T) -> Unit,
    fromEdge: (HandleEdge?, (Expanded) -> Unit) -> Expanded?,
    dropOnEditor: (T, (Expanded) -> Unit) -> Expanded,
    expandToEdge: (Expanded) -> ExpandedSide<Data>,
    handler: ((PersistentList<T>) -> PersistentList<T>) -> Unit,
    garbagePanel: @Composable (DragSource<T>, List<T>) -> Unit,
    editorPanel: @Composable (Data) -> Unit,
    libraryPanel: @Composable (DragSource<T>) -> Unit,
) {
    var trash by remember { mutableStateOf(Trash<T>()) }
    val offer = dragTarget.offer

    val (expanded, setExpanded) = remember { mutableStateOf<Expanded?>(null) }.apply {
        if (offer != null) value = null
    }
    val expandedSide = expanded?.let { expandToEdge(it) }

    val trashState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragTarget,
            onEnter = { willBeDeleted(true) },
            onExit = { willBeDeleted(false) },
            onEnd = { willBeDeleted(false) },
            onGiven = { offer ->
                handler { all ->
                    val logic = offer.item
                    if (!offer.originatesFromGrid(LogicTrashDragSource)) {
                        trash = trash.discard(logic)
                    }
                    all.indexOfFirst { it.id == logic.id }
                        .takeIf { it >= 0 }
                        ?.let { all.removingAt(it) }
                        ?: all
                }
            }
        )

        dragSource == null -> null

        expandedSide is ExpandedSide.Trash -> DrawerHandleState.Expanded(
            expandedSide.edge,
            dragSource
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        state = trashState,
        setExpanded = { setExpanded(fromEdge(it, setExpanded)) },
        side = HandleEdge.Left,
        foreground = Palette.Maroon,
        highlightForeground = Palette.Red,
        hiddenSlide = if (trash.garbage.isNotEmpty()) 16 else 500,
        buttonContent = { TrashControls(it) },
        drawerContent = { garbagePanel(it, trash.garbage) },
    )

    val createState = when {
        offer != null -> DrawerHandleState.Dragging(dragTarget, onGiven = { offer ->
            setExpanded(dropOnEditor(offer.item, setExpanded))
        })

        expandedSide is ExpandedSide.Editor -> DrawerHandleState.Expanded(
            expandedSide.edge,
            expandedSide.data
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        state = createState,
        setExpanded = { setExpanded(fromEdge(it, setExpanded)) },
        side = HandleEdge.Bottom,
        foreground = Palette.Navy,
        highlightForeground = Palette.Cyan,
        buttonContent = {
            CreateTarget(
                foreground = it,
                isEditMode = dragTarget.offer != null,
            )
        },
    ) { Column(Modifier.fillMaxSize()) { editorPanel(it) } }

    val libraryState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragTarget,
            onGiven = { onLibraryDrop(it.item) },
        )

        dragSource == null -> null

        expandedSide is ExpandedSide.Library -> DrawerHandleState.Expanded(
            expandedSide.edge,
            dragSource
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)

        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        state = libraryState,
        setExpanded = { setExpanded(fromEdge(it, setExpanded)) },
        side = HandleEdge.Right,
        foreground = Palette.Forest,
        highlightForeground = Palette.Green,
        buttonContent = { LibraryTarget(it) },
        drawerContent = { libraryPanel(it) },
    )
}