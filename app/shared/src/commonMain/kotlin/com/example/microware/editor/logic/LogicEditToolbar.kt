package com.example.microware.editor.logic

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.microware.editor.ExpandedSide
import com.example.microware.editor.Toolbar
import com.example.microware.editor.logic.ExpandedLogic.Editor
import com.example.microware.game.events.LogicEvent
import com.example.microware.game.events.LogicsEvent
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.library.loadLogicLibrary
import com.example.microware.library.saveLogicLibrary
import com.example.microware.ui.components.HandleEdge
import com.example.microware.ui.gestures.DragController
import com.example.microware.util.Id
import com.example.microware.util.byId
import com.example.microware.util.upserting

private var library by mutableStateOf(loadLogicLibrary())

private sealed interface ExpandedLogic {
    fun edge(): ExpandedSide<LogicBuildHandler>

    data object Trash : ExpandedLogic {
        override fun edge() = ExpandedSide.Trash<LogicBuildHandler>()
    }

    data class Editor(val builder: LogicBuildHandler) : ExpandedLogic {
        override fun edge() = ExpandedSide.Editor(builder)
    }

    data object Library : ExpandedLogic {
        override fun edge() = ExpandedSide.Library<LogicBuildHandler>()
    }
}

@Composable
internal fun BoxWithConstraintsScope.LogicEditToolbar(
    dragController: DragController<Logic>,
    usedTags: Set<LogicTag>,
    handler: (LogicsEvent) -> Unit,
    willBeDeleted: MutableState<Boolean>,
) {
    fun updateLogic(id: Id, logicEvent: LogicEvent, setExpanded: (ExpandedLogic) -> Unit) {
        handler { logics ->
            val oldLogic = logics.byId(id) ?: return@handler logics
            val newLogic = logicEvent.modifyLogic(oldLogic)
            setExpanded(Editor(LogicBuildHandler.Update(newLogic) {
                updateLogic(
                    id,
                    it,
                    setExpanded
                )
            }))
            logics.upserting(newLogic)
        }
    }

    fun fromEdge(edge: HandleEdge?, setExpanded: (ExpandedLogic) -> Unit): ExpandedLogic? =
        when (edge) {
            null -> null
            HandleEdge.Left -> ExpandedLogic.Trash
            HandleEdge.Bottom -> Editor(LogicBuildHandler.New { trigger ->
                val newLogic = Logic(trigger)
                handler { it.addingAt(0, newLogic) }
                setExpanded(Editor(LogicBuildHandler.Update(newLogic) {
                    updateLogic(
                        newLogic.id,
                        it,
                        setExpanded
                    )
                }))
            })

            HandleEdge.Right -> ExpandedLogic.Library
        }


    Toolbar(
        dragTarget = dragController,
        dragSource = dragController,
        willBeDeleted = { willBeDeleted.value = it },
        onLibraryDrop = { library = library.add(it); saveLogicLibrary(library) },
        trashSourceId = LogicTrashDragSource,
        fromEdge = ::fromEdge,
        dropOnEditor = { logic, setExpanded ->
            Editor(LogicBuildHandler.Update(logic) { updateLogic(logic.id, it, setExpanded) })
        },
        expandToEdge = { it.edge() },
        handler = { action -> handler { logics -> action(logics) } },
        garbagePanel = { _, garbage ->
            BoxWithConstraints {
                LogicTable(
                    items = garbage,
                    dragController = dragController,
                    sourceId = LogicTrashDragSource,
                )
            }
        },
        editorPanel = {
            LogicBuilder(
                usedTags = usedTags,
                handler = it,
            )
        },
        libraryPanel = {
            BoxWithConstraints {
                LogicTable(
                    items = library.items,
                    dragController = dragController,
                    sourceId = LogicLibraryDragSource,
                )
            }
        },
    )
}
