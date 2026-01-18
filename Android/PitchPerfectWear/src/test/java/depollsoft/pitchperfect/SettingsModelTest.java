package depollsoft.pitchperfect;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.util.Preferences;

/**
 * Unit tests for {@link SettingsModel}.
 * Tests preference operations for toggle notes and wake lock settings.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SettingsModelTest {

    @BeforeClass
    public static void setUpClass() {
        // Enable test mode to use in-memory preferences
        Preferences.setTestMode(true);
    }

    @AfterClass
    public static void tearDownClass() {
        // Disable test mode after all tests
        Preferences.setTestMode(false);
    }

    @Before
    public void setUp() throws Exception {
        // Clear test values before each test for isolation
        Preferences.clearTestValues();

        // Re-initialize the static fields in SettingsModel to reset trackables
        resetSettingsModelTrackables();
    }

    private void resetSettingsModelTrackables() throws Exception {
        // Force re-initialization of preferences with default values
        Preferences.initialize("depollsoft.pitchperfect.ToggleNote", false);
        Preferences.initialize("depollsoft.pitchperfect.WakeLock", false);
    }

    // Toggle Notes Tests

    @Test
    public void getToggleNotes_returnsFalse_byDefault() {
        boolean result = SettingsModel.getToggleNotes();

        assertFalse("Toggle notes should be false by default", result);
    }

    @Test
    public void setToggleNotes_true_updatesValue() {
        SettingsModel.setToggleNotes(true);

        assertTrue("Toggle notes should be true after setting", SettingsModel.getToggleNotes());
    }

    @Test
    public void setToggleNotes_false_updatesValue() {
        SettingsModel.setToggleNotes(true);
        SettingsModel.setToggleNotes(false);

        assertFalse("Toggle notes should be false after setting", SettingsModel.getToggleNotes());
    }

    @Test
    public void toggleNotes_persistsAcrossReads() {
        SettingsModel.setToggleNotes(true);

        // Multiple reads should return the same persisted value
        assertTrue(SettingsModel.getToggleNotes());
        assertTrue(SettingsModel.getToggleNotes());
        assertTrue(SettingsModel.getToggleNotes());
    }

    @Test
    public void toggleNotes_multipleToggles_trackCorrectly() {
        assertFalse(SettingsModel.getToggleNotes());

        SettingsModel.setToggleNotes(true);
        assertTrue(SettingsModel.getToggleNotes());

        SettingsModel.setToggleNotes(false);
        assertFalse(SettingsModel.getToggleNotes());

        SettingsModel.setToggleNotes(true);
        assertTrue(SettingsModel.getToggleNotes());
    }

    // Wake Lock Tests

    @Test
    public void getWakeLock_returnsFalse_byDefault() {
        boolean result = SettingsModel.getWakeLock();

        assertFalse("Wake lock should be false by default", result);
    }

    @Test
    public void setWakeLock_true_updatesValue() {
        SettingsModel.setWakeLock(true);

        assertTrue("Wake lock should be true after setting", SettingsModel.getWakeLock());
    }

    @Test
    public void setWakeLock_false_updatesValue() {
        SettingsModel.setWakeLock(true);
        SettingsModel.setWakeLock(false);

        assertFalse("Wake lock should be false after setting", SettingsModel.getWakeLock());
    }

    @Test
    public void wakeLock_persistsAcrossReads() {
        SettingsModel.setWakeLock(true);

        // Multiple reads should return the same persisted value
        assertTrue(SettingsModel.getWakeLock());
        assertTrue(SettingsModel.getWakeLock());
        assertTrue(SettingsModel.getWakeLock());
    }

    @Test
    public void wakeLock_multipleToggles_trackCorrectly() {
        assertFalse(SettingsModel.getWakeLock());

        SettingsModel.setWakeLock(true);
        assertTrue(SettingsModel.getWakeLock());

        SettingsModel.setWakeLock(false);
        assertFalse(SettingsModel.getWakeLock());

        SettingsModel.setWakeLock(true);
        assertTrue(SettingsModel.getWakeLock());
    }

    // Combined Settings Tests

    @Test
    public void toggleNotes_and_wakeLock_areIndependent() {
        // Set only toggle notes
        SettingsModel.setToggleNotes(true);

        assertTrue("Toggle notes should be true", SettingsModel.getToggleNotes());
        assertFalse("Wake lock should still be false", SettingsModel.getWakeLock());

        // Set only wake lock
        SettingsModel.setWakeLock(true);

        assertTrue("Toggle notes should still be true", SettingsModel.getToggleNotes());
        assertTrue("Wake lock should be true", SettingsModel.getWakeLock());

        // Unset toggle notes
        SettingsModel.setToggleNotes(false);

        assertFalse("Toggle notes should be false", SettingsModel.getToggleNotes());
        assertTrue("Wake lock should still be true", SettingsModel.getWakeLock());
    }

    @Test
    public void bothSettings_canBeTrueSimultaneously() {
        SettingsModel.setToggleNotes(true);
        SettingsModel.setWakeLock(true);

        assertTrue(SettingsModel.getToggleNotes());
        assertTrue(SettingsModel.getWakeLock());
    }

    @Test
    public void bothSettings_canBeFalseSimultaneously() {
        // First set to true
        SettingsModel.setToggleNotes(true);
        SettingsModel.setWakeLock(true);

        // Then set back to false
        SettingsModel.setToggleNotes(false);
        SettingsModel.setWakeLock(false);

        assertFalse(SettingsModel.getToggleNotes());
        assertFalse(SettingsModel.getWakeLock());
    }

    // Edge Case Tests

    @Test
    public void setToggleNotes_sameTrueValue_noError() {
        SettingsModel.setToggleNotes(true);
        SettingsModel.setToggleNotes(true);
        SettingsModel.setToggleNotes(true);

        assertTrue(SettingsModel.getToggleNotes());
    }

    @Test
    public void setToggleNotes_sameFalseValue_noError() {
        SettingsModel.setToggleNotes(false);
        SettingsModel.setToggleNotes(false);
        SettingsModel.setToggleNotes(false);

        assertFalse(SettingsModel.getToggleNotes());
    }

    @Test
    public void setWakeLock_sameTrueValue_noError() {
        SettingsModel.setWakeLock(true);
        SettingsModel.setWakeLock(true);
        SettingsModel.setWakeLock(true);

        assertTrue(SettingsModel.getWakeLock());
    }

    @Test
    public void setWakeLock_sameFalseValue_noError() {
        SettingsModel.setWakeLock(false);
        SettingsModel.setWakeLock(false);
        SettingsModel.setWakeLock(false);

        assertFalse(SettingsModel.getWakeLock());
    }

    @Test
    public void rapidToggling_handledCorrectly() {
        for (int i = 0; i < 100; i++) {
            boolean value = (i % 2 == 0);
            SettingsModel.setToggleNotes(value);
            SettingsModel.setWakeLock(!value);

            assertEquals("Toggle notes mismatch at iteration " + i, value, SettingsModel.getToggleNotes());
            assertEquals("Wake lock mismatch at iteration " + i, !value, SettingsModel.getWakeLock());
        }
    }
}
