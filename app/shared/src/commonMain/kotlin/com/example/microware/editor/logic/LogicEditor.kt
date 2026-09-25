package com.example.microware.editor.logic
import com.example.microware.editor.IconButton
import com.example.microware.game.events.LogicEvent
import com.example.microware.game.events.LogicsEvent
import com.example.microware.game.model.Action
import com.example.microware.game.model.Direction
import com.example.microware.game.model.Game
import com.example.microware.game.model.Logic
import com.example.microware.game.model.LogicTag
import com.example.microware.game.model.Trigger
import com.example.microware.game.sprite.Palette
import com.example.microware.ui.components.HandleSize
import com.example.microware.ui.components.IntegerSliderControl
import com.example.microware.ui.components.IntegerSliderDimension
import com.example.microware.ui.components.TrashGap
import com.example.microware.ui.gestures.DragController
import com.example.microware.ui.layout.insets
import com.example.microware.util.Id
import com.example.microware.util.toDp
import com.example.microware.util.toPx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Easing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.util.lerp
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ActionChoice { Move, MoveTo, Win, Lose }
private enum class TriggerLayout { Large, Small }

sealed interface LogicEditorPage {
    object List : LogicEditorPage
    data class Editing(val index: Int?) : LogicEditorPage
}

private const val MaxActionSpeed = 100

private data class DirectionStar(
    val crossAxisPosition: Float,
    val travelOffset: Float,
    val radius: Float,
)

private data class TrainCloud(
    val x: Float,
    val y: Float,
    val scale: Float,
)

private data class TrainSteamPuff(
    val x: Float,
    val y: Float,
    val driftX: Float,
    val driftY: Float,
    val radius: Float,
    val delay: Float,
)

private val trainClouds = listOf(
    TrainCloud(.2f, .14f, .9f),
    TrainCloud(.72f, .22f, .7f),
)

private val trainSteamPuffs = listOf(
    TrainSteamPuff(.59f, .27f, -.08f, -.15f, .055f, .0f),
    TrainSteamPuff(.62f, .28f, .09f, -.2f, .05f, .18f),
    TrainSteamPuff(.57f, .28f, -.14f, -.24f, .045f, .34f),
)

private data class ConfettiPiece(
    val startX: Float,
    val startY: Float,
    val driftX: Float,
    val driftY: Float,
    val length: Float,
    val thickness: Float,
    val rotation: Float,
    val color: Color,
    val delay: Float,
)

