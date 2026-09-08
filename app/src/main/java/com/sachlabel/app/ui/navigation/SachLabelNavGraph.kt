package com.sachlabel.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sachlabel.app.data.mock.MockProducts
import com.sachlabel.app.ui.components.BottomTab
import com.sachlabel.app.ui.components.SachLabelBottomNav
import com.sachlabel.app.ui.screens.*
import com.sachlabel.app.ui.theme.AlertCrimson
import com.sachlabel.app.ui.theme.BackgroundSurface
import com.sachlabel.app.ui.theme.PrimaryGreen
import com.sachlabel.app.viewmodel.ScanViewModel
import com.sachlabel.app.viewmodel.SettingsViewModel

object Routes {
    const val WELCOME = "welcome"
    const val LANGUAGE_SELECT = "language_select"
    const val HOME = "home"
    const val HISTORY = "history"
    const val CAPTURE_FRONT = "capture_front"
    const val CAPTURE_BACK = "capture_back"
    const val PROCESSING = "processing"
    const val RESULT = "result"
    const val HEALTH_CONTEXT = "health_context"
    const val WHAT_WE_CHECK = "what_we_check"
    const val MORE = "more"
    const val MOCK_SELECT = "mock_select"
}

@Composable
fun SachLabelNavGraph() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()
    val scanViewModel: ScanViewModel = viewModel()

    val selectedLanguage by settingsViewModel.selectedLanguage.collectAsState()
    val isFirstLaunch by settingsViewModel.isFirstLaunch.collectAsState()
    val savedScans by scanViewModel.savedScans.collectAsState()

    // Keep scanViewModel's language in sync
    LaunchedEffect(selectedLanguage) {
        scanViewModel.selectedLanguage = selectedLanguage
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.HOME

    val showBottomBar = currentRoute in listOf(
        Routes.HOME,
        Routes.HISTORY,
        Routes.WHAT_WE_CHECK,
        Routes.MORE
    )

    val startDestination = if (isFirstLaunch) Routes.WELCOME else Routes.HOME

    val context = androidx.compose.ui.platform.LocalContext.current
    val localizedContext = remember(selectedLanguage) {
        val locale = java.util.Locale(selectedLanguage.code)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalContext provides localizedContext
    ) {
        Scaffold(
            containerColor = BackgroundSurface,
            bottomBar = {
            if (showBottomBar) {
                SachLabelBottomNav(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        when (tab) {
                            BottomTab.HOME -> {
                                if (currentRoute != Routes.HOME) {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                }
                            }
                            BottomTab.HISTORY -> {
                                if (currentRoute != Routes.HISTORY) {
                                    navController.navigate(Routes.HISTORY)
                                }
                            }
                            BottomTab.SCAN -> {
                                scanViewModel.startScan()
                                navController.navigate(Routes.CAPTURE_FRONT)
                            }
                            BottomTab.STANDARDS -> {
                                if (currentRoute != Routes.WHAT_WE_CHECK) {
                                    navController.navigate(Routes.WHAT_WE_CHECK)
                                }
                            }
                            BottomTab.MORE -> {
                                if (currentRoute != Routes.MORE) {
                                    navController.navigate(Routes.MORE)
                                }
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
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else androidx.compose.ui.unit.Dp(0f))
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                // Stitch Screen 1: Welcome & Onboarding
                composable(Routes.WELCOME) {
                    WelcomeScreen(
                        selectedLanguage = selectedLanguage,
                        onLanguageClick = { navController.navigate(Routes.LANGUAGE_SELECT) },
                        onStartScan = {
                            settingsViewModel.selectLanguage(selectedLanguage)
                            scanViewModel.startScan()
                            navController.navigate(Routes.CAPTURE_FRONT)
                        },
                        onDemoClick = {
                            settingsViewModel.selectLanguage(selectedLanguage)
                            navController.navigate(Routes.MOCK_SELECT)
                        },
                        onSkip = {
                            settingsViewModel.selectLanguage(selectedLanguage)
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }
                    )
                }

                // Stitch Screen 2: Language Selection
                composable(Routes.LANGUAGE_SELECT) {
                    LanguageSelectScreen(
                        currentLanguage = selectedLanguage,
                        onLanguageSelected = { language ->
                            settingsViewModel.selectLanguage(language)
                            navController.popBackStack()
                        }
                    )
                }

                // Stitch Screen 3: Home Screen
                composable(Routes.HOME) {
                    HomeScreen(
                        selectedLanguage = selectedLanguage,
                        savedScans = savedScans,
                        onScanClick = {
                            scanViewModel.startScan()
                            navController.navigate(Routes.CAPTURE_FRONT)
                        },
                        onHistoryClick = { navController.navigate(Routes.HISTORY) },
                        onMockDemoClick = { navController.navigate(Routes.MOCK_SELECT) },
                        onSavedScanClick = { item ->
                            scanViewModel.showSavedScan(item)
                            navController.navigate(Routes.RESULT)
                        },
                        onLanguageClick = { navController.navigate(Routes.LANGUAGE_SELECT) },
                        onWhatWeCheckClick = { navController.navigate(Routes.WHAT_WE_CHECK) }
                    )
                }

                // Stitch Screen 8: History & Verified Audits
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        selectedLanguage = selectedLanguage,
                        savedScans = savedScans,
                        onSavedScanClick = { item ->
                            scanViewModel.showSavedScan(item)
                            navController.navigate(Routes.RESULT)
                        },
                        onAuditSelected = { scenario ->
                            scanViewModel.runMockScenario(scenario)
                            navController.navigate(Routes.RESULT)
                        },
                        onScanClick = {
                            scanViewModel.startScan()
                            navController.navigate(Routes.CAPTURE_FRONT)
                        },
                        onLanguageClick = { navController.navigate(Routes.LANGUAGE_SELECT) }
                    )
                }

                // Stitch Screen 4: Front Camera Capture
                composable(Routes.CAPTURE_FRONT) {
                    CaptureFrontScreen(
                        onPhotoCaptured = { imagePath ->
                            scanViewModel.onFrontCaptured(imagePath)
                            navController.navigate(Routes.CAPTURE_BACK)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Stitch Screen 5: Back Camera Capture
                composable(Routes.CAPTURE_BACK) {
                    CaptureBackScreen(
                        onPhotoCaptured = { imagePath ->
                            scanViewModel.onBackCaptured(imagePath)
                            navController.navigate(Routes.PROCESSING) {
                                popUpTo(Routes.CAPTURE_FRONT) { inclusive = true }
                            }
                        },
                        onRetakeFront = {
                            scanViewModel.retakeFront()
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Stitch Screen 6: Processing Screen
                composable(Routes.PROCESSING) {
                    val uiState by scanViewModel.uiState.collectAsState()

                    LaunchedEffect(uiState) {
                        if (uiState is ScanViewModel.ScanUiState.Result) {
                            navController.navigate(Routes.RESULT) {
                                popUpTo(Routes.PROCESSING) { inclusive = true }
                            }
                        }
                    }

                    when (val state = uiState) {
                        is ScanViewModel.ScanUiState.Processing -> {
                            ProcessingScreen(step = state.step)
                        }
                        is ScanViewModel.ScanUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(BackgroundSurface)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = state.message,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AlertCrimson,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            scanViewModel.reset()
                                            navController.navigate(Routes.HOME) {
                                                popUpTo(Routes.HOME) { inclusive = true }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                    ) {
                                        Text("Return to Home")
                                    }
                                }
                            }
                        }
                        else -> {
                            ProcessingScreen(step = ScanViewModel.ProcessingStep.READING_LABEL)
                        }
                    }
                }

                // Stitch Screen 7: Claim Check / Result Screen
                composable(Routes.RESULT) {
                    val uiState by scanViewModel.uiState.collectAsState()
                    ResultScreen(
                        uiState = uiState,
                        selectedLanguage = selectedLanguage,
                        onHealthContextClick = { navController.navigate(Routes.HEALTH_CONTEXT) },
                        onScanAnother = {
                            scanViewModel.reset()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Stitch Screen 8: Health Context
                composable(Routes.HEALTH_CONTEXT) {
                    val uiState by scanViewModel.uiState.collectAsState()
                    HealthContextScreen(
                        scan = (uiState as? ScanViewModel.ScanUiState.Result)?.scan,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Stitch Screen 9: Truth Standards / What We Check
                composable(Routes.WHAT_WE_CHECK) {
                    WhatWeCheckScreen(
                        selectedLanguage = selectedLanguage,
                        onBack = { navController.popBackStack() },
                        onLanguageClick = { navController.navigate(Routes.LANGUAGE_SELECT) }
                    )
                }

                // Stitch Screen 10: More / Settings
                composable(Routes.MORE) {
                    MoreScreen(
                        selectedLanguage = selectedLanguage,
                        onLanguageClick = { navController.navigate(Routes.LANGUAGE_SELECT) },
                        onStandardsClick = { navController.navigate(Routes.WHAT_WE_CHECK) },
                        onDemoClick = { navController.navigate(Routes.MOCK_SELECT) }
                    )
                }

                // Demo Scenario Selector
                composable(Routes.MOCK_SELECT) {
                    MockSelectScreen(
                        scenarios = MockProducts.ALL,
                        onScenarioSelected = { scenario ->
                            scanViewModel.runMockScenario(scenario)
                            navController.navigate(Routes.PROCESSING)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
    }
}
