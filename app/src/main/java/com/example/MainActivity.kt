package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val geminiViewModel: GeminiViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = AppPreferences(this)
        
        setContent {
            val themeMode by prefs.themeMode.collectAsState(initial = "system")
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemDark
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: "home"
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(if (currentRoute == "home") "Silo VPN" else "Trợ lý AI") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            actions = {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(onClick = { expanded = true }) {
                                    Icon(
                                        imageVector = when(themeMode) {
                                            "light" -> Icons.Default.LightMode
                                            "dark" -> Icons.Default.DarkMode
                                            else -> Icons.Default.SettingsBrightness
                                        },
                                        contentDescription = "Giao diện"
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Theo hệ thống") },
                                        onClick = { 
                                            scope.launch { prefs.setThemeMode("system") }
                                            expanded = false 
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sáng") },
                                        onClick = { 
                                            scope.launch { prefs.setThemeMode("light") }
                                            expanded = false 
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Tối") },
                                        onClick = { 
                                            scope.launch { prefs.setThemeMode("dark") }
                                            expanded = false 
                                        }
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.VpnLock, contentDescription = "VPN") },
                                label = { Text("VPN") },
                                selected = currentRoute == "home",
                                onClick = {
                                    if (currentRoute != "home") {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = {
                                    BadgedBox(badge = { Badge { Text("AI") } }) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Trợ lý AI")
                                    }
                                },
                                label = { Text("Trợ lý") },
                                selected = currentRoute == "chat",
                                onClick = {
                                    if (currentRoute != "chat") {
                                        navController.navigate("chat") {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(prefs = prefs)
                            }
                            composable("chat") {
                                GeminiChatScreen(viewModel = geminiViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
