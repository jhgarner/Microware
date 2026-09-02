package com.example.microware

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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private enum class GameEditorPage { World, Logic }

@Composable
fun GameEditor(
    mutableGame: MutableState<Game>,
    library: MutableState<Library>,
) {
    var page by remember { mutableStateOf<GameEditorPage?>(null) }

    when (page) {
        null -> GameEditorMenu(onSelect = { page = it })
        GameEditorPage.World -> EditorPage(onBack = { page = null }) {
            val mutableWorld = remember(mutableGame) { mutableGame.worldState() }
            WorldEditor(mutableWorld = mutableWorld, library = library)
        }
        GameEditorPage.Logic -> EditorPage(onBack = { page = null }) {
            LogicEditor(
                world = mutableGame.value.world,
                logics = mutableGame.value.logics,
                onLogicsChange = { mutableGame.value = mutableGame.value.copy(logics = it) },
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
private fun EditorPage(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Palette.Navy.color)
                .padding(start = insets.leftDp(), top = insets.topDp(), end = insets.rightDp()),
        ) {
            IconButton(description = "Back to game editor menu", onClick = onBack) {
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
    onClick: () -> Unit,
    icon: DrawScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(48.dp).padding(4.dp).semantics {
            contentDescription = description
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Palette.Cyan.color else Palette.OffWhite.color,
            contentColor = Color.Black,
            disabledContainerColor = Palette.LightGray.color,
        ),
        border = BorderStroke(2.dp, Color.Black),
        shape = RectangleShape,
        contentPadding = PaddingValues(8.dp),
    ) {
        Canvas(Modifier.fillMaxSize(), onDraw = icon)
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
    drawLine(Color.Black, tip, Offset(size.width * .85f, size.height * .5f), stroke, StrokeCap.Round)
    drawLine(Color.Black, tip, Offset(size.width * .45f, size.height * .2f), stroke, StrokeCap.Round)
    drawLine(Color.Black, tip, Offset(size.width * .45f, size.height * .8f), stroke, StrokeCap.Round)
}

internal fun DrawScope.drawWorldIcon() {
    val stroke = 2.dp.toPx()
    drawRect(Color.Black, style = Stroke(stroke))
    drawCircle(Palette.Cyan.color, size.minDimension * .13f, Offset(size.width * .3f, size.height * .35f))
    drawRect(
        Palette.Green.color,
        Offset(size.width * .55f, size.height * .55f),
        Size(size.width * .25f, size.height * .25f),
    )
}

internal fun DrawScope.drawLogicIcon() {
    val stroke = 3.dp.toPx()
    val points = listOf(
        Offset(size.width * .22f, size.height * .25f),
        Offset(size.width * .75f, size.height * .25f),
        Offset(size.width * .75f, size.height * .75f),
    )
    drawLine(Color.Black, points[0], points[1], stroke)
    drawLine(Color.Black, points[1], points[2], stroke)
    points.forEachIndexed { index, point ->
        drawCircle(if (index == 0) Palette.Yellow.color else Palette.Red.color, size.minDimension * .12f, point)
        drawCircle(Color.Black, size.minDimension * .12f, point, style = Stroke(stroke / 2))
    }
}
