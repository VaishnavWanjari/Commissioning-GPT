package com.commissioning.momrecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.commissioning.momrecorder.ui.navigation.AppNavigation
import com.commissioning.momrecorder.ui.theme.MinutesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinutesTheme {
                AppNavigation()
            }
        }
    }
}
