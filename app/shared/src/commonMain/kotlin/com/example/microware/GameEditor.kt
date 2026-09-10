package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private enum class GameEditorPage { World, Logic, Play }

@Composable
fun GameEditor(
    mutableGame: MutableState<Game>,
    library: MutableState<Library>,
    onBack: () -> Unit = {},
) {
    var page by remember { mutableStateOf(GameEditorPage.World) }
    var logicPage by remember { mutableStateOf<LogicEditorPage>(LogicEditorPage.List) }
    var lastEditorPage by remember { mutableStateOf(GameEditorPage.World) }
    var playSession by remember { mutableStateOf(0) }
    val logicEnabled = mutableGame.value.world.logicTags.isNotEmpty() ||
            mutableGame.value.logics.isNotEmpty()
    val mutableWorld = remember(mutableGame) { mutableGame.worldState() }
    val worldEditorState = rememberWorldEditorState()
    val logicEditorState = rememberLogicEditorState()
    val player = remember(playSession) { Player(mutableGame.value) }
    val pagerState = rememberPagerState(initialPage = GameEditorPage.World.ordinal) {
        GameEditorPage.entries.size
    }

    LaunchedEffect(logicEnabled) {
        if (!logicEnabled && page == GameEditorPage.Logic) {
            page = GameEditorPage.World
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        GameEditorPage.entries[pagerState.settledPage]
            .takeUnless { it == GameEditorPage.Play }
            ?.let { lastEditorPage = it }
    }

    LaunchedEffect(page) {
        pagerState.animateScrollToPage(
            page = page.ordinal,
            animationSpec = tween(durationMillis = 300),
        )
    }

    val settledPage = GameEditorPage.entries[pagerState.settledPage]
    val toolbarPage = if (settledPage == GameEditorPage.Play) lastEditorPage else settledPage
    val handlesVisible = page != GameEditorPage.Play

    EditorPage(
        page = page,
        logicEnabled = logicEnabled,
        onPageChange = { selectedPage ->
            if (selectedPage != GameEditorPage.Logic || logicEnabled) {
                if (selectedPage == GameEditorPage.World) {
                    logicPage = LogicEditorPage.List
                }
                if (selectedPage == GameEditorPage.Play && page != GameEditorPage.Play) {
                    playSession++
                }
                page = selectedPage
            }
        },
        onBack = {
            if (page == GameEditorPage.Logic && logicPage is LogicEditorPage.Editing) {
                logicPage = LogicEditorPage.List
            } else {
                onBack()
            }
        },
        backDescription = if (page == GameEditorPage.Logic && logicPage is LogicEditorPage.Editing) {
            "Back to logic list"
        } else {
            "Back from game editor"
        },
    ) {
        val activeDragController = when (settledPage) {
            GameEditorPage.World -> worldEditorState.dragController
            GameEditorPage.Logic -> logicEditorState.dragController
            GameEditorPage.Play -> null
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    activeDragController?.setRootCoordinates(it)
                    logicEditorState.rootOriginInWindow = it.positionInWindow()
                }
                .then(activeDragController?.let { Modifier.dragObserver(it) } ?: Modifier),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
            ) { pageIndex ->
                when (GameEditorPage.entries[pageIndex]) {
                    GameEditorPage.World -> WorldEditorContent(
                        mutableWorld = mutableWorld,
                        state = worldEditorState,
                    )

                    GameEditorPage.Logic -> LogicEditorContent(
                        world = mutableGame.value.world,
                        logics = mutableGame.value.logics,
                        onLogicsChange = {
                            mutableGame.value = mutableGame.value.copy(logics = it)
                        },
                        state = logicEditorState,
                        page = logicPage,
                        onPageChange = { logicPage = it },
                    )

                    GameEditorPage.Play -> player.PlayGame(active = page == GameEditorPage.Play)
                }
            }

            when (toolbarPage) {
                GameEditorPage.World -> WorldEditorToolbar(
                    state = worldEditorState,
                    mutableWorld = mutableWorld,
                    library = library,
                    isVisible = handlesVisible,
                )

                GameEditorPage.Logic -> LogicEditorToolbar(
                    state = logicEditorState,
                    world = mutableGame.value.world,
                    logics = mutableGame.value.logics,
                    library = library,
                    page = logicPage,
                    onLogicsChange = { mutableGame.value = mutableGame.value.copy(logics = it) },
                    onPageChange = { logicPage = it },
                    isVisible = handlesVisible,
                )

                GameEditorPage.Play -> Unit
            }
        }
    }
}

