package com.example.microware.editor.logic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Parameter
import com.example.microware.game.model.Selector
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import com.example.microware.ui.components.Grid
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.gestures.Offer
import com.example.microware.ui.icons.LogicTagRow
import com.example.microware.ui.icons.drawAction
import com.example.microware.ui.icons.drawArrowIcon
import com.example.microware.ui.icons.drawNumberIcon
import com.example.microware.ui.icons.drawParameter
import com.example.microware.ui.icons.drawTrigger
import com.example.microware.util.Id

internal object LogicListDragSource
internal object LogicTrashDragSource
internal object LogicLibraryDragSource

private val LogicCardHeight = 60.dp
private val LoneTagSize = 24.dp
private val GroupedTagSize = 16.dp

@Composable
internal fun BoxWithConstraintsScope.LogicTable(
    items: List<Logic>,
    dragController: DragController<Logic>,
    sourceId: Any,
    onMove: (Id, Int) -> Unit = { _, _ -> },
    onDrop: (Offer<Logic>, Int) -> Unit = { _, _ -> },
    showsGhost: Boolean = false,
) {
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
        ghost = if (showsGhost) { logic -> LogicCard(logic, Modifier.fillMaxSize()) } else null,
    ) { logic ->
        LogicCard(logic, Modifier.fillMaxSize())
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(40.dp).padding(5.dp)) {
                drawTrigger(logic.trigger)
            }
            TriggerTargets(logic.trigger)
        }
        Canvas(Modifier.size(28.dp).padding(4.dp)) {
            drawArrowIcon()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                logic.actions.take(5).forEach { action ->
                    Canvas(Modifier.size(36.dp).padding(4.dp)) {
                        drawAction(action)
                    }
                    when (action) {
                        is Action.Move -> LoneSelector(action.mover, logic.trigger)
                        is Action.MoveTo -> MoverAndTarget(action.mover, action.target, logic.trigger)
                        is Action.MoveWithin -> MoverAndTarget(action.mover, action.target, logic.trigger)
                        is Action.Win -> Unit
                        is Action.Lose -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerTargets(trigger: Trigger) = when (trigger) {
    Trigger.Start -> Unit
    is Trigger.Press -> LoneTags(trigger.target)
    is Trigger.Release -> LoneTags(trigger.target)
    is Trigger.Contact -> Column(horizontalAlignment = Alignment.End) {
        Row {
            if (trigger.firstTargetCount > 1) TargetCount(trigger.firstTargetCount)
            LogicTagRow(trigger.firstTarget.tags, GroupedTagSize)
        }
        LogicTagRow(trigger.secondTarget.tags, GroupedTagSize)
    }
}

@Composable
private fun TargetCount(count: Int) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(Modifier.size(GroupedTagSize)) { drawNumberIcon(count, textMeasurer) }
}

@Composable
private fun MoverAndTarget(mover: Selector, target: Selector, trigger: Trigger) {
    Column {
        GroupedSelector(mover, trigger)
        GroupedSelector(target, trigger)
    }
}

@Composable
private fun LoneSelector(selector: Selector, trigger: Trigger) = when (selector) {
    is TagFilter -> LoneTags(selector)
    is Parameter -> ParameterIcon(selector, trigger, LoneTagSize)
}

@Composable
private fun GroupedSelector(selector: Selector, trigger: Trigger) = when (selector) {
    is TagFilter -> LogicTagRow(selector.tags, GroupedTagSize)
    is Parameter -> ParameterIcon(selector, trigger, GroupedTagSize)
}

@Composable
private fun LoneTags(filter: TagFilter) =
    LogicTagRow(filter.tags, if (filter.tags.size > 1) GroupedTagSize else LoneTagSize)

@Composable
private fun ParameterIcon(parameter: Parameter, trigger: Trigger, size: Dp) =
    Canvas(Modifier.size(size)) { drawParameter(parameter, trigger) }

@Preview
@Composable
fun PreviewCard() {
    LogicCard(
        logic = Logic(
            trigger = Trigger.Start,
            actions = listOf(
                Action.MoveTo(
                    mover = TagFilter(LogicTag(2)),
                    target = TagFilter(LogicTag(2)),
                    speed = null,
                ),
                Action.Move(
                    mover = TagFilter(LogicTag(2)),
                    target = Direction.N,
                    speed = 20,
                ),
            ),
        ),
    )
}

@Preview
@Composable
fun PreviewMultiTagCard() {
    val tags = listOf(3, 20, 37, 54, 11).map(::LogicTag)
    LogicCard(
        logic = Logic(
            trigger = Trigger.Press(TagFilter(tags.take(3).toSet())),
            actions = listOf(
                Action.MoveTo(
                    mover = TagFilter(tags.take(2).toSet()),
                    target = TagFilter(tags.toSet()),
                    speed = null,
                ),
                Action.Move(
                    mover = TagFilter(tags.take(4).toSet()),
                    target = Direction.N,
                    speed = 20,
                ),
            ),
        ),
    )
}

@Preview
@Composable
fun PreviewParameterCard() {
    LogicCard(
        logic = Logic(
            trigger = Trigger.Collision(
                firstTarget = TagFilter(LogicTag(2)),
                secondTarget = TagFilter(LogicTag(9)),
                firstTargetCount = 2,
            ),
            actions = listOf(
                Action.MoveTo(
                    mover = Parameter.RandomFirstTarget,
                    target = TagFilter(LogicTag(2)),
                    speed = null,
                ),
                Action.Move(
                    mover = Parameter.SecondTarget,
                    target = Direction.N,
                    speed = 20,
                ),
            ),
        ),
    )
}
