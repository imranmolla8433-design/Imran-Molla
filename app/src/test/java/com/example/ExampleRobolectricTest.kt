package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun authGate_andSavingTracker_initializesAtDecember2026_togglesPayments_andNavigatesMonths() {
        // 1. Verify AuthScreen is presented with Google Sign-In, Login/Register/Forgot Password tabs, and Remember Me
        composeTestRule.onNodeWithTag("google_sign_in_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_login").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_register").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_name_input").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tab_forgot_password").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("auth_submit_button").performScrollTo().assertIsDisplayed()

        // Continue to Dashboard via local backup mode for headless Robolectric UI verification
        val continueBtn = composeTestRule.onNodeWithTag("continue_local_backup_button")
        continueBtn.performScrollTo()
        continueBtn.performClick()
        composeTestRule.waitForIdle()

        // 2. Verify app title and initial target ₹10,400 card
        composeTestRule.onNodeWithTag("app_title_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_total_target").assertIsDisplayed()

        // 3. Verify calendar starts automatically at December 2026
        val badgeNode = composeTestRule.onNodeWithTag("calendar_month_saved_badge")
        badgeNode.performScrollTo()
        badgeNode.assertTextContains("₹0 / ₹1,500")

        // 4. Click December 2, 2026 saving date to mark as PAID
        val dec2Node = composeTestRule.onNodeWithTag("calendar_day_2026-12-02")
        dec2Node.performScrollTo()
        dec2Node.performClick()
        composeTestRule.waitForIdle()

        // Verify monthly badge updates to ₹100 / ₹1,500
        badgeNode.performScrollTo()
        badgeNode.assertTextContains("₹100 / ₹1,500")

        // 5. Click December 2, 2026 again to toggle back to DUE
        dec2Node.performScrollTo()
        dec2Node.performClick()
        composeTestRule.waitForIdle()
        badgeNode.performScrollTo()
        badgeNode.assertTextContains("₹0 / ₹1,500")

        // 6. Navigate to next month (January 2027)
        val nextBtn = composeTestRule.onNodeWithTag("next_month_button")
        nextBtn.performScrollTo()
        nextBtn.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("current_month_header").assertIsDisplayed()

        // 7. Open Reset Confirmation Dialog and cancel
        val resetBtn = composeTestRule.onNodeWithTag("reset_all_button")
        resetBtn.performScrollTo()
        resetBtn.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reset_dialog_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_reset_button").performClick()
        composeTestRule.waitForIdle()
    }
}
