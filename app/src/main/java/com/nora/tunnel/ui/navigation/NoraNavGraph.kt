package com.nora.tunnel.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.nora.tunnel.data.database.TunnelProfile
import com.nora.tunnel.ui.screens.dns.DnsCenter
import com.nora.tunnel.ui.screens.home.HomeScreen
import com.nora.tunnel.ui.screens.lab.NetworkLab
import com.nora.tunnel.ui.screens.logs.LogCenter
import com.nora.tunnel.ui.screens.profiles.ProfileScreen
import com.nora.tunnel.ui.screens.routing.RoutingStudio
import com.nora.tunnel.ui.screens.security.SecurityCenter
import com.nora.tunnel.ui.screens.settings.SettingsScreen
import com.nora.tunnel.ui.screens.statistics.HistoryScreen
import com.nora.tunnel.ui.screens.statistics.LiveDashboard

data class NavItem(val route: String, val label: String, val icon: ImageVector)

val navItems = listOf(
    NavItem("home", "Home", Icons.Filled.Home),
    NavItem("profiles", "Profiles", Icons.Filled.List),
    NavItem("routing", "Routing", Icons.Filled.Share),
    NavItem("lab", "Lab", Icons.Filled.Science),
    NavItem("logs", "Logs", Icons.Filled.Article),
    NavItem("security", "Security", Icons.Filled.Security)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoraNavGraph(
    onConnectRequest: (TunnelProfile)->Unit = {},
    onDisconnect: ()->Unit = {}
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NORA TUNNEL") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(onConnectRequest, onDisconnect) }
            composable("profiles") { ProfileScreen(onConnectRequest) }
            composable("routing") { RoutingStudio() }
            composable("lab") { NetworkLab() }
            composable("logs") { LogCenter() }
            composable("security") { SecurityCenter() }
            composable("settings") { SettingsScreen() }
            composable("dns") { DnsCenter() }
            composable("stats") { LiveDashboard() }
            composable("history") { HistoryScreen(emptyList()) {} }
        }
    }
}
