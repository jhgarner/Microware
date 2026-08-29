package com.example.microware

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun BoxWithConstraintsScope.WorldEditToolbar(
    settableOffer: MutableState<Offer?>,
    settableWorld: MutableState<World>,
    editObject: (Obj) -> Unit,
    settableTrash: MutableState<Trash>,
    library: MutableState<Library>,
) {
    var trashDrawerOpen by remember { mutableStateOf(false) }
    var libraryDrawerOpen by remember { mutableStateOf(false) }
    var showingCreateDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var trash by settableTrash
    var world by settableWorld
    var offered by settableOffer
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
        modifier = Modifier.zIndex(if (trashDrawerOpen) 1f else 0f),
        placement = Placement(Side.Right, trashBounds.left),
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
        availableWidth = controlsWidth,
        onTrashClick = { trashDrawerOpen = !trashDrawerOpen },
        isExpanded = trashDrawerOpen,
        onBoundsChanged = { trashBounds = it },
        modifier = Modifier.zIndex(if (trashDrawerOpen) 1f else 0f)
            .align(Alignment.BottomStart)
            .offset { IntOffset(trashFleeingOffset.roundToInt(), 0) }
    )


    var createState: TrashState by remember { mutableStateOf(TrashState.Closed) }
    CreateTarget(
        onClick = { showingCreateDialog = true },
        isEditMode = offered != null,
        isActive = createState == TrashState.Hovered,
        modifier = Modifier.zIndex(0.75f)
            .align(Alignment.BottomCenter)
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
        modifier = Modifier.zIndex(
            if (libraryDrawerOpen) 1f else 0f
        ),
        placement = Placement(Side.Left, libraryBounds.right),
        onDismissRequest = { libraryDrawerOpen = false },
    ) {
        ObjectTable(
            items = library.value.objects,
            settableOffer = settableOffer,
            from = Unit,
        )
    }

    LibraryControls(
        offered = offered,
        onGiven = { library.value = library.value.add(it); saveLibrary(library.value) },
        isExpanded = libraryDrawerOpen,
        availableWidth = controlsWidth,
        onClick = {
            val opening = !libraryDrawerOpen
            if (opening) {
                trash = trash.close()
            }
            libraryDrawerOpen = opening
        },
        onBounds = { libraryBounds = it },
        modifier = Modifier.zIndex(if (libraryDrawerOpen) 1f else 0f)
            .align(Alignment.BottomEnd)
            .offset {
                IntOffset(libraryFleeingOffset.roundToInt(), 0)
            }
    )
}
