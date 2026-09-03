package com.example.microware

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class ActionChoice { Move, MoveTo, Win, Lose }
private enum class TriggerLayout { Large, Small }

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
    var triggerLayout by remember(initialLogic) {
        mutableStateOf(if (initialLogic == null) TriggerLayout.Large else TriggerLayout.Small)
    }
    var detailsVisible by remember(initialLogic) { mutableStateOf(initialLogic != null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.leftDp() + 8.dp,
                end = insets.rightDp() + 8.dp,
                bottom = insets.bottomDp() + 8.dp,
            ),
    ) {
        val largeTriggerSpacing = 8.dp
        val compactButtonSize = 48.dp
        val compactRowHeight = compactButtonSize
        val largeButtonSize = minOf(
            (maxWidth - largeTriggerSpacing * 2) / 3,
            maxHeight,
        )
        val initialTriggerAreaHeight = if (detailsVisible) compactRowHeight else largeButtonSize
        val triggerAreaHeight = remember(maxWidth, maxHeight, initialLogic) {
            Animatable(initialTriggerAreaHeight.value)
        }
        val triggerButtonWidth = remember(maxWidth, maxHeight, initialLogic) {
            Animatable((if (detailsVisible) compactButtonSize else largeButtonSize).value)
        }
        val triggerButtonHeight = remember(maxWidth, maxHeight, initialLogic) {
            Animatable((if (detailsVisible) compactButtonSize else largeButtonSize).value)
        }
        val triggerButtonSpacing = remember(maxWidth, maxHeight, initialLogic) {
            Animatable(if (detailsVisible) 0f else largeTriggerSpacing.value)
        }

        LaunchedEffect(trigger, maxWidth, maxHeight) {
            if (trigger == null || detailsVisible) return@LaunchedEffect

            coroutineScope {
                launch {
                    triggerAreaHeight.animateTo(
                        targetValue = compactRowHeight.value,
                        animationSpec = tween(durationMillis = 300),
                    )
                }
                launch {
                    triggerButtonWidth.animateTo(
                        targetValue = compactButtonSize.value,
                        animationSpec = tween(durationMillis = 300),
                    )
                }
                launch {
                    triggerButtonHeight.animateTo(
                        targetValue = compactButtonSize.value,
                        animationSpec = tween(durationMillis = 300),
                    )
                }
                launch {
                    triggerButtonSpacing.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 300),
                    )
                }
            }

            triggerLayout = TriggerLayout.Small
            detailsVisible = true
        }

        Column(Modifier.fillMaxSize()) {
            TriggerButtons(
                trigger = trigger,
                modifier = Modifier.fillMaxWidth().height(triggerAreaHeight.value.dp),
                layout = triggerLayout,
                buttonWidth = triggerButtonWidth.value.dp,
                buttonHeight = triggerButtonHeight.value.dp,
                buttonSpacing = triggerButtonSpacing.value.dp,
                onStartSelected = { trigger = Trigger.Start },
                onTapSelected = { trigger = Trigger.Tap(defaultTag) },
                onCollisionSelected = { trigger = Trigger.Collision(defaultTag, defaultTag) },
            )

            AnimatedVisibility(
                visible = detailsVisible,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            ) {
                Column(Modifier.fillMaxSize()) {
                    trigger?.let { selectedTrigger ->
                        TriggerParameters(selectedTrigger, usedTags) { trigger = it }
                        Canvas(Modifier.fillMaxWidth().height(24.dp)) { drawDownArrowIcon() }
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
        }
    }
}

@Composable
private fun TriggerButtons(
    trigger: Trigger?,
    modifier: Modifier,
    layout: TriggerLayout,
    buttonWidth: Dp,
    buttonHeight: Dp,
    buttonSpacing: Dp,
    onStartSelected: () -> Unit,
    onTapSelected: () -> Unit,
    onCollisionSelected: () -> Unit,
) {
    if (layout == TriggerLayout.Large) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriggerButton(
                description = "Game start trigger",
                selected = trigger is Trigger.Start,
                modifier = Modifier.size(buttonWidth, buttonHeight),
                buttonSize = null,
                onClick = onStartSelected,
            )
            TriggerButton(
                description = "Tap trigger",
                selected = trigger is Trigger.Tap,
                modifier = Modifier.size(buttonWidth, buttonHeight),
                buttonSize = null,
                onClick = onTapSelected,
            )
            TriggerButton(
                description = "Collision trigger",
                selected = trigger is Trigger.Collision,
                modifier = Modifier.size(buttonWidth, buttonHeight),
                buttonSize = null,
                onClick = onCollisionSelected,
            )
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriggerButton("Game start trigger", trigger is Trigger.Start, onClick = onStartSelected)
            TriggerButton("Tap trigger", trigger is Trigger.Tap, onClick = onTapSelected)
            TriggerButton("Collision trigger", trigger is Trigger.Collision, onClick = onCollisionSelected)
        }
    }
}

