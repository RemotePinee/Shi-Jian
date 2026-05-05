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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
    object Cookbook : Screen("cookbook", "食谱库", Icons.AutoMirrored.Filled.MenuBook)
    object CookbookDetail : Screen("cookbook_detail", "食谱详情", Icons.Default.Receipt)
    object Chat : Screen("chat", "厨神", Icons.AutoMirrored.Filled.Chat)
    object MysteryBox : Screen("mystery", "开盲盒", Icons.Default.Casino)
    object Sauce : Screen("sauce", "调料箱", Icons.Default.Kitchen)
    object Fortune : Screen("fortune", "厨神算命", Icons.Default.AutoAwesome)
    object Favorites : Screen("favorites", "收藏", Icons.Default.Favorite)
    object Gallery : Screen("gallery", "图库", Icons.Default.PhotoLibrary)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
    object Tips : Screen("tips", "烹饪教程", Icons.Default.Book)
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
        val cookbookRepository = CookbookRepository(this)
        
        // Initialize ViewModels lazily to improve startup performance
        val homeViewModel by lazy { HomeViewModel(aiRepository, favRepository, galleryRepository) }
        val cookbookViewModel by lazy { CookbookViewModel(cookbookRepository) }
        val todayEatViewModel by lazy { TodayEatViewModel(aiRepository, favRepository, galleryRepository, cookbookRepository) }
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
                @SuppressLint("VisibleForTests")
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
                                            screen == Screen.Discovery && currentDestination?.route in listOf(
                                                Screen.MysteryBox.route, 
                                                Screen.Sauce.route, 
                                                Screen.Fortune.route, 
                                                Screen.Settings.route, 
                                                Screen.Cookbook.route, 
                                                Screen.CookbookDetail.route,
                                                Screen.Tips.route
                                            )
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
                                                Screen.Discovery -> Color(0xFF818CF8) // Indigo
                                                Screen.Cookbook -> Color(0xFF34D399) // Emerald
                                                Screen.CookbookDetail -> Color(0xFF34D399)
                                                Screen.Chat -> Color(0xFFC084FC) // Soft Purple
                                                Screen.MysteryBox -> Color(0xFFFACC15) // Yellow
                                                Screen.Sauce -> Color(0xFF60A5FA) // Blue
                                                Screen.Fortune -> Color(0xFF818CF8) // Indigo
                                                Screen.Favorites -> Color(0xFFF87171) // Red
                                                Screen.Gallery -> Color(0xFF4ADE80) // Green
                                                Screen.Settings -> Color(0xFF9CA3AF) // Gray
                                                Screen.Tips -> Color(0xFF818CF8) // Indigo
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
                                    composable(Screen.Cookbook.route) {
                                        CookbookScreen(
                                            viewModel = cookbookViewModel,
                                            onBack = { navController.popBackStack() },
                                            onRecipeClick = { recipe ->
                                                cookbookViewModel.selectRecipe(recipe)
                                                navController.navigate(Screen.CookbookDetail.route)
                                            }
                                        )
                                    }
                                    composable(Screen.CookbookDetail.route) {
                                        val selectedRecipe by cookbookViewModel.selectedRecipe.collectAsState()
                                        val favorites by favRepository.favoritesFlow.collectAsState()
                                        
                                        // Need to map CookbookRecipe to FavoriteRecipe to check if it is a favorite. 
                                        // For now, we'll check by name, or ideally we'd map it fully. 
                                        // Let's use a simple approach: if the name exists in favorites.
                                        val isFavorite = favorites.any { it.recipe.name == selectedRecipe?.name }
                                        
                                        CookbookDetailScreen(
                                            recipe = selectedRecipe,
                                            isFavorite = isFavorite,
                                            onBack = { navController.popBackStack() },
                                            onToggleFavorite = {
                                                // We need to convert CookbookRecipe to a standard Recipe and save it
                                                selectedRecipe?.let { cr ->
                                                    // Smart Extraction: Merge name and quantity, but only strip "note-like" parentheses
                                                    val smartIngredients = if (cr.portions.isNotEmpty()) {
                                                        cr.portions.map { 
                                                            it.replace(Regex("[(（][^)）]*?[喜不代可放看口味选比如包含或者推荐][^)）]*?[)）]"), "")
                                                              .replace(Regex("""[，\s]?推荐合计重量.*$"""), "")
                                                              .trim() 
                                                        }
                                                    } else {
                                                        cr.ingredients
                                                    }

                                                    val recipe = com.shijian.data.model.Recipe(
                                                        id = cr.id,
                                                        name = cr.name,
                                                        cuisine = cr.categoryName,
                                                        ingredients = smartIngredients,
                                                        steps = cr.steps.map { com.shijian.data.model.RecipeStep(step = it.step, description = it.description) },
                                                        difficulty = when(cr.difficulty) {
                                                            1, 2 -> "easy"
                                                            3 -> "medium"
                                                            else -> "hard"
                                                        },
                                                        tips = cr.tips
                                                    )
                                                    
                                                    if (isFavorite) {
                                                        // We'd ideally find the favorite ID
                                                        val fav = favorites.find { it.recipe.name == cr.name }
                                                        if (fav != null) {
                                                            favRepository.removeFavorite(fav.recipe.id)
                                                        }
                                                    } else {
                                                        val fav = com.shijian.data.model.FavoriteRecipe(
                                                            id = java.util.UUID.randomUUID().toString(),
                                                            recipe = recipe,
                                                            favoriteDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                        )
                                                        favRepository.addFavorite(fav)
                                                    }
                                                }
                                            },
                                            onAskAiImprovement = {
                                                selectedRecipe?.let { recipe ->
                                                    val prompt = cookbookViewModel.buildAiImprovementPrompt(recipe)
                                                    aiChatViewModel.sendMessage(prompt)
                                                    navController.navigate(Screen.Chat.route)
                                                }
                                            }
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
                                     composable(Screen.Tips.route) {
                                         TipsScreen(
                                             viewModel = cookbookViewModel,
                                             onBack = { navController.popBackStack() },
                                             onNavigate = { route -> navController.navigate(route) }
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
