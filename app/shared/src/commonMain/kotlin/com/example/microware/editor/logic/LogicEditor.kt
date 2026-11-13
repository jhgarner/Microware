package com.example.microware.editor.logic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.game.events.LogicEvent
import com.example.microware.game.events.LogicsEvent
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Parameter
import com.example.microware.game.model.Selector
import com.example.microware.game.model.TagFilter
import com.example.microware.game.model.Trigger
import com.example.microware.game.model.parameters
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.components.IconPopupButton
import com.example.microware.ui.components.IntegerSliderControl
import com.example.microware.ui.components.IntegerSliderDimension
import com.example.microware.ui.components.TrashGap
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.icons.drawAction
import com.example.microware.ui.icons.drawArrowIcon
import com.example.microware.ui.icons.drawCollisionIcon
import com.example.microware.ui.icons.drawDirectionIcon
import com.example.microware.ui.icons.drawDownArrowIcon
import com.example.microware.ui.icons.drawDownIcon
import com.example.microware.ui.icons.drawLogicTags
import com.example.microware.ui.icons.drawLoseIcon
import com.example.microware.ui.icons.drawMoveIcon
import com.example.microware.ui.icons.drawMoveToIcon
import com.example.microware.ui.icons.drawMoveWithinIcon
import com.example.microware.ui.icons.drawNumberIcon
import com.example.microware.ui.icons.drawSeparationIcon
import com.example.microware.ui.icons.drawStartIcon
import com.example.microware.ui.icons.drawTapIcon
import com.example.microware.ui.icons.drawTrashIcon
import com.example.microware.ui.icons.drawWinIcon
import com.example.microware.ui.layout.insets
import com.example.microware.util.Id
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class ActionChoice { Move, MoveTo, MoveWithin, Win, Lose }
private enum class TriggerLayout { Large, Small }

sealed interface LogicEditorPage {
    object List : LogicEditorPage
    data class Editing(val index: Int?) : LogicEditorPage
}

