package depollsoft.tagmaster

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.privacy.TelemetryConsent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PrivacyConsentTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    /** Settings is Compose; the privacy choices dialog it opens is the shared platform dialog. */
    private fun openPrivacyChoices() {
        compose.onNodeWithTag("privacyChoicesButton").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun independentChoicesPersistAndCanBeRevoked() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("telemetry_consent", Context.MODE_PRIVATE).edit().clear().commit()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { TelemetryConsent.applyChoices(false, false) }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            openPrivacyChoices()
            onView(withText("Usage analytics")).inRoot(isDialog()).check(matches(isNotChecked()))
            onView(withText("Crash reports")).inRoot(isDialog()).check(matches(isNotChecked()))
            val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(context.getExternalFilesDir(null), "privacy-choices.png").outputStream().use {
                screen.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screen.recycle()
            onView(withText("Usage analytics")).inRoot(isDialog()).perform(click())
            onView(withText("Save choices")).inRoot(isDialog()).perform(click())
            assertTrue(PrivacyChoices(context).analytics)
            assertFalse(PrivacyChoices(context).crashes)
            scenario.recreate()
            openPrivacyChoices()
            onView(withText("Usage analytics")).inRoot(isDialog()).check(matches(isChecked()))
            onView(withText("Crash reports")).inRoot(isDialog()).check(matches(isNotChecked()))
            onView(withText("Decline both")).inRoot(isDialog()).perform(click())
            assertTrue(PrivacyChoices(context).hasChosen)
            assertFalse(PrivacyChoices(context).analytics)
            assertFalse(PrivacyChoices(context).crashes)
        }
    }
}
