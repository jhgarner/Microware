package com.example.microware

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal data class GameStoreState(
    val games: List<Game>,
    val nextGameId: Int,
)

internal sealed interface GameEvent {
    data object CreateGame : GameEvent
    data class UpdateWorld(val gameId: Int, val world: World) : GameEvent
    data class UpdateLogics(val gameId: Int, val logics: List<Logic>) : GameEvent
    data class MoveGame(val gameId: Int, val targetIndex: Int) : GameEvent
    data class DeleteGame(val gameId: Int) : GameEvent
    data class RestoreGame(val game: Game, val index: Int) : GameEvent
}

@OptIn(FlowPreview::class)
internal class GameStore(
    repository: GameRepository,
    scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(repository.load().toState())
    private val pendingSaves = MutableStateFlow<GameStoreState?>(null)

    val state: StateFlow<GameStoreState> = _state.asStateFlow()

    init {
        scope.launch {
            pendingSaves
                .filterNotNull()
                .debounce(100)
                .collectLatest { repository.save(it.toStoredGames()) }
        }
    }

    fun dispatch(event: GameEvent) {
        val updatedState = reduce(_state.value, event)
        if (updatedState == _state.value) return

        _state.value = updatedState
        pendingSaves.value = updatedState
    }

    fun createGame(): Int {
        val id = _state.value.nextGameId
        dispatch(GameEvent.CreateGame)
        return id
    }

    private fun reduce(state: GameStoreState, event: GameEvent): GameStoreState = when (event) {
        GameEvent.CreateGame -> state.copy(
            games = state.games + Game(
                world = World(),
                logics = emptyList(),
                id = state.nextGameId,
            ),
            nextGameId = state.nextGameId + 1,
        )

        is GameEvent.UpdateWorld -> state.copy(
            games = state.games.map { game ->
                if (game.id == event.gameId) game.copy(world = event.world) else game
            },
        )

        is GameEvent.UpdateLogics -> state.copy(
            games = state.games.map { game ->
                if (game.id == event.gameId) game.copy(logics = event.logics) else game
            },
        )

        is GameEvent.MoveGame -> state.copy(
            games = state.games.moveGame(event.gameId, event.targetIndex),
        )

        is GameEvent.DeleteGame -> state.copy(
            games = state.games.filterNot { it.id == event.gameId },
        )

        is GameEvent.RestoreGame -> {
            if (state.games.any { it.id == event.game.id }) {
                state
            } else {
                val index = event.index.coerceIn(0, state.games.size)
                state.copy(
                    games = state.games.toMutableList().apply {
                        add(index, event.game)
                    },
                    nextGameId = maxOf(state.nextGameId, event.game.id + 1),
                )
            }
        }
    }
}

private fun List<Game>.moveGame(gameId: Int, targetIndex: Int): List<Game> {
    if (size < 2) return this
    val sourceIndex = indexOfFirst { it.id == gameId }
    if (sourceIndex < 0) return this

    val destinationIndex = targetIndex.coerceIn(0, lastIndex)
    if (sourceIndex == destinationIndex) return this

    return toMutableList().apply {
        val game = removeAt(sourceIndex)
        add(destinationIndex.coerceIn(0, size), game)
    }
}

private data class DeletedGame(
    val game: Game,
    val index: Int,
)

internal class GameSession(private val store: GameStore) {
    private var deletedGames by mutableStateOf(emptyList<DeletedGame>())

    val canUndo: Boolean
        get() = deletedGames.isNotEmpty()

    fun createGame(): Int = store.createGame()

    fun moveGame(gameId: Int, targetIndex: Int) {
        store.dispatch(GameEvent.MoveGame(gameId, targetIndex))
    }

    fun deleteGame(gameId: Int) {
        val games = store.state.value.games
        val index = games.indexOfFirst { it.id == gameId }
        if (index < 0) return

        deletedGames += DeletedGame(games[index], index)
        store.dispatch(GameEvent.DeleteGame(gameId))
    }

    fun undoDelete() {
        val deleted = deletedGames.lastOrNull() ?: return
        deletedGames = deletedGames.dropLast(1)
        store.dispatch(GameEvent.RestoreGame(deleted.game, deleted.index))
    }
}
