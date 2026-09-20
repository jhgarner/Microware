package com.example.microware

import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal data class GameStoreState(
    val games: PersistentList<Game>,
)

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
                .debounce(100.milliseconds)
                .collectLatest { repository.save(it.toStoredGames()) }
        }
    }

    fun dispatch(event: GamesEvent) {
        val updatedState = reduce(_state.value, event)
        if (updatedState == _state.value) return

        _state.value = updatedState
        pendingSaves.value = updatedState
    }

    private fun reduce(state: GameStoreState, event: GamesEvent): GameStoreState =
        state.copy(games = event.modifyGames(state.games))
}
