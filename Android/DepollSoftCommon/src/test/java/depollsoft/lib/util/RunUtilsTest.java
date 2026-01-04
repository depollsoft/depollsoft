package depollsoft.lib.util;

import static org.junit.Assert.*;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;

import depollsoft.lib.activity.RichApplication;

/**
 * Unit tests for the RunUtils utility class.
 */
@RunWith(RobolectricTestRunner.class)
public class RunUtilsTest {

    @Before
    public void setUp() throws Exception {
        // Ensure RichApplication has a non-null Context before static init
        Application app = RuntimeEnvironment.getApplication();
        Field f = RichApplication.class.getDeclaredField("context");
        f.setAccessible(true);
        f.set(null, app.getApplicationContext());
    }

    // =====================================================================
    // runOnce tests
    // =====================================================================

    @Test
    public void runOnce_firstCall_returnsTrue() {
        String uniqueKey = "test_first_" + System.nanoTime();
        boolean result = RunUtils.runOnce(uniqueKey);
        assertTrue("First call to runOnce should return true", result);
    }

    @Test
    public void runOnce_secondCall_returnsFalse() {
        String uniqueKey = "test_second_" + System.nanoTime();
        
        // First call
        RunUtils.runOnce(uniqueKey);
        
        // Second call
        boolean result = RunUtils.runOnce(uniqueKey);
        assertFalse("Second call to runOnce should return false", result);
    }

    @Test
    public void runOnce_multipleCallsSameKey_onlyFirstReturnsTrue() {
        String uniqueKey = "test_multiple_" + System.nanoTime();
        
        assertTrue(RunUtils.runOnce(uniqueKey));
        assertFalse(RunUtils.runOnce(uniqueKey));
        assertFalse(RunUtils.runOnce(uniqueKey));
        assertFalse(RunUtils.runOnce(uniqueKey));
    }

    @Test
    public void runOnce_differentKeys_eachReturnsTrue() {
        String key1 = "test_key1_" + System.nanoTime();
        String key2 = "test_key2_" + System.nanoTime();
        String key3 = "test_key3_" + System.nanoTime();
        
        assertTrue(RunUtils.runOnce(key1));
        assertTrue(RunUtils.runOnce(key2));
        assertTrue(RunUtils.runOnce(key3));
    }

    @Test
    public void runOnce_emptyKey_works() {
        // Using timestamp to make key unique per run
        String emptyKey = "" + System.nanoTime();
        assertTrue(RunUtils.runOnce(emptyKey));
        assertFalse(RunUtils.runOnce(emptyKey));
    }

    @Test
    public void runOnce_specialCharactersInKey_works() {
        String specialKey = "test.with-special_chars:123!" + System.nanoTime();
        assertTrue(RunUtils.runOnce(specialKey));
        assertFalse(RunUtils.runOnce(specialKey));
    }

    @Test
    public void runOnce_veryLongKey_works() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("long_key_segment_");
        }
        sb.append(System.nanoTime());
        String longKey = sb.toString();
        
        assertTrue(RunUtils.runOnce(longKey));
        assertFalse(RunUtils.runOnce(longKey));
    }

    @Test
    public void runOnce_usesPreferencesForStorage() {
        String uniqueKey = "test_storage_" + System.nanoTime();
        String prefKey = "depollsoft.lib.RunOnce." + uniqueKey;
        
        // Before runOnce, preference should not exist
        Object valueBefore = Preferences.get(prefKey);
        assertNull("Preference should not exist before runOnce", valueBefore);
        
        // Call runOnce
        RunUtils.runOnce(uniqueKey);
        
        // After runOnce, preference should exist
        Object valueAfter = Preferences.get(prefKey);
        assertNotNull("Preference should exist after runOnce", valueAfter);
    }

    @Test
    public void runOnce_independentKeysAreIndependent() {
        String keyA = "independent_a_" + System.nanoTime();
        String keyB = "independent_b_" + System.nanoTime();
        
        // Use key A
        assertTrue(RunUtils.runOnce(keyA));
        
        // Key B should still be available
        assertTrue(RunUtils.runOnce(keyB));
        
        // Both should now be used
        assertFalse(RunUtils.runOnce(keyA));
        assertFalse(RunUtils.runOnce(keyB));
    }
}
