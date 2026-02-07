package com.metrolist.music.wear.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Success(val results: List<WearSong>) : SearchState()
    data class Error(val message: String) : SearchState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: WearSearchRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var lastQuery: String = ""

    fun search(query: String) {
        Timber.d("Searching for: $query")
        lastQuery = query
        viewModelScope.launch {
            _state.value = SearchState.Loading
            repository.search(query)
                .onSuccess { results ->
                    Timber.d("Search returned ${results.size} results")
                    _state.value = SearchState.Success(results)
                }
                .onFailure { error ->
                    Timber.e(error, "Search failed")
                    _state.value = SearchState.Error(error.message ?: "Search failed")
                }
        }
    }

    fun retry() {
        if (lastQuery.isNotBlank()) {
            search(lastQuery)
        }
    }
}
