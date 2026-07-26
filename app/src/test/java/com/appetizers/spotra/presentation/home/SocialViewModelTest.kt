package com.appetizers.spotra.presentation.home

import com.appetizers.spotra.domain.model.FriendProfile
import com.appetizers.spotra.domain.model.FriendshipStatus
import com.appetizers.spotra.domain.repository.FriendRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val friend = FriendProfile(
        id = "user-2",
        firstName = "Akshat",
        lastName = "Jain",
        program = "Software Engineering",
        term = "2B",
        friendshipId = "friendship-1",
        friendshipStatus = FriendshipStatus.ACCEPTED,
        isRequester = true,
    )

    private val outgoing = FriendProfile(
        id = "user-3",
        firstName = "Vraj",
        lastName = "Bhavsar",
        friendshipId = "friendship-2",
        friendshipStatus = FriendshipStatus.PENDING,
        isRequester = true,
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
    fun `removing a friend drops them from the friends list`() = runTest(dispatcher) {
        val friends = RecordingFriendRepository(listOf(friend, outgoing))
        val viewModel = SocialViewModel(friends)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.friends.size)

        viewModel.removeFriend(friend)
        advanceUntilIdle()

        assertEquals("friendship-1", friends.removedFriendshipId)
        assertTrue(viewModel.state.value.friends.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `cancelling a sent request drops it from outgoing requests`() = runTest(dispatcher) {
        val friends = RecordingFriendRepository(listOf(friend, outgoing))
        val viewModel = SocialViewModel(friends)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.outgoingRequests.size)

        viewModel.cancelRequest(outgoing)
        advanceUntilIdle()

        assertEquals("friendship-2", friends.removedFriendshipId)
        assertTrue(viewModel.state.value.outgoingRequests.isEmpty())
        // Removing a request must not disturb the accepted friend alongside it.
        assertEquals(1, viewModel.state.value.friends.size)
    }

    @Test
    fun `a failed removal surfaces an error and keeps the friend listed`() = runTest(dispatcher) {
        val friends = RecordingFriendRepository(listOf(friend), failRemoval = true)
        val viewModel = SocialViewModel(friends)
        advanceUntilIdle()

        viewModel.removeFriend(friend)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertEquals(1, viewModel.state.value.friends.size)
    }

    @Test
    fun `a profile with no friendship id is left alone`() = runTest(dispatcher) {
        val friends = RecordingFriendRepository(listOf(friend))
        val viewModel = SocialViewModel(friends)
        advanceUntilIdle()

        viewModel.removeFriend(friend.copy(friendshipId = null))
        advanceUntilIdle()

        assertNull(friends.removedFriendshipId)
        assertEquals(1, viewModel.state.value.friends.size)
    }
}

private class RecordingFriendRepository(
    private val profiles: List<FriendProfile>,
    private val failRemoval: Boolean = false,
) : FriendRepository {
    var removedFriendshipId: String? = null

    override suspend fun currentUserId(): String = "user-1"
    override suspend fun fetchFriendProfiles(): List<FriendProfile> = profiles
    override suspend fun searchUsers(query: String, excludeIds: Set<String>): List<FriendProfile> =
        emptyList()

    override suspend fun fetchSuggested(acceptedFriendIds: Set<String>): List<FriendProfile> =
        emptyList()

    override suspend fun sendRequest(toUserId: String) = Unit
    override suspend fun acceptRequest(friendshipId: String) = Unit
    override suspend fun declineRequest(friendshipId: String) = Unit

    override suspend fun removeFriendship(friendshipId: String) {
        if (failRemoval) error("network down")
        removedFriendshipId = friendshipId
    }

    override suspend fun fetchFriendsAtSpot(spotSlug: String): List<FriendProfile> = emptyList()
}
