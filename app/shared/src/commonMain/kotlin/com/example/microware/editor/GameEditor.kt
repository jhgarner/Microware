package com.example.microware.editor

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.microware.Palette
import com.example.microware.editor.logic.LogicEditToolbar
import com.example.microware.editor.logic.LogicEditorContent
import com.example.microware.editor.logic.LogicEditorPage
import com.example.microware.editor.logic.rememberLogicEditorState
import com.example.microware.editor.world.ObjectOrderEditor
import com.example.microware.editor.world.WorldEditToolbar
import com.example.microware.editor.world.WorldEditorContent
import com.example.microware.editor.world.rememberWorldEditorState
import com.example.microware.game.events.GameEvents
import com.example.microware.game.model.Game
import com.example.microware.game.runtime.Player
import com.example.microware.platform.SystemBackHandler
import com.example.microware.ui.components.ButtonHandle
import com.example.microware.ui.components.HandleType
import com.example.microware.ui.components.IconButton
import com.example.microware.ui.components.Scrim
import com.example.microware.ui.gestures.dragObserver
import com.example.microware.ui.icons.drawBackIcon
import com.example.microware.ui.icons.drawLogicIcon
import com.example.microware.ui.icons.drawObjectOrderIcon
import com.example.microware.ui.icons.drawPlayIcon
import com.example.microware.ui.icons.drawPublishIcon
import com.example.microware.ui.icons.drawWorldIcon
import com.example.microware.ui.layout.insets

private enum class GameEditorPage { World, Objects, Logic, Play, Publish }

@Composable
fun BoxWithConstraintsScope.GameEditor(
    game: Game,
    handler: (GameEvents) -> Unit,
    onBack: () -> Unit = {},
    publish: @Composable (Game) -> Unit,
) {
    var page by remember { mutableStateOf(GameEditorPage.World) }
    var logicPage by remember { mutableStateOf<LogicEditorPage>(LogicEditorPage.List) }
    var lastEditorPage by remember { mutableStateOf(GameEditorPage.World) }
    var playSession by remember { mutableStateOf(0) }
    val logicEnabled = game.world.logicTags.isNotEmpty() || game.logics.isNotEmpty()
    val worldEditorState = rememberWorldEditorState()
    val logicEditorState = rememberLogicEditorState()
    val player = remember(playSession) { Player(game) }
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

    val toolbarPage = GameEditorPage.entries[pagerState.targetPage]

    val onPageChange1 = { selectedPage: GameEditorPage ->
        if (selectedPage != GameEditorPage.Logic || logicEnabled) {
            if (selectedPage == GameEditorPage.World) {
                logicPage = LogicEditorPage.List
            }
            if (selectedPage == GameEditorPage.Play) {
                playSession++
            }
            page = selectedPage
        }
    }
    val onBack1 = {
        if (page == GameEditorPage.Logic && logicPage is LogicEditorPage.Editing) {
            logicPage = LogicEditorPage.List
        } else {
            onBack()
        }
    }
    SystemBackHandler(onBack = onBack1)
    val activeDragController = when (toolbarPage) {
        GameEditorPage.World,
        GameEditorPage.Objects,
            -> worldEditorState.dragController

        GameEditorPage.Logic -> logicEditorState.dragController
        GameEditorPage.Play -> null
        GameEditorPage.Publish -> null
    }
    Box(
        modifier = activeDragController?.let { Modifier.dragObserver(it) } ?: Modifier,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Palette.OffWhite.color)
                    .padding(start = insets.leftDp(), top = insets.topDp(), end = insets.rightDp()),
            ) {
                IconButton(
                    description = if (page == GameEditorPage.Logic && logicPage is LogicEditorPage.Editing) {
                        "Back to logic list"
                    } else {
                        "Back from game editor"
                    },
                    modifier = Modifier.size(52.dp),
                    onClick = onBack1,
                ) { _ -> drawBackIcon() }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    EditorTabButton(
                        description = "Edit world",
                        selected = page == GameEditorPage.World,
                        onClick = { onPageChange1(GameEditorPage.World) },
                    ) { _ -> drawWorldIcon() }
                    EditorTabButton(
                        description = "Order objects",
                        selected = page == GameEditorPage.Objects,
                        onClick = { onPageChange1(GameEditorPage.Objects) },
                    ) { _ -> drawObjectOrderIcon() }
                    EditorTabButton(
                        description = "Edit logic",
                        selected = page == GameEditorPage.Logic,
                        enabled = logicEnabled,
                        onClick = { onPageChange1(GameEditorPage.Logic) },
                    ) { _ -> drawLogicIcon(enabled = logicEnabled) }
                    EditorTabButton(
                        description = "Play game",
                        selected = page == GameEditorPage.Play,
                        onClick = { onPageChange1(GameEditorPage.Play) },
                    ) { _ -> drawPlayIcon() }
                    EditorTabButton(
                        description = "Publish Game",
                        selected = page == GameEditorPage.Publish,
                        onClick = { onPageChange1(GameEditorPage.Publish) },
                    ) { drawPublishIcon(it) }
                }
            }
            Box(Modifier.weight(1f)) {
                BoxWithConstraints() {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false,
                    ) { pageIndex ->
                        when (GameEditorPage.entries[pageIndex]) {
                            GameEditorPage.World -> WorldEditorContent(
                                world = game.world,
                                handler = handler,
                                state = worldEditorState,
                            )

                            GameEditorPage.Objects -> ObjectOrderEditor(
                                world = game.world,
                                handler = handler,
                                state = worldEditorState,
                            )

                            GameEditorPage.Logic -> LogicEditorContent(
                                logics = game.logics,
                                handler = handler,
                                state = logicEditorState,
                                page = logicPage,
                                onPageChange = { logicPage = it },
                            )

                            GameEditorPage.Play -> player.PlayGame(active = page == GameEditorPage.Play)
                            GameEditorPage.Publish -> publish(game)
                        }
                    }
                }
            }
        }
        Scrim()
        when (toolbarPage) {
            GameEditorPage.World, GameEditorPage.Objects -> this@GameEditor.WorldEditToolbar(
                dragTarget = worldEditorState.dragController,
                willBeDeleted = worldEditorState.mutableWillBeDeleted,
                world = game.world,
                handler = handler,
                dragSource = when (toolbarPage) {
                    GameEditorPage.World -> worldEditorState.dragController
                    GameEditorPage.Objects -> null
                },
            )

            GameEditorPage.Logic -> this@GameEditor.LogicEditToolbar(
                dragController = logicEditorState.dragController,
                willBeDeleted = logicEditorState.willBeDeleted,
                usedTags = game.world.logicTags.keys,
                handler = handler,
            )

            GameEditorPage.Play, GameEditorPage.Publish -> {
                this@GameEditor.ButtonHandle(HandleType.Trash)
                this@GameEditor.ButtonHandle(HandleType.Create)
                this@GameEditor.ButtonHandle(HandleType.Library)
            }
        }
    }
}

@Composable
private fun RowScope.EditorTabButton(
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
        modifier = Modifier.weight(1f, fill = false).size(52.dp),
        onClick = onClick,
        icon = icon,
    )
}
