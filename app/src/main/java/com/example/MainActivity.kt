package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.apprules.AppRulesScreen
import com.example.apprules.AppRulesViewModel
import com.example.auth.AuthViewModel
import com.example.auth.LoginScreen
import com.example.auth.RegisterScreen
import com.example.auth.TokenStore
import com.example.onboarding.EnableKeyboardScreen
import com.example.onboarding.WelcomeScreen
import com.example.preview.LivePreviewScreen
import com.example.preview.LivePreviewViewModel
import com.example.settings.SettingsScreen
import com.example.toneprofile.ToneProfileViewModel
import com.example.toneprofile.ToneSetupScreen
import com.example.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Welcome : Screen("welcome", "Welcome")
    object EnableKeyboard : Screen("enable_keyboard", "Enable")
    object ToneSetup : Screen("tone_setup", "Tone", Icons.Default.Person)
    object AppRules : Screen("app_rules", "Rules", Icons.Default.List)
    object Preview : Screen("preview", "Preview", Icons.Default.PlayArrow)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.ToneSetup,
    Screen.AppRules,
    Screen.Preview,
    Screen.Settings
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isLoggedIn by tokenStore.isLoggedIn.collectAsState(initial = false)
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        
                        val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }
                        if (showBottomBar) {
                            NavigationBar {
                                bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon!!, contentDescription = null) },
                                        label = { Text(screen.title) },
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (isLoggedIn) Screen.Welcome.route else Screen.Login.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            val authViewModel: AuthViewModel = hiltViewModel()
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                                onLoginSuccess = {
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Register.route) {
                            val authViewModel: AuthViewModel = hiltViewModel()
                            RegisterScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                                onRegisterSuccess = {
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Welcome.route) {
                            WelcomeScreen(
                                onNext = { navController.navigate(Screen.EnableKeyboard.route) }
                            )
                        }
                        composable(Screen.EnableKeyboard.route) {
                            EnableKeyboardScreen(
                                onDone = {
                                    navController.navigate(Screen.ToneSetup.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.ToneSetup.route) {
                            val viewModel: ToneProfileViewModel = hiltViewModel()
                            ToneSetupScreen(viewModel)
                        }
                        composable(Screen.AppRules.route) {
                            val viewModel: AppRulesViewModel = hiltViewModel()
                            AppRulesScreen(viewModel)
                        }
                        composable(Screen.Preview.route) {
                            val viewModel: LivePreviewViewModel = hiltViewModel()
                            LivePreviewScreen(viewModel)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                tokenStore = tokenStore,
                                onSignOut = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
