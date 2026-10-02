package com.example.microware.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.Modifier
import com.example.microware.editor.GameEditor
import com.example.microware.game.events.GamesEvent.Companion.forGame
import com.example.microware.game.sprite.Palette
import com.example.microware.games.GamesPage
import com.example.microware.games.store.AppGameRepository
import com.example.microware.games.store.GameStore
import com.example.microware.ui.components.Overlay
import com.example.microware.ui.layout.Insets
import com.example.microware.ui.layout.LocalInsets
import com.example.microware.util.Id

@Composable
fun App() {

    MaterialTheme {
        BoxWithConstraints(
            modifier = Modifier
                .background(Palette.OffWhite.color)
                .fillMaxSize(),
        ) {
            val gameStoreScope = rememberCoroutineScope()
            val gameStore = remember {
                GameStore(
                    repository = AppGameRepository,
                    scope = gameStoreScope,
                )
            }
            val gameState by gameStore.state.collectAsState()
            var selectedGameId by remember { mutableStateOf<Id?>(null) }
            CompositionLocalProvider(LocalInsets provides Insets.new()) {
                var navigation by remember { mutableStateOf<HomeNavigation?>(null) }
                when (navigation) {
                    null -> Home { navigation = it }
                    HomeNavigation.Editor -> {
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
                                handler = gameStore::dispatch,
                                onBack = { navigation = null },
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
        Overlay()
    }
}
