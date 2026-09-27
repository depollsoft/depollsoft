package depollsoft.pitchperfect.lib;

import depollsoft.lib.state.StateList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for music theory concepts across multiple classes.
 * Tests the interaction between Note, Key, Accidental, KeyType, and PitchedSong.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MusicTheoryIntegrationTest {

    // Disable audio playback for testing
    private static final Note.NotePlayer MOCK_PLAYER = new Note.NotePlayer() {
        @Override
        public void play(Note n) {
            // No-op for testing
        }

        @Override
        public void stop(Note n) {
            // No-op for testing
        }
    };

    @Before
    public void setUp() {
        Note.setPlayer(MOCK_PLAYER);
    }

    // ==================== Circle of Fifths Tests ====================

    @Test
    public void testCircleOfFifths_majorKeysCorrectOrder() {
        StateList<Key> majorKeys = Key.getMajorKeys();
        
        // Expected order by accidentals: -6 (Gb) to +6 (F#)
        int prevAccidentals = Integer.MIN_VALUE;
        for (Key k : majorKeys) {
            assertTrue("Keys should be in order of accidentals",
                    k.getNumAccidentals() >= prevAccidentals);
            prevAccidentals = k.getNumAccidentals();
        }
    }

    @Test
    public void testCircleOfFifths_minorKeysCorrectOrder() {
        StateList<Key> minorKeys = Key.getMinorKeys();
        
        int prevAccidentals = Integer.MIN_VALUE;
        for (Key k : minorKeys) {
            assertTrue("Keys should be in order of accidentals",
                    k.getNumAccidentals() >= prevAccidentals);
            prevAccidentals = k.getNumAccidentals();
        }
    }

    @Test
    public void testRelativeKeys_CMajorAndAMinor() {
        // C Major and A Minor share the same key signature (no sharps/flats)
        Key cMajor = findMajorKey("C", Accidental.Natural);
        Key aMinor = findMinorKey("A", Accidental.Natural);
        
        assertNotNull("C Major should exist", cMajor);
        assertNotNull("A Minor should exist", aMinor);
        assertEquals("C Major and A Minor should have same number of accidentals",
                cMajor.getNumAccidentals(), aMinor.getNumAccidentals());
    }

    @Test
    public void testRelativeKeys_GMajorAndEMinor() {
        // G Major and E Minor share the same key signature (1 sharp)
        Key gMajor = findMajorKey("G", Accidental.Natural);
        Key eMinor = findMinorKey("E", Accidental.Natural);
        
        assertNotNull("G Major should exist", gMajor);
        assertNotNull("E Minor should exist", eMinor);
        assertEquals("G Major and E Minor should have same number of accidentals",
                gMajor.getNumAccidentals(), eMinor.getNumAccidentals());
    }

    @Test
    public void testRelativeKeys_FMajorAndDMinor() {
        // F Major and D Minor share the same key signature (1 flat)
        Key fMajor = findMajorKey("F", Accidental.Natural);
        Key dMinor = findMinorKey("D", Accidental.Natural);
        
        assertNotNull("F Major should exist", fMajor);
        assertNotNull("D Minor should exist", dMinor);
        assertEquals("F Major and D Minor should have same number of accidentals",
                fMajor.getNumAccidentals(), dMinor.getNumAccidentals());
    }

    // ==================== Enharmonic Equivalents Tests ====================

    @Test
    public void testEnharmonicEquivalents_CSharpAndDFlat() {
        Note cSharp = Note.findNote("C", Accidental.Sharp, 4);
        Note dFlat = Note.findNote("D", Accidental.Flat, 4);
        
        assertNotNull("C# should exist", cSharp);
        assertNotNull("Db should exist", dFlat);
        assertEquals("C# and Db should have same frequency",
                cSharp.getFrequency(), dFlat.getFrequency(), 0.001);
    }

    @Test
    public void testEnharmonicEquivalents_FSharpAndGFlat() {
        Note fSharp = Note.findNote("F", Accidental.Sharp, 4);
        Note gFlat = Note.findNote("G", Accidental.Flat, 4);
        
        assertNotNull("F# should exist", fSharp);
        assertNotNull("Gb should exist", gFlat);
        assertEquals("F# and Gb should have same frequency",
                fSharp.getFrequency(), gFlat.getFrequency(), 0.001);
    }

    @Test
    public void testEnharmonicEquivalents_inPrunedNotes() {
        List<Note> pruned = Note.getPrunedNotes();
        
        // Find C# or Db in pruned list
        Note enharmonicNote = null;
        for (Note n : pruned) {
            if (n.getOctave() == 4 && 
                ((n.getFriendlyName().equals("C") && n.getAccidental() == Accidental.Sharp) ||
                 (n.getFriendlyName().equals("D") && n.getAccidental() == Accidental.Flat))) {
                enharmonicNote = n;
                break;
            }
        }
        
        assertNotNull("Should find one enharmonic note (C# or Db)", enharmonicNote);
        
        // If it has an alternate, that's the enharmonic equivalent
        if (enharmonicNote.getAlternate() != null) {
            assertEquals("Enharmonic equivalents should have same frequency",
                    enharmonicNote.getFrequency(), 
                    enharmonicNote.getAlternate().getFrequency(), 0.001);
        }
    }

    // ==================== Octave Relationships Tests ====================

    @Test
    public void testOctaveRelationship_frequencyDoubles() {
        Note a4 = Note.findNote("A", Accidental.Natural, 4);
        Note a5 = Note.findNote("A", Accidental.Natural, 5);
        
        assertNotNull("A4 should exist", a4);
        assertNotNull("A5 should exist", a5);
        assertEquals("A5 should be double A4 frequency",
                a4.getFrequency() * 2, a5.getFrequency(), 0.1);
    }

    @Test
    public void testOctaveRelationship_frequencyHalves() {
        Note c4 = Note.findNote("C", Accidental.Natural, 4);
        Note c3 = Note.findNote("C", Accidental.Natural, 3);
        
        assertNotNull("C4 should exist", c4);
        assertNotNull("C3 should exist", c3);
        assertEquals("C3 should be half C4 frequency",
                c4.getFrequency() / 2, c3.getFrequency(), 0.1);
    }

    @Test
    public void testOctaveRelationship_twoOctavesIsQuadruple() {
        Note c2 = Note.findNote("C", Accidental.Natural, 2);
        Note c4 = Note.findNote("C", Accidental.Natural, 4);
        
        assertNotNull("C2 should exist", c2);
        assertNotNull("C4 should exist", c4);
        assertEquals("C4 should be 4x C2 frequency",
                c2.getFrequency() * 4, c4.getFrequency(), 0.1);
    }

    // ==================== Note Frequency Tests ====================

    @Test
    public void testA440Standard() {
        Note a4 = Note.findNote("A", Accidental.Natural, 4);
        assertNotNull("A4 should exist", a4);
        assertEquals("A4 should be 440 Hz (standard tuning)", 440.0, a4.getFrequency(), 1.0);
    }

    @Test
    public void testMiddleC_frequency() {
        Note c4 = Note.getC4();
        assertNotNull("Middle C (C4) should exist", c4);
        // C4 is approximately 261.63 Hz
        assertEquals("Middle C should be approximately 261.63 Hz", 261.63, c4.getFrequency(), 1.0);
    }

    // ==================== Song and Key Integration Tests ====================

    @Test
    public void testPitchedSong_withAllMajorKeys() {
        StateList<Key> majorKeys = Key.getMajorKeys();
        
        for (Key k : majorKeys) {
            PitchedSong song = new PitchedSong();
            song.setName("Song in " + k.getFriendlyName() + " Major");
            song.setKey(k);
            
            assertNotNull("Song should have key", song.getKey());
            assertEquals("Song key should match", k, song.getKey());
            assertEquals(KeyType.Major, song.getKey().getKeyType());
        }
    }

    @Test
    public void testPitchedSong_withAllMinorKeys() {
        StateList<Key> minorKeys = Key.getMinorKeys();
        
        for (Key k : minorKeys) {
            PitchedSong song = new PitchedSong();
            song.setName("Song in " + k.getFriendlyName() + " Minor");
            song.setKey(k);
            
            assertNotNull("Song should have key", song.getKey());
            assertEquals("Song key should match", k, song.getKey());
            assertEquals(KeyType.Minor, song.getKey().getKeyType());
        }
    }

    @Test
    public void testSongSorting_alphabetical() {
        PitchedSong[] songs = new PitchedSong[3];
        songs[0] = new PitchedSong();
        songs[0].setName("Zebra Song");
        songs[1] = new PitchedSong();
        songs[1].setName("Apple Song");
        songs[2] = new PitchedSong();
        songs[2].setName("Mango Song");
        
        java.util.Arrays.sort(songs);
        
        assertEquals("Apple Song", songs[0].getName());
        assertEquals("Mango Song", songs[1].getName());
        assertEquals("Zebra Song", songs[2].getName());
    }

    // ==================== Key Signature Tests ====================

    @Test
    public void testKeySignatures_sharpKeyNotes() {
        // Sharp keys: G, D, A, E, B, F#
        String[] sharpKeyNotes = {"G", "D", "A", "E", "B"};
        int[] expectedSharps = {1, 2, 3, 4, 5};
        
        for (int i = 0; i < sharpKeyNotes.length; i++) {
            Key key = findMajorKey(sharpKeyNotes[i], Accidental.Natural);
            assertNotNull(sharpKeyNotes[i] + " Major should exist", key);
            assertEquals(sharpKeyNotes[i] + " Major should have " + expectedSharps[i] + " sharps",
                    expectedSharps[i], key.getNumAccidentals());
        }
    }

    @Test
    public void testKeySignatures_flatKeyNotes() {
        // Flat keys: F, Bb, Eb, Ab, Db, Gb
        Key fMajor = findMajorKey("F", Accidental.Natural);
        assertNotNull("F Major should exist", fMajor);
        assertEquals("F Major should have 1 flat", -1, fMajor.getNumAccidentals());
        
        Key bFlatMajor = findMajorKey("B", Accidental.Flat);
        assertNotNull("Bb Major should exist", bFlatMajor);
        assertEquals("Bb Major should have 2 flats", -2, bFlatMajor.getNumAccidentals());
    }

    @Test
    public void testKeySignatures_enharmonicKeys() {
        // F# Major (6 sharps) and Gb Major (6 flats) are enharmonic
        Key fSharpMajor = findMajorKey("F", Accidental.Sharp);
        Key gFlatMajor = findMajorKey("G", Accidental.Flat);
        
        assertNotNull("F# Major should exist", fSharpMajor);
        assertNotNull("Gb Major should exist", gFlatMajor);
        assertEquals("F# Major should have 6 sharps", 6, fSharpMajor.getNumAccidentals());
        assertEquals("Gb Major should have 6 flats", -6, gFlatMajor.getNumAccidentals());
        
        // They should have the same pitch
        assertEquals("F# and Gb should have same frequency",
                fSharpMajor.getNote().getFrequency(),
                gFlatMajor.getNote().getFrequency(), 0.001);
    }

    // ==================== Helper Methods ====================

    private Key findMajorKey(String noteName, Accidental accidental) {
        for (Key k : Key.getMajorKeys()) {
            if (k.getNote().getFriendlyName().equals(noteName) &&
                    k.getNote().getAccidental() == accidental) {
                return k;
            }
        }
        return null;
    }

    private Key findMinorKey(String noteName, Accidental accidental) {
        for (Key k : Key.getMinorKeys()) {
            if (k.getNote().getFriendlyName().equals(noteName) &&
                    k.getNote().getAccidental() == accidental) {
                return k;
            }
        }
        return null;
    }
}
