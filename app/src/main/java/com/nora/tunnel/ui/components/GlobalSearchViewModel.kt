package com.nora.tunnel.ui.components

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.*

class GlobalSearchViewModel: ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    fun onQuery(s: String) { _query.value = s }
    // Real: combine query with ProfileRepository.observe(), RoutingRepository, LogRepository, HistoryRepository
    // Filtered results shown in SearchBar suggestions
}