@Composable
private fun EditorPage(
    page: GameEditorPage,
    logicEnabled: Boolean,
    onPageChange: (GameEditorPage) -> Unit,
    onBack: () -> Unit,
    backDescription: String,
    content: @Composable () -> Unit,
) {
    SystemBackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.OffWhite.color)
                .padding(start = insets.leftDp(), top = insets.topDp(), end = insets.rightDp()),
        ) {
            IconButton(
                description = backDescription,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onBack,
            ) { _ -> drawBackIcon() }
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                EditorTabButton(
                    description = "Edit world",
                    selected = page == GameEditorPage.World,
                    onClick = { onPageChange(GameEditorPage.World) },
                ) { _ -> drawWorldIcon() }
                EditorTabButton(
                    description = "Edit logic",
                    selected = page == GameEditorPage.Logic,
                    enabled = logicEnabled,
                    onClick = { onPageChange(GameEditorPage.Logic) },
                ) { _ -> drawLogicIcon(logicEnabled) }
                EditorTabButton(
                    description = "Play game",
                    selected = page == GameEditorPage.Play,
                    onClick = { onPageChange(GameEditorPage.Play) },
                ) { _ -> drawPlayIcon() }
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun EditorTabButton(
    description: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    icon: DrawScope.(Float) -> Unit,
) {
    IconButton(
        description = description,
        selected = selected,
        enabled = enabled,
        padding = 0.dp,
        buttonSize = 52.dp,
        onClick = onClick,
        icon = icon,
    )
}

@Composable
internal fun IconButton(
    description: String,
    modifier: Modifier = Modifier,
    padding: Dp = 4.dp,
    selected: Boolean = false,
    enabled: Boolean = true,
    buttonSize: Dp? = 48.dp,
    onClick: () -> Unit,
    icon: DrawScope.(Float) -> Unit,
) {
    val iconAnimation = remember { Animatable(0f) }
    var animationRequest by remember { mutableStateOf(0) }

    LaunchedEffect(animationRequest) {
        iconAnimation.snapTo(0f)
        iconAnimation.animateTo(
            1f,
            animationSpec = tween(durationMillis = 800, easing = LinearEasing)
        )
    }

    Button(
        onClick = {
            animationRequest++
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .then(buttonSize?.let { Modifier.size(it) } ?: Modifier)
            .padding(padding)
            .semantics {
                contentDescription = description
            },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Palette.LightGray.color else Palette.OffWhite.color,
            contentColor = Color.Black,
            disabledContainerColor = Palette.DarkGray.color,
        ),
        border = BorderStroke(2.dp, Color.Black),
        shape = RectangleShape,
        contentPadding = PaddingValues(8.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            icon(iconAnimation.value)
        }
    }
}

private fun MutableState<Game>.worldState(): MutableState<World> = object : MutableState<World> {
    override var value: World
        get() = this@worldState.value.world
        set(value) {
            this@worldState.value = this@worldState.value.copy(world = value)
        }

    override fun component1(): World = value

    override fun component2(): (World) -> Unit = { value = it }
}

internal fun DrawScope.drawBackIcon() {
    val stroke = 3.dp.toPx()
    val tip = Offset(size.width * .15f, size.height * .5f)
    drawLine(
        Color.Black,
        tip,
        Offset(size.width * .85f, size.height * .5f),
        stroke,
        StrokeCap.Round
    )
    drawLine(
        Color.Black,
        tip,
        Offset(size.width * .45f, size.height * .2f),
        stroke,
        StrokeCap.Round
    )
    drawLine(
        Color.Black,
        tip,
        Offset(size.width * .45f, size.height * .8f),
        stroke,
        StrokeCap.Round
    )
}

internal fun DrawScope.drawPlayIcon() {
    val path = Path().apply {
        moveTo(size.width * .34f, size.height * .2f)
        lineTo(size.width * .76f, size.height * .5f)
        lineTo(size.width * .34f, size.height * .8f)
        close()
    }
    drawPath(path, Palette.Green.color)
    drawPath(path, Color.Black, style = Stroke(2.dp.toPx()))
}

internal fun DrawScope.drawWorldIcon() {
    val stroke = 2.dp.toPx()
    fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    fun Float.x(): Float = size.width * this
    fun Float.y(): Float = size.width * this

    val mountain = Path().apply {
        moveTo(.08f.x(), .68f.y())
        lineTo(.3f.x(), .23f.y())
        lineTo(.43f.x(), .46f.y())
        lineTo(.61f.x(), .16f.y())
        lineTo(.92f.x(), .68f.y())
        close()
    }
    drawPath(mountain, Palette.Navy.color)
    drawPath(mountain, Color.Black, style = Stroke(stroke))

    val snow = Path().apply {
        moveTo(.3f.x(), .23f.y())
        lineTo(.22f.x(), .39f.y())
        lineTo(.3f.x(), .35f.y())
        lineTo(.39f.x(), .39f.y())
        close()
    }
    drawPath(snow, Palette.OffWhite.color)
    drawPath(snow, Color.Black, style = Stroke(stroke))

    val snow2 = Path().apply {
        moveTo(.61f.x(), .16f.y())
        lineTo(.51f.x(), .33f.y())
        lineTo(.61f.x(), .28f.y())
        lineTo(.712f.x(), .33f.y())
        close()
    }
    drawPath(snow2, Palette.OffWhite.color)
    drawPath(snow2, Color.Black, style = Stroke(stroke))

    val house = Path().apply {
        moveTo(.25f.x(), .53f.y())
        lineTo(.45f.x(), .38f.y())
        lineTo(.65f.x(), .53f.y())
        lineTo(.62f.x(), .53f.y())
        lineTo(.62f.x(), .83f.y())
        lineTo(.29f.x(), .83f.y())
        lineTo(.29f.x(), .53f.y())
        close()
    }
    drawPath(house, Palette.Orange.color)
    drawPath(house, Color.Black, style = Stroke(stroke))

    drawRect(
        color = Palette.Navy.color,
        topLeft = point(.40f, .70f),
        size = Size(size.width * .10f, size.height * .13f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.40f, .70f),
        size = Size(size.width * .10f, size.height * .13f),
        style = Stroke(stroke / 1.5f),
    )
    drawRect(
        color = Palette.Cyan.color,
        topLeft = point(.35f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.35f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
        style = Stroke(stroke / 1.5f),
    )

    drawRect(
        color = Palette.Cyan.color,
        topLeft = point(.48f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.48f, .56f),
        size = Size(size.width * .07f, size.height * .07f),
        style = Stroke(stroke / 1.5f),
    )
}

internal fun DrawScope.drawLogicIcon(enabled: Boolean = true) {
    val pageStroke = 2.dp.toPx()
    val textStroke = 1.5.dp.toPx()
    val pageColor = if (enabled) Palette.Tan.color else Palette.OffWhite.color
    val outlineColor = if (enabled) Color.Black else Palette.DarkGray.color
    val tagColor = if (enabled) null else Palette.DarkGray.color
    fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    fun Float.x(): Float = size.width * this
    fun Float.y(): Float = size.width * this

    val leftPage = Path().apply {
        moveTo(.5f.x(), .23f.y())
        lineTo(.34f.x(), .2f.y())
        lineTo(.17f.x(), .21f.y())
        lineTo(.17f.x(), .73f.y())
        lineTo(.34f.x(), .72f.y())
        lineTo(.5f.x(), .75f.y())
        close()
    }
    val rightPage = Path().apply {
        moveTo(.5f.x(), .23f.y())
        lineTo(.66f.x(), .2f.y())
        lineTo(.83f.x(), .21f.y())
        lineTo(.83f.x(), .73f.y())
        lineTo(.66f.x(), .72f.y())
        lineTo(.5f.x(), .75f.y())
        close()
    }

    drawPath(leftPage, pageColor)
    drawPath(rightPage, pageColor)
    drawPath(leftPage, outlineColor, style = Stroke(pageStroke))
    drawPath(rightPage, outlineColor, style = Stroke(pageStroke))
    drawLine(outlineColor, point(.5f, .23f), point(.5f, .75f), pageStroke)

    drawLine(Palette.DarkGray.color, point(.23f, .34f), point(.43f, .34f), textStroke)
    drawLine(Palette.DarkGray.color, point(.23f, .63f), point(.43f, .63f), textStroke)
    drawLine(Palette.DarkGray.color, point(.57f, .34f), point(.77f, .34f), textStroke)
    drawLine(Palette.DarkGray.color, point(.57f, .63f), point(.77f, .63f), textStroke)

    with(LogicTag(58)) {
        drawAt(point(.33f, .49f), size.minDimension * .17f, color = tagColor)
    }
    with(LogicTag(29)) {
        drawAt(point(.67f, .49f), size.minDimension * .17f, color = tagColor)
    }
}
