package com.example.microware

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private enum class ActionChoice { Move, MoveTo, Win, Lose }

@Composable
fun LogicEditor(
    world: World,
    logics: List<Logic>,
    onLogicsChange: (List<Logic>) -> Unit,
) {
    var editedLogicIndex by remember { mutableStateOf<Int?>(null) }

    if (editedLogicIndex != null) {
        val index = editedLogicIndex
        val existingLogic = index?.takeIf { it >= 0 }?.let(logics::getOrNull)
        LogicBuilder(
            usedTags = world.logicTags.keys,
            initialLogic = existingLogic,
            onCancel = { editedLogicIndex = null },
            onSave = { changedLogic ->
                onLogicsChange(
                    if (index == null || index < 0) {
                        logics + changedLogic
                    } else {
                        logics.mapIndexed { candidateIndex, logic ->
                            if (candidateIndex == index) changedLogic else logic
                        }
                    }
                )
                editedLogicIndex = null
            },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.leftDp(),
                end = insets.rightDp(),
                bottom = insets.bottomDp(),
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp, 8.dp, 8.dp, 72.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(logics) { index, logic ->
                LogicCard(
                    logic = logic,
                    onEdit = { editedLogicIndex = index },
                    onDelete = {
                        onLogicsChange(logics.filterIndexed { candidateIndex, _ -> candidateIndex != index })
                    },
                )
            }
        }

        IconButton(
            description = "Create logic",
            modifier = Modifier.align(Alignment.BottomCenter).size(64.dp),
            onClick = { editedLogicIndex = -1 },
        ) { drawPlusIcon() }
    }
}

@Composable
private fun LogicBuilder(
    usedTags: Set<LogicTag>,
    initialLogic: Logic?,
    onCancel: () -> Unit,
    onSave: (Logic) -> Unit,
) {
    val defaultTag = usedTags.firstOrNull() ?: LogicTag.all.first()
    var trigger by remember(initialLogic) { mutableStateOf(initialLogic?.trigger) }
    var actions by remember(initialLogic) { mutableStateOf(initialLogic?.actions.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.leftDp() + 8.dp,
                end = insets.rightDp() + 8.dp,
                bottom = insets.bottomDp() + 8.dp,
            ),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TriggerButton("Game start trigger", trigger is Trigger.Start) {
                trigger = Trigger.Start
            }
            TriggerButton("Tap trigger", trigger is Trigger.Tap) {
                trigger = Trigger.Tap(defaultTag)
            }
            TriggerButton("Collision trigger", trigger is Trigger.Collision) {
                trigger = Trigger.Collision(defaultTag, defaultTag)
            }
        }

        trigger?.let { selectedTrigger ->
            TriggerParameters(selectedTrigger, usedTags) { trigger = it }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(actions) { index, action ->
                EditableActionCard(
                    action = action,
                    usedTags = usedTags,
                    onChange = { changed ->
                        actions = actions.mapIndexed { candidateIndex, current ->
                            if (candidateIndex == index) changed else current
                        }
                    },
                    onDelete = {
                        actions = actions.filterIndexed { candidateIndex, _ -> candidateIndex != index }
                    },
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    ActionButton(ActionChoice.Move) {
                        actions = actions + Action.Move(defaultTag, Direction.E, 1)
                    }
                    ActionButton(ActionChoice.MoveTo) {
                        actions = actions + Action.MoveTo(defaultTag, defaultTag, 1)
                    }
                    ActionButton(ActionChoice.Win) { actions = actions + Action.Win }
                    ActionButton(ActionChoice.Lose) { actions = actions + Action.Lose }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton("Cancel logic", onClick = onCancel) { drawCloseIcon() }
            IconButton(
                description = "Save logic",
                enabled = trigger != null && actions.isNotEmpty(),
                onClick = { trigger?.let { onSave(Logic(it, actions)) } },
            ) { drawCheckIcon() }
        }
    }
}

@Composable
private fun TriggerButton(description: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(description, selected = selected, onClick = onClick) {
        when {
            description.startsWith("Game") -> drawStartIcon()
            description.startsWith("Tap") -> drawTapIcon()
            else -> drawCollisionIcon()
        }
    }
}

@Composable
private fun TriggerParameters(
    trigger: Trigger,
    usedTags: Set<LogicTag>,
    onChange: (Trigger) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (trigger) {
            Trigger.Start -> Unit
            is Trigger.Tap -> LogicTagPicker(
                selectedTag = trigger.target,
                onSelectTag = { it?.let { tag -> onChange(Trigger.Tap(tag)) } },
                usedTags = usedTags,
                allowClear = false,
            )
            is Trigger.Collision -> {
                LogicTagPicker(
                    selectedTag = trigger.firstTarget,
                    onSelectTag = { it?.let { tag -> onChange(trigger.copy(firstTarget = tag)) } },
                    usedTags = usedTags,
                    allowClear = false,
                )
                Canvas(Modifier.size(28.dp)) { drawCollisionIcon() }
                LogicTagPicker(
                    selectedTag = trigger.secondTarget,
                    onSelectTag = { it?.let { tag -> onChange(trigger.copy(secondTarget = tag)) } },
                    usedTags = usedTags,
                    allowClear = false,
                )
            }
        }
    }
}

