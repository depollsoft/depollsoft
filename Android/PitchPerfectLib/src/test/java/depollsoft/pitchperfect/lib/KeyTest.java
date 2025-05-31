package depollsoft.pitchperfect.lib;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.bindroid.trackable.TrackableCollection;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class KeyTest {

    @Before
    public void setUp() {
        // Reset static collections for clean testing
        Key.resetKeys();
    }

    @Test
    public void testGetMajorKeys_ReturnsCorrectCount() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        
        assertNotNull("Major keys should not be null", majorKeys);
        assertEquals("Should have 13 major keys", 13, majorKeys.size());
    }

    @Test
    public void testGetMinorKeys_ReturnsCorrectCount() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        
        assertNotNull("Minor keys should not be null", minorKeys);
        assertEquals("Should have 13 minor keys", 13, minorKeys.size());
    }

    @Test
    public void testMajorKeys_ContainsCMajor() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        
        Key cMajor = null;
        for (Key key : majorKeys) {
            if (key.getKeySignature() == 0 && key.getKeyType() == KeyType.Major) {
                cMajor = key;
                break;
            }
        }
        
        assertNotNull("Should contain C Major key", cMajor);
        assertEquals("C Major should have 0 key signature", 0, cMajor.getKeySignature());
        assertEquals("Should be Major type", KeyType.Major, cMajor.getKeyType());
    }

    @Test
    public void testMinorKeys_ContainsAMinor() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        
        Key aMinor = null;
        for (Key key : minorKeys) {
            if (key.getKeySignature() == 0 && key.getKeyType() == KeyType.Minor) {
                aMinor = key;
                break;
            }
        }
        
        assertNotNull("Should contain A Minor key", aMinor);
        assertEquals("A Minor should have 0 key signature", 0, aMinor.getKeySignature());
        assertEquals("Should be Minor type", KeyType.Minor, aMinor.getKeyType());
    }

    @Test
    public void testMajorKeys_KeySignatureRange() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        
        boolean hasNegative6 = false;
        boolean hasPositive6 = false;
        
        for (Key key : majorKeys) {
            int keySignature = key.getKeySignature();
            assertTrue("Key signature should be between -6 and 6", 
                      keySignature >= -6 && keySignature <= 6);
            
            if (keySignature == -6) hasNegative6 = true;
            if (keySignature == 6) hasPositive6 = true;
        }
        
        assertTrue("Should have key with -6 signature", hasNegative6);
        assertTrue("Should have key with +6 signature", hasPositive6);
    }

    @Test
    public void testMinorKeys_KeySignatureRange() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        
        boolean hasNegative6 = false;
        boolean hasPositive6 = false;
        
        for (Key key : minorKeys) {
            int keySignature = key.getKeySignature();
            assertTrue("Key signature should be between -6 and 6", 
                      keySignature >= -6 && keySignature <= 6);
            
            if (keySignature == -6) hasNegative6 = true;
            if (keySignature == 6) hasPositive6 = true;
        }
        
        assertTrue("Should have key with -6 signature", hasNegative6);
        assertTrue("Should have key with +6 signature", hasPositive6);
    }

    @Test
    public void testGetMajorKeys_Singleton_ReturnsSameInstance() {
        TrackableCollection<Key> first = Key.getMajorKeys();
        TrackableCollection<Key> second = Key.getMajorKeys();
        
        assertSame("Should return same instance on multiple calls", first, second);
    }

    @Test
    public void testGetMinorKeys_Singleton_ReturnsSameInstance() {
        TrackableCollection<Key> first = Key.getMinorKeys();
        TrackableCollection<Key> second = Key.getMinorKeys();
        
        assertSame("Should return same instance on multiple calls", first, second);
    }

    @Test
    public void testMajorKeys_AllHaveMajorType() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        
        for (Key key : majorKeys) {
            assertEquals("All major keys should have Major type", KeyType.Major, key.getKeyType());
        }
    }

    @Test
    public void testMinorKeys_AllHaveMinorType() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        
        for (Key key : minorKeys) {
            assertEquals("All minor keys should have Minor type", KeyType.Minor, key.getKeyType());
        }
    }

    @Test
    public void testMajorKeys_UniqueKeySignatures() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        
        for (int i = 0; i < majorKeys.size(); i++) {
            for (int j = i + 1; j < majorKeys.size(); j++) {
                assertNotEquals("Each major key should have unique key signature",
                               majorKeys.get(i).getKeySignature(),
                               majorKeys.get(j).getKeySignature());
            }
        }
    }

    @Test
    public void testMinorKeys_UniqueKeySignatures() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        
        for (int i = 0; i < minorKeys.size(); i++) {
            for (int j = i + 1; j < minorKeys.size(); j++) {
                assertNotEquals("Each minor key should have unique key signature",
                               minorKeys.get(i).getKeySignature(),
                               minorKeys.get(j).getKeySignature());
            }
        }
    }

    @Test
    public void testKey_Constructor() {
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key cMajor = new Key(cNote, KeyType.Major, 0);
        
        assertNotNull("Key should not be null", cMajor);
        assertEquals("Key signature should be 0", 0, cMajor.getKeySignature());
        assertEquals("Key type should be Major", KeyType.Major, cMajor.getKeyType());
        assertEquals("Note should match", cNote, cMajor.getNote());
    }

    @Test
    public void testKey_ConstructorWithNegativeSignature() {
        Note fNote = Note.findNote("F", Accidental.Natural, 4);
        Key fMajor = new Key(fNote, KeyType.Major, -1);
        
        assertEquals("Key signature should be -1", -1, fMajor.getKeySignature());
        assertEquals("Key type should be Major", KeyType.Major, fMajor.getKeyType());
    }

    @Test
    public void testKey_ConstructorWithPositiveSignature() {
        Note gNote = Note.findNote("G", Accidental.Natural, 4);
        Key gMajor = new Key(gNote, KeyType.Major, 1);
        
        assertEquals("Key signature should be 1", 1, gMajor.getKeySignature());
        assertEquals("Key type should be Major", KeyType.Major, gMajor.getKeyType());
    }

    @Test
    public void testMajorKeys_ContainsExpectedFlats() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        
        // Check for some expected flat keys
        boolean hasFMajor = false;
        boolean hasBflatMajor = false;
        
        for (Key key : majorKeys) {
            Note note = key.getNote();
            if (note.noteName.equals("F") && note.accidental == Accidental.Natural && key.getKeySignature() == -1) {
                hasFMajor = true;
            }
            if (note.noteName.equals("B") && note.accidental == Accidental.Flat && key.getKeySignature() == -2) {
                hasBflatMajor = true;
            }
        }
        
        assertTrue("Should contain F Major", hasFMajor);
        assertTrue("Should contain Bb Major", hasBflatMajor);
    }

    @Test
    public void testMajorKeys_ContainsExpectedSharps() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        
        // Check for some expected sharp keys
        boolean hasGMajor = false;
        boolean hasFsharpMajor = false;
        
        for (Key key : majorKeys) {
            Note note = key.getNote();
            if (note.noteName.equals("G") && note.accidental == Accidental.Natural && key.getKeySignature() == 1) {
                hasGMajor = true;
            }
            if (note.noteName.equals("F") && note.accidental == Accidental.Sharp && key.getKeySignature() == 6) {
                hasFsharpMajor = true;
            }
        }
        
        assertTrue("Should contain G Major", hasGMajor);
        assertTrue("Should contain F# Major", hasFsharpMajor);
    }
}