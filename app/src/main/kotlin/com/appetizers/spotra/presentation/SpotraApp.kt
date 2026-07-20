package com.appetizers.spotra.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.appetizers.spotra.AppContainer
import com.appetizers.spotra.presentation.components.SpotraLogo
import com.appetizers.spotra.presentation.home.HomeScreen
import com.appetizers.spotra.presentation.navigation.Routes
import com.appetizers.spotra.presentation.onboarding.CompleteScreen
import com.appetizers.spotra.presentation.onboarding.EmailScreen
import com.appetizers.spotra.presentation.onboarding.NameScreen
import com.appetizers.spotra.presentation.onboarding.OnboardingEvent
import com.appetizers.spotra.presentation.onboarding.OnboardingViewModel
import com.appetizers.spotra.presentation.onboarding.OtpScreen
import com.appetizers.spotra.presentation.onboarding.ProgramScreen
import com.appetizers.spotra.presentation.onboarding.WelcomeScreen

@Composable
fun SpotraApp(container: AppContainer) {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel(
        factory = AppViewModel.Factory(container.getStartRoute)
    )
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(
            container.authRepository,
            container.profileRepository,
            container.draftRepository
        )
    )
    val startRoute by appViewModel.startRoute.collectAsStateWithLifecycle()
    val state by onboardingViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val goBack: () -> Unit = {
        if (!navController.navigateUp()) {
            navController.navigate(Routes.Welcome) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(startRoute) {
        if (startRoute != Routes.Loading) {
            navController.navigate(startRoute) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(onboardingViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            onboardingViewModel.events.collect { event ->
                when (event) {
                    is OnboardingEvent.Navigate -> {
                        val clearFlow = event.route == Routes.Home
                        navController.navigate(event.route) {
                            launchSingleTop = true
                            if (clearFlow) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    NavHost(navController, startDestination = Routes.Loading) {
        composable(Routes.Loading) { LoadingScreen() }
        composable(Routes.Welcome) {
            WelcomeScreen(
                onCreateAccount = {
                    onboardingViewModel.beginRegistration()
                    navController.navigate(Routes.Name)
                },
                onSignIn = {
                    onboardingViewModel.beginSignIn()
                    navController.navigate(Routes.SignIn)
                }
            )
        }
        composable(Routes.Name) { NameScreen(state, goBack, onboardingViewModel) }
        composable(Routes.Email) { EmailScreen(state, goBack, onboardingViewModel, signIn = false) }
        composable(Routes.Otp) { OtpScreen(state, goBack, onboardingViewModel) }
        composable(Routes.Program) { ProgramScreen(state, goBack, onboardingViewModel) }
        composable(Routes.Complete) { CompleteScreen(state, onboardingViewModel::finishOnboarding) }
        composable(Routes.SignIn) { EmailScreen(state, goBack, onboardingViewModel, signIn = true) }
        composable(Routes.Home) { HomeScreen(container.homeRepository, container.socialRepository) }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SpotraLogo()
            Spacer(Modifier.height(16.dp))
            Text(
                "spotra",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
