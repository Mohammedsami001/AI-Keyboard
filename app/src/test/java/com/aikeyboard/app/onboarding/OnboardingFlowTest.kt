package com.aikeyboard.app.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboarding_navigates_to_enable_keyboard() {
        var navigated = false
        
        composeTestRule.setContent {
            WelcomeScreen(onNext = { navigated = true })
        }

        // Slide 0 -> 1
        composeTestRule.onNodeWithText("Next").performClick()
        
        // Slide 1 -> 2
        composeTestRule.onNodeWithText("Next").performClick()
        
        // Slide 2 -> Complete
        composeTestRule.onNodeWithText("Get Started").performClick()
        
        // Assert that the onNext callback was fired indicating navigation
        assert(navigated)
    }
}
