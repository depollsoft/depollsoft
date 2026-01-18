package depollsoft.tagmaster;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.util.Preferences;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RatingsModelTest {

    private static final String RATED_IDS_KEY = "tagmaster.RatedIds";
    private List<Integer> mockRatedIds;

    @Before
    public void setUp() throws Exception {
        // Initialize RichApplication context for Preferences
        Context app = RuntimeEnvironment.getApplication();
        Field contextField = RichApplication.class.getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(null, app);
        
        mockRatedIds = new ArrayList<>();
    }

    @Test
    public void addRating_newId_addsIdToList() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            // Reinitialize the static state
            resetRatingsModelState(ratedIds);
            
            RatingsModel.addRating(42);
            
            assertTrue(ratedIds.contains(42));
            prefsMock.verify(() -> Preferences.set(eq(RATED_IDS_KEY), any()));
        }
    }

    @Test
    public void addRating_multipleIds_addsAllIds() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            RatingsModel.addRating(1);
            RatingsModel.addRating(2);
            RatingsModel.addRating(3);
            
            assertEquals(3, ratedIds.size());
            assertTrue(ratedIds.contains(1));
            assertTrue(ratedIds.contains(2));
            assertTrue(ratedIds.contains(3));
        }
    }

    @Test
    public void isRated_existingId_returnsTrue() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            ratedIds.add(42);
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            assertTrue(RatingsModel.isRated(42));
        }
    }

    @Test
    public void isRated_nonExistingId_returnsFalse() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            assertFalse(RatingsModel.isRated(999));
        }
    }

    @Test
    public void isRated_afterAddRating_returnsTrue() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            assertFalse(RatingsModel.isRated(100));
            RatingsModel.addRating(100);
            assertTrue(RatingsModel.isRated(100));
        }
    }

    @Test
    public void removeRating_existingId_removesIdFromList() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            ratedIds.add(42);
            ratedIds.add(100);
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            RatingsModel.removeRating(42);
            
            assertFalse(ratedIds.contains(42));
            assertTrue(ratedIds.contains(100));
            prefsMock.verify(() -> Preferences.set(eq(RATED_IDS_KEY), any()));
        }
    }

    @Test
    public void removeRating_nonExistingId_noEffect() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            ratedIds.add(42);
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            int sizeBefore = ratedIds.size();
            
            RatingsModel.removeRating(999);
            
            assertEquals(sizeBefore, ratedIds.size());
        }
    }

    @Test
    public void isRated_afterRemoveRating_returnsFalse() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            ratedIds.add(42);
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            assertTrue(RatingsModel.isRated(42));
            RatingsModel.removeRating(42);
            assertFalse(RatingsModel.isRated(42));
        }
    }

    @Test
    public void addRating_zeroId_addsZeroToList() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            RatingsModel.addRating(0);
            
            assertTrue(ratedIds.contains(0));
        }
    }

    @Test
    public void addRating_negativeId_addsNegativeIdToList() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            RatingsModel.addRating(-1);
            
            assertTrue(ratedIds.contains(-1));
        }
    }

    @Test
    public void addRating_duplicateId_addsDuplicateToList() {
        try (MockedStatic<Preferences> prefsMock = Mockito.mockStatic(Preferences.class)) {
            List<Integer> ratedIds = new ArrayList<>();
            prefsMock.when(() -> Preferences.<List<Integer>>get(RATED_IDS_KEY)).thenReturn(ratedIds);
            
            resetRatingsModelState(ratedIds);
            
            RatingsModel.addRating(42);
            RatingsModel.addRating(42);
            
            // The model allows duplicate ratings
            assertEquals(2, ratedIds.stream().filter(id -> id == 42).count());
        }
    }

    /**
     * Helper method to reset the static ratedIds field via reflection.
     * This is necessary because RatingsModel uses static state.
     */
    private void resetRatingsModelState(List<Integer> newList) {
        try {
            java.lang.reflect.Field field = RatingsModel.class.getDeclaredField("ratedIds");
            field.setAccessible(true);
            field.set(null, newList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset RatingsModel state", e);
        }
    }
}
