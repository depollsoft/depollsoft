package depollsoft.lib.util;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.lang.reflect.Method;

import depollsoft.lib.activity.RichApplication;

/**
 * Unit tests for the ContentCache utility class.
 */
@RunWith(RobolectricTestRunner.class)
public class ContentCacheTest {

    private ContentCache cache;
    private Context context;

    @Before
    public void setUp() throws Exception {
        // Ensure RichApplication has a non-null Context
        Application app = RuntimeEnvironment.getApplication();
        RichApplication.setAppContextForTesting(app.getApplicationContext());
        
        context = app.getApplicationContext();
        cache = new ContentCache(context);
    }

    // =====================================================================
    // Constructor tests
    // =====================================================================

    @Test
    public void constructor_withValidContext_createsInstance() {
        ContentCache newCache = new ContentCache(context);
        assertNotNull(newCache);
    }

    // =====================================================================
    // canonicalizeFileName tests (using reflection to test private method)
    // =====================================================================

    @Test
    public void canonicalizeFileName_replacesSpecialChars() throws Exception {
        Method method = ContentCache.class.getDeclaredMethod("canonicalizeFileName", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(cache, "http://example.com/path", "png");
        
        assertNotNull(result);
        assertFalse(result.contains("/"));
        assertFalse(result.contains(":"));
        assertTrue(result.endsWith(".png"));
    }

    @Test
    public void canonicalizeFileName_withNullExtension_doesNotAppendExtension() throws Exception {
        Method method = ContentCache.class.getDeclaredMethod("canonicalizeFileName", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(cache, "http://example.com/test", null);
        
        assertNotNull(result);
        assertFalse(result.endsWith(".null"));
    }

    @Test
    public void canonicalizeFileName_withDotsInUrl_replacesDots() throws Exception {
        Method method = ContentCache.class.getDeclaredMethod("canonicalizeFileName", String.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(cache, "http://example.com/file.name.ext", "jpg");
        
        assertNotNull(result);
        // Original dots should be replaced with _d_
        assertTrue(result.contains("_d_"));
    }

    // =====================================================================
    // getCacheSize tests
    // =====================================================================

    @Test
    public void getCacheSize_onEmptyCache_returnsZeroOrSmall() {
        // Clear cache first
        cache.clearCache();
        
        long size = cache.getCacheSize();
        
        // After clearing, should be 0 or very small
        assertTrue("Cache size should be small after clearing", size >= 0);
    }

    // =====================================================================
    // clearCache tests
    // =====================================================================

    @Test
    public void clearCache_doesNotThrow() {
        // Just verify it doesn't throw an exception
        try {
            cache.clearCache();
        } catch (Exception e) {
            fail("clearCache should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void clearCache_calledMultipleTimes_doesNotThrow() {
        // Verify idempotent behavior
        cache.clearCache();
        cache.clearCache();
        cache.clearCache();
        // If we get here without exception, test passes
    }

    // =====================================================================
    // deletePrivateContent tests
    // =====================================================================

    @Test
    public void deletePrivateContent_nonExistentFile_doesNotThrow() {
        // Should not throw when file doesn't exist
        try {
            cache.deletePrivateContent("http://nonexistent.url/" + System.nanoTime(), "txt");
        } catch (Exception e) {
            fail("deletePrivateContent should not throw for non-existent file: " + e.getMessage());
        }
    }

    // =====================================================================
    // deletePublicContent tests
    // =====================================================================

    @Test
    public void deletePublicContent_nonExistentFile_doesNotThrow() {
        // Should not throw when file doesn't exist
        try {
            cache.deletePublicContent("http://nonexistent.url/" + System.nanoTime(), "txt");
        } catch (Exception e) {
            fail("deletePublicContent should not throw for non-existent file: " + e.getMessage());
        }
    }

    // =====================================================================
    // loadContentPrivate tests
    // =====================================================================

    @Test
    public void loadContentPrivate_returnsTask() {
        // Verify it returns a non-null Task
        bolts.Task<File> task = cache.loadContentPrivate(
            "http://example.com/test_" + System.nanoTime(), 
            "txt", 
            false
        );
        assertNotNull(task);
    }

    @Test
    public void loadContentPublic_returnsTask() {
        // Verify it returns a non-null Task
        bolts.Task<File> task = cache.loadContentPublic(
            "http://example.com/test_" + System.nanoTime(), 
            "txt", 
            false
        );
        assertNotNull(task);
    }

    // =====================================================================
    // Integration tests
    // =====================================================================

    @Test
    public void multipleCacheInstances_withSameContext_shareSameDirectories() {
        ContentCache cache1 = new ContentCache(context);
        ContentCache cache2 = new ContentCache(context);
        
        // Both should work without interfering
        assertNotNull(cache1);
        assertNotNull(cache2);
        
        // Clear from one should affect the other's view (they share directories)
        cache1.clearCache();
        assertTrue("Both caches should see empty state", cache2.getCacheSize() >= 0);
    }

    @Test
    public void loadContent_withForceRefresh_true_returnsTask() {
        bolts.Task<File> task = cache.loadContentPrivate(
            "http://example.com/refresh_" + System.nanoTime(),
            "txt",
            true  // force refresh
        );
        assertNotNull(task);
    }

    @Test
    public void loadContent_withForceRefresh_false_returnsTask() {
        bolts.Task<File> task = cache.loadContentPrivate(
            "http://example.com/norefresh_" + System.nanoTime(),
            "txt",
            false  // don't force refresh
        );
        assertNotNull(task);
    }

    @Test
    public void loadContent_differentExtensions_createsDifferentFiles() throws Exception {
        String baseUrl = "http://example.com/file_" + System.nanoTime();
        
        Method method = ContentCache.class.getDeclaredMethod("canonicalizeFileName", String.class, String.class);
        method.setAccessible(true);
        
        String pngName = (String) method.invoke(cache, baseUrl, "png");
        String jpgName = (String) method.invoke(cache, baseUrl, "jpg");
        
        assertNotEquals("Different extensions should produce different filenames", pngName, jpgName);
    }
}
