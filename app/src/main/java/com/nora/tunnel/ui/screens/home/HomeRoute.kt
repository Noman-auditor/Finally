package com.nora.tunnel.ui.screens.home

import androidx.compose.runtime.*
import com.nora.tunnel.data.database.TunnelProfile
import com.nora.tunnel.tunnel.TunnelManager
import com.nora.tunnel.tunnel.TunnelState
import org.koin.androidx.compose.koinViewModel

// Overload used by NoraNavGraph. It feeds live state into the original HomeScreen(state, onConnect, onDisconnect).
@Composable
fun HomeScreen(
    onConnectRequest: (TunnelProfile) -> Unit,
    onDisconnect: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by TunnelManager.state.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val profile = profiles.firstOrNull()
    HomeScreen(
        state = state,
        onConnect = {
            if(profile != null) onConnectRequest(profile)
            else TunnelManager.notifyState(TunnelState.Error("No profile", "Create or import a profile first."))
        },
        onDisconnect = onDisconnect
    )
}
