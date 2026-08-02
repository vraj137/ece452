package com.appetizers.spotra.presentation.home

import com.appetizers.spotra.domain.model.BadgeId
import com.appetizers.spotra.domain.model.StudyTerm
import com.appetizers.spotra.domain.model.UserBadge
import com.appetizers.spotra.domain.model.UserProfile
import com.appetizers.spotra.domain.repository.AuthRepository
import com.appetizers.spotra.domain.repository.AuthUser
import com.appetizers.spotra.domain.repository.BadgeRepository
import com.appetizers.spotra.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val initialProfile = UserProfile(
        userId = "user-1",
        firstName = "Vraj",
        lastName = "Bhavsar",
        email = "vbhavsar@uwaterloo.ca",
        program = "Computer Engineering",
        studyTerm = StudyTerm.FOUR_A,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saving academic profile updates program and year`() = runTest(dispatcher) {
        val profiles = RecordingProfileRepository(initialProfile)
        val viewModel = buildViewModel(profiles)
        advanceUntilIdle()
        var completed = false

        viewModel.saveAcademicProfile("  Software Engineering  ", StudyTerm.THREE_B) {
            completed = true
        }
        advanceUntilIdle()

        assertTrue(completed)
        assertEquals("Software Engineering", profiles.savedProfile?.program)
        assertEquals(StudyTerm.THREE_B, profiles.savedProfile?.studyTerm)
        assertEquals("Software Engineering", viewModel.state.value.profile?.program)
        assertFalse(viewModel.state.value.isAcademicProfileSaving)
    }

    @Test
    fun `academic save preserves a changed location visibility`() = runTest(dispatcher) {
        val profiles = RecordingProfileRepository(initialProfile)
        val viewModel = buildViewModel(profiles)
        advanceUntilIdle()

        viewModel.setLocationVisibility(LocationVisibility.Visible)
        viewModel.saveAcademicProfile("Computer Science", StudyTerm.TWO_A)
        advanceUntilIdle()

        assertEquals("visible", profiles.savedProfile?.locationVisibility)
        assertEquals("Computer Science", profiles.savedProfile?.program)
        assertEquals(StudyTerm.TWO_A, profiles.savedProfile?.studyTerm)
        assertEquals(LocationVisibility.Visible, viewModel.state.value.locationVisibility)
    }

    @Test
    fun `everyone visibility persists its explicit database value`() = runTest(dispatcher) {
        val profiles = RecordingProfileRepository(initialProfile)
        val viewModel = buildViewModel(profiles)
        advanceUntilIdle()

        viewModel.setLocationVisibility(LocationVisibility.Everyone)
        advanceUntilIdle()

        assertEquals("everyone", profiles.savedProfile?.locationVisibility)
        assertEquals(LocationVisibility.Everyone, viewModel.state.value.locationVisibility)
    }

    private fun buildViewModel(profiles: ProfileRepository) = ProfileViewModel(
        profileRepository = profiles,
        authRepository = ProfileAuthRepository(),
        friendRepository = null,
        badgeRepository = EmptyBadgeRepository(),
    )
}

private class RecordingProfileRepository(
    private val initialProfile: UserProfile,
) : ProfileRepository {
    var savedProfile: UserProfile? = null

    override suspend fun getProfile(userId: String): UserProfile = initialProfile

    override suspend fun saveProfile(profile: UserProfile) {
        savedProfile = profile
    }

    override suspend fun updateLocationVisibility(userId: String, visibility: String) {
        savedProfile = initialProfile.copy(locationVisibility = visibility)
    }
}

private class ProfileAuthRepository : AuthRepository {
    private val user = AuthUser("user-1", "vbhavsar@uwaterloo.ca")

    override suspend fun currentUser(): AuthUser = user
    override suspend fun sendOtp(email: String, createUser: Boolean) = Unit
    override suspend fun verifyOtp(email: String, token: String): AuthUser = user
    override suspend fun signOut() = Unit
}

private class EmptyBadgeRepository : BadgeRepository {
    override suspend fun getBadges(userId: String): List<UserBadge> = emptyList()
    override suspend fun awardBadge(userId: String, badgeId: BadgeId) = Unit
}
