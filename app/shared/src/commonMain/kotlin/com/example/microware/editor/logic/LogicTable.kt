package com.example.microware.editor.logic
import com.example.microware.game.model.Logic
import com.example.microware.game.sprite.Palette
import com.example.microware.ui.components.Grid
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.Offer
import com.example.microware.ui.gestures.dragSource
import com.example.microware.util.Id

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal object LogicListDragSource
internal object LogicTrashDragSource
internal object LogicLibraryDragSource

private val LogicCardHeight = 60.dp

@Composable
internal fun LogicTable(
    items: List<Logic>,
    dragController: DragController<Logic>,
    sourceId: Any,
    modifier: Modifier = Modifier,
    belowList: @Composable () -> Unit = {},
    onMove: (Id, Int) -> Unit = { _, _ -> },
    onDrop: (Offer<Logic>, Int) -> Unit = { _, _ -> },
) {
    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Grid(
                items = items,
                itemSize = DpSize(
                    width = maxWidth - 16.dp,
                    height = LogicCardHeight,
                ),
                dragSource = dragController,
                dragTarget = dragController,
                sourceId = sourceId,
                onMove = onMove,
                onDrop = onDrop,
            ) { logic ->
                LogicCard(logic, Modifier.fillMaxSize())
            }
        }
        belowList()
    }
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