@Composable
private fun LogicCard(logic: Logic, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Palette.OffWhite.color)
            .border(BorderStroke(2.dp, Color.Black))
            .clickable(onClick = onEdit)
            .semantics { contentDescription = "Edit logic" }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(40.dp).padding(5.dp)) { drawTrigger(logic.trigger) }
        Canvas(Modifier.size(28.dp).padding(4.dp)) { drawArrowIcon() }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            logic.actions.take(5).forEach { action ->
                Canvas(Modifier.size(36.dp).padding(4.dp)) { drawAction(action) }
            }
        }
        IconButton("Delete logic", onClick = onDelete) { drawTrashIcon() }
    }
}

@Composable
private fun EditableActionCard(
    action: Action,
    usedTags: Set<LogicTag>,
    onChange: (Action) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (action) {
            is Action.Move -> {
                LogicTagPicker(
                    action.mover,
                    { it?.let { tag -> onChange(Action.Move(tag, action.target, action.speed)) } },
                    usedTags = usedTags,
                    allowClear = false,
                )
                IconButton("Change direction", onClick = {
                    val directions = listOf(Direction.N, Direction.E, Direction.S, Direction.W)
                    val nextDirection = directions[(directions.indexOf(action.target) + 1) % directions.size]
                    onChange(Action.Move(action.mover, nextDirection, action.speed))
                }) { drawDirectionIcon(action.target) }
                SpeedControl(action.speed) { onChange(Action.Move(action.mover, action.target, it)) }
            }
            is Action.MoveTo -> {
                LogicTagPicker(
                    action.mover,
                    { it?.let { tag -> onChange(Action.MoveTo(tag, action.target, action.speed)) } },
                    usedTags = usedTags,
                    allowClear = false,
                )
                LogicTagPicker(
                    action.target,
                    { it?.let { tag -> onChange(Action.MoveTo(action.mover, tag, action.speed)) } },
                    usedTags = usedTags,
                    allowClear = false,
                )
                IconButton("Toggle teleport", selected = action.speed == null, onClick = {
                    onChange(Action.MoveTo(action.mover, action.target, if (action.speed == null) 1 else null))
                }) { drawTeleportIcon() }
                action.speed?.let { speed ->
                    SpeedControl(speed) {
                        onChange(Action.MoveTo(action.mover, action.target, it))
                    }
                }
            }
            Action.Win, Action.Lose -> Unit
        }
        Box(Modifier.weight(1f))
        IconButton("Delete action", onClick = onDelete) { drawTrashIcon() }
    }
}

@Composable
private fun SpeedControl(speed: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton("Slower", enabled = speed > 1, onClick = { onChange(speed - 1) }) {
            drawMinusIcon()
        }
        Canvas(Modifier.size(24.dp)) {
            repeat(speed.coerceAtMost(5)) { index ->
                val y = size.height * (index + 1) / (speed.coerceAtMost(5) + 1)
                drawLine(Color.Black, Offset(size.width * .25f, y), Offset(size.width * .75f, y), 2.dp.toPx())
            }
        }
        IconButton("Faster", enabled = speed < 5, onClick = { onChange(speed + 1) }) {
            drawPlusIcon()
        }
    }
}

@Composable
private fun ActionButton(choice: ActionChoice, onClick: () -> Unit) {
    IconButton("Add ${choice.name} action", onClick = onClick) {
        when (choice) {
            ActionChoice.Move -> drawDirectionIcon(Direction.E)
            ActionChoice.MoveTo -> drawMoveToIcon()
            ActionChoice.Win -> drawWinIcon()
            ActionChoice.Lose -> drawLoseIcon()
        }
    }
}

private fun DrawScope.drawTrigger(trigger: Trigger) = when (trigger) {
    Trigger.Start -> drawStartIcon()
    is Trigger.Tap -> drawTapIcon()
    is Trigger.Collision -> drawCollisionIcon()
}

private fun DrawScope.drawAction(action: Action) = when (action) {
    is Action.Move -> drawDirectionIcon(action.target)
    is Action.MoveTo -> drawMoveToIcon()
    Action.Win -> drawWinIcon()
    Action.Lose -> drawLoseIcon()
}

