package com.example.microware

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun BoxWithConstraintsScope.LogicEditToolbar(
    offered: Offer?,
    dragController: DragController,
    usedTags: Set<LogicTag>,
    logics: List<Logic>,
    editing: Boolean,
    editingIndex: Int?,
    editingLogic: Logic?,
    onLogicChange: (Logic) -> Unit,
    onLogicsChange: (List<Logic>) -> Unit,
    onPageChange: (LogicEditorPage) -> Unit,
    settableTrash: MutableState<Trash>,
    settableLibrary: MutableState<Library>,
    willBeDeleted: MutableState<Boolean>,
) {
    var trash by settableTrash
    var library by settableLibrary
    val expandedSide = remember { mutableStateOf<HandleEdge?>(null) }

    LaunchedEffect(offered != null) {
        if (offered != null) {
            expandedSide.value = null
        }
    }
    LaunchedEffect(editing) {
        if (!editing) {
            expandedSide.value = null
        }
    }

    DrawerWithHandle(
        side = HandleEdge.Left,
        expandedSide = expandedSide,
        dragController = dragController,
        foreground = Palette.Maroon,
        highlightForeground = Palette.Red,
        hiddenSlide = if (trash.logicGarbage.isNotEmpty()) 16 else 500,
        offered = offered,
        onDragEnter = { willBeDeleted.value = true },
        onDragExit = { willBeDeleted.value = false },
        onDragEnd = { willBeDeleted.value = false },
        onGiven = { offer, _ ->
            val logic = offer.item as? Logic ?: return@DrawerWithHandle
            if (offer.from !== LogicTrashDragSource) {
                trash = trash.discard(logic)
            }
            (offer.from as? LogicListItemDragSource)?.index?.let { index ->
                onLogicsChange(logics.filterIndexed { candidateIndex, _ -> candidateIndex != index })
                if (editingIndex == index) {
                    onPageChange(LogicEditorPage.List)
                } else if (editingIndex != null && index < editingIndex) {
                    onPageChange(LogicEditorPage.Editing(editingIndex - 1))
                }
            }
        },
        buttonContent = { TrashControls(it) },
        drawerContent = {
            LogicTable(
                items = trash.logicGarbage,
                dragController = dragController,
                from = LogicTrashDragSource,
                offered = offered,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )

    DrawerWithHandle(
        side = HandleEdge.Bottom,
        expandedSide = expandedSide,
        dragController = dragController,
        foreground = Palette.Navy,
        highlightForeground = Palette.Cyan,
        offered = offered,
        onGiven = { offer, _ ->
            val logic = offer.item as? Logic ?: return@DrawerWithHandle
            val sourceIndex = (offer.from as? LogicListItemDragSource)?.index
            val logicToEdit = sourceIndex?.let { logic } ?: logic.copy(id = logics.nextId())
            val index = sourceIndex ?: logics.size.also {
                onLogicsChange(logics + logicToEdit)
            }
            onPageChange(LogicEditorPage.Editing(index))
            expandedSide.value = HandleEdge.Bottom
        },
        extraOnExpand = { onPageChange(LogicEditorPage.Editing(null)) },
        extraOnDismiss = { onPageChange(LogicEditorPage.List) },
        buttonContent = {
            CreateTarget(
                foreground = it,
                isEditMode = offered != null || editing,
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            LogicBuilder(
                usedTags = usedTags,
                initialLogic = editingLogic,
                onLogicChange = onLogicChange,
            )
        }
    }

    DrawerWithHandle(
        side = HandleEdge.Right,
        expandedSide = expandedSide,
        dragController = dragController,
        foreground = Palette.Forest,
        highlightForeground = Palette.Green,
        offered = offered,
        onGiven = { offer, _ ->
            val logic = offer.item as? Logic ?: return@DrawerWithHandle
            if (offer.from !== LogicLibraryDragSource) {
                library = library.add(logic)
                saveLibrary(library)
            }
        },
        buttonContent = { LibraryTarget(it) },
        drawerContent = {
            LogicTable(
                items = library.logics,
                dragController = dragController,
                from = LogicLibraryDragSource,
                offered = offered,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
