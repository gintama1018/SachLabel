package com.sachlabel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sachlabel.app.ui.navigation.SachLabelNavGraph
import com.sachlabel.app.ui.theme.SachLabelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SachLabelTheme {
                SachLabelNavGraph()
            }
        }
    }
}
