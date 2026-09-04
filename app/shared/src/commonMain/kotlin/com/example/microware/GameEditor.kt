package com.example.microware

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private enum class GameEditorPage { World, Logic }

@Composable
fun GameEditor(
    mutableGame: MutableState<Game>,
    library: MutableState<Library>,
) {
    var page by remember { mutableStateOf<GameEditorPage?>(null) }
    var logicPage by remember { mutableStateOf<LogicEditorPage>(LogicEditorPage.List) }

    when (page) {
        null -> GameEditorMenu(onSelect = {
            if (it == GameEditorPage.Logic) {
                logicPage = LogicEditorPage.List
            }
            page = it
        })

        GameEditorPage.World -> EditorPage(onBack = { page = null }) {
            val mutableWorld = remember(mutableGame) { mutableGame.worldState() }
            WorldEditor(mutableWorld = mutableWorld, library = library)
        }

        GameEditorPage.Logic -> EditorPage(
            onBack = {
                if (logicPage is LogicEditorPage.Editing) {
                    logicPage = LogicEditorPage.List
                } else {
                    page = null
                }
            },
            backDescription = if (logicPage is LogicEditorPage.Editing) {
                "Back to logic list"
            } else {
                "Back to game editor menu"
            },
        ) {
            LogicEditor(
                world = mutableGame.value.world,
                logics = mutableGame.value.logics,
                onLogicsChange = { mutableGame.value = mutableGame.value.copy(logics = it) },
                page = logicPage,
                onPageChange = { logicPage = it },
            )
        }
    }
}

@Composable
private fun GameEditorMenu(onSelect: (GameEditorPage) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = insets.leftDp() + 24.dp,
                top = insets.topDp() + 24.dp,
                end = insets.rightDp() + 24.dp,
                bottom = insets.bottomDp() + 24.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuButton("Edit world", Palette.Forest.color, { drawWorldIcon() }) {
            onSelect(GameEditorPage.World)
        }
        MenuButton("Edit logic", Palette.Maroon.color, { drawLogicIcon() }) {
            onSelect(GameEditorPage.Logic)
        }
    }
}

@Composable
private fun MenuButton(
    description: String,
    color: Color,
    icon: DrawScope.() -> Unit,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(120.dp).semantics { contentDescription = description },
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.OffWhite.color,
            contentColor = color,
        ),
        border = BorderStroke(4.dp, color),
        shape = RectangleShape,
        contentPadding = PaddingValues(20.dp),
    ) {
        Canvas(Modifier.fillMaxSize(), onDraw = icon)
    }
}

@Composable
private fun EditorPage(
    onBack: () -> Unit,
    backDescription: String = "Back to game editor menu",
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
            IconButton(description = backDescription, onClick = onBack) { _ ->
                drawBackIcon()
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
internal fun IconButton(
    description: String,
    modifier: Modifier = Modifier,
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
            .padding(4.dp)
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
        lineTo(.715f.x(), .33f.y())
        close()
    }
    drawPath(snow2, Palette.OffWhite.color)
    drawPath(snow2, Color.Black, style = Stroke(stroke))

//    val beach = Path().apply {
//        moveTo(.56f.x(), .59f.y())
//        lineTo(.73f.x(), .5f.y())
//        lineTo(.94f.x(), .61f.y())
//        lineTo(.94f.x(), .84f.y())
//        lineTo(.56f.x(), .84f.y())
//        close()
//    }
//    drawPath(beach, Palette.Tan.color)
//    drawPath(beach, Color.Black, style = Stroke(stroke))
//
//    val water = Path().apply {
//        moveTo(.8f.x(), .55f.y())
//        lineTo(.94f.x(), .62f.y())
//        lineTo(.94f.x(), .84f.y())
//        lineTo(.79f.x(), .84f.y())
//        lineTo(.84f.x(), .72f.y())
//        lineTo(.78f.x(), .64f.y())
//        close()
//    }
//    drawPath(water, Palette.Cyan.color)
//    drawPath(water, Color.Black, style = Stroke(stroke))

    val house = Path().apply {
        moveTo(.25f.x(), .53f.y())
        lineTo(.44f.x(), .38f.y())
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
        topLeft = point(.40f, .69f),
        size = Size(size.width * .10f, size.height * .13f),
    )
    drawRect(
        color = Color.Black,
        topLeft = point(.40f, .69f),
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

internal fun DrawScope.drawLogicIcon() {
    val pageStroke = 2.dp.toPx()
    val textStroke = 1.5.dp.toPx()
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

    drawPath(leftPage, Palette.Tan.color)
    drawPath(rightPage, Palette.Tan.color)
    drawPath(leftPage, Color.Black, style = Stroke(pageStroke))
    drawPath(rightPage, Color.Black, style = Stroke(pageStroke))
    drawLine(Color.Black, point(.5f, .23f), point(.5f, .75f), pageStroke)

    drawLine(Palette.DarkGray.color, point(.23f, .34f), point(.43f, .34f), textStroke)
    drawLine(Palette.DarkGray.color, point(.23f, .63f), point(.43f, .63f), textStroke)
    drawLine(Palette.DarkGray.color, point(.57f, .34f), point(.77f, .34f), textStroke)
    drawLine(Palette.DarkGray.color, point(.57f, .63f), point(.77f, .63f), textStroke)

    with(LogicTag(58)) {
        drawAt(point(.33f, .49f), size.minDimension * .17f)
    }
    with(LogicTag(29)) {
        drawAt(point(.67f, .49f), size.minDimension * .17f)
    }
}
