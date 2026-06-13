package com.appetizers.spotra.presentation.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.appetizers.spotra.presentation.theme.SpotraTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcomeScreenShowsBrandingAndInvokesPrimaryAction() {
        var createAccountClicked = false

        composeRule.setContent {
            SpotraTheme {
                WelcomeScreen(
                    onCreateAccount = { createAccountClicked = true },
                    onSignIn = {}
                )
            }
        }

        composeRule.onNodeWithText("Find your perfect\nstudy spot at UW.").assertIsDisplayed()
        composeRule.onNodeWithText("Create account").performClick()

        assertTrue(createAccountClicked)
    }
}
