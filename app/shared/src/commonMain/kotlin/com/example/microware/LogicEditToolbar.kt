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
import com.example.microware.ExpandedLogic.Editor

private var library by mutableStateOf(loadLogicLibrary())

private sealed interface ExpandedLogic {
    val edge: HandleEdge

    data object Trash : ExpandedLogic {
        override val edge = HandleEdge.Left
    }

    data class Editor(val builder: LogicBuildHandler) : ExpandedLogic {
        override val edge = HandleEdge.Bottom
    }

    data object Library : ExpandedLogic {
        override val edge = HandleEdge.Right
    }

    companion object {
    }
}

@Composable
internal fun BoxWithConstraintsScope.LogicEditToolbar(
    dragController: DragController<Logic>,
    usedTags: Set<LogicTag>,
    handler: (LogicsEvent) -> Unit,
    willBeDeleted: MutableState<Boolean>,
) {
    var trash by remember { mutableStateOf(Trash()) }
    val offer = dragController.offer

    val (expandedSide, setExpanded) = remember { mutableStateOf<ExpandedLogic?>(null) }.apply {
        if (offer != null) value = null
    }

    fun updateLogic(id: Int, logicEvent: LogicEvent) {
        handler { logics ->
            val oldLogic = logics.byId(id) ?: return@handler logics
            val newLogic = logicEvent.modifyLogic(oldLogic)
            setExpanded(Editor(LogicBuildHandler.Update(newLogic) { updateLogic(id, it) }))
            logics.upserting(newLogic)
        }
    }

    fun fromEdge(edge: HandleEdge?): ExpandedLogic? = when (edge) {
        null -> null
        HandleEdge.Left -> ExpandedLogic.Trash
        HandleEdge.Bottom -> Editor(LogicBuildHandler.New { trigger ->
            val newLogic = Logic(trigger)
            handler { it.addingAt(0, newLogic) }
            setExpanded(Editor(LogicBuildHandler.Update(newLogic) { updateLogic(newLogic.id, it) }))
        })

        HandleEdge.Right -> ExpandedLogic.Library
    }

    val trashState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragController,
            onEnter = { willBeDeleted.value = true },
            onExit = { willBeDeleted.value = false },
            onEnd = { willBeDeleted.value = false },
            onGiven = { offer, _ ->
                handler { logics ->
                    val logic = offer.item
                    if (offer.from !== LogicTrashDragSource) {
                        trash = trash.discard(logic)
                    }
                    logics.indexOfFirst { it.id == logic.id }
                        .takeIf { it >= 0 }
                        ?.let { logics.removingAt(it) }
                        ?: logics
                }
            }
        )

        expandedSide is ExpandedLogic.Trash -> DrawerHandleState.Expanded(
            expandedSide.edge,
            dragController
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        state = trashState,
        setExpanded = { setExpanded(fromEdge(it)) },
        side = HandleEdge.Left,
        foreground = Palette.Maroon,
        highlightForeground = Palette.Red,
        hiddenSlide = if (trash.logicGarbage.isNotEmpty()) 16 else 500,
        buttonContent = { TrashControls(it) },
        drawerContent = {
            LogicTable(
                items = trash.logicGarbage,
                dragController = dragController,
                from = LogicTrashDragSource,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

    val createState = when {
        offer != null -> DrawerHandleState.Dragging(dragController, onGiven = { offer, _ ->
            setExpanded(Editor(LogicBuildHandler.Update(offer.item) {
                updateLogic(
                    offer.item.id,
                    it
                )
            }))
        })

        expandedSide is ExpandedLogic.Editor -> DrawerHandleState.Expanded(
            expandedSide.edge,
            expandedSide.builder
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        state = createState,
        setExpanded = { setExpanded(fromEdge(it)) },
        side = HandleEdge.Bottom,
        foreground = Palette.Navy,
        highlightForeground = Palette.Cyan,
        buttonContent = {
            CreateTarget(
                foreground = it,
                isEditMode = dragController.offer != null,
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            LogicBuilder(
                usedTags = usedTags,
                handler = it,
            )
        }
    }

    val libraryState = when {
        offer != null -> DrawerHandleState.Dragging(
            dragController,
            onEnter = { willBeDeleted.value = true },
            onExit = { willBeDeleted.value = false },
            onEnd = { willBeDeleted.value = false },
            onGiven = { offer, _ ->
                val logic = offer.item
                if (offer.from !== LogicLibraryDragSource) {
                    library = library.add(logic)
                    saveLogicLibrary(library)
                }
            },
        )

        expandedSide is ExpandedLogic.Library -> DrawerHandleState.Expanded(
            expandedSide.edge,
            dragController
        )

        expandedSide != null -> DrawerHandleState.Expanded(expandedSide.edge, null)
        else -> DrawerHandleState.Waiting()
    }
    DrawerWithHandle(
        state = libraryState,
        setExpanded = { setExpanded(fromEdge(it)) },
        side = HandleEdge.Right,
        foreground = Palette.Forest,
        highlightForeground = Palette.Green,
        buttonContent = { LibraryTarget(it) },
        drawerContent = {
            LogicTable(
                items = library.items,
                dragController = dragController,
                from = LogicLibraryDragSource,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
