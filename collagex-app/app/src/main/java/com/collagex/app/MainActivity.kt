package com.collagex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.collagex.app.data.AppViewModel
import com.collagex.app.ui.screens.CarouselScreen
import com.collagex.app.ui.screens.EditorScreen
import com.collagex.app.ui.screens.ExportScreen
import com.collagex.app.ui.screens.HomeScreen
import com.collagex.app.ui.screens.PhotoPickerScreen
import com.collagex.app.ui.screens.StyleSelectScreen
import com.collagex.app.ui.theme.CollageXTheme

object Routes {
    const val HOME = "home"
    const val PICKER = "picker"
    const val STYLE = "style"
    const val EDITOR = "editor"
    const val CAROUSEL = "carousel"
    const val EXPORT = "export"
}

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            CollageXTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(onGetStarted = { navController.navigate(Routes.PICKER) })
                    }
                    composable(Routes.PICKER) {
                        PhotoPickerScreen(
                            viewModel = viewModel,
                            onNext = { navController.navigate(Routes.STYLE) },
                        )
                    }
                    composable(Routes.STYLE) {
                        StyleSelectScreen(
                            viewModel = viewModel,
                            onNext = { navController.navigate(Routes.EDITOR) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.EDITOR) {
                        EditorScreen(
                            viewModel = viewModel,
                            onNext = { navController.navigate(Routes.CAROUSEL) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.CAROUSEL) {
                        CarouselScreen(
                            viewModel = viewModel,
                            onNext = { navController.navigate(Routes.EXPORT) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Routes.EXPORT) {
                        ExportScreen(
                            viewModel = viewModel,
                            onDone = {
                                navController.popBackStack(Routes.HOME, inclusive = false)
                            },
                        )
                    }
                }
            }
        }
    }
}