private val winConfetti = listOf(
    ConfettiPiece(.3f, .2f, -.3f, -.48f, .09f, .035f, -25f, Palette.Red.color, .02f),
    ConfettiPiece(.4f, .18f, -.16f, -.66f, .07f, .03f, 20f, Palette.Cyan.color, .08f),
    ConfettiPiece(.5f, .16f, .02f, -.78f, .08f, .035f, 75f, Palette.Orange.color, .04f),
    ConfettiPiece(.6f, .18f, .2f, -.6f, .07f, .03f, -45f, Palette.Purple.color, .1f),
    ConfettiPiece(.7f, .2f, .34f, -.43f, .09f, .035f, 35f, Palette.Green.color, .06f),
    ConfettiPiece(.35f, .21f, -.5f, -.3f, .06f, .03f, 60f, Palette.Pink.color, .14f),
    ConfettiPiece(.65f, .21f, .48f, -.28f, .06f, .03f, -70f, Palette.Yellow.color, .12f),
)

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
    var rootBounds by remember { mutableStateOf(Rect.Zero) }

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
        modifier = modifier.onGloballyPositioned { rootBounds = it.boundsInWindow() },
    ) {
        LogicTable(
            items = logics,
            dragController = dragController,
            sourceId = LogicListDragSource,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.leftDp(),
                    end = insets.rightDp(),
                    top = insets.topDp(),
                ),
            belowList = {
                Spacer(Modifier.height(HandleSize + TrashGap + insets.bottomDp()))
            },
            onMove = ::handleLogicMove,
            onDrop = { offer, targetIndex -> handleLogicDrop(offer.item, targetIndex) },
        )
        dragController.offer?.let { offer ->
            Box(
                modifier = Modifier
                    .size(offer.size.width.toDp(), offer.size.height.toDp())
                    .offset {
                        (offer.offset - rootBounds.topLeft - offer.localOffset).round()
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
                            usedTags
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
                                        }
                                    )
                                },
                                onDelete = {
                                    updateActions(
                                        actions.filterIndexed { candidateIndex, _ ->
                                            candidateIndex != index
                                        }
                                    )
                                },
                            )
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ActionButton(ActionChoice.Move) {
                                    updateActions(
                                        actions + Action.Move(
                                            defaultTag,
                                            Direction.E,
                                            50
                                        )
                                    )
                                }
                                ActionButton(ActionChoice.MoveTo) {
                                    updateActions(
                                        actions + Action.MoveTo(
                                            defaultTag,
                                            defaultTag,
                                            50
                                        )
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
                Alignment.CenterHorizontally
            ),
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
            horizontalArrangement = Arrangement.spacedBy(
                buttonSpacing,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriggerButton("Game start trigger", trigger is Trigger.Start, onClick = onStartSelected)
            TriggerButton("Tap trigger", trigger is Trigger.Tap, onClick = onTapSelected)
            TriggerButton(
                "Collision trigger",
                trigger is Trigger.Collision,
                onClick = onCollisionSelected
            )
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
                        Alignment.CenterHorizontally
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
                                        action.speed
                                    )
                                )
                            }
                        },
                        usedTags = usedTags,
                        allowClear = false,
                    )
                    IconButton("Change direction", onClick = {
                        val directions = listOf(Direction.N, Direction.E, Direction.S, Direction.W)
                        val nextDirection =
                            directions[(directions.indexOf(action.target) + 1) % directions.size]
                        onChange(Action.Move(action.mover, nextDirection, action.speed))
                    }) { time -> drawDirectionIcon(action.target, time) }
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
                        Alignment.CenterHorizontally
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
                                        action.speed
                                    )
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
                                        action.speed
                                    )
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

internal fun DrawScope.drawTrigger(trigger: Trigger) = when (trigger) {
    Trigger.Start -> drawStartIcon()
    is Trigger.Tap -> drawTapIcon()
    is Trigger.Collision -> drawCollisionIcon()
}

internal fun DrawScope.drawAction(action: Action) = when (action) {
    is Action.Move -> drawTrainIcon()
    is Action.MoveTo -> drawMoveToIcon()
    Action.Win -> drawWinIcon()
    Action.Lose -> drawLoseIcon()
}