@Composable
private fun TriggerButton(
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    buttonSize: Dp? = 48.dp,
    onClick: () -> Unit,
) {
    IconButton(
        description = description,
        modifier = modifier,
        selected = selected,
        buttonSize = buttonSize,
        onClick = onClick,
    ) {
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
        Canvas(Modifier.size(28.dp).padding(4.dp)) { drawEditIcon() }
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
    Column(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black).padding(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(36.dp).padding(4.dp)) { drawAction(action) }
            Box(Modifier.weight(1f))
            IconButton("Delete action", onClick = onDelete) { drawTrashIcon() }
        }

        when (action) {
            is Action.Move -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            is Action.MoveTo -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LogicTagPicker(
                    action.mover,
                    { it?.let { tag -> onChange(Action.MoveTo(tag, action.target, action.speed)) } },
                    usedTags = usedTags,
                    allowClear = false,
                )
                Canvas(Modifier.size(18.dp)) { drawArrowIcon() }
                LogicTagPicker(
                    action.target,
                    { it?.let { tag -> onChange(Action.MoveTo(action.mover, tag, action.speed)) } },
                    usedTags = usedTags,
                    allowClear = false,
                )
                IconButton("Toggle instant teleport", modifier = Modifier.size(40.dp), selected = action.speed == null, onClick = {
                    onChange(Action.MoveTo(action.mover, action.target, if (action.speed == null) 1 else null))
                }) { drawTeleportIcon() }
                action.speed?.let { speed ->
                    SpeedControl(speed, compact = true) {
                        onChange(Action.MoveTo(action.mover, action.target, it))
                    }
                }
            }
            Action.Win, Action.Lose -> Unit
        }
    }
}

@Composable
private fun SpeedControl(speed: Int, compact: Boolean = false, onChange: (Int) -> Unit) {
    val controlSize = if (compact) 40.dp else 48.dp
    val indicatorSize = if (compact) 20.dp else 24.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton("Slower", modifier = Modifier.size(controlSize), enabled = speed > 1, onClick = { onChange(speed - 1) }) {
            drawMinusIcon()
        }
        Canvas(Modifier.size(indicatorSize)) {
            repeat(speed.coerceAtMost(5)) { index ->
                val y = size.height * (index + 1) / (speed.coerceAtMost(5) + 1)
                drawLine(Color.Black, Offset(size.width * .25f, y), Offset(size.width * .75f, y), 2.dp.toPx())
            }
        }
        IconButton("Faster", modifier = Modifier.size(controlSize), enabled = speed < 5, onClick = { onChange(speed + 1) }) {
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
    val touchPoint = Offset(size.width * .5f, size.height * .2f)
    drawCircle(Palette.Cyan.color, size.minDimension * .18f, touchPoint, style = Stroke(2.dp.toPx()))
    val finger = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * .5f, size.height * .82f)
        lineTo(size.width * .34f, size.height * .62f)
        lineTo(size.width * .34f, size.height * .5f)
        quadraticTo(size.width * .34f, size.height * .43f, size.width * .42f, size.height * .5f)
        lineTo(size.width * .42f, size.height * .22f)
        quadraticTo(size.width * .42f, size.height * .12f, size.width * .5f, size.height * .12f)
        quadraticTo(size.width * .58f, size.height * .12f, size.width * .58f, size.height * .22f)
        lineTo(size.width * .58f, size.height * .48f)
        quadraticTo(size.width * .82f, size.height * .43f, size.width * .82f, size.height * .62f)
        lineTo(size.width * .72f, size.height * .82f)
        close()
    }
    drawPath(finger, Palette.OffWhite.color)
    drawPath(finger, Color.Black, style = Stroke(2.dp.toPx()))
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
    val arrowEnd = Offset(size.width * .68f, size.height * .5f)
    drawLine(Color.Black, Offset(size.width * .38f, size.height * .5f), arrowEnd, 2.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .55f, size.height * .38f), arrowEnd, 2.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .55f, size.height * .62f), arrowEnd, 2.dp.toPx())
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

private fun DrawScope.drawDownArrowIcon() {
    val centerX = size.width / 2f
    val end = Offset(centerX, size.height * .85f)
    val strokeWidth = 2.dp.toPx()
    drawLine(Color.Black, Offset(centerX, size.height * .1f), end, strokeWidth, StrokeCap.Round)
    drawLine(Color.Black, Offset(centerX - size.height * .22f, size.height * .62f), end, strokeWidth, StrokeCap.Round)
    drawLine(Color.Black, Offset(centerX + size.height * .22f, size.height * .62f), end, strokeWidth, StrokeCap.Round)
}

private fun DrawScope.drawEditIcon() {
    val strokeWidth = 3.dp.toPx()
    drawLine(
        Color.Black,
        Offset(size.width * .2f, size.height * .8f),
        Offset(size.width * .75f, size.height * .25f),
        strokeWidth,
        StrokeCap.Round,
    )
    drawLine(
        Color.Black,
        Offset(size.width * .68f, size.height * .18f),
        Offset(size.width * .82f, size.height * .32f),
        strokeWidth,
        StrokeCap.Round,
    )
    drawCircle(Color.Black, strokeWidth * .65f, Offset(size.width * .18f, size.height * .82f))
}

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
    drawLine(Color.Black, Offset(size.width * .4f, size.height * .15f), Offset(size.width * .6f, size.height * .15f), 2.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .4f, size.height * .4f), Offset(size.width * .4f, size.height * .75f), 1.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .6f, size.height * .4f), Offset(size.width * .6f, size.height * .75f), 1.dp.toPx())
}

private fun DrawScope.drawTeleportIcon() {
    drawLine(Palette.Cyan.color, Offset(size.width * .15f, size.height * .18f), Offset(size.width * .15f, size.height * .82f), 3.dp.toPx())
    drawLine(Palette.Cyan.color, Offset(size.width * .85f, size.height * .18f), Offset(size.width * .85f, size.height * .82f), 3.dp.toPx())
    val bolt = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * .55f, size.height * .12f)
        lineTo(size.width * .28f, size.height * .55f)
        lineTo(size.width * .48f, size.height * .55f)
        lineTo(size.width * .4f, size.height * .88f)
        lineTo(size.width * .72f, size.height * .42f)
        lineTo(size.width * .52f, size.height * .42f)
        close()
    }
    drawPath(bolt, Palette.Yellow.color)
    drawPath(bolt, Color.Black, style = Stroke(1.dp.toPx()))
}
