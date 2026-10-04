package com.example.microware.editor.logic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.example.microware.Palette
import com.example.microware.game.events.LogicEvent
import com.example.microware.game.events.LogicsEvent
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Trigger
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.components.IntegerSliderControl
import com.example.microware.ui.components.IntegerSliderDimension
import com.example.microware.ui.components.TrashGap
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.icons.drawAction
import com.example.microware.ui.icons.drawArrowIcon
import com.example.microware.ui.icons.drawCollisionIcon
import com.example.microware.ui.icons.drawDirectionIcon
import com.example.microware.ui.icons.drawDownArrowIcon
import com.example.microware.ui.icons.drawLoseIcon
import com.example.microware.ui.icons.drawMoveToIcon
import com.example.microware.ui.icons.drawStartIcon
import com.example.microware.ui.icons.drawTapIcon
import com.example.microware.ui.icons.drawTrainIcon
import com.example.microware.ui.icons.drawTrashIcon
import com.example.microware.ui.icons.drawWinIcon
import com.example.microware.ui.layout.insets
import com.example.microware.util.Id
import com.example.microware.util.toDp
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class ActionChoice { Move, MoveTo, Win, Lose }
private enum class TriggerLayout { Large, Small }

sealed interface LogicEditorPage {
    object List : LogicEditorPage
    data class Editing(val index: Int?) : LogicEditorPage
}

private const val MaxActionSpeed = 100

internal class LogicEditorState {
    val dragController = DragController<Logic>()
    val willBeDeleted = mutableStateOf(false)
}

@Composable
internal fun rememberLogicEditorState(): LogicEditorState = remember { LogicEditorState() }

@Composable
internal fun LogicEditorContent(
    logics: List<Logic>,
    handler: (LogicsEvent) -> Unit,
    state: LogicEditorState,
    page: LogicEditorPage,
    onPageChange: (LogicEditorPage) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val dragController = state.dragController
    val editingIndex = (page as? LogicEditorPage.Editing)?.index
    var rootBounds by remember { mutableStateOf<(Offset) -> Offset>({ it }) }

    fun applyLogicsChange(updatedLogics: List<Logic>) {
        handler { updatedLogics.toPersistentList() }
        val editingLogicId = editingIndex?.let(logics::getOrNull)?.id ?: return
        val updatedEditingIndex = updatedLogics.indexOfFirst { it.id == editingLogicId }
        if (updatedEditingIndex != editingIndex) {
            onPageChange(LogicEditorPage.Editing(updatedEditingIndex))
        }
    }

    fun handleLogicDrop(logic: Logic, targetIndex: Int) {
        val updatedLogics = logics.toMutableList().apply {
            add(targetIndex, logic.newId())
        }
        applyLogicsChange(updatedLogics)
    }

    fun handleLogicMove(id: Id, targetIndex: Int) {
        val sourceIndex = logics.indexOfFirst { it.id == id }
        if (sourceIndex == targetIndex) return

        val updatedLogics = logics.toMutableList().apply {
            val logic = removeAt(sourceIndex)
            add(targetIndex, logic)
        }
        applyLogicsChange(updatedLogics)
    }

    Box(
        modifier = modifier.onGloballyPositioned { rootBounds = it::windowToLocal },
    ) {
        Column {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        start = insets.leftDp(),
                        end = insets.rightDp(),
                    ),
            ) {
                LogicTable(
                    items = logics,
                    dragController = dragController,
                    sourceId = LogicListDragSource,
                    onMove = ::handleLogicMove,
                    onDrop = { offer, targetIndex -> handleLogicDrop(offer.item, targetIndex) },
                )
            }
            Spacer(Modifier.height(HandleSize + TrashGap + insets.bottomDp()))
        }
        dragController.offer?.let { offer ->
            Box(
                modifier = Modifier
                    .size(offer.size.width.toDp(), offer.size.height.toDp())
                    .offset {
                        (rootBounds(offer.offset) - offer.localOffset).round()
                    },
            ) {
                LogicCard(offer.item, Modifier.fillMaxSize())
            }
        }
    }
}

