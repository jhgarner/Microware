package com.example.microware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal object LogicListDragSource
internal object LogicTrashDragSource
internal object LogicLibraryDragSource

@Composable
internal fun LogicTable(
    items: List<Logic>,
    dragController: DragController<Logic>,
    from: Any,
    modifier: Modifier = Modifier,
    listModifier: Modifier = Modifier.fillMaxSize(),
    belowList: @Composable () -> Unit = {},
    willBeDeleted: Boolean = false,
    onDrop: (Offer<Logic>, Int) -> Logic,
    scrollToTopRequest: Int = 0,
    ghostWidth: Dp? = null,
) {
    GhostingLazyColumn(
        items = items,
        dragController = dragController,
        modifier = modifier,
        listModifier = listModifier,
        belowList = belowList,
        willBeDeleted = willBeDeleted,
        onDrop = onDrop,
        ghostWidth = ghostWidth,
        ghostContent = { logic, itemModifier -> LogicCard(logic, itemModifier) },
        itemContent = { logic, itemModifier ->
            LogicCard(logic, itemModifier.fillMaxWidth())
        },
        draggedItemAlpha = { isBeingDeleted -> if (isBeingDeleted) .5f else 0f },
        contentPadding = PaddingValues(
            horizontal = 8.dp,
            vertical = 4.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        scrollToTopRequest = scrollToTopRequest,
    )
}

@Composable
internal fun LogicCard(
    logic: Logic,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Palette.OffWhite.color)
            .border(2.dp, Palette.Black.color, RoundedCornerShape(8.dp))
            .padding(8.dp)
            .semantics { contentDescription = "Drag logic item" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(40.dp).padding(5.dp)) {
            drawTrigger(logic.trigger)
        }
        Canvas(Modifier.size(28.dp).padding(4.dp)) {
            drawArrowIcon()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                logic.actions.take(5).forEach { action ->
                    Canvas(Modifier.size(36.dp).padding(4.dp)) {
                        drawAction(action)
                    }
                }
            }
        }
    }
}
