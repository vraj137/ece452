package com.appetizers.spotra.presentation

import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.ProfileRepository
import com.appetizers.spotra.domain.usecase.GetStartRouteUseCase
import com.appetizers.spotra.presentation.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signed out users start at welcome`() = runTest(dispatcher) {
        val viewModel = AppViewModel(useCase(auth = null, profile = null))
        advanceUntilIdle()
        assertEquals(Routes.Welcome, viewModel.startRoute.value)
    }

    @Test
    fun `users with complete profiles start at home`() = runTest(dispatcher) {
        val user = AuthUser("user-1", "student@uwaterloo.ca")
        val profile = UserProfile(
            userId = user.id,
            firstName = "Vraj",
            lastName = "Patel",
            email = user.email,
            program = "Computer Engineering",
            studyTerm = StudyTerm.FOUR_A
        )
        val viewModel = AppViewModel(useCase(auth = user, profile = profile))
        advanceUntilIdle()
        assertEquals(Routes.Home, viewModel.startRoute.value)
    }

    @Test
    fun `authenticated users without profiles resume onboarding`() = runTest(dispatcher) {
        val user = AuthUser("user-1", "student@uwaterloo.ca")
        val viewModel = AppViewModel(useCase(auth = user, profile = null))
        advanceUntilIdle()
        assertEquals(Routes.Name, viewModel.startRoute.value)
    }

    @Test
    fun `network error during profile fetch routes to name not welcome`() = runTest(dispatcher) {
        val user = AuthUser("user-1", "student@uwaterloo.ca")
        val viewModel = AppViewModel(useCase(auth = user, profileThrows = true))
        advanceUntilIdle()
        assertEquals(Routes.Name, viewModel.startRoute.value)
    }

    private fun useCase(
        auth: AuthUser?,
        profile: UserProfile? = null,
        profileThrows: Boolean = false
    ) = GetStartRouteUseCase(
        authRepository = FakeAuthRepository(auth),
        profileRepository = FakeProfileRepository(profile, profileThrows)
    )
}

private class FakeAuthRepository(private val user: AuthUser?) : AuthRepository {
    override suspend fun currentUser(): AuthUser? = user
    override suspend fun sendOtp(email: String, createUser: Boolean) = Unit
    override suspend fun verifyOtp(email: String, token: String): AuthUser = requireNotNull(user)
    override suspend fun signOut() = Unit
}

private class FakeProfileRepository(
    private val profile: UserProfile?,
    private val throws: Boolean = false
) : ProfileRepository {
    override suspend fun getProfile(userId: String): UserProfile? {
        if (throws) error("Network error")
        return profile
    }
    override suspend fun saveProfile(profile: UserProfile) = Unit
}
