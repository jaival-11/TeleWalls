package me.jaival.telewalls.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.jaival.telewalls.ui.components.AnimatedBottomBar
import me.jaival.telewalls.ui.screens.auth.AuthScreen
import me.jaival.telewalls.ui.screens.detail.DetailScreen
import me.jaival.telewalls.ui.screens.favorites.FavoritesScreen
import me.jaival.telewalls.ui.screens.home.HomeScreen
import me.jaival.telewalls.ui.screens.onboarding.OnboardingScreen
import me.jaival.telewalls.ui.screens.settings.SettingsScreen
import me.jaival.telewalls.ui.screens.upload.UploadScreen
import me.jaival.telewalls.viewmodel.AuthViewModel
import me.jaival.telewalls.viewmodel.DetailViewModel
import me.jaival.telewalls.viewmodel.HomeViewModel
import me.jaival.telewalls.viewmodel.UploadViewModel

@Composable
fun TeleWallsNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ScreenRoutes.HOME

    val homeViewModel: HomeViewModel = hiltViewModel()
    val uploadViewModel: UploadViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val detailViewModel: DetailViewModel = hiltViewModel()

    val isSetupCompleted by authViewModel.isSetupCompleted.collectAsState()

    androidx.compose.runtime.LaunchedEffect(isSetupCompleted) {
        if (!isSetupCompleted && currentRoute != ScreenRoutes.ONBOARDING) {
            navController.navigate(ScreenRoutes.ONBOARDING) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showBottomBar = isSetupCompleted && currentRoute in listOf(
        ScreenRoutes.HOME,
        ScreenRoutes.FAVORITES,
        ScreenRoutes.UPLOAD,
        ScreenRoutes.SETTINGS
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                AnimatedBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo(ScreenRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = if (isSetupCompleted) ScreenRoutes.HOME else ScreenRoutes.ONBOARDING,
                enterTransition = { fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) }
            ) {
                composable(ScreenRoutes.ONBOARDING) {
                    OnboardingScreen(
                        viewModel = authViewModel,
                        onComplete = {
                            navController.navigate(ScreenRoutes.HOME) {
                                popUpTo(ScreenRoutes.ONBOARDING) { inclusive = true }
                            }
                        }
                    )
                }

                composable(ScreenRoutes.HOME) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onWallpaperClick = { id ->
                            navController.navigate(ScreenRoutes.detailRoute(id))
                        }
                    )
                }

                composable(ScreenRoutes.FAVORITES) {
                    FavoritesScreen(
                        viewModel = homeViewModel,
                        onWallpaperClick = { id ->
                            navController.navigate(ScreenRoutes.detailRoute(id))
                        }
                    )
                }

                composable(ScreenRoutes.UPLOAD) {
                    UploadScreen(
                        viewModel = uploadViewModel,
                        onUploadSuccess = {
                            navController.navigate(ScreenRoutes.HOME) {
                                popUpTo(ScreenRoutes.HOME) { inclusive = true }
                            }
                        }
                    )
                }

                composable(ScreenRoutes.SETTINGS) {
                    SettingsScreen(authViewModel = authViewModel)
                }

                composable(
                    route = ScreenRoutes.DETAIL,
                    arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType }),
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(350)) },
                    exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(350)) }
                ) { backStackEntry ->
                    val wallpaperId = backStackEntry.arguments?.getString("wallpaperId") ?: ""
                    DetailScreen(
                        wallpaperId = wallpaperId,
                        viewModel = detailViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
