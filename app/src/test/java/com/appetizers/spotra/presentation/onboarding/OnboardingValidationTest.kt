package com.appetizers.spotra.presentation.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingValidationTest {
    @Test
    fun `UW email validation accepts exact domain case insensitively`() {
        assertTrue(OnboardingValidation.isValidUwEmail("student@uwaterloo.ca"))
        assertTrue(OnboardingValidation.isValidUwEmail("STUDENT@UWATERLOO.CA"))
    }

    @Test
    fun `UW email validation rejects subdomains and lookalikes`() {
        assertFalse(OnboardingValidation.isValidUwEmail("student@edu.uwaterloo.ca"))
        assertFalse(OnboardingValidation.isValidUwEmail("student@uwaterloo.ca.example.com"))
        assertFalse(OnboardingValidation.isValidUwEmail("student@gmail.com"))
    }

    @Test
    fun `OTP validation requires exactly six digits`() {
        assertTrue(OnboardingValidation.isValidOtp("123456"))
        assertTrue(OnboardingValidation.isValidOtp("000000"))
        assertFalse(OnboardingValidation.isValidOtp("12345"))
        assertFalse(OnboardingValidation.isValidOtp("12345a"))
    }

    @Test
    fun `name validation trims input`() {
        assertTrue(OnboardingValidation.isValidName(" Vraj "))
        assertFalse(OnboardingValidation.isValidName(" A "))
    }
}
