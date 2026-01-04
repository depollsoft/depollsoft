package depollsoft.pitchperfect;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.bindroid.trackable.TrackableCollection;

import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Key;
import depollsoft.pitchperfect.lib.KeyType;
import depollsoft.pitchperfect.lib.Note;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class KeySignatureModelTest {

    @Before
    public void setUp() {
        // Ensure common notes are initialized before tests
        Note.getCommonNotes();
    }

    // ========== Key Signature Mode Switching Tests ==========

    @Test
    public void constructor_defaultsToMajorMode() {
        KeySignatureModel model = new KeySignatureModel();
        assertTrue(model.getIsMajor());
    }

    @Test
    public void setIsMajor_true_switchesToMajorMode() {
        KeySignatureModel model = new KeySignatureModel();
        model.setIsMajor(false); // First set to minor
        model.setIsMajor(true);  // Then switch back to major
        
        assertTrue(model.getIsMajor());
    }

    @Test
    public void setIsMajor_false_switchesToMinorMode() {
        KeySignatureModel model = new KeySignatureModel();
        model.setIsMajor(false);
        
        assertFalse(model.getIsMajor());
    }

    @Test
    public void getIsMajor_reflectsCurrentMode() {
        KeySignatureModel model = new KeySignatureModel();
        
        assertTrue(model.getIsMajor());
        
        model.setIsMajor(false);
        assertFalse(model.getIsMajor());
        
        model.setIsMajor(true);
        assertTrue(model.getIsMajor());
    }

    @Test
    public void modeSwitching_canToggleMultipleTimes() {
        KeySignatureModel model = new KeySignatureModel();
        
        for (int i = 0; i < 5; i++) {
            model.setIsMajor(false);
            assertFalse("Toggle " + i + " to minor failed", model.getIsMajor());
            
            model.setIsMajor(true);
            assertTrue("Toggle " + i + " to major failed", model.getIsMajor());
        }
    }

    // ========== Major Keys Collection Tests ==========

    @Test
    public void constructor_initializesMajorKeys() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> majorKeys = model.getMajorKeys();
        
        assertNotNull(majorKeys);
        assertEquals(13, majorKeys.size()); // 13 major keys (circle of fifths + enharmonic)
    }

    @Test
    public void majorKeys_containsAllExpectedKeys() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> majorKeys = model.getMajorKeys();
        
        // Verify some expected major keys exist
        assertTrue(containsKeyWithNote(majorKeys, "C", Accidental.Natural, KeyType.Major));
        assertTrue(containsKeyWithNote(majorKeys, "G", Accidental.Natural, KeyType.Major));
        assertTrue(containsKeyWithNote(majorKeys, "D", Accidental.Natural, KeyType.Major));
        assertTrue(containsKeyWithNote(majorKeys, "F", Accidental.Natural, KeyType.Major));
        assertTrue(containsKeyWithNote(majorKeys, "B", Accidental.Flat, KeyType.Major));
    }

    @Test
    public void majorKeys_containsCMajorWithZeroAccidentals() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> majorKeys = model.getMajorKeys();
        
        Key cMajor = findKeyWithNote(majorKeys, "C", Accidental.Natural);
        assertNotNull("C Major should exist", cMajor);
        assertEquals(KeyType.Major, cMajor.getKeyType());
        assertEquals(0, cMajor.getNumAccidentals());
    }

    @Test
    public void majorKeys_hasCorrectAccidentalCounts() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> majorKeys = model.getMajorKeys();
        
        // G Major has 1 sharp
        Key gMajor = findKeyWithNote(majorKeys, "G", Accidental.Natural);
        assertNotNull(gMajor);
        assertEquals(1, gMajor.getNumAccidentals());
        
        // F Major has 1 flat
        Key fMajor = findKeyWithNote(majorKeys, "F", Accidental.Natural);
        assertNotNull(fMajor);
        assertEquals(-1, fMajor.getNumAccidentals());
    }

    @Test
    public void majorKeys_allHaveMajorKeyType() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> majorKeys = model.getMajorKeys();
        
        for (int i = 0; i < majorKeys.size(); i++) {
            assertEquals("Key at index " + i + " should be Major", 
                KeyType.Major, majorKeys.get(i).getKeyType());
        }
    }

    // ========== Minor Keys Collection Tests ==========

    @Test
    public void constructor_initializesMinorKeys() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> minorKeys = model.getMinorKeys();
        
        assertNotNull(minorKeys);
        assertEquals(13, minorKeys.size()); // 13 minor keys (relative to major keys)
    }

    @Test
    public void minorKeys_containsAllExpectedKeys() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> minorKeys = model.getMinorKeys();
        
        // Verify some expected minor keys exist
        assertTrue(containsKeyWithNote(minorKeys, "A", Accidental.Natural, KeyType.Minor));
        assertTrue(containsKeyWithNote(minorKeys, "E", Accidental.Natural, KeyType.Minor));
        assertTrue(containsKeyWithNote(minorKeys, "D", Accidental.Natural, KeyType.Minor));
    }

    @Test
    public void minorKeys_containsAMinorWithZeroAccidentals() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> minorKeys = model.getMinorKeys();
        
        Key aMinor = findKeyWithNote(minorKeys, "A", Accidental.Natural);
        assertNotNull("A Minor should exist", aMinor);
        assertEquals(KeyType.Minor, aMinor.getKeyType());
        assertEquals(0, aMinor.getNumAccidentals());
    }

    @Test
    public void minorKeys_hasCorrectAccidentalCounts() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> minorKeys = model.getMinorKeys();
        
        // E Minor has 1 sharp
        Key eMinor = findKeyWithNote(minorKeys, "E", Accidental.Natural);
        assertNotNull(eMinor);
        assertEquals(1, eMinor.getNumAccidentals());
        
        // D Minor has 1 flat
        Key dMinor = findKeyWithNote(minorKeys, "D", Accidental.Natural);
        assertNotNull(dMinor);
        assertEquals(-1, dMinor.getNumAccidentals());
    }

    @Test
    public void minorKeys_allHaveMinorKeyType() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> minorKeys = model.getMinorKeys();
        
        for (int i = 0; i < minorKeys.size(); i++) {
            assertEquals("Key at index " + i + " should be Minor", 
                KeyType.Minor, minorKeys.get(i).getKeyType());
        }
    }

    // ========== Setter Tests ==========

    @Test
    public void setMajorKeys_updatesCollection() {
        KeySignatureModel model = new KeySignatureModel();
        
        TrackableCollection<Key> customKeys = new TrackableCollection<>();
        customKeys.add(new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0));
        
        model.setMajorKeys(customKeys);
        
        assertEquals(1, model.getMajorKeys().size());
    }

    @Test
    public void setMinorKeys_updatesCollection() {
        KeySignatureModel model = new KeySignatureModel();
        
        TrackableCollection<Key> customKeys = new TrackableCollection<>();
        customKeys.add(new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Minor, 0));
        
        model.setMinorKeys(customKeys);
        
        assertEquals(1, model.getMinorKeys().size());
    }

    // ========== Relationship Tests ==========

    @Test
    public void majorAndMinorKeys_areIndependent() {
        KeySignatureModel model = new KeySignatureModel();
        
        TrackableCollection<Key> majorKeys = model.getMajorKeys();
        TrackableCollection<Key> minorKeys = model.getMinorKeys();
        
        assertNotSame(majorKeys, minorKeys);
    }

    @Test
    public void majorAndMinorKeys_haveSameSize() {
        KeySignatureModel model = new KeySignatureModel();
        
        assertEquals(model.getMajorKeys().size(), model.getMinorKeys().size());
    }

    @Test
    public void relativeMinors_haveMatchingAccidentals() {
        KeySignatureModel model = new KeySignatureModel();
        TrackableCollection<Key> majorKeys = model.getMajorKeys();
        TrackableCollection<Key> minorKeys = model.getMinorKeys();
        
        // C Major (0 accidentals) relative minor is A minor (0 accidentals)
        Key cMajor = findKeyWithNote(majorKeys, "C", Accidental.Natural);
        Key aMinor = findKeyWithNote(minorKeys, "A", Accidental.Natural);
        
        assertNotNull(cMajor);
        assertNotNull(aMinor);
        assertEquals(cMajor.getNumAccidentals(), aMinor.getNumAccidentals());
    }

    @Test
    public void keySignatureModel_preservesStateAfterMultipleAccesses() {
        KeySignatureModel model = new KeySignatureModel();
        
        // Access multiple times
        TrackableCollection<Key> majorKeys1 = model.getMajorKeys();
        TrackableCollection<Key> majorKeys2 = model.getMajorKeys();
        TrackableCollection<Key> minorKeys1 = model.getMinorKeys();
        TrackableCollection<Key> minorKeys2 = model.getMinorKeys();
        
        // Should return same instances
        assertSame(majorKeys1, majorKeys2);
        assertSame(minorKeys1, minorKeys2);
    }

    // ========== Helper Methods ==========

    private boolean containsKeyWithNote(TrackableCollection<Key> keys, String noteName, 
            Accidental accidental, KeyType keyType) {
        for (int i = 0; i < keys.size(); i++) {
            Key key = keys.get(i);
            if (key.getNote().getFriendlyName().equals(noteName) 
                && key.getNote().getAccidental() == accidental
                && key.getKeyType() == keyType) {
                return true;
            }
        }
        return false;
    }

    private Key findKeyWithNote(TrackableCollection<Key> keys, String noteName, Accidental accidental) {
        for (int i = 0; i < keys.size(); i++) {
            Key key = keys.get(i);
            if (key.getNote().getFriendlyName().equals(noteName) 
                && key.getNote().getAccidental() == accidental) {
                return key;
            }
        }
        return null;
    }
}