sealed interface LogicBuildHandler {
    data class New(val new: (Trigger) -> Unit) : LogicBuildHandler
    data class Update(val logic: Logic, val update: (LogicEvent) -> Unit) : LogicBuildHandler
}

@Composable
internal fun LogicBuilder(
    usedTags: Set<LogicTag>,
    handler: LogicBuildHandler,
) {
    val updater = handler as? LogicBuildHandler.Update
    val initialLogic = updater?.logic
    val defaultTag = usedTags.firstOrNull() ?: LogicTag.all.first()
    var trigger by remember(initialLogic) { mutableStateOf(initialLogic?.trigger) }
    var actions by remember(initialLogic) { mutableStateOf(initialLogic?.actions.orEmpty()) }
    var triggerLayout by remember(initialLogic) {
        mutableStateOf(if (initialLogic == null) TriggerLayout.Large else TriggerLayout.Small)
    }
    var detailsVisible by remember(initialLogic) { mutableStateOf(initialLogic != null) }

    fun updateTrigger(trigger: Trigger) {
        when (handler) {
            is LogicBuildHandler.New -> handler.new(trigger)
            is LogicBuildHandler.Update -> handler.update { logic -> logic.copy(trigger = trigger) }
        }
    }

    fun updateActions(actions: List<Action>) {
        when (handler) {
            is LogicBuildHandler.New -> throw RuntimeException("Something went really wrong with the ui")
            is LogicBuildHandler.Update -> handler.update { logic -> logic.copy(actions = actions) }
        }
    }

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
                onStartSelected = { updateTrigger(Trigger.Start) },
                onTapSelected = { updateTrigger(Trigger.Tap(defaultTag)) },
                onCollisionSelected = {
                    updateTrigger(Trigger.Collision(defaultTag, defaultTag))
                },
            )

            AnimatedVisibility(
                visible = detailsVisible,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            ) {
                Column(Modifier.fillMaxSize()) {
                    trigger?.let { selectedTrigger ->
                        TriggerParameters(
                            selectedTrigger,
                            usedTags,
                        ) { updateTrigger(it) }
                        Canvas(Modifier.fillMaxWidth().height(24.dp)) { drawDownArrowIcon() }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(actions) { index, action ->
                            EditableActionCard(
                                action = action,
                                usedTags = usedTags,
                                onChange = { changed ->
                                    updateActions(
                                        actions.mapIndexed { candidateIndex, current ->
                                            if (candidateIndex == index) changed else current
                                        },
                                    )
                                },
                                onDelete = {
                                    updateActions(
                                        actions.filterIndexed { candidateIndex, _ ->
                                            candidateIndex != index
                                        },
                                    )
                                },
                            )
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                ActionButton(ActionChoice.Move) {
                                    updateActions(
                                        actions + Action.Move(
                                            defaultTag,
                                            Direction.E,
                                            50,
                                        ),
                                    )
                                }
                                ActionButton(ActionChoice.MoveTo) {
                                    updateActions(
                                        actions + Action.MoveTo(
                                            defaultTag,
                                            defaultTag,
                                            50,
                                        ),
                                    )
                                }
                                ActionButton(ActionChoice.Win) {
                                    updateActions(actions + Action.Win)
                                }
                                ActionButton(ActionChoice.Lose) {
                                    updateActions(actions + Action.Lose)
                                }
                            }
                        }
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
            horizontalArrangement = Arrangement.spacedBy(
                buttonSpacing,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriggerButton(
                description = "Game start trigger",
                selected = trigger is Trigger.Start,
                modifier = Modifier.size(buttonWidth, buttonHeight),
                onClick = onStartSelected,
            )
            TriggerButton(
                description = "Tap trigger",
                selected = trigger is Trigger.Tap,
                modifier = Modifier.size(buttonWidth, buttonHeight),
                onClick = onTapSelected,
            )
            TriggerButton(
                description = "Collision trigger",
                selected = trigger is Trigger.Collision,
                modifier = Modifier.size(buttonWidth, buttonHeight),
                onClick = onCollisionSelected,
            )
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(
                buttonSpacing,
                Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriggerButton("Game start trigger", trigger is Trigger.Start, onClick = onStartSelected)
            TriggerButton("Tap trigger", trigger is Trigger.Tap, onClick = onTapSelected)
            TriggerButton(
                "Collision trigger",
                trigger is Trigger.Collision,
                onClick = onCollisionSelected,
            )
        }
    }
}

@Composable
private fun TriggerButton(
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier.size(48.dp),
    onClick: () -> Unit,
) {
    IconButton(
        description = description,
        modifier = modifier,
        selected = selected,
        onClick = onClick,
    ) { time ->
        when {
            description.startsWith("Collision") -> drawCollisionIcon(time)
            description.startsWith("Game") -> drawStartIcon(time)
            description.startsWith("Tap") -> drawTapIcon(time)
            else -> drawCollisionIcon(time)
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
private fun EditableActionCard(
    action: Action,
    usedTags: Set<LogicTag>,
    onChange: (Action) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, Color.LightGray, shape = RoundedCornerShape(8.dp))
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(36.dp).padding(4.dp)) { drawAction(action) }
            Box(Modifier.weight(1f))
            IconButton("Delete action", onClick = onDelete) { _ -> drawTrashIcon() }
        }

        when (action) {
            is Action.Move -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        6.dp,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LogicTagPicker(
                        action.mover,
                        {
                            it?.let { tag ->
                                onChange(
                                    Action.Move(
                                        tag,
                                        action.target,
                                        action.speed,
                                    ),
                                )
                            }
                        },
                        usedTags = usedTags,
                        allowClear = false,
                    )
                    IconButton(
                        "Change direction",
                        onClick = {
                            val directions =
                                listOf(Direction.N, Direction.E, Direction.S, Direction.W)
                            val nextDirection =
                                directions[(directions.indexOf(action.target) + 1) % directions.size]
                            onChange(Action.Move(action.mover, nextDirection, action.speed))
                        },
                    ) { time -> drawDirectionIcon(action.target, time) }
                }
                SpeedControl(action.speed) {
                    it?.let { speed ->
                        onChange(Action.Move(action.mover, action.target, speed))
                    }
                }
            }

            is Action.MoveTo -> Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        6.dp,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LogicTagPicker(
                        action.mover,
                        {
                            it?.let { tag ->
                                onChange(
                                    Action.MoveTo(
                                        tag,
                                        action.target,
                                        action.speed,
                                    ),
                                )
                            }
                        },
                        usedTags = usedTags,
                        allowClear = false,
                    )
                    Canvas(Modifier.size(18.dp)) { drawArrowIcon() }
                    LogicTagPicker(
                        action.target,
                        {
                            it?.let { tag ->
                                onChange(
                                    Action.MoveTo(
                                        action.mover,
                                        tag,
                                        action.speed,
                                    ),
                                )
                            }
                        },
                        usedTags = usedTags,
                        allowClear = false,
                    )
                }
                SpeedControl(action.speed, allowInfinity = true) {
                    onChange(Action.MoveTo(action.mover, action.target, it))
                }
            }

            Action.Win, Action.Lose -> Unit
        }
    }
}

@Composable
private fun SpeedControl(
    speed: Int?,
    allowInfinity: Boolean = false,
    onChange: (Int?) -> Unit,
) {
    IntegerSliderControl(
        dimension = IntegerSliderDimension.Speed,
        value = speed,
        maxValue = MaxActionSpeed,
        onValueChange = onChange,
        modifier = Modifier.padding(horizontal = 8.dp),
        activeColor = Palette.Black,
        allowInfinity = allowInfinity,
    )
}

@Composable
private fun ActionButton(choice: ActionChoice, onClick: () -> Unit) {
    IconButton("Add ${choice.name} action", onClick = onClick) { time ->
        when (choice) {
            ActionChoice.Move -> drawTrainIcon(time)
            ActionChoice.MoveTo -> drawMoveToIcon()
            ActionChoice.Win -> drawWinIcon(time)
            ActionChoice.Lose -> drawLoseIcon()
        }
    }
}
