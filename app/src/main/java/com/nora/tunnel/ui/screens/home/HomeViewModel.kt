package com.nora.tunnel.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nora.tunnel.data.database.TunnelProfile
import com.nora.tunnel.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repo: ProfileRepository) : ViewModel() {
    val profiles: StateFlow<List<TunnelProfile>> =
        repo.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
