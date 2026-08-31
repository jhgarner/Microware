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

@Composable
fun BoxWithConstraintsScope.WorldEditToolbar(
    settableOffer: MutableState<Offer?>,
    settableWorld: MutableState<World>,
    settableObjectEdited: MutableState<Obj?>,
    willBeDeleted: MutableState<Boolean>,
    editObject: (Obj) -> Unit,
    settableTrash: MutableState<Trash>,
    settableLibrary: MutableState<Library>,
) {
    var trash by settableTrash
    var library by settableLibrary
    var world by settableWorld
    var objectBeingEdited by settableObjectEdited

    val expandedSide = remember { mutableStateOf<HandleEdge?>(null) }
    if (settableOffer.value != null) {
        expandedSide.value = null
    }

    DrawerWithHandle(
        buttonModifier = Modifier.dropTarget(
            offered = settableOffer.value,
            onEnter = { willBeDeleted.value = true },
            onEnd = { willBeDeleted.value = false },
            onExit = { willBeDeleted.value = false }),
        side = HandleEdge.Left,
        expandedSide = expandedSide,
        foreground = Palette.Maroon,
        highlightForeground = Palette.Red,
        hiddenSlide = if (trash.garbage.isNotEmpty()) 16 else 500,
        settableOffer = settableOffer,
        onGiven = { trash = trash.discard(it.obj); world = world.deleteObject(it.obj.id) },
        buttonContent = { TrashControls(it) },
        drawerContent = { ObjectTable(items = trash.garbage, settableOffer = settableOffer) },
    )

    DrawerWithHandle(
        side = HandleEdge.Bottom,
        expandedSide = expandedSide,
        foreground = Palette.Navy,
        highlightForeground = Palette.Cyan,
        settableOffer = settableOffer,
        onGiven = { editObject(it.obj) },
        extraOnDismiss = { objectBeingEdited = null },
        buttonContent = { CreateTarget(foreground = it, isEditMode = settableOffer.value != null) }
    ) {
        objectBeingEdited?.let { obj ->
            Column(modifier = Modifier.fillMaxSize()) {
                EditableCanvas(
                    sprite = obj.sprite,
                    edit = { edit ->
                        val newObj = obj.copy(sprite = obj.sprite.update(edit))
                        objectBeingEdited = newObj
                        world = world.updateObject(newObj)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        } ?: CreateObjectForm(
            onCreate = { width, height ->
                val obj = Obj(world.nextId(), width, height)
                world = world.addObject(obj)
                objectBeingEdited = obj
            }
        )
    }

    DrawerWithHandle(
        side = HandleEdge.Right,
        expandedSide = expandedSide,
        foreground = Palette.Forest,
        highlightForeground = Palette.Green,
        settableOffer = settableOffer,
        onGiven = { library = library.add(it.obj); saveLibrary(library) },
        buttonContent = { LibraryTarget(it) },
        drawerContent = { ObjectTable(items = library.objects, settableOffer = settableOffer) },
    )
}
