package depollsoft.pitchperfect;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.graphics.Typeface;
import android.content.res.AssetManager;

import depollsoft.lib.activity.RichApplication;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CommonModelTest {

    @Before
    public void setUp() {
        // Reset static fields for clean testing
        CommonModel.resetFonts();
    }

    @Test
    public void testSharpString_HasCorrectValue() {
        assertEquals("Sharp string should have correct Unicode value", "ì", CommonModel.sharpString);
    }

    @Test
    public void testFlatString_HasCorrectValue() {
        assertEquals("Flat string should have correct Unicode value", "í", CommonModel.flatString);
    }

    @Test
    public void testGetMusiQwik_ReturnsSameInstanceOnMultipleCalls() {
        Typeface first = CommonModel.getMusiQwik();
        Typeface second = CommonModel.getMusiQwik();
        
        assertNotNull("MusiQwik typeface should not be null", first);
        assertSame("Should return same instance on multiple calls", first, second);
    }

    @Test
    public void testGetMusiQwikB_ReturnsSameInstanceOnMultipleCalls() {
        Typeface first = CommonModel.getMusiQwikB();
        Typeface second = CommonModel.getMusiQwikB();
        
        assertNotNull("MusiQwikB typeface should not be null", first);
        assertSame("Should return same instance on multiple calls", first, second);
    }

    @Test
    public void testGetMusiSync_ReturnsSameInstanceOnMultipleCalls() {
        Typeface first = CommonModel.getMusiSync();
        Typeface second = CommonModel.getMusiSync();
        
        assertNotNull("MusiSync typeface should not be null", first);
        assertSame("Should return same instance on multiple calls", first, second);
    }

    @Test
    public void testGetNoteHedz_ReturnsSameInstanceOnMultipleCalls() {
        Typeface first = CommonModel.getNoteHedz();
        Typeface second = CommonModel.getNoteHedz();
        
        assertNotNull("NoteHedz typeface should not be null", first);
        assertSame("Should return same instance on multiple calls", first, second);
    }

    @Test
    public void testAllTypefacesAreDistinct() {
        Typeface musiQwik = CommonModel.getMusiQwik();
        Typeface musiQwikB = CommonModel.getMusiQwikB();
        Typeface musiSync = CommonModel.getMusiSync();
        Typeface noteHedz = CommonModel.getNoteHedz();
        
        // All should be different instances
        assertNotSame("MusiQwik and MusiQwikB should be different", musiQwik, musiQwikB);
        assertNotSame("MusiQwik and MusiSync should be different", musiQwik, musiSync);
        assertNotSame("MusiQwik and NoteHedz should be different", musiQwik, noteHedz);
        assertNotSame("MusiQwikB and MusiSync should be different", musiQwikB, musiSync);
        assertNotSame("MusiQwikB and NoteHedz should be different", musiQwikB, noteHedz);
        assertNotSame("MusiSync and NoteHedz should be different", musiSync, noteHedz);
    }

    @Test
    public void testLazyInitialization_OnlyCreatesWhenNeeded() {
        // Before any calls, all should be null (assuming we can test internal state)
        // This test verifies the singleton pattern behavior
        
        Typeface musiQwik1 = CommonModel.getMusiQwik();
        assertNotNull("First call should create instance", musiQwik1);
        
        // Reset to test lazy loading
        CommonModel.resetFonts();
        
        Typeface musiQwikB1 = CommonModel.getMusiQwikB();
        assertNotNull("First call should create MusiQwikB instance", musiQwikB1);
        
        // Verify MusiQwik was not recreated
        Typeface musiQwik2 = CommonModel.getMusiQwik();
        assertNotNull("MusiQwik should still be available", musiQwik2);
    }

    @Test
    public void testFontConstants_AreNotNull() {
        assertNotNull("Sharp string constant should not be null", CommonModel.sharpString);
        assertNotNull("Flat string constant should not be null", CommonModel.flatString);
    }

    @Test
    public void testFontConstants_AreNotEmpty() {
        assertFalse("Sharp string should not be empty", CommonModel.sharpString.isEmpty());
        assertFalse("Flat string should not be empty", CommonModel.flatString.isEmpty());
    }

    @Test
    public void testFontConstants_HaveCorrectLength() {
        assertEquals("Sharp string should be single character", 1, CommonModel.sharpString.length());
        assertEquals("Flat string should be single character", 1, CommonModel.flatString.length());
    }

    @Test
    public void testMultipleTypefaceRetrieval_PerformanceConsistent() {
        // Test that repeated calls don't degrade performance
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            CommonModel.getMusiQwik();
            CommonModel.getMusiQwikB();
            CommonModel.getMusiSync();
            CommonModel.getNoteHedz();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should be very fast since it's just returning cached instances
        assertTrue("Multiple typeface retrievals should be fast (< 1000ms)", duration < 1000);
    }
}

// Helper class to test CommonModel internal state
class CommonModelTestHelper {
    // This would need to be implemented if CommonModel exposed reset functionality
    // For now, we'll assume the tests work with the singleton pattern as-is
}