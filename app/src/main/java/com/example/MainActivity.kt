package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TaskOverallStatus
import com.example.ui.MainViewModel
import com.example.ui.NavDestination
import com.example.ui.components.PlanReviewDialog
import com.example.ui.components.TaskPilotBottomNav
import com.example.ui.components.TaskPilotTopBar
import com.example.ui.components.WakeWordDetectedOverlay
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.FirstLaunchPermissionScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.TaskPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.prefsRepo.themeMode.collectAsState()

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            TaskPilotTheme(darkTheme = isDarkTheme) {
                TaskPilotRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TaskPilotRoot(viewModel: MainViewModel) {
    val onboardingCompleted by viewModel.prefsRepo.onboardingCompleted.collectAsState()
    val showWakeOverlay by viewModel.showWakeOverlay.collectAsState()
    val detectedWakeWord by viewModel.detectedWakeWord.collectAsState()
    val spokenText by viewModel.voiceHelper.spokenText.collectAsState()

    // Speak to Wake hotword listener popup overlay
    if (showWakeOverlay) {
        WakeWordDetectedOverlay(
            wakeWord = detectedWakeWord,
            recognizedText = spokenText,
            onDismiss = { viewModel.dismissWakeOverlay() },
            onSubmitCommand = { command ->
                viewModel.updateInputInstruction(command)
                viewModel.generatePlan(command)
            }
        )
    }

    if (!onboardingCompleted) {
        FirstLaunchPermissionScreen(
            viewModel = viewModel,
            onFinishOnboarding = {
                viewModel.prefsRepo.setOnboardingCompleted(true)
            }
        )
    } else {
        TaskPilotApp(viewModel = viewModel)
    }
}

@Composable
fun TaskPilotApp(viewModel: MainViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val overallStatus by viewModel.executionEngine.overallStatus.collectAsState()
    val reviewPlan by viewModel.reviewPlan.collectAsState()

    val isTaskActive = overallStatus == TaskOverallStatus.RUNNING ||
            overallStatus == TaskOverallStatus.WAITING_CONFIRMATION ||
            overallStatus == TaskOverallStatus.WAITING_USER_ACTION

    // Back handler: return to Home destination if not already on Home
    BackHandler(enabled = currentDestination != NavDestination.HOME) {
        viewModel.setDestination(NavDestination.HOME)
    }

    // Plan review dialog
    if (reviewPlan != null) {
        PlanReviewDialog(
            plan = reviewPlan!!,
            onConfirm = { plan -> viewModel.startExecutingPlan(plan) },
            onDismiss = { viewModel.dismissPlanReview() },
            onToggleStep = { index -> viewModel.toggleStepEnabled(index) }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TaskPilotTopBar(
                currentStatus = overallStatus,
                onStatusClick = {
                    if (isTaskActive) {
                        viewModel.setDestination(NavDestination.TASKS)
                    } else {
                        viewModel.setDestination(NavDestination.HOME)
                    }
                }
            )
        },
        bottomBar = {
            TaskPilotBottomNav(
                currentDestination = currentDestination,
                onDestinationSelected = { dest -> viewModel.setDestination(dest) },
                activeTaskRunning = isTaskActive
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.98f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
                        )
                },
                label = "screen_transition"
            ) { destination ->
                when (destination) {
                    NavDestination.HOME -> HomeScreen(viewModel = viewModel)
                    NavDestination.TASKS -> TasksScreen(viewModel = viewModel)
                    NavDestination.APPS -> AppsScreen(viewModel = viewModel)
                    NavDestination.ACTIVITY -> ActivityScreen(viewModel = viewModel)
                    NavDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