private fun DrawScope.drawStartIcon(t: Float = 0f) {
    for (i in 0..<8) {
        val delay = lerp(0.2f, 0.4f, i / 8f)
        val endX = cos(2f * PI.toFloat() / 8f * i - PI.toFloat() / 2f)
        val endY = sin(2f * PI.toFloat() / 8f * i - PI.toFloat() / 2f)
        val shineStart = animate(t - delay, 0.5f) {
            anim(.5f, .3f)
            anim(1.1f, .2f)
        }
        val shineEnd = animate(t - delay, 0.5f) {
            anim(1.1f, .3f)
            anim(1.1f, .2f)
        }
        lineCenter(
            Palette.Orange,
            Offset(shineStart * endX, shineStart * endY),
            Offset(shineEnd * endX, shineEnd * endY),
            10f
        )
    }
    val compress = animate(t) {
        anim(0.09f, .1f)
        anim(-.15f, .2f)
        anim(-.15f, .5f)
        anim(0.09f, .2f)
        anim(0f, .1f)
    }
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * (.15f - compress), size.height * (.15f - compress))
        lineTo(size.width * (.85f + compress), size.height * .5f)
        lineTo(size.width * (.15f - compress), size.height * (.85f + compress))
        close()
    }
    drawPath(path, Palette.Green.color)
    drawPath(path, Color.Black, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawTapIcon(t: Float = 0f) {
    val touchPoint = Offset(size.width * .5f, size.height * .2f)
    val radScale = animate(t, 1.0f) {
        anim(1.0f, .3f)
        anim(1.4f, .2f)
        anim(1.0f, .2f)
    }
    drawCircle(
        Palette.Cyan.color,
        size.minDimension * .18f * radScale,
        touchPoint,
        style = Stroke(2.dp.toPx())
    )
    val fingerMove = animate(t) {
        anim(0f, .2f)
        anim(.1f, .1f)
        anim(.1f, .3f)
        anim(0f, .2f)
    }
    val finger = androidx.compose.ui.graphics.Path().apply {
        moveTo(size.width * .5f, size.height * .82f)
        lineTo(size.width * .34f, size.height * .62f)
        lineTo(size.width * .34f, size.height * .5f)
        quadraticTo(size.width * .34f, size.height * .43f, size.width * .42f, size.height * .5f)
        lineTo(size.width * .42f, size.height * (.22f + fingerMove))
        quadraticTo(
            size.width * .42f,
            size.height * (.12f + fingerMove),
            size.width * .5f,
            size.height * (.12f + fingerMove)
        )
        quadraticTo(
            size.width * .58f,
            size.height * (.12f + fingerMove),
            size.width * .58f,
            size.height * (.22f + fingerMove)
        )
        lineTo(size.width * .58f, size.height * .48f)
        quadraticTo(size.width * .82f, size.height * .43f, size.width * .82f, size.height * .62f)
        lineTo(size.width * .72f, size.height * .82f)
        close()
    }
    drawPath(finger, Palette.OffWhite.color)
    drawPath(finger, Color.Black, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawCollisionIcon() {
    drawCollisionIcon(0f)
}

private fun DrawScope.drawCollisionIcon(t: Float) {
    val separation = animate(t, 0f) {
        anim(.45f, 0.4f)
        anim(.05f, .15f)
        anim(.35f, .3f)
        anim(.0f, .15f)
    }
    drawCircle(
        Palette.Cyan.color,
        size.minDimension * .25f,
        Offset(size.width * (.36f - separation), size.height * .5f),
    )
    drawCircle(
        Palette.Red.color,
        size.minDimension * .25f,
        Offset(size.width * (.64f + separation), size.height * .5f),
    )
}

private fun DrawScope.drawDirectionIcon(direction: Direction, t: Float = 0f) {
    val travelAxis = when (direction) {
        Direction.N -> Offset(0f, -1f)
        Direction.S -> Offset(0f, 1f)
        Direction.E -> Offset(1f, 0f)
        Direction.W -> Offset(-1f, 0f)
    }
    val stars = listOf(
        DirectionStar(-.08f, -.5f, .016f),
        DirectionStar(.08f, -.65f, .018f),
        DirectionStar(.92f, -.35f, .024f),
        DirectionStar(1.08f, -.2f, .02f),
        DirectionStar(.3f, -.05f, .015f),
        DirectionStar(.7f, .25f, .02f),
        DirectionStar(-.04f, .4f, .013f),
        DirectionStar(.16f, .55f, .016f),
        DirectionStar(.84f, .82f, .022f),
        DirectionStar(1.04f, .95f, .018f),
    )
    val starAlpha = animate(t) {
        anim(1f, .15f)
        anim(1f, .65f)
        anim(0f, .2f)
    }
    val starTravel = animate(t, 1.25f) {
        anim(-.25f, 1f)
    }
    val starElongation = animate(t) {
        anim(0f, .15f)
        anim(.12f, .15f)
        anim(.12f, .5f)
        anim(0f, .2f)
    }
    stars.forEach { star ->
        if (starAlpha > 0f) {
            val travelPosition = starTravel + star.travelOffset
            val starCenter = when (direction) {
                Direction.N -> Offset(
                    size.width * star.crossAxisPosition,
                    size.height * (1f - travelPosition),
                )

                Direction.S -> Offset(
                    size.width * star.crossAxisPosition,
                    size.height * travelPosition,
                )

                Direction.E -> Offset(
                    size.width * travelPosition,
                    size.height * star.crossAxisPosition,
                )

                Direction.W -> Offset(
                    size.width * (1f - travelPosition),
                    size.height * star.crossAxisPosition,
                )
            }
            val starColor = Color.Black.copy(alpha = starAlpha)
            val starRadius = size.minDimension * star.radius
            if (starElongation == 0f) {
                drawCircle(starColor, starRadius, starCenter)
            } else {
                val halfLength = size.minDimension * starElongation / 2f
                drawLine(
                    color = starColor,
                    start = starCenter - travelAxis * halfLength,
                    end = starCenter + travelAxis * halfLength,
                    strokeWidth = starRadius * 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    val (baseStart, baseEnd, baseArrowHead) = when (direction) {
        Direction.N -> Triple(
            Offset(size.width * .5f, size.height * .88f),
            Offset(size.width * .5f, size.height * .12f),
            listOf(
                Offset(size.width * .25f, size.height * .38f),
                Offset(size.width * .75f, size.height * .38f)
            ),
        )

        Direction.S -> Triple(
            Offset(size.width * .5f, size.height * .12f),
            Offset(size.width * .5f, size.height * .88f),
            listOf(
                Offset(size.width * .25f, size.height * .62f),
                Offset(size.width * .75f, size.height * .62f)
            ),
        )

        Direction.E -> Triple(
            Offset(size.width * .12f, size.height * .5f),
            Offset(size.width * .88f, size.height * .5f),
            listOf(
                Offset(size.width * .62f, size.height * .25f),
                Offset(size.width * .62f, size.height * .75f)
            ),
        )

        Direction.W -> Triple(
            Offset(size.width * .88f, size.height * .5f),
            Offset(size.width * .12f, size.height * .5f),
            listOf(
                Offset(size.width * .38f, size.height * .25f),
                Offset(size.width * .38f, size.height * .75f)
            ),
        )
    }
    val arrowElongation = animate(t) {
        anim(.06f, .18f)
        anim(.1f, .22f)
        anim(.1f, .4f)
        anim(0f, .2f)
    }
    val arrowExtension = travelAxis * (size.minDimension * arrowElongation)
    val start = baseStart - arrowExtension
    val end = baseEnd + arrowExtension
    val arrowHead = baseArrowHead.map { it + arrowExtension }
    val strokeWidth = 3.dp.toPx()
    drawLine(Color.Black, start, end, strokeWidth, StrokeCap.Round)
    arrowHead.forEach { point ->
        drawLine(Color.Black, point, end, strokeWidth, StrokeCap.Round)
    }
}

private fun DrawScope.drawTrainIcon(t: Float = 0f) {
    val strokeWidth = 2.dp.toPx()
    val point = { x: Float, y: Float -> Offset(size.width * x, size.height * y) }

    val cloudTravel = animate(t) {
        anim(-1f, 1f)
    }
    trainClouds.forEach { cloud ->
        drawTrainCloud(cloud.x + cloudTravel, cloud.y, cloud.scale, strokeWidth)
        drawTrainCloud(cloud.x + cloudTravel + 1f, cloud.y, cloud.scale, strokeWidth)
    }

    drawRoundRect(
        color = Palette.DarkGray.color,
        topLeft = point(.17f, .57f),
        size = Size(size.width * .67f, size.height * .12f),
        cornerRadius = CornerRadius(size.minDimension * .025f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.17f, .57f),
        size = Size(size.width * .67f, size.height * .12f),
        cornerRadius = CornerRadius(size.minDimension * .025f),
        style = Stroke(strokeWidth),
    )

    drawRect(
        color = Palette.Orange.color,
        topLeft = point(.2f, .34f),
        size = Size(size.width * .22f, size.height * .28f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.2f, .34f),
        size = Size(size.width * .22f, size.height * .28f),
        style = Stroke(strokeWidth),
    )
    drawPath(
        Path().apply {
            moveTo(point(.16f, .34f).x, point(.16f, .34f).y)
            lineTo(point(.46f, .34f).x, point(.46f, .34f).y)
            lineTo(point(.42f, .29f).x, point(.42f, .29f).y)
            lineTo(point(.2f, .29f).x, point(.2f, .29f).y)
            close()
        },
        Palette.Maroon.color,
    )
    drawLine(
        Color.Black,
        point(.16f, .34f),
        point(.46f, .34f),
        strokeWidth,
        StrokeCap.Round,
    )

    drawRect(
        color = Palette.Cyan.color,
        topLeft = point(.25f, .39f),
        size = Size(size.width * .1f, size.height * .1f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.25f, .39f),
        size = Size(size.width * .1f, size.height * .1f),
        style = Stroke(strokeWidth / 1.5f),
    )

    drawRoundRect(
        color = Palette.Rust.color,
        topLeft = point(.38f, .4f),
        size = Size(size.width * .43f, size.height * .2f),
        cornerRadius = CornerRadius(size.minDimension * .08f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.38f, .4f),
        size = Size(size.width * .43f, size.height * .2f),
        cornerRadius = CornerRadius(size.minDimension * .08f),
        style = Stroke(strokeWidth),
    )

    drawRoundRect(
        color = Palette.DarkGray.color,
        topLeft = point(.56f, .26f),
        size = Size(size.width * .1f, size.height * .17f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.56f, .26f),
        size = Size(size.width * .1f, size.height * .17f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
        style = Stroke(strokeWidth),
    )
    drawRoundRect(
        color = Palette.DarkGray.color,
        topLeft = point(.53f, .24f),
        size = Size(size.width * .16f, size.height * .05f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = point(.53f, .24f),
        size = Size(size.width * .16f, size.height * .05f),
        cornerRadius = CornerRadius(size.minDimension * .02f),
        style = Stroke(strokeWidth),
    )

    val cowcatcher = Path().apply {
        moveTo(point(.76f, .57f).x, point(.76f, .57f).y)
        lineTo(point(.91f, .7f).x, point(.91f, .7f).y)
        lineTo(point(.83f, .7f).x, point(.83f, .7f).y)
        lineTo(point(.73f, .62f).x, point(.73f, .62f).y)
        close()
    }
    drawPath(cowcatcher, Palette.Yellow.color)
    drawPath(cowcatcher, Color.Black, style = Stroke(strokeWidth))
    drawCircle(Palette.Yellow.color, size.minDimension * .045f, point(.78f, .43f))
    drawCircle(
        Color.Black,
        size.minDimension * .045f,
        point(.78f, .43f),
        style = Stroke(strokeWidth / 1.5f)
    )

    trainSteamPuffs.forEach { puff ->
        val localTime = t - puff.delay
        val alpha = animate(localTime) {
            anim(1f, .12f)
            anim(1f, .25f)
            anim(0f, .21f)
        }
        if (alpha <= 0f) return@forEach

        val travel = animate(localTime) {
            anim(1f, .58f)
        }
        val puffCenter = point(
            puff.x + puff.driftX * travel,
            puff.y + puff.driftY * travel,
        )
        val puffRadius = size.minDimension * puff.radius
        drawCircle(
            Palette.LightGray.color.copy(alpha = alpha),
            puffRadius,
            puffCenter,
        )
        drawCircle(
            Palette.DarkGray.color.copy(alpha = alpha),
            puffRadius,
            puffCenter,
            style = Stroke(strokeWidth / 1.5f),
        )
    }

    val wheelRotation = animate(t) {
        anim(360f, .8f)
        anim(0f, .2f)
    }
    val wheelRadius = size.minDimension * .105f
    val leftWheel = point(.3f, .72f)
    val rightWheel = point(.68f, .72f)
    drawTrainWheel(leftWheel, wheelRadius, wheelRotation, strokeWidth)
    drawTrainWheel(rightWheel, wheelRadius, wheelRotation, strokeWidth)

    val couplingAngle = animate(t) {
        anim(360f, .8f)
        anim(0f, .2f)
    } * PI.toFloat() / 180f
    val crankRadius = wheelRadius * .55f
    val leftCrank = leftWheel + Offset(
        cos(couplingAngle) * crankRadius,
        sin(couplingAngle) * crankRadius,
    )
    val rightCrank = rightWheel + Offset(
        cos(couplingAngle + PI.toFloat() * .65f) * crankRadius,
        sin(couplingAngle + PI.toFloat() * .65f) * crankRadius,
    )
    drawLine(Palette.Yellow.color, leftCrank, rightCrank, strokeWidth, StrokeCap.Round)
    drawCircle(Color.Black, strokeWidth * .75f, leftCrank)
    drawCircle(Color.Black, strokeWidth * .75f, rightCrank)
}

private fun DrawScope.drawTrainCloud(
    centerX: Float,
    centerY: Float,
    scale: Float,
    strokeWidth: Float,
) {
    val center = Offset(size.width * centerX, size.height * centerY)
    val cloudWidth = size.width * .24f * scale
    val cloudHeight = size.height * .1f * scale
    val cloud = Path().apply {
        moveTo(center.x - cloudWidth * .5f, center.y + cloudHeight * .25f)
        quadraticTo(
            center.x - cloudWidth * .52f,
            center.y - cloudHeight * .2f,
            center.x - cloudWidth * .25f,
            center.y - cloudHeight * .15f,
        )
        quadraticTo(
            center.x - cloudWidth * .14f,
            center.y - cloudHeight * .75f,
            center.x + cloudWidth * .1f,
            center.y - cloudHeight * .48f,
        )
        quadraticTo(
            center.x + cloudWidth * .32f,
            center.y - cloudHeight * .7f,
            center.x + cloudWidth * .38f,
            center.y - cloudHeight * .1f,
        )
        quadraticTo(
            center.x + cloudWidth * .55f,
            center.y - cloudHeight * .05f,
            center.x + cloudWidth * .5f,
            center.y + cloudHeight * .25f,
        )
        close()
    }
    drawPath(cloud, Palette.LightGray.color)
    drawPath(cloud, Palette.DarkGray.color, style = Stroke(strokeWidth / 1.5f))
}

private fun DrawScope.drawTrainWheel(
    center: Offset,
    radius: Float,
    rotation: Float,
    strokeWidth: Float,
) {
    drawCircle(Color.Black, radius, center)
    drawCircle(Palette.DarkGray.color, radius * .8f, center)
    rotate(rotation, pivot = center) {
        repeat(4) { index ->
            val angle = 2f * PI.toFloat() * index / 4f
            drawLine(
                Palette.LightGray.color,
                center,
                center + Offset(
                    cos(angle) * radius * .65f,
                    sin(angle) * radius * .65f,
                ),
                strokeWidth / 1.5f,
                StrokeCap.Round,
            )
        }
    }
    drawCircle(Palette.LightGray.color, radius * .15f, center)
    drawCircle(Color.Black, radius, center, style = Stroke(strokeWidth / 1.5f))
}

private fun DrawScope.drawMoveToIcon() {
    drawCircle(
        Palette.Cyan.color,
        size.minDimension * .18f,
        Offset(size.width * .2f, size.height * .5f)
    )
    val arrowEnd = Offset(size.width * .68f, size.height * .5f)
    drawLine(Color.Black, Offset(size.width * .38f, size.height * .5f), arrowEnd, 2.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .55f, size.height * .38f), arrowEnd, 2.dp.toPx())
    drawLine(Color.Black, Offset(size.width * .55f, size.height * .62f), arrowEnd, 2.dp.toPx())
    drawRect(
        Palette.Red.color,
        Offset(size.width * .7f, size.height * .32f),
        Size(size.width * .28f, size.height * .36f)
    )
}

private fun DrawScope.drawWinIcon(t: Float = 0f) {
    val strokeWidth = 2.dp.toPx()
    val left = size.width * .27f
    val right = size.width * .73f
    val top = size.height * .2f
    val bottom = size.height * .6f

    val leftHandle = Path().apply {
        moveTo(left + strokeWidth, size.height * .28f)
        quadraticTo(size.width * .1f, size.height * .23f, size.width * .14f, size.height * .42f)
        quadraticTo(size.width * .17f, size.height * .53f, left + strokeWidth, size.height * .48f)
    }
    val rightHandle = Path().apply {
        moveTo(right - strokeWidth, size.height * .28f)
        quadraticTo(size.width * .9f, size.height * .23f, size.width * .86f, size.height * .42f)
        quadraticTo(size.width * .83f, size.height * .53f, right - strokeWidth, size.height * .48f)
    }
    drawPath(leftHandle, Color.Black, style = Stroke(strokeWidth))
    drawPath(rightHandle, Color.Black, style = Stroke(strokeWidth))

    val cup = Path().apply {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(size.width * .69f, size.height * .4f)
        quadraticTo(size.width * .66f, bottom, size.width * .5f, bottom)
        quadraticTo(size.width * .34f, bottom, size.width * .31f, size.height * .4f)
        close()
    }
    drawPath(cup, Palette.Yellow.color)
    drawPath(cup, Color.Black, style = Stroke(strokeWidth))
    drawLine(
        Color.Black,
        Offset(size.width * .5f, bottom),
        Offset(size.width * .5f, size.height * .76f),
        strokeWidth,
        StrokeCap.Round,
    )
    drawRoundRect(
        color = Palette.Yellow.color,
        topLeft = Offset(size.width * .32f, size.height * .74f),
        size = Size(size.width * .36f, size.height * .1f),
        cornerRadius = CornerRadius(strokeWidth),
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = Offset(size.width * .32f, size.height * .74f),
        size = Size(size.width * .36f, size.height * .1f),
        cornerRadius = CornerRadius(strokeWidth),
        style = Stroke(strokeWidth),
    )

    winConfetti.forEach { piece ->
        val localTime = t - piece.delay
        val alpha = animate(localTime) {
            anim(0f, .08f)
            anim(1f, .12f)
            anim(1f, .44f)
            anim(0f, .16f)
        }
        if (alpha <= 0f) return@forEach

        val travel = animate(localTime) {
            anim(0f, .12f)
            anim(1f, .62f)
            anim(1.1f, .16f)
        }
        val rotation = animate(localTime, piece.rotation) {
            anim(piece.rotation + 180f, .28f)
            anim(piece.rotation + 360f, .62f)
        }
        val confettiCenter = Offset(
            size.width * (piece.startX + piece.driftX * travel),
            size.height * (piece.startY + piece.driftY * travel),
        )
        rotate(rotation, pivot = confettiCenter) {
            drawRoundRect(
                color = piece.color.copy(alpha = alpha),
                topLeft = Offset(
                    confettiCenter.x - size.width * piece.length / 2f,
                    confettiCenter.y - size.height * piece.thickness / 2f,
                ),
                size = Size(
                    size.width * piece.length,
                    size.height * piece.thickness,
                ),
                cornerRadius = CornerRadius(size.height * piece.thickness / 2f),
            )
        }
    }
}

private fun DrawScope.drawLoseIcon() {
    drawCircle(Palette.Red.color, size.minDimension * .5f, center)
    drawCloseIcon()
}

internal fun DrawScope.drawArrowIcon() = drawDirectionIcon(Direction.E)

private fun DrawScope.drawDownArrowIcon() {
    val centerX = size.width / 2f
    val end = Offset(centerX, size.height * .85f)
    val strokeWidth = 2.dp.toPx()
    drawLine(Color.Black, Offset(centerX, size.height * .1f), end, strokeWidth, StrokeCap.Round)
    drawLine(
        Color.Black,
        Offset(centerX - size.height * .22f, size.height * .62f),
        end,
        strokeWidth,
        StrokeCap.Round
    )
    drawLine(
        Color.Black,
        Offset(centerX + size.height * .22f, size.height * .62f),
        end,
        strokeWidth,
        StrokeCap.Round
    )
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
    drawLine(
        Color.Black,
        Offset(size.width * .5f, size.height * .15f),
        Offset(size.width * .5f, size.height * .85f),
        3.dp.toPx()
    )
    drawLine(
        Color.Black,
        Offset(size.width * .15f, size.height * .5f),
        Offset(size.width * .85f, size.height * .5f),
        3.dp.toPx()
    )
}

private fun DrawScope.drawMinusIcon() {
    drawLine(
        Color.Black,
        Offset(size.width * .15f, size.height * .5f),
        Offset(size.width * .85f, size.height * .5f),
        3.dp.toPx()
    )
}

private fun DrawScope.drawCloseIcon() {
    drawLine(
        Color.Black,
        Offset(size.width * .2f, size.height * .2f),
        Offset(size.width * .8f, size.height * .8f),
        3.dp.toPx()
    )
    drawLine(
        Color.Black,
        Offset(size.width * .8f, size.height * .2f),
        Offset(size.width * .2f, size.height * .8f),
        3.dp.toPx()
    )
}

private fun DrawScope.drawCheckIcon() {
    drawLine(
        Color.Black,
        Offset(size.width * .15f, size.height * .52f),
        Offset(size.width * .42f, size.height * .78f),
        3.dp.toPx()
    )
    drawLine(
        Color.Black,
        Offset(size.width * .42f, size.height * .78f),
        Offset(size.width * .88f, size.height * .2f),
        3.dp.toPx()
    )
}

private fun DrawScope.drawTrashIcon() {
    drawRect(
        Color.Black,
        Offset(size.width * .25f, size.height * .3f),
        Size(size.width * .5f, size.height * .55f),
        style = Stroke(2.dp.toPx())
    )
    drawLine(
        Color.Black,
        Offset(size.width * .15f, size.height * .25f),
        Offset(size.width * .85f, size.height * .25f),
        2.dp.toPx()
    )
    drawLine(
        Color.Black,
        Offset(size.width * .4f, size.height * .15f),
        Offset(size.width * .6f, size.height * .15f),
        2.dp.toPx()
    )
    drawLine(
        Color.Black,
        Offset(size.width * .4f, size.height * .4f),
        Offset(size.width * .4f, size.height * .75f),
        1.dp.toPx()
    )
    drawLine(
        Color.Black,
        Offset(size.width * .6f, size.height * .4f),
        Offset(size.width * .6f, size.height * .75f),
        1.dp.toPx()
    )
}

data class Frame(val start: Float, val stop: Float, val ease: Easing, val duration: Float)
data class AnimBuilder(val initial: Float, val actions: MutableList<Frame>) {
    fun get(t: Float): Float {
        if (t <= 0f || t >= 1f) {
            return initial
        }
        var remaining = t
        for (action in actions) {
            if (remaining <= action.duration) {
                return lerp(
                    action.start,
                    action.stop,
                    action.ease.transform(remaining / action.duration)
                )
            }
            remaining -= action.duration
        }
        return initial
    }

    fun anim(newValue: Float, duration: Float, ease: Easing = EaseInOut) {
        val from = actions.lastOrNull()?.stop ?: initial
        actions.add(Frame(from, newValue, ease, duration))
    }
}

fun animate(t: Float, initial: Float = 0f, builder: AnimBuilder.() -> Unit): Float {
    val state = AnimBuilder(initial, mutableListOf())
    state.builder()
    return state.get(t)
}

fun DrawScope.lineCenter(color: Palette, start: Offset, end: Offset, width: Float) {
    val halfSize = Offset(size.width, size.height) * .5f;
    drawLine(
        color.color,
        Offset(start.x * size.width, start.y * size.height) * .5f + halfSize,
        Offset(end.x * size.width, end.y * size.height) * .5f + halfSize,
        width
    )
}
