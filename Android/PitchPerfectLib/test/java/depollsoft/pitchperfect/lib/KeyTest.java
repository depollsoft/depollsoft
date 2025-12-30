package depollsoft.pitchperfect.lib;

import com.bindroid.trackable.TrackableCollection;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the Key class.
 * Tests musical key creation, major/minor keys, and key properties.
 */
public class KeyTest {

    static class DummyPlayer implements Note.NotePlayer {
        @Override
        public void play(Note n) { }
        @Override
        public void stop(Note n) { }
    }

    @Before
    public void setUp() {
        Note.setPlayer(new DummyPlayer());
    }

    // ==================== Original Tests (Enhanced) ====================

    @Test
    public void major_and_minor_keys_have_expected_count() {
        assertEquals(13, Key.getMajorKeys().size());
        assertEquals(13, Key.getMinorKeys().size());
    }

    @Test
    public void friendly_name_and_equality() {
        Key majorC = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        Key majorCAgain = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        assertEquals("C", majorC.getFriendlyName());
        assertTrue(majorC.equals(majorCAgain));

        Key minorA = new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Minor, 0);
        assertEquals("a", minorA.getFriendlyName());
        assertFalse(majorC.equals(minorA));
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testDefaultConstructor_createsKeyOfCMajor() {
        Key key = new Key();
        assertNotNull("Key should not be null", key);
        assertNotNull("Note should not be null", key.getNote());
        assertEquals(KeyType.Major, key.getKeyType());
        assertEquals(0, key.getNumAccidentals());
    }

    @Test
    public void testParameterizedConstructor() {
        Note gNote = Note.findNote("G", Accidental.Natural, 4);
        Key gMajor = new Key(gNote, KeyType.Major, 1);
        assertEquals("G", gMajor.getNote().getFriendlyName());
        assertEquals(KeyType.Major, gMajor.getKeyType());
        assertEquals(1, gMajor.getNumAccidentals());
    }

    @Test
    public void testConstructor_minorKey() {
        Note aNote = Note.findNote("A", Accidental.Natural, 4);
        Key aMinor = new Key(aNote, KeyType.Minor, 0);
        assertEquals(KeyType.Minor, aMinor.getKeyType());
        assertEquals("A", aMinor.getNote().getFriendlyName());
    }

    // ==================== Setters and Getters Tests ====================

    @Test
    public void testSetNote() {
        Key key = new Key();
        Note dNote = Note.findNote("D", Accidental.Natural, 4);
        key.setNote(dNote);
        assertEquals("D", key.getNote().getFriendlyName());
    }

    @Test
    public void testSetKeyType() {
        Key key = new Key();
        key.setKeyType(KeyType.Minor);
        assertEquals(KeyType.Minor, key.getKeyType());
    }

    @Test
    public void testSetNumAccidentals() {
        Key key = new Key();
        key.setNumAccidentals(3);
        assertEquals(3, key.getNumAccidentals());
    }

    @Test
    public void testSetNumAccidentals_negative() {
        Key key = new Key();
        key.setNumAccidentals(-4);
        assertEquals(-4, key.getNumAccidentals());
    }

    // ==================== getAccidental Tests ====================

    @Test
    public void testGetAccidental_natural() {
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key key = new Key(cNote, KeyType.Major, 0);
        assertEquals(Accidental.Natural, key.getAccidental());
    }

    @Test
    public void testGetAccidental_sharp() {
        Note fSharp = Note.findNote("F", Accidental.Sharp, 4);
        Key key = new Key(fSharp, KeyType.Major, 6);
        assertEquals(Accidental.Sharp, key.getAccidental());
    }

    @Test
    public void testGetAccidental_flat() {
        Note bFlat = Note.findNote("B", Accidental.Flat, 4);
        Key key = new Key(bFlat, KeyType.Major, -2);
        assertEquals(Accidental.Flat, key.getAccidental());
    }

    // ==================== getFriendlyName Tests ====================

