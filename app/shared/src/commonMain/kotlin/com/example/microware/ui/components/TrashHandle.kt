package com.example.microware.ui.components
import com.example.microware.ui.gestures.DragSource
import com.example.microware.ui.gestures.DragTarget
import com.example.microware.ui.gestures.dragSource
import com.example.microware.util.Identifiable

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun <T : Identifiable<T>> BoxWithConstraintsScope.TrashHandle(
    dragTarget: DragTarget<T>,
    dragSource: DragSource<T>?,
    trashSourceId: Any,
    activeEdge: HandleEdge?,
    setExpanded: (HandleEdge?) -> Unit,
    willBeDeleted: (Boolean) -> Unit,
    onDelete: (T) -> Unit,
    garbagePanel: @Composable (DragSource<T>, List<T>) -> Unit,
) {
    var trash by remember { mutableStateOf(Trash<T>()) }
    val offer = dragTarget.offer

    val state = when {
        offer != null -> DrawerHandleState.Dragging(
            dragTarget = dragTarget,
            onEnter = { willBeDeleted(true) },
            onExit = { willBeDeleted(false) },
            onEnd = { willBeDeleted(false) },
            onGiven = { offer ->
                if (!offer.originatesFromGrid(trashSourceId)) {
                    trash = trash.discard(offer.item)
                }
                onDelete(offer.item)
            },
        )

        dragSource == null -> null

        activeEdge == HandleEdge.Left -> DrawerHandleState.Expanded(
            HandleEdge.Left,
            dragSource,
        )

        activeEdge != null -> DrawerHandleState.Expanded(activeEdge, null)
        else -> DrawerHandleState.Waiting()
    }

    DrawerWithHandle(
        handleType = HandleType.Trash,
        state = state,
        setExpanded = setExpanded,
        hiddenSlide = if (trash.garbage.isNotEmpty()) 16 else 500,
    ) { garbagePanel(it, trash.garbage) }
}