private fun DrawScope.drawStartIcon() {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * .25f, size.height * .15f)
        lineTo(size.width * .85f, size.height * .5f)
        lineTo(size.width * .25f, size.height * .85f)
        close()
    }
    drawPath(path, Palette.Green.color)
    drawPath(path, Color.Black, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawTapIcon() {
    drawCircle(Palette.Cyan.color, size.minDimension * .28f, center, style = Stroke(3.dp.toPx()))
    drawCircle(Color.Black, size.minDimension * .08f, center)
}

private fun DrawScope.drawCollisionIcon() {
    drawCircle(Palette.Cyan.color, size.minDimension * .25f, Offset(size.width * .36f, size.height * .5f))
    drawCircle(Palette.Red.color, size.minDimension * .25f, Offset(size.width * .64f, size.height * .5f))
}

private fun DrawScope.drawDirectionIcon(direction: Direction) {
    val (start, end, arrowHead) = when (direction) {
        Direction.N -> Triple(
            Offset(size.width * .5f, size.height * .88f),
            Offset(size.width * .5f, size.height * .12f),
            listOf(Offset(size.width * .25f, size.height * .38f), Offset(size.width * .75f, size.height * .38f)),
        )
        Direction.S -> Triple(
            Offset(size.width * .5f, size.height * .12f),
            Offset(size.width * .5f, size.height * .88f),
            listOf(Offset(size.width * .25f, size.height * .62f), Offset(size.width * .75f, size.height * .62f)),
        )
        Direction.E -> Triple(
            Offset(size.width * .12f, size.height * .5f),
            Offset(size.width * .88f, size.height * .5f),
            listOf(Offset(size.width * .62f, size.height * .25f), Offset(size.width * .62f, size.height * .75f)),
        )
        Direction.W -> Triple(
            Offset(size.width * .88f, size.height * .5f),
            Offset(size.width * .12f, size.height * .5f),
            listOf(Offset(size.width * .38f, size.height * .25f), Offset(size.width * .38f, size.height * .75f)),
        )
    }
    val strokeWidth = 3.dp.toPx()
    drawLine(Color.Black, start, end, strokeWidth, StrokeCap.Round)
    arrowHead.forEach { point ->
        drawLine(Color.Black, point, end, strokeWidth, StrokeCap.Round)
    }
}

private fun DrawScope.drawMoveToIcon() {
    drawCircle(Palette.Cyan.color, size.minDimension * .18f, Offset(size.width * .2f, size.height * .5f))
    drawLine(Color.Black, Offset(size.width * .38f, size.height * .5f), Offset(size.width * .7f, size.height * .5f), 3.dp.toPx())
    drawRect(Palette.Red.color, Offset(size.width * .7f, size.height * .32f), Size(size.width * .28f, size.height * .36f))
}

private fun DrawScope.drawWinIcon() {
    drawCircle(Palette.Yellow.color, size.minDimension * .38f, center)
    drawLine(Color.Black, Offset(size.width * .28f, size.height * .52f), Offset(size.width * .44f, size.height * .68f), 3.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .44f, size.height * .68f), Offset(size.width * .75f, size.height * .32f), 3.dp.toPx())
}

private fun DrawScope.drawLoseIcon() {
    drawCircle(Palette.Red.color, size.minDimension * .38f, center)
    drawCloseIcon()
}

private fun DrawScope.drawArrowIcon() = drawDirectionIcon(Direction.E)

private fun DrawScope.drawPlusIcon() {
    drawLine(Color.Black, Offset(size.width * .5f, size.height * .15f), Offset(size.width * .5f, size.height * .85f), 3.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .15f, size.height * .5f), Offset(size.width * .85f, size.height * .5f), 3.dp.toPx())
}

private fun DrawScope.drawMinusIcon() {
    drawLine(Color.Black, Offset(size.width * .15f, size.height * .5f), Offset(size.width * .85f, size.height * .5f), 3.dp.toPx())
}

private fun DrawScope.drawCloseIcon() {
    drawLine(Color.Black, Offset(size.width * .2f, size.height * .2f), Offset(size.width * .8f, size.height * .8f), 3.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .8f, size.height * .2f), Offset(size.width * .2f, size.height * .8f), 3.dp.toPx())
}

private fun DrawScope.drawCheckIcon() {
    drawLine(Color.Black, Offset(size.width * .15f, size.height * .52f), Offset(size.width * .42f, size.height * .78f), 3.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .42f, size.height * .78f), Offset(size.width * .88f, size.height * .2f), 3.dp.toPx())
}

private fun DrawScope.drawTrashIcon() {
    drawRect(Color.Black, Offset(size.width * .25f, size.height * .3f), Size(size.width * .5f, size.height * .55f), style = Stroke(2.dp.toPx()))
    drawLine(Color.Black, Offset(size.width * .15f, size.height * .25f), Offset(size.width * .85f, size.height * .25f), 2.dp.toPx())
}

private fun DrawScope.drawTeleportIcon() {
    drawLine(Color.Black, Offset(size.width * .18f, size.height * .25f), Offset(size.width * .18f, size.height * .75f), 3.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .82f, size.height * .25f), Offset(size.width * .82f, size.height * .75f), 3.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .35f, size.height * .5f), Offset(size.width * .65f, size.height * .5f), 3.dp.toPx())
}
