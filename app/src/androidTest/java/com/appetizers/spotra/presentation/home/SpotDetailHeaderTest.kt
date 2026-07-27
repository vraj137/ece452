package com.appetizers.spotra.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.appetizers.spotra.domain.model.SpotPhoto
import com.appetizers.spotra.domain.model.StudySpotDetail
import com.appetizers.spotra.presentation.theme.SpotraTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SpotDetailHeaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun spot(photos: List<SpotPhoto> = emptyList()) = StudySpotDetail(
        id = "poets-lounge",
        name = "POETS Lounge",
        building = "Carl A. Pollock Hall",
        floor = "First-floor atrium",
        badge = "Lounge",
        photos = photos
    )

    @Test
    fun headerWithoutPhotosShowsNameLocationAndBackButton() {
        var backClicked = false

        composeRule.setContent {
            SpotraTheme {
                SpotDetailHeader(
                    spot = spot(),
                    friendsStudying = emptyList(),
                    onBack = { backClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("POETS Lounge").assertIsDisplayed()
        composeRule.onNodeWithText("Carl A. Pollock Hall · First-floor atrium").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backClicked)
    }

    @Test
    fun headerWithPhotosStillShowsNameAndBackButton() {
        var backClicked = false

        composeRule.setContent {
            SpotraTheme {
                SpotDetailHeader(
                    spot = spot(
                        photos = listOf(
                            SpotPhoto(url = "https://example.test/poets-1.jpg", caption = "Booth seating"),
                            SpotPhoto(url = "https://example.test/poets-2.jpg")
                        )
                    ),
                    friendsStudying = emptyList(),
                    onBack = { backClicked = true }
                )
            }
        }

        composeRule.onNodeWithText("POETS Lounge").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backClicked)
    }

    @Test
    fun photoPageAnnouncesItsPositionToScreenReaders() {
        composeRule.setContent {
            SpotraTheme {
                SpotDetailHeader(
                    spot = spot(
                        photos = listOf(
                            SpotPhoto(url = "https://example.test/poets-1.jpg", caption = "Booth seating"),
                            SpotPhoto(url = "https://example.test/poets-2.jpg")
                        )
                    ),
                    friendsStudying = emptyList(),
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Booth seating (1 of 2)").assertExists()
    }
}
