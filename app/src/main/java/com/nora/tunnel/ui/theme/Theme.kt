package com.nora.tunnel.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val NoraColors = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    background = Color(0xFF0F1115),
    surface = Color(0xFF0F1115)
)

@Composable
fun NoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NoraColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
