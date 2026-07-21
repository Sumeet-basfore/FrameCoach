package com.framecoach.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.framecoach.app.ui.overlay.OnboardingOverlay
import com.framecoach.app.ui.settings.AppPreferences
import com.framecoach.app.ui.settings.SettingsScreen
import com.framecoach.app.ui.theme.FrameTheme
import org.junit.Rule
import org.junit.Test

class CameraHudUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun onboardingOverlay_displaysTitleAndTips_andDismissesOnButtonClick() {
        var dismissed = false

        composeTestRule.setContent {
            FrameTheme {
                OnboardingOverlay(
                    visible = true,
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Verify title and tips exist
        composeTestRule.onNodeWithText("Welcome to FrameCoach").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rule of Thirds").assertIsDisplayed()
        composeTestRule.onNodeWithText("Live Directional Guidance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Good Zone Feedback").assertIsDisplayed()

        // Perform click on Got it button
        composeTestRule.onNodeWithText("Got it!").performClick()

        assert(dismissed)
    }

    @Test
    fun settingsScreen_displaysHeaderAndToggleOptions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = AppPreferences(context)
        var navigatedBack = false

        composeTestRule.setContent {
            FrameTheme {
                SettingsScreen(
                    prefs = prefs,
                    onNavigateBack = { navigatedBack = true }
                )
            }
        }

        // Verify header and toggle options
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Viewfinder Grid").assertIsDisplayed()
        composeTestRule.onNodeWithText("Haptic Feedback").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio Coaching").assertIsDisplayed()
        composeTestRule.onNodeWithText("Composition Style").assertIsDisplayed()

        // Verify styles exist
        composeTestRule.onNodeWithText("Rule of Thirds").assertIsDisplayed()
        composeTestRule.onNodeWithText("Golden Ratio").assertIsDisplayed()
        composeTestRule.onNodeWithText("Center Grid").assertIsDisplayed()
    }
}