    @Test
    public void testGetFriendlyName_majorKey_uppercase() {
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key cMajor = new Key(cNote, KeyType.Major, 0);
        assertEquals("C", cMajor.getFriendlyName());
    }

    @Test
    public void testGetFriendlyName_minorKey_lowercase() {
        Note aNote = Note.findNote("A", Accidental.Natural, 4);
        Key aMinor = new Key(aNote, KeyType.Minor, 0);
        assertEquals("a", aMinor.getFriendlyName());
    }

    @Test
    public void testGetFriendlyName_sharpMajorKey() {
        Note fSharp = Note.findNote("F", Accidental.Sharp, 4);
        Key fSharpMajor = new Key(fSharp, KeyType.Major, 6);
        // Note: Key.getFriendlyName returns just the note letter (uppercase for major)
        assertEquals("F", fSharpMajor.getFriendlyName());
    }

    @Test
    public void testGetFriendlyName_flatMinorKey() {
        Note eFlat = Note.findNote("E", Accidental.Flat, 4);
        Key eFlatMinor = new Key(eFlat, KeyType.Minor, -6);
        // Note: Key.getFriendlyName returns just the note letter (lowercase for minor)
        assertEquals("e", eFlatMinor.getFriendlyName());
    }

    // ==================== equals Tests ====================

    @Test
    public void testEquals_sameKey() {
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key key1 = new Key(cNote, KeyType.Major, 0);
        Key key2 = new Key(cNote, KeyType.Major, 0);
        assertTrue("Same keys should be equal", key1.equals(key2));
    }

    @Test
    public void testEquals_differentNote() {
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Note gNote = Note.findNote("G", Accidental.Natural, 4);
        Key cMajor = new Key(cNote, KeyType.Major, 0);
        Key gMajor = new Key(gNote, KeyType.Major, 1);
        assertFalse("Different notes should not be equal", cMajor.equals(gMajor));
    }

    @Test
    public void testEquals_differentKeyType() {
        Note aNote = Note.findNote("A", Accidental.Natural, 4);
        Key aMajor = new Key(aNote, KeyType.Major, 3);
        Key aMinor = new Key(aNote, KeyType.Minor, 0);
        assertFalse("Different key types should not be equal", aMajor.equals(aMinor));
    }

    @Test
    public void testEquals_withNull() {
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key key = new Key(cNote, KeyType.Major, 0);
        assertFalse("Key should not equal null", key.equals(null));
    }

    @Test
    public void testEquals_withDifferentType() {
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key key = new Key(cNote, KeyType.Major, 0);
        assertFalse("Key should not equal different type", key.equals("C Major"));
    }

    // ==================== getMajorKeys Tests ====================

