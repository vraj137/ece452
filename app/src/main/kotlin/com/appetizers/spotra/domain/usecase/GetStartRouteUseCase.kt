package com.appetizers.spotra.domain.usecase

import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.ProfileRepository

sealed interface StartRoute {
    data object Welcome : StartRoute
    data object Name : StartRoute
    data object Home : StartRoute
}

class GetStartRouteUseCase(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(): StartRoute {
        val user = authRepository.currentUser() ?: return StartRoute.Welcome
        val profile = runCatching { profileRepository.getProfile(user.id) }
            .getOrElse { return StartRoute.Name }
        return if (profile?.onboardingComplete == true) StartRoute.Home else StartRoute.Name
    }
}
