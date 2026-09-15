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
import com.example.microware.GamesEvent.Companion.forGame

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
            var selectedGameId by remember { mutableStateOf<Int?>(null) }
            CompositionLocalProvider(LocalInsets provides Insets.new()) {
                val gameId = selectedGameId
                val game = gameId?.let { id ->
                    gameState.games.firstOrNull { it.id == id }
                }
                val gameIndex = gameId?.let { id ->
                    gameState.games.indexOfFirst { it.id == id }
                }
                if (game == null || gameIndex == null) {
                    GamesPage(
                        games = gameState.games,
                        selectGame = { selectedGameId = it },
                        handler = gameStore::dispatch
                    )
                } else {
                    GameEditor(
                        game = game,
                        handler = { event -> gameStore.dispatch(event.forGame(gameIndex)) },
                        onBack = { selectedGameId = null },
                    )
                }
            }
        }
    }
}
