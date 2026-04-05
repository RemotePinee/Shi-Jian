package com.shijian

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.annotation.SuppressLint
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.unit.IntOffset
import com.shijian.data.repository.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.SavedStateHandle
import com.shijian.data.model.ConfigData

import com.shijian.ui.screens.*
import com.shijian.ui.components.*
import com.shijian.ui.theme.EatWhatTheme
import com.shijian.ui.viewmodel.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "觅食", Icons.Default.Restaurant)
    object Discovery : Screen("discovery", "发现", Icons.Default.Explore)
    object Chat : Screen("chat", "厨神", Icons.AutoMirrored.Filled.Chat)
    object MysteryBox : Screen("mystery", "开盲盒", Icons.Default.Casino)
    object Sauce : Screen("sauce", "调料箱", Icons.Default.Kitchen)
    object Fortune : Screen("fortune", "厨神算命", Icons.Default.AutoAwesome)
    object Favorites : Screen("favorites", "收藏", Icons.Default.Favorite)
    object Gallery : Screen("gallery", "图库", Icons.Default.PhotoLibrary)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

private var isSplashShownInProcess = false

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Phase 4: Extreme Startup Warm-up (IO Thread)
        lifecycleScope.launch(Dispatchers.IO) {
            @Suppress("UNUSED_EXPRESSION")
            ConfigData.cuisines.size // Trigger object allocations in background
        }

        val settingsRepository = SettingsRepository(this)
        
        val aiRepository = AiRepository(this, settingsRepository)
        val favRepository = FavoriteRepository(this)
        val galleryRepository = GalleryRepository(this)
        
        // Initialize ViewModels lazily to improve startup performance
        val homeViewModel by lazy { HomeViewModel(aiRepository, favRepository, galleryRepository) }
        val todayEatViewModel by lazy { TodayEatViewModel(aiRepository, favRepository, galleryRepository) }
        val sauceDesignViewModel by lazy { SauceDesignViewModel(aiRepository, favRepository, galleryRepository) }
        val fortuneCookingViewModel by lazy { FortuneCookingViewModel(aiRepository, favRepository, galleryRepository) }
        val favoritesViewModel by lazy { FavoritesViewModel(favRepository, aiRepository, galleryRepository) }
        val galleryViewModel by lazy { GalleryViewModel(galleryRepository) }
        val settingsViewModel by lazy { SettingsViewModel(settingsRepository, aiRepository) }
        val database by lazy { com.shijian.data.local.AppDatabase.getDatabase(this) }
        val chatRepository by lazy { ChatRepository(database.chatDao()) }
        
        // Properly managed ViewModel with SavedStateHandle support
        // We use this factory to injection dependencies and enable state survival
        @SuppressLint("RestrictedApi")
        val aiChatViewModel: AiChatViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST", "RestrictedApi")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("VisibleForTesting")
                @SuppressLint("VisibleForTesting")
                val handle = SavedStateHandle()



                return AiChatViewModel(
                    aiRepository, 
                    chatRepository, 
                    favRepository, 
                    settingsRepository, 
                    galleryRepository,
                    handle
                ) as T

            }
        })[AiChatViewModel::class.java]

        setContent {
            EatWhatTheme {
                var showSplash by remember { 
                    // Optimization: Skip splash if activity is being recreated 
                    // OR if it's already been shown in this process lifecycle
                    mutableStateOf(savedInstanceState == null && !isSplashShownInProcess) 
                }
                
                LaunchedEffect(showSplash) {
                    if (showSplash) {
                        delay(3000)
                        showSplash = false
                        isSplashShownInProcess = true
                    }
                }

                // Unified AnimatedContent to restore the "Premium" transition effect
                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        if (initialState && !targetState) {
                            // High-end Reveal: Splash fades/scales out, Home fades/scales in
                            (fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 1.1f, animationSpec = tween(800)))
                                .togetherWith(fadeOut(animationSpec = tween(800)) + scaleOut(targetScale = 0.9f, animationSpec = tween(800)))
                        } else {
                            // Zero-flash startup: No animation for initial splash appearance
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "master_transition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen()
                    } else {
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        
                        Scaffold(
                            bottomBar = {
                                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                                NeoNavigationBar {
                                    NAVIGATION_SCREENS.forEach { screen ->
                                        val isDiscoveryChild = remember(currentDestination?.route) {
                                            screen == Screen.Discovery && currentDestination?.route in listOf(Screen.MysteryBox.route, Screen.Sauce.route, Screen.Fortune.route, Screen.Settings.route)
                                        }
                                        val selected = currentDestination?.route == screen.route || isDiscoveryChild
                                        NeoNavigationItem(
                                            selected = selected,
                                            isProminent = (screen == Screen.Chat),
                                            onClick = {
                                                focusManager.clearFocus() // Clear focus before navigating
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = screen.icon,
                                            label = screen.label,
                                            selectedColor = when(screen) {
                                                Screen.Home -> Color(0xFFFB923C) // Match Header Orange
                                                Screen.Discovery -> Color(0xFFA78BFA) // Violet
                                                Screen.Chat -> Color(0xFFC084FC) // Soft Purple
                                                Screen.MysteryBox -> Color(0xFFFACC15) // Yellow
                                                Screen.Sauce -> Color(0xFF60A5FA) // Blue
                                                Screen.Fortune -> Color(0xFFA78BFA) // Violet
                                                Screen.Favorites -> Color(0xFFF87171) // Red
                                                Screen.Gallery -> Color(0xFF4ADE80) // Green
                                                Screen.Settings -> Color(0xFF9CA3AF) // Gray
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            val aniSpec = tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
                            val offsetSpec = tween<IntOffset>(durationMillis = 400, easing = FastOutSlowInEasing)
                            val enterSpec = fadeIn(aniSpec) + slideInVertically(offsetSpec) { it / 12 } + scaleIn(aniSpec, initialScale = 0.98f)
                            val exitSpec = fadeOut(aniSpec) + slideOutVertically(offsetSpec) { it / 12 } + scaleOut(aniSpec, targetScale = 0.98f)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFFACC15))
                                    .padding(innerPadding)
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Screen.Chat.route,
                                    modifier = Modifier.fillMaxSize(),
                                    enterTransition = { enterSpec },
                                    exitTransition = { exitSpec },
                                    popEnterTransition = { enterSpec },
                                    popExitTransition = { exitSpec }
                                ) {
                                    composable(Screen.Home.route) { 
                                        HomeScreen(
                                            viewModel = homeViewModel
                                        ) 
                                    }
                                    composable(Screen.Discovery.route) {
                                        DiscoveryScreen(
                                            onNavigate = { route -> navController.navigate(route) },
                                            onSettingsClick = { navController.navigate(Screen.Settings.route) }
                                        )
                                    }
                                    composable(Screen.MysteryBox.route) { 
                                        TodayEatScreen(
                                            viewModel = todayEatViewModel,
                                            onBack = { navController.popBackStack() }
                                        ) 
                                    }
                                    composable(Screen.Sauce.route) { 
                                        SauceDesignScreen(
                                            viewModel = sauceDesignViewModel,
                                            onBack = { navController.popBackStack() }
                                        ) 
                                    }
                                    composable(Screen.Fortune.route) { 
                                        FortuneCookingScreen(
                                            viewModel = fortuneCookingViewModel,
                                            onBack = { navController.popBackStack() }
                                        ) 
                                    }
                                    composable(Screen.Chat.route) {
                                        AiChatScreen(
                                             viewModel = aiChatViewModel,
                                             onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                                         )
                                    }
                                    composable(Screen.Favorites.route) { FavoritesScreen(favoritesViewModel) }
                                    composable(Screen.Gallery.route) {
                                        GalleryScreen(
                                            viewModel = galleryViewModel,
                                            onNavigateHome = {
                                                navController.navigate(Screen.Home.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                     composable(Screen.Settings.route) { 
                                         SettingsScreen(
                                             viewModel = settingsViewModel,
                                             onBack = { navController.popBackStack() }
                                         ) 
                                     }
                                }
                            }
                        }
                    }
                }



            }
        }

    }

}

private val NAVIGATION_SCREENS = listOf(Screen.Home, Screen.Discovery, Screen.Chat, Screen.Favorites, Screen.Gallery)
