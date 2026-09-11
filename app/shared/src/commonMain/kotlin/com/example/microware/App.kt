package com.example.microware

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun App() {

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Palette.OffWhite.color)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val gameStoreScope = rememberCoroutineScope()
            val gameStore = remember {
                GameStore(
                    repository = AppGameRepository,
                    scope = gameStoreScope,
                )
            }
            val gameState by gameStore.state.collectAsState()
            val gameSession = remember(gameStore) { GameSession(gameStore) }
            var selectedGameId by remember { mutableStateOf<Int?>(null) }
            CompositionLocalProvider(LocalInsets provides Insets.new()) {
                val library = remember { mutableStateOf(loadLibrary()) }
                val gameId = selectedGameId
                val game = gameId?.let { id ->
                    gameState.games.firstOrNull { it.id == id }
                }
                if (game == null) {
                    GamesPage(
                        games = gameState.games,
                        canUndo = gameSession.canUndo,
                        onOpenGame = { selectedGameId = it },
                        onCreateGame = gameSession::createGame,
                        onDeleteGame = gameSession::deleteGame,
                        onUndoDelete = gameSession::undoDelete,
                        onMoveGame = gameSession::moveGame,
                    )
                } else {
                    GameEditor(
                        game = game,
                        library = library,
                        onWorldChange = { world ->
                            gameStore.dispatch(GameEvent.UpdateWorld(game.id, world))
                        },
                        onLogicsChange = { logics ->
                            gameStore.dispatch(GameEvent.UpdateLogics(game.id, logics))
                        },
                        onBack = { selectedGameId = null },
                    )
                }
            }
        }
    }
}
