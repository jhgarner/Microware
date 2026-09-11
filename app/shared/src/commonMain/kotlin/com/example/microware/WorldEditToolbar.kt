package com.example.microware

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

@Composable
fun BoxWithConstraintsScope.WorldEditToolbar(
    offered: Offer?,
    dragController: DragController,
    isVisible: Boolean = true,
    world: World,
    onWorldChange: (World) -> Unit,
    settableObjectEdited: MutableState<Obj?>,
    willBeDeleted: MutableState<Boolean>,
    settableTrash: MutableState<Trash>,
    settableLibrary: MutableState<Library>,
) {
    var trash by settableTrash
    var library by settableLibrary
    var objectBeingEdited by settableObjectEdited

    val expandedSide = remember { mutableStateOf<HandleEdge?>(null) }
    LaunchedEffect(offered != null) {
        if (offered != null) {
            expandedSide.value = null
        }
    }
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            expandedSide.value = null
        }
    }

    DrawerWithHandle(
        isVisible = isVisible,
        side = HandleEdge.Left,
        expandedSide = expandedSide,
        dragController = dragController,
        foreground = Palette.Maroon,
        highlightForeground = Palette.Red,
        hiddenSlide = if (trash.garbage.isNotEmpty()) 16 else 500,
        offered = offered,
        onDragEnter = { willBeDeleted.value = true },
        onDragExit = { willBeDeleted.value = false },
        onDragEnd = { willBeDeleted.value = false },
        onGiven = { offer, _ ->
            trash = trash.discard(offer.obj)
            onWorldChange(world.deleteObject(offer.obj.id))
        },
        buttonContent = { TrashControls(it) },
        drawerContent = { ObjectTable(items = trash.garbage, dragController = dragController) },
    )

    DrawerWithHandle(
        isVisible = isVisible,
        side = HandleEdge.Bottom,
        expandedSide = expandedSide,
        dragController = dragController,
        foreground = Palette.Navy,
        highlightForeground = Palette.Cyan,
        offered = offered,
        onGiven = { offer, _ ->
            objectBeingEdited = offer.obj
            expandedSide.value = HandleEdge.Bottom
        },
        extraOnDismiss = { objectBeingEdited = null },
        buttonContent = { CreateTarget(foreground = it, isEditMode = offered != null) }
    ) {
        if (objectBeingEdited == null) {
            val obj = Obj(world.nextId(), 20, 20)
            onWorldChange(world.addObject(obj))
            objectBeingEdited = obj
        }
        objectBeingEdited?.let { obj ->
            Column(modifier = Modifier.fillMaxSize()) {
                EditableCanvas(
                    sprite = obj.sprite,
                    commit = { committedSprite ->
                        val newObj = obj.copy(sprite = committedSprite)
                        objectBeingEdited = newObj
                        onWorldChange(world.updateObject(newObj))
                    },
                    resize = { bounds ->
                        val newObj = obj.copy(
                            offset = obj.offset + Offset(
                                bounds.left.toFloat(),
                                bounds.top.toFloat(),
                            ),
                            sprite = obj.sprite.resized(bounds),
                        )
                        objectBeingEdited = world.clamped(newObj)
                        onWorldChange(world.updateObject(newObj))
                    },
                    logicTag = world.logicTagFor(obj.id),
                    onLogicTagChange = { logicTag ->
                        onWorldChange(world.assignLogicTag(obj.id, logicTag))
                    },
                    modifier = Modifier.weight(1f),
                    editorKey = obj.id,
                )
            }
        }
    }

    DrawerWithHandle(
        isVisible = isVisible,
        side = HandleEdge.Right,
        expandedSide = expandedSide,
        dragController = dragController,
        foreground = Palette.Forest,
        highlightForeground = Palette.Green,
        offered = offered,
        onGiven = { offer, _ ->
            library = library.add(offer.obj)
            saveLibrary(library)
        },
        buttonContent = { LibraryTarget(it) },
        drawerContent = { ObjectTable(items = library.objects, dragController = dragController) },
    )
}
