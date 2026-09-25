package depollsoft.lib.util;

import static org.junit.Assert.*;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.HashMap;
import java.util.Map;

import depollsoft.lib.activity.RichApplication;

@RunWith(RobolectricTestRunner.class)
public class PreferencesTest {

    @Before
    public void setUp() throws Exception {
        // Ensure RichApplication has a non-null Context before Preferences static init
        Application app = RuntimeEnvironment.getApplication();
        RichApplication.setAppContextForTesting(app.getApplicationContext());
    }

    @Test
    public void setAndGet_simpleString() {
        String key = "prefs.simple";
        assertTrue(Preferences.set(key, "hello"));
        String value = Preferences.get(key);
        assertEquals("hello", value);
    }

    @Test
    public void setAndGet_map_convertsMappingListRoundTrip() {
        String key = "prefs.map";
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(Preferences.set(key, map));

        @SuppressWarnings("unchecked")
        Map<String, Integer> rt = Preferences.get(key);
        assertEquals(2, rt.size());
        assertEquals(Integer.valueOf(1), rt.get("a"));
        assertEquals(Integer.valueOf(2), rt.get("b"));
    }

    @Test
    public void initialize_keepsExistingValueWhenTypeMatches() {
        String key = "prefs.init";
        assertTrue(Preferences.set(key, 5));

        // Should detect existing Integer and not overwrite
        Preferences.initialize(key, 10, Integer.class);
        Integer value = Preferences.get(key);
        assertEquals(Integer.valueOf(5), value);
    }

    @Test
    public void initialize_setsDefaultWhenMissing() {
        String key = "prefs.init.default";
        Preferences.initialize(key, 42);
        Integer value = Preferences.get(key);
        assertEquals(Integer.valueOf(42), value);
    }
}

