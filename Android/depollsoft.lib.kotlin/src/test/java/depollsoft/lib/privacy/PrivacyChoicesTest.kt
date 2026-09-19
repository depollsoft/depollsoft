package depollsoft.lib.privacy

import android.content.Context
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PrivacyChoicesTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    @Before fun clearChoices() {
        context.getSharedPreferences("telemetry_consent", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun newInstallCollectsNothing() {
        val choices = PrivacyChoices(context)
        assertFalse(choices.hasChosen)
        assertFalse(choices.analytics)
        assertFalse(choices.crashes)
    }
    @Test fun flagsWithoutAnExplicitChoiceDoNotEnableCollection() {
        context.getSharedPreferences("telemetry_consent", Context.MODE_PRIVATE).edit()
            .putBoolean("analytics", true).putBoolean("crashes", true).commit()
        assertFalse(PrivacyChoices(context).analytics)
        assertFalse(PrivacyChoices(context).crashes)
    }
    @Test fun choicesAreIndependentAndPersistAcrossInstances() {
        PrivacyChoices(context).save(analytics = true, crashes = false)
        assertTrue(PrivacyChoices(context).analytics)
        assertFalse(PrivacyChoices(context).crashes)
        PrivacyChoices(context).save(analytics = false, crashes = true)
        assertFalse(PrivacyChoices(context).analytics)
        assertTrue(PrivacyChoices(context).crashes)
    }
    @Test fun decliningIsRememberedAndRevokesBothChoices() {
        PrivacyChoices(context).save(analytics = true, crashes = true)
        PrivacyChoices(context).save(analytics = false, crashes = false)
        assertTrue(PrivacyChoices(context).hasChosen)
        assertFalse(PrivacyChoices(context).analytics)
        assertFalse(PrivacyChoices(context).crashes)
    }
}
