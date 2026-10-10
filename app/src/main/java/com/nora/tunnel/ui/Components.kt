package com.nora.tunnel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Bg = Color(0xFF050711)
internal val Panel = Color(0xFF0D1220)
internal val Accent = Color(0xFF8B5CF6)
internal val Cyan = Color(0xFF22D3EE)
internal val Good = Color(0xFF45F0B0)
internal val Warn = Color(0xFFFFC857)
internal val Bad = Color(0xFFFF6B8B)
internal val Dim = Color(0xFF98A2C5)

@Composable
fun NoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Accent), content = content)
}

@Composable
internal fun ScreenHeader(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Dim)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(text, color = Dim, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
internal fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Dim, fontSize = 11.sp)
            }
            Switch(checked, onChange)
        }
    }
}

@Composable
internal fun InfoCard(title: String, text: String, accent: Color = Cyan) {
    Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(text, color = Dim, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Dim, fontSize = 9.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
