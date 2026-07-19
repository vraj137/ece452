package com.appetizers.spotra.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.StreakRepository
import com.appetizers.spotra.domain.usecase.AwardBadgesUseCase
import com.appetizers.spotra.domain.usecase.GetStartRouteUseCase
import com.appetizers.spotra.domain.usecase.StartRoute
import com.appetizers.spotra.presentation.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val getStartRoute: GetStartRouteUseCase,
    private val authRepository: AuthRepository,
    private val streakRepository: StreakRepository,
    private val awardBadgesUseCase: AwardBadgesUseCase,
) : ViewModel() {
    private val _startRoute = MutableStateFlow<String>(Routes.Loading)
    val startRoute: StateFlow<String> = _startRoute.asStateFlow()

    private val _loginStreak = MutableStateFlow(0)
    val loginStreak: StateFlow<Int> = _loginStreak.asStateFlow()

    init {
        resolveStartRoute()
    }

    fun resolveStartRoute() {
        viewModelScope.launch {
            val route = getStartRoute()
            _startRoute.value = when (route) {
                StartRoute.Home -> Routes.Home
                StartRoute.Name -> Routes.Name
                StartRoute.Welcome -> Routes.Welcome
            }
            if (route == StartRoute.Home || route == StartRoute.Name) {
                authRepository.currentUser()?.id?.let { userId ->
                    runCatching {
                        val streak = streakRepository.recordLogin(userId)
                        awardBadgesUseCase.onLogin(userId, streak)
                        _loginStreak.value = streak
                    }
                }
            }
        }
    }

    class Factory(
        private val getStartRoute: GetStartRouteUseCase,
        private val authRepository: AuthRepository,
        private val streakRepository: StreakRepository,
        private val awardBadgesUseCase: AwardBadgesUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(getStartRoute, authRepository, streakRepository, awardBadgesUseCase) as T
    }
}
