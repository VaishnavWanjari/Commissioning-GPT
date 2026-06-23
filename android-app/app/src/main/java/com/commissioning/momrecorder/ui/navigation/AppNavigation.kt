package com.commissioning.momrecorder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.commissioning.momrecorder.ui.detail.MomDetailScreen
import com.commissioning.momrecorder.ui.home.HomeScreen
import com.commissioning.momrecorder.ui.onboarding.OnboardingScreen
import com.commissioning.momrecorder.ui.settings.SettingsScreen
import com.commissioning.momrecorder.viewmodel.MainViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val MOM_DETAIL = "mom_detail/{momId}"
    fun momDetail(id: String) = "mom_detail/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val vm: MainViewModel = viewModel()
    val onboardingDone = vm.prefs.onboardingDone

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) Routes.HOME else Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    vm.prefs.onboardingDone = true
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onNavigateToDetail = { momId ->
                    navController.navigate(Routes.momDetail(momId))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MOM_DETAIL,
            arguments = listOf(navArgument("momId") { type = NavType.StringType })
        ) { backStack ->
            val momId = backStack.arguments?.getString("momId") ?: return@composable
            MomDetailScreen(
                momId = momId,
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
