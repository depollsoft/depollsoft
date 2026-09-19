package depollsoft.pitchperfect

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import depollsoft.lib.privacy.PrivacyChoices
import depollsoft.lib.privacy.TelemetryConsent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PrivacyConsentTest {
    @Test fun independentChoicesPersistAndCanBeRevoked() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("telemetry_consent", Context.MODE_PRIVATE).edit().clear().commit()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { TelemetryConsent.applyChoices(false, false) }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.privacyChoicesButton)).perform(scrollTo(), click())
            onView(withText("Usage analytics")).check(matches(isNotChecked()))
            onView(withText("Crash reports")).check(matches(isNotChecked()))
            val screen = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(context.getExternalFilesDir(null), "privacy-choices.png").outputStream().use {
                screen.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            screen.recycle()
            onView(withText("Usage analytics")).perform(click())
            onView(withText("Save choices")).perform(click())
            assertTrue(PrivacyChoices(context).analytics)
            assertFalse(PrivacyChoices(context).crashes)
            scenario.recreate()
            onView(withId(R.id.privacyChoicesButton)).perform(scrollTo(), click())
            onView(withText("Usage analytics")).check(matches(isChecked()))
            onView(withText("Crash reports")).check(matches(isNotChecked()))
            onView(withText("Decline both")).perform(click())
            assertTrue(PrivacyChoices(context).hasChosen)
            assertFalse(PrivacyChoices(context).analytics)
            assertFalse(PrivacyChoices(context).crashes)
        }
    }
}
