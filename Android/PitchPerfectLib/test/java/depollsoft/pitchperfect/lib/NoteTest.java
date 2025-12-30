package depollsoft.pitchperfect.lib;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the Note class.
 * Tests music note creation, frequency calculation, and note lookup.
 */
public class NoteTest {

    static class DummyPlayer implements Note.NotePlayer {
        int plays = 0;
        int stops = 0;

        @Override
        public void play(Note n) {
            plays++;
        }

        @Override
        public void stop(Note n) {
            stops++;
        }

        void reset() {
            plays = 0;
            stops = 0;
        }
    }

    private DummyPlayer player;

    @Before
    public void setUp() {
        player = new DummyPlayer();
        Note.setPlayer(player);
    }

    // ==================== Original Test (Enhanced) ====================

    @Test
    public void find_and_toString_and_play_stop() {
        Note cSharp = Note.findNote("C", Accidental.Sharp, 4);
        assertNotNull(cSharp);
        assertEquals("C#", cSharp.toString());

        Note bFlat = Note.findNote("B", Accidental.Flat, 4);
        assertNotNull(bFlat);
        assertEquals("Bb", bFlat.toString());

        // exercise play/stop paths with guard
        cSharp.play();
        cSharp.stop();
        assertTrue(player.plays >= 1);
        assertTrue(player.stops >= 1);

        assertFalse(Note.getCommonNotes().isEmpty());
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testNoteConstructor_withFrequency() {
        Note note = new Note("A", 4, Accidental.Natural, 440.0);
        assertEquals("A", note.getFriendlyName());
        assertEquals(4, note.getOctave());
        assertEquals(Accidental.Natural, note.getAccidental());
        assertEquals(440.0, note.getFrequency(), 0.001);
    }

    @Test
    public void testNoteConstructor_withKeyNumber() {
        Note note = new Note("A", 4, Accidental.Natural, 49);
        assertEquals("A", note.getFriendlyName());
        assertEquals(4, note.getOctave());
        assertEquals(Accidental.Natural, note.getAccidental());
        assertEquals(Integer.valueOf(49), note.getKeyNumber());
        // A4 = 440 Hz (piano key 49)
        assertEquals(440.0, note.getFrequency(), 0.001);
    }

    @Test
    public void testDefaultConstructor_createsEmptyNote() {
        Note note = new Note();
        assertNull(note.getFriendlyName());
        assertNull(note.getAccidental());
        assertEquals(0, note.getOctave());
    }

    // ==================== Setters and Getters Tests ====================

    @Test
    public void testSetFriendlyName() {
        Note note = new Note();
        note.setFriendlyName("C");
        assertEquals("C", note.getFriendlyName());
    }

    @Test
    public void testSetOctave() {
        Note note = new Note();
        note.setOctave(5);
        assertEquals(5, note.getOctave());
    }

    @Test
    public void testSetAccidental() {
        Note note = new Note();
        note.setAccidental(Accidental.Sharp);
        assertEquals(Accidental.Sharp, note.getAccidental());
    }

    @Test
    public void testSetFrequency() {
        Note note = new Note();
        note.setFrequency(440.0);
        assertEquals(440.0, note.getFrequency(), 0.001);
    }

    @Test
    public void testSetKeyNumber_calculatesFrequency() {
        Note note = new Note();
        note.setKeyNumber(49); // A4
        assertEquals(440.0, note.getFrequency(), 0.001);
    }

    @Test
    public void testSetKeyNumber_middleC() {
        Note note = new Note();
        note.setKeyNumber(40); // C4 (middle C)
        // C4 frequency should be approximately 261.63 Hz
        assertEquals(261.63, note.getFrequency(), 0.1);
    }

    // ==================== findNote Tests ====================

    @Test
    public void testFindNote_naturalC4() {
        Note c4 = Note.findNote("C", Accidental.Natural, 4);
        assertNotNull("Should find C4", c4);
        assertEquals("C", c4.getFriendlyName());
        assertEquals(Accidental.Natural, c4.getAccidental());
        assertEquals(4, c4.getOctave());
    }

    @Test
    public void testFindNote_sharpF4() {
        Note fSharp4 = Note.findNote("F", Accidental.Sharp, 4);
        assertNotNull("Should find F#4", fSharp4);
        assertEquals("F", fSharp4.getFriendlyName());
        assertEquals(Accidental.Sharp, fSharp4.getAccidental());
        assertEquals(4, fSharp4.getOctave());
    }

    @Test
    public void testFindNote_flatB4() {
        Note bFlat4 = Note.findNote("B", Accidental.Flat, 4);
        assertNotNull("Should find Bb4", bFlat4);
        assertEquals("B", bFlat4.getFriendlyName());
        assertEquals(Accidental.Flat, bFlat4.getAccidental());
        assertEquals(4, bFlat4.getOctave());
    }

    @Test
    public void testFindNote_nonExistent_returnsNull() {
        Note invalid = Note.findNote("X", Accidental.Natural, 4);
        assertNull("Should return null for non-existent note", invalid);
    }

    @Test
    public void testFindNote_allNaturalNotes() {
        String[] noteNames = {"A", "B", "C", "D", "E", "F", "G"};
        for (String name : noteNames) {
            Note note = Note.findNote(name, Accidental.Natural, 4);
            assertNotNull("Should find natural " + name + "4", note);
            assertEquals(name, note.getFriendlyName());
        }
    }

    // ==================== getC4 Tests ====================

    @Test
    public void testGetC4_returnsMiddleC() {
        Note c4 = Note.getC4();
        assertNotNull("C4 should exist", c4);
        assertEquals("C", c4.getFriendlyName());
        assertEquals(Accidental.Natural, c4.getAccidental());
        assertEquals(4, c4.getOctave());
    }

    @Test
    public void testGetC4_sameInstanceReturned() {
        Note c4First = Note.getC4();
        Note c4Second = Note.getC4();
        assertSame("Should return same C4 instance", c4First, c4Second);
    }

    // ==================== getCommonNotes Tests ====================

    @Test
    public void testGetCommonNotes_notEmpty() {
        List<Note> notes = Note.getCommonNotes();
        assertNotNull("Common notes list should not be null", notes);
        assertFalse("Common notes list should not be empty", notes.isEmpty());
    }

    @Test
    public void testGetCommonNotes_containsAllOctaves() {
        List<Note> notes = Note.getCommonNotes();
        boolean hasOctave0 = false;
        boolean hasOctave7 = false;
        for (Note n : notes) {
            if (n.getOctave() == 0) hasOctave0 = true;
            if (n.getOctave() == 7) hasOctave7 = true;
        }
        assertTrue("Should have notes in octave 0", hasOctave0);
        assertTrue("Should have notes in octave 7", hasOctave7);
    }

    @Test
    public void testGetCommonNotes_containsAllNoteNames() {
        List<Note> notes = Note.getCommonNotes();
        String[] noteNames = {"A", "B", "C", "D", "E", "F", "G"};
        for (String name : noteNames) {
            boolean found = false;
            for (Note n : notes) {
                if (name.equals(n.getFriendlyName())) {
                    found = true;
                    break;
                }
            }
            assertTrue("Should contain note " + name, found);
        }
    }

    // ==================== getPrunedNotes Tests ====================

    @Test
    public void testGetPrunedNotes_notEmpty() {
        List<Note> notes = Note.getPrunedNotes();
        assertNotNull("Pruned notes list should not be null", notes);
        assertFalse("Pruned notes list should not be empty", notes.isEmpty());
    }

    @Test
    public void testGetPrunedNotes_noConsecutiveDuplicateFrequencies() {
        List<Note> notes = Note.getPrunedNotes();
        for (int i = 1; i < notes.size(); i++) {
            assertNotEquals("Adjacent notes should have different frequencies",
                    notes.get(i - 1).getFrequency(), notes.get(i).getFrequency(), 0.001);
        }
    }

    @Test
    public void testGetPrunedNotes_smallerThanCommonNotes() {
        List<Note> common = Note.getCommonNotes();
        List<Note> pruned = Note.getPrunedNotes();
        assertTrue("Pruned notes should be smaller than or equal to common notes",
                pruned.size() <= common.size());
    }

    // ==================== equals Tests ====================

    @Test
    public void testEquals_sameNote() {
        Note note1 = new Note("A", 4, Accidental.Natural, 440.0);
        Note note2 = new Note("A", 4, Accidental.Natural, 440.0);
        assertTrue("Same notes should be equal", note1.equals(note2));
    }

    @Test
    public void testEquals_differentName() {
        Note note1 = new Note("A", 4, Accidental.Natural, 440.0);
        Note note2 = new Note("B", 4, Accidental.Natural, 493.88);
        assertFalse("Different names should not be equal", note1.equals(note2));
    }

    @Test
    public void testEquals_differentAccidental() {
        Note note1 = new Note("A", 4, Accidental.Natural, 440.0);
        Note note2 = new Note("A", 4, Accidental.Sharp, 466.16);
        assertFalse("Different accidentals should not be equal", note1.equals(note2));
    }

    @Test
    public void testEquals_differentOctave() {
        Note note1 = new Note("A", 4, Accidental.Natural, 440.0);
        Note note2 = new Note("A", 5, Accidental.Natural, 880.0);
        assertFalse("Different octaves should not be equal", note1.equals(note2));
    }

    @Test
    public void testEquals_withNull() {
        Note note = new Note("A", 4, Accidental.Natural, 440.0);
        assertFalse("Note should not equal null", note.equals(null));
    }

    @Test
    public void testEquals_withDifferentType() {
        Note note = new Note("A", 4, Accidental.Natural, 440.0);
        assertFalse("Note should not equal different type", note.equals("A4"));
    }

    // ==================== toString Tests ====================

    @Test
    public void testToString_naturalNote() {
        Note note = new Note("C", 4, Accidental.Natural, 261.63);
        assertEquals("C", note.toString());
    }

    @Test
    public void testToString_sharpNote() {
        Note note = new Note("F", 4, Accidental.Sharp, 369.99);
        assertEquals("F#", note.toString());
    }

    @Test
    public void testToString_flatNote() {
        Note note = new Note("B", 4, Accidental.Flat, 466.16);
        assertEquals("Bb", note.toString());
    }

    // ==================== Alternate Notes Tests ====================

    @Test
    public void testSetAlternate() {
        Note note1 = new Note("C", 4, Accidental.Sharp, 277.18);
        Note note2 = new Note("D", 4, Accidental.Flat, 277.18);
        note1.setAlternate(note2);
        assertSame("Alternate should be set", note2, note1.getAlternate());
    }

    @Test
    public void testGetAlternate_defaultNull() {
        Note note = new Note("C", 4, Accidental.Natural, 261.63);
        assertNull("Default alternate should be null", note.getAlternate());
    }

    // ==================== Frequency Calculation Tests ====================

    @Test
    public void testFrequencyCalculation_A4() {
        Note note = new Note();
        note.setKeyNumber(49);
        assertEquals("A4 should be 440 Hz", 440.0, note.getFrequency(), 0.001);
    }

    @Test
    public void testFrequencyCalculation_A5_doubleFrequency() {
        Note a4 = new Note();
        a4.setKeyNumber(49);
        Note a5 = new Note();
        a5.setKeyNumber(61);
        assertEquals("A5 should be double A4 frequency",
                a4.getFrequency() * 2, a5.getFrequency(), 0.1);
    }

    @Test
    public void testFrequencyCalculation_A3_halfFrequency() {
        Note a4 = new Note();
        a4.setKeyNumber(49);
        Note a3 = new Note();
        a3.setKeyNumber(37);
        assertEquals("A3 should be half A4 frequency",
                a4.getFrequency() / 2, a3.getFrequency(), 0.1);
    }

    // ==================== Play/Stop Tests ====================

    @Test
    public void testPlay_incrementsPlayCount() {
        player.reset();
        Note note = Note.findNote("A", Accidental.Natural, 4);
        note.play();
        assertTrue("Play should be called", player.plays >= 1);
    }

    @Test
    public void testStop_incrementsStopCount() {
        player.reset();
        Note note = Note.findNote("A", Accidental.Natural, 4);
        note.play();
        note.stop();
        assertTrue("Stop should be called", player.stops >= 1);
    }

    @Test
    public void testGetIsPlaying_defaultFalse() {
        Note note = new Note("A", 4, Accidental.Natural, 440.0);
        assertFalse("Default isPlaying should be false", note.getIsPlaying());
    }
}

