package com.example.microware.editor

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.microware.ui.components.DrawerHandleState
import com.example.microware.ui.components.DrawerWithHandle
import com.example.microware.ui.components.HandleEdge
import com.example.microware.ui.components.HandleType
import com.example.microware.ui.components.TrashHandle
import com.example.microware.ui.gestures.DragSource
import com.example.microware.ui.gestures.DragTarget
import com.example.microware.util.Identifiable
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
    trashSourceId: Any = Unit,
    fromEdge: (HandleEdge?, (Expanded) -> Unit) -> Expanded?,
    dropOnEditor: (T, (Expanded) -> Unit) -> Expanded,
    expandToEdge: (Expanded) -> ExpandedSide<Data>,
    handler: ((PersistentList<T>) -> PersistentList<T>) -> Unit,
    garbagePanel: @Composable (DragSource<T>, List<T>) -> Unit,
    editorPanel: @Composable (Data) -> Unit,
    libraryPanel: @Composable (DragSource<T>) -> Unit,
) {
    val offer = dragTarget.offer

    val (expanded, setExpanded) = remember { mutableStateOf<Expanded?>(null) }.apply {
        if (offer != null) value = null
    }
    val expandedSide = expanded?.let { expandToEdge(it) }

    TrashHandle(
        dragTarget = dragTarget,
        dragSource = dragSource,
        trashSourceId = trashSourceId,
        activeEdge = expandedSide?.edge,
        setExpanded = { setExpanded(fromEdge(it, setExpanded)) },
        willBeDeleted = willBeDeleted,
        onDelete = { item ->
            handler { all ->
                all.indexOfFirst { it.id == item.id }
                    .takeIf { it >= 0 }
                    ?.let { all.removingAt(it) }
                    ?: all
            }
        },
        garbagePanel = garbagePanel,
    )

    val createState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragTarget,
            onGiven = { offer ->
                setExpanded(dropOnEditor(offer.item, setExpanded))
            },
        )

        expandedSide is ExpandedSide.Editor -> DrawerHandleState.Expanded(
            expandedSide.edge,
            expandedSide.data,
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        handleType = if (dragTarget.offer == null) HandleType.Create else HandleType.Edit,
        state = createState,
        setExpanded = { setExpanded(fromEdge(it, setExpanded)) },
    ) { Column(Modifier.fillMaxSize()) { editorPanel(it) } }

    val libraryState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragTarget,
            onGiven = { onLibraryDrop(it.item) },
        )

        dragSource == null -> null

        expandedSide is ExpandedSide.Library -> DrawerHandleState.Expanded(
            expandedSide.edge,
            dragSource,
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)

        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        handleType = HandleType.Library,
        state = libraryState,
        setExpanded = { setExpanded(fromEdge(it, setExpanded)) },
    ) { libraryPanel(it) }
}
