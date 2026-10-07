package com.nora.tunnel.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoScreen(title: String, lines: List<String>, extra: @Composable ColumnScope.() -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
        extra()
    }
}