    @Test
    public void testGetMajorKeys_notNull() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        assertNotNull("Major keys collection should not be null", majorKeys);
    }

    @Test
    public void testGetMajorKeys_containsCMajor() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        boolean found = false;
        for (Key k : majorKeys) {
            if (k.getNote().getFriendlyName().equals("C") &&
                    k.getNote().getAccidental() == Accidental.Natural &&
                    k.getKeyType() == KeyType.Major) {
                found = true;
                assertEquals("C Major should have 0 accidentals", 0, k.getNumAccidentals());
                break;
            }
        }
        assertTrue("Should contain C Major", found);
    }

    @Test
    public void testGetMajorKeys_containsGMajor() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        boolean found = false;
        for (Key k : majorKeys) {
            if (k.getNote().getFriendlyName().equals("G") &&
                    k.getNote().getAccidental() == Accidental.Natural) {
                found = true;
                assertEquals("G Major should have 1 sharp", 1, k.getNumAccidentals());
                break;
            }
        }
        assertTrue("Should contain G Major", found);
    }

    @Test
    public void testGetMajorKeys_containsFlatKeys() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        boolean hasFlat = false;
        for (Key k : majorKeys) {
            if (k.getAccidental() == Accidental.Flat) {
                hasFlat = true;
                assertTrue("Flat keys should have negative accidentals", k.getNumAccidentals() < 0);
            }
        }
        assertTrue("Should have flat keys", hasFlat);
    }

    @Test
    public void testGetMajorKeys_containsSharpKeys() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        boolean hasSharp = false;
        for (Key k : majorKeys) {
            if (k.getAccidental() == Accidental.Sharp) {
                hasSharp = true;
                assertTrue("Sharp keys should have positive accidentals", k.getNumAccidentals() > 0);
            }
        }
        assertTrue("Should have sharp keys", hasSharp);
    }

    @Test
    public void testGetMajorKeys_allAreMajorType() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        for (Key k : majorKeys) {
            assertEquals("All keys should be Major type", KeyType.Major, k.getKeyType());
        }
    }

    @Test
    public void testGetMajorKeys_sameInstanceReturned() {
        TrackableCollection<Key> first = Key.getMajorKeys();
        TrackableCollection<Key> second = Key.getMajorKeys();
        assertSame("Should return same instance", first, second);
    }

    // ==================== getMinorKeys Tests ====================

    @Test
    public void testGetMinorKeys_notNull() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        assertNotNull("Minor keys collection should not be null", minorKeys);
    }

    @Test
    public void testGetMinorKeys_containsAMinor() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        boolean found = false;
        for (Key k : minorKeys) {
            if (k.getNote().getFriendlyName().equals("A") &&
                    k.getNote().getAccidental() == Accidental.Natural &&
                    k.getKeyType() == KeyType.Minor) {
                found = true;
                assertEquals("A Minor should have 0 accidentals", 0, k.getNumAccidentals());
                break;
            }
        }
        assertTrue("Should contain A Minor", found);
    }

    @Test
    public void testGetMinorKeys_containsEMinor() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        boolean found = false;
        for (Key k : minorKeys) {
            if (k.getNote().getFriendlyName().equals("E") &&
                    k.getNote().getAccidental() == Accidental.Natural &&
                    k.getKeyType() == KeyType.Minor) {
                found = true;
                assertEquals("E Minor should have 1 sharp", 1, k.getNumAccidentals());
                break;
            }
        }
        assertTrue("Should contain E Minor", found);
    }

    @Test
    public void testGetMinorKeys_allAreMinorType() {
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();
        for (Key k : minorKeys) {
            assertEquals("All keys should be Minor type", KeyType.Minor, k.getKeyType());
        }
    }

    @Test
    public void testGetMinorKeys_sameInstanceReturned() {
        TrackableCollection<Key> first = Key.getMinorKeys();
        TrackableCollection<Key> second = Key.getMinorKeys();
        assertSame("Should return same instance", first, second);
    }

    // ==================== Circle of Fifths Tests ====================

    @Test
    public void testCircleOfFifths_majorAndMinorParallel() {
        // C Major and A Minor are relative keys (same key signature)
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        TrackableCollection<Key> minorKeys = Key.getMinorKeys();

        Key cMajor = null;
        Key aMinor = null;

        for (Key k : majorKeys) {
            if (k.getNote().getFriendlyName().equals("C") &&
                    k.getNote().getAccidental() == Accidental.Natural) {
                cMajor = k;
                break;
            }
        }

        for (Key k : minorKeys) {
            if (k.getNote().getFriendlyName().equals("A") &&
                    k.getNote().getAccidental() == Accidental.Natural) {
                aMinor = k;
                break;
            }
        }

        assertNotNull("C Major should exist", cMajor);
        assertNotNull("A Minor should exist", aMinor);
        assertEquals("C Major and A Minor should have same accidentals count",
                cMajor.getNumAccidentals(), aMinor.getNumAccidentals());
    }

    @Test
    public void testCircleOfFifths_accidentalsRange() {
        TrackableCollection<Key> majorKeys = Key.getMajorKeys();
        for (Key k : majorKeys) {
            assertTrue("Accidentals should be between -6 and 6",
                    k.getNumAccidentals() >= -6 && k.getNumAccidentals() <= 6);
        }
    }
}

