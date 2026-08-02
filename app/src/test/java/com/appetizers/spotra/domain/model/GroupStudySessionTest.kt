package com.appetizers.spotra.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupStudySessionTest {
    @Test
    fun `only a public group creator can end the group`() {
        assertTrue(group(isOwner = true, visibility = GroupVisibility.Public).canEndGroup)
        assertFalse(group(isOwner = false, visibility = GroupVisibility.Public).canEndGroup)
    }

    @Test
    fun `private group end behavior remains owner based`() {
        assertTrue(group(isOwner = true, visibility = GroupVisibility.Private).canEndGroup)
        assertFalse(group(isOwner = false, visibility = GroupVisibility.Private).canEndGroup)
    }

    private fun group(isOwner: Boolean, visibility: GroupVisibility) = GroupStudySession(
        id = "group-1",
        title = "Study group",
        subtitle = "",
        proximityLabel = "",
        members = emptyList(),
        isOwner = isOwner,
        visibility = visibility,
    )
}