private const val MaxActionSpeed = 3000
private const val MaxCollisionCount = 100

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

    Column(modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                ),
        ) {
            LogicTable(
                items = logics,
                dragController = dragController,
                sourceId = LogicListDragSource,
                onMove = ::handleLogicMove,
                onDrop = { offer, targetIndex -> handleLogicDrop(offer.item, targetIndex) },
                showsGhost = true,
            )
        }
        Spacer(Modifier.height(HandleSize + TrashGap + insets.bottomDp()))
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
    val defaultTag = TagFilter(usedTags.firstOrNull() ?: LogicTag(0))
    var trigger by remember(initialLogic) { mutableStateOf(initialLogic?.trigger) }
    var actions by remember(initialLogic) { mutableStateOf(initialLogic?.actions.orEmpty()) }
    var triggerLayout by remember(initialLogic) {
        mutableStateOf(if (initialLogic == null) TriggerLayout.Large else TriggerLayout.Small)
    }
    var detailsVisible by remember(initialLogic) { mutableStateOf(initialLogic != null) }

    fun updateTrigger(trigger: Trigger) {
        when (handler) {
            is LogicBuildHandler.New -> handler.new(trigger)
            is LogicBuildHandler.Update -> handler.update { logic ->
                logic.copy(
                    trigger = trigger,
                    actions = logic.actions.map { it.keepingParametersOf(trigger, defaultTag) },
                )
            }
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
                start = 8.dp,
                end = 8.dp,
                bottom = insets.bottomDp() + 8.dp,
            ),
    ) {
        val largeTriggerSpacing = 8.dp
        val compactButtonSize = 48.dp
        val compactRowHeight = compactButtonSize
        val largeButtonSize = minOf(
            (maxWidth - largeTriggerSpacing * 4) / 5,
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
                onPressSelected = { updateTrigger(Trigger.Press(defaultTag)) },
                onReleaseSelected = { updateTrigger(Trigger.Release(defaultTag)) },
                onCollisionSelected = {
                    updateTrigger(Trigger.Collision(defaultTag, defaultTag))
                },
                onSeparationSelected = {
                    updateTrigger(Trigger.Separation(defaultTag, defaultTag))
                },
            )

            AnimatedVisibility(
                visible = detailsVisible,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                enter = fadeIn(animationSpec = tween(durationMillis = 180)),
            ) {
                Column(Modifier.fillMaxSize()) {
                    val selectedTrigger = trigger ?: return@Column
                    TriggerParameters(
                        selectedTrigger,
                        usedTags,
                    ) { updateTrigger(it) }
                    Canvas(Modifier.fillMaxWidth().height(24.dp)) { drawDownArrowIcon() }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(actions) { index, action ->
                            EditableActionCard(
                                action = action,
                                trigger = selectedTrigger,
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
                                ActionButton(ActionChoice.MoveWithin) {
                                    updateActions(
                                        actions + Action.MoveWithin(
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
    onPressSelected: () -> Unit,
    onReleaseSelected: () -> Unit,
    onCollisionSelected: () -> Unit,
    onSeparationSelected: () -> Unit,
) {
    val buttonModifier = if (layout == TriggerLayout.Large) {
        Modifier.size(buttonWidth, buttonHeight)
    } else {
        Modifier.size(48.dp)
    }

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
            modifier = buttonModifier,
            onClick = onStartSelected,
            icon = { drawStartIcon(it) },
        )
        TriggerButton(
            description = "Press trigger",
            selected = trigger is Trigger.Press,
            modifier = buttonModifier,
            onClick = onPressSelected,
            icon = { drawDownIcon(it) },
        )
        TriggerButton(
            description = "Release trigger",
            selected = trigger is Trigger.Release,
            modifier = buttonModifier,
            onClick = onReleaseSelected,
            icon = { drawTapIcon(it) },
        )
        TriggerButton(
            description = "Collision trigger",
            selected = trigger is Trigger.Collision,
            modifier = buttonModifier,
            onClick = onCollisionSelected,
            icon = { drawCollisionIcon(it) },
        )
        TriggerButton(
            description = "Separation trigger",
            selected = trigger is Trigger.Separation,
            modifier = buttonModifier,
            onClick = onSeparationSelected,
            icon = { drawSeparationIcon(it) },
        )
    }
}

@Composable
private fun TriggerButton(
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier.size(48.dp),
    onClick: () -> Unit,
    icon: DrawScope.(Float) -> Unit,
) {
    IconButton(
        description = description,
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        icon = icon,
    )
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
            is Trigger.Press -> TagFilterPicker(trigger.target, usedTags) {
                onChange(Trigger.Press(it))
            }

            is Trigger.Release -> TagFilterPicker(trigger.target, usedTags) {
                onChange(Trigger.Release(it))
            }

            is Trigger.Collision -> ContactParameters(
                trigger = trigger,
                usedTags = usedTags,
                icon = { drawCollisionIcon() },
                createTrigger = Trigger::Collision,
                onChange = onChange,
            )

            is Trigger.Separation -> ContactParameters(
                trigger = trigger,
                usedTags = usedTags,
                icon = { drawSeparationIcon() },
                createTrigger = Trigger::Separation,
                onChange = onChange,
            )
        }
    }
}

@Composable
private fun ContactParameters(
    trigger: Trigger.Contact,
    usedTags: Set<LogicTag>,
    icon: DrawScope.() -> Unit,
    createTrigger: (TagFilter, TagFilter, Int) -> Trigger,
    onChange: (Trigger) -> Unit,
) {
    fun change(
        firstTarget: TagFilter = trigger.firstTarget,
        secondTarget: TagFilter = trigger.secondTarget,
        count: Int = trigger.firstTargetCount,
    ) = onChange(createTrigger(firstTarget, secondTarget, count))

    ContactCountPicker(trigger.firstTargetCount, trigger.firstTarget) { change(count = it) }
    TagFilterPicker(trigger.firstTarget, usedTags) { change(firstTarget = it) }
    Canvas(Modifier.size(28.dp), onDraw = icon)
    TagFilterPicker(trigger.secondTarget, usedTags) { change(secondTarget = it) }
}

@Composable
private fun ContactCountPicker(
    count: Int,
    counted: TagFilter,
    onChange: (Int) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    IconPopupButton(
        description = "Colliding objects needed: $count",
        modifier = Modifier.size(48.dp),
        icon = { drawNumberIcon(count, textMeasurer) },
    ) {
        IntegerSliderControl(
            label = "Colliding objects needed",
            value = count,
            maxValue = MaxCollisionCount,
            onValueChange = { onChange(it!!) },
            modifier = Modifier.width(280.dp).padding(8.dp),
            activeColor = Palette.Black,
        ) { drawLogicTags(counted.tags) }
    }
}

@Composable
private fun EditableActionCard(
    action: Action,
    trigger: Trigger,
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
            IconButton(
                "Delete action",
                modifier = Modifier.size(48.dp),
                onClick = onDelete,
            ) { _ -> drawTrashIcon() }
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
                    SelectorPicker(action.mover, trigger, usedTags) {
                        onChange(Action.Move(it, action.target, action.speed))
                    }
                    IconButton(
                        "Change direction",
                        modifier = Modifier.size(48.dp),
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

            is Action.MoveTo -> TargetedMovementEditor(
                mover = action.mover,
                target = action.target,
                speed = action.speed,
                trigger = trigger,
                usedTags = usedTags,
                createAction = Action::MoveTo,
                onChange = onChange,
            )

            is Action.MoveWithin -> TargetedMovementEditor(
                mover = action.mover,
                target = action.target,
                speed = action.speed,
                trigger = trigger,
                usedTags = usedTags,
                createAction = Action::MoveWithin,
                onChange = onChange,
            )

            Action.Win, Action.Lose -> Unit
        }
    }
}

@Composable
private fun TargetedMovementEditor(
    mover: Selector,
    target: Selector,
    speed: Int?,
    trigger: Trigger,
    usedTags: Set<LogicTag>,
    createAction: (Selector, Selector, Int?) -> Action,
    onChange: (Action) -> Unit,
) {
    Column(
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
            SelectorPicker(mover, trigger, usedTags) { onChange(createAction(it, target, speed)) }
            Canvas(Modifier.size(18.dp)) { drawArrowIcon() }
            SelectorPicker(target, trigger, usedTags) { onChange(createAction(mover, it, speed)) }
        }
        SpeedControl(speed, allowInfinity = true) {
            onChange(createAction(mover, target, it))
        }
    }
}

/** Parameters that [trigger] does not offer are replaced by [fallback]. */
private fun Action.keepingParametersOf(trigger: Trigger, fallback: TagFilter): Action {
    fun Selector.kept() = if (this is Parameter && this !in trigger.parameters) fallback else this

    return when (this) {
        is Action.Move -> Action.Move(mover.kept(), target, speed)
        is Action.MoveTo -> Action.MoveTo(mover.kept(), target.kept(), speed)
        is Action.MoveWithin -> Action.MoveWithin(mover.kept(), target.kept(), speed)
        Action.Win, Action.Lose -> this
    }
}

@Composable
private fun SpeedControl(
    speed: Int?,
    allowInfinity: Boolean = false,
    modifier: Modifier = Modifier.padding(horizontal = 8.dp),
    onChange: (Int?) -> Unit,
) {
    IntegerSliderControl(
        dimension = IntegerSliderDimension.Speed,
        value = speed,
        maxValue = MaxActionSpeed,
        onValueChange = onChange,
        modifier = modifier,
        activeColor = Palette.Black,
        allowInfinity = allowInfinity,
    )
}

@Composable
private fun ActionButton(choice: ActionChoice, onClick: () -> Unit) {
    IconButton(
        "Add ${choice.name} action",
        modifier = Modifier.size(48.dp),
        onClick = onClick,
    ) { time ->
        when (choice) {
            ActionChoice.Move -> drawMoveIcon(Direction.E, time)
            ActionChoice.MoveTo -> drawMoveToIcon(time)
            ActionChoice.MoveWithin -> drawMoveWithinIcon(time)
            ActionChoice.Win -> drawWinIcon(time)
            ActionChoice.Lose -> drawLoseIcon()
        }
    }
}

@Preview(widthDp = 360)
@Composable
fun CollisionTriggerParametersPreview() {
    var trigger: Trigger by remember {
        mutableStateOf(
            Trigger.Collision(
                firstTarget = TagFilter(LogicTag(0), LogicTag(5)),
                secondTarget = TagFilter(LogicTag(9)),
                firstTargetCount = 2,
            ),
        )
    }
    Box(Modifier.background(Palette.OffWhite.color)) {
        TriggerParameters(trigger, usedTags = emptySet()) { trigger = it }
    }
}
