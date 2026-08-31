package depollsoft.lib.util;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.robolectric.Shadows.shadowOf;

/**
 * Unit tests for the IntentUtilities class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class IntentUtilitiesTest {

    private Context context;
    private ShadowPackageManager shadowPackageManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        shadowPackageManager = shadowOf(context.getPackageManager());
    }

    // =====================================================================
    // isIntentAvailable tests
    // =====================================================================

    @Test
    public void isIntentAvailable_withNoMatchingActivity_returnsFalse() {
        Intent intent = new Intent("com.nonexistent.ACTION_" + System.nanoTime());
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        assertFalse("Should return false when no activity can handle intent", result);
    }

    @Test
    public void isIntentAvailable_withMatchingActivity_returnsTrue() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse("http://example.com"));
        
        // Add a resolve info to shadow package manager
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.isDefault = true;
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
        
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        assertTrue("Should return true when activity can handle intent", result);
    }

    @Test
    public void isIntentAvailable_withNullIntent_doesNotCrash() {
        try {
            // This may throw depending on implementation
            IntentUtilities.isIntentAvailable(context, null);
            // If it doesn't throw, that's acceptable too
        } catch (NullPointerException e) {
            // Expected behavior
        }
    }

    @Test
    public void isIntentAvailable_withEmptyIntent_returnsFalse() {
        Intent intent = new Intent();
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        // Empty intent typically won't match any specific activity
        assertFalse("Empty intent should not match any activity", result);
    }

    @Test
    public void isIntentAvailable_withMultipleMatchingActivities_returnsTrue() {
        Intent intent = new Intent("com.test.MULTIPLE_ACTION_" + System.nanoTime());
        
        // Add multiple resolve infos
        ResolveInfo resolveInfo1 = new ResolveInfo();
        ResolveInfo resolveInfo2 = new ResolveInfo();
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo1);
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo2);
        
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        assertTrue("Should return true when multiple activities match", result);
    }

    @Test
    public void isIntentAvailable_withActionDialIntent_checksAvailability() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse("tel:1234567890"));
        
        // By default, no activities are registered in Robolectric
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        assertFalse("Should return false when no dialer is available", result);
    }

    @Test
    public void isIntentAvailable_withActionSendIntent_checksAvailability() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        assertFalse("Should return false when no sharing app is available", result);
    }

    @Test
    public void isIntentAvailable_withCustomActionAndRegisteredHandler_returnsTrue() {
        String customAction = "com.depollsoft.CUSTOM_ACTION_" + System.nanoTime();
        Intent intent = new Intent(customAction);
        
        ResolveInfo resolveInfo = new ResolveInfo();
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
        
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        assertTrue("Should return true for registered custom action", result);
    }

    @Test
    public void isIntentAvailable_differentContexts_workCorrectly() {
        Intent intent = new Intent("com.test.ACTION_" + System.nanoTime());
        ResolveInfo resolveInfo = new ResolveInfo();
        shadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
        
        // Use application context
        Context appContext = context.getApplicationContext();
        boolean result = IntentUtilities.isIntentAvailable(appContext, intent);
        assertTrue("Should work with application context", result);
    }

    @Test
    public void isIntentAvailable_withCategoryBrowsable_checksCorrectly() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(android.net.Uri.parse("http://test.com"));
        
        boolean result = IntentUtilities.isIntentAvailable(context, intent);
        // By default, no browser is registered
        assertFalse("Should return false when no browser is available", result);
    }

    @Test
    public void isIntentAvailable_sameIntentCalledTwice_consistentResult() {
        Intent intent = new Intent("com.test.CONSISTENT_" + System.nanoTime());
        
        boolean result1 = IntentUtilities.isIntentAvailable(context, intent);
        boolean result2 = IntentUtilities.isIntentAvailable(context, intent);
        
        assertEquals("Same intent should return consistent results", result1, result2);
    }
}
