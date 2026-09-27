package depollsoft.lib.util;

import static org.junit.Assert.*;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;


import depollsoft.lib.activity.RichApplication;

@RunWith(RobolectricTestRunner.class)
public class VersioningTest {

    @Before
    public void setUp() throws Exception {
        // Ensure RichApplication has a non-null Context before Versioning static init
        Application app = RuntimeEnvironment.getApplication();
        RichApplication.setAppContextForTesting(app.getApplicationContext());
    }

    @Test
    public void firstRun_flags_areConsistent() {
        // Touch the class to trigger static initialization
        int cur = Versioning.getCurrentVersion();
        int last = Versioning.getLastVersionSeen();

        assertTrue("Expected first run sentinel", Versioning.isFirstRun());
        assertTrue("Expected first run of this version", Versioning.isFirstRunOfVersion());
        assertTrue("Current version should be non-negative", cur >= 0);
        assertEquals(Integer.MIN_VALUE, last);
    }
}

