package com.example.microware

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun BoxWithConstraintsScope.WorldEditToolbar(
    settableOffer: MutableState<Offer?>,
    settableWorld: MutableState<World>,
    settableObjectEdited: MutableState<Obj?>,
    createDrawerOpen: MutableState<Boolean>,
    onCreate: () -> Unit,
    editObject: (Obj) -> Unit,
    settableTrash: MutableState<Trash>,
    library: MutableState<Library>,
) {
    var trashDrawerOpen by remember { mutableStateOf(false) }
    var libraryDrawerOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var trash by settableTrash
    var world by settableWorld
    var offered by settableOffer
    var objectBeingEdited by settableObjectEdited
    var createDrawerOpen by createDrawerOpen
    trashDrawerOpen = trashDrawerOpen && offered == null
    libraryDrawerOpen = libraryDrawerOpen && offered == null

    val controlsWidth = with(density) { maxWidth.toPx() }

    val fleeingTarget = if (libraryDrawerOpen) {
        -controlsWidth
    } else if (trashDrawerOpen) {
        controlsWidth
    } else {
        0f
    }
    val trashFleeingOffset by animateFloatAsState(
        targetValue = if (libraryDrawerOpen) fleeingTarget else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "Fleeing buttons"
    )
    val createFleeingOffset by animateFloatAsState(
        targetValue = if (libraryDrawerOpen || trashDrawerOpen) fleeingTarget else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "Fleeing buttons"
    )
    val libraryFleeingOffset by animateFloatAsState(
        targetValue = if (trashDrawerOpen) fleeingTarget else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "Fleeing buttons"
    )

    var trashBounds by remember { mutableStateOf(Rect.Zero) }
    Drawer(
        placement = Placement(HandleEdge.Left, trashBounds.left),
        isExpanded = trashDrawerOpen,
        onDismissRequest = { trash = trash.close() },
    ) {
        ObjectTable(
            items = trash.garbage,
            settableOffer = settableOffer,
            from = Unit,
        )
    }

    TrashControls(
        trash = settableTrash,
        onGiven = { world = world.deleteObject(it.obj.id) },
        offered = offered,
        onTrashClick = { trashDrawerOpen = !trashDrawerOpen },
        isExpanded = trashDrawerOpen,
        onBoundsChanged = { trashBounds = it },
        modifier = Modifier.offset { IntOffset(trashFleeingOffset.roundToInt(), 0) }
    )

    Drawer(
        placement = Placement(HandleEdge.Bottom, 0f),
        isExpanded = createDrawerOpen,
        onDismissRequest = {
            objectBeingEdited = null
            createDrawerOpen = false
        },
    ) {
        objectBeingEdited?.let { obj ->
            Column(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = {
                        objectBeingEdited = null
                        createDrawerOpen = false
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Done")
                }
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

    var createState: TrashState by remember { mutableStateOf(TrashState.Closed) }
    CreateTarget(
        onClick = onCreate,
        isEditMode = offered != null,
        isActive = createState == TrashState.Hovered,
        isExpanded = createDrawerOpen,
        onBoundsChanged = {},
        modifier = Modifier
            .offset { IntOffset(createFleeingOffset.roundToInt(), 0) }
            .dropTarget(
                offered,
                onStart = { createState = TrashState.Open },
                onEnter = { createState = TrashState.Hovered },
                onExit = { createState = TrashState.Open },
                onGiven = { editObject(it.obj) },
                onEnd = { createState = TrashState.Closed })
    )

    var libraryBounds by remember { mutableStateOf(Rect.Zero) }
    Drawer(
        placement = Placement(HandleEdge.Right, libraryBounds.right),
        isExpanded = libraryDrawerOpen,
        onDismissRequest = { libraryDrawerOpen = false },
    ) {
        ObjectTable(
            items = library.value.objects,
            settableOffer = settableOffer,
            from = Unit,
        )
    }

    LibraryTarget(
        offered = offered,
        onGiven = { library.value = library.value.add(it); saveLibrary(library.value) },
        isExpanded = libraryDrawerOpen,
        onClick = {
            val opening = !libraryDrawerOpen
            if (opening) {
                trash = trash.close()
            }
            libraryDrawerOpen = opening
        },
        onBoundsChanged = { libraryBounds = it },
        modifier = Modifier
            .offset {
                IntOffset(libraryFleeingOffset.roundToInt(), 0)
            }
    )
}
