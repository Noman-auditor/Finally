package com.nora.tunnel.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

// Material3 1.2.1 API: query/onQueryChange/active (the inputField/expanded form needs Material3 1.4+)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchBar(query: String, onQuery: (String)->Unit) {
    DockedSearchBar(
        query = query,
        onQueryChange = onQuery,
        onSearch = {},
        active = false,
        onActiveChange = {}
    ) {
        // Searches: Profiles, Protocols, Routing rules, Logs, History
    }
}
