package depollsoft.pitchperfect.converters;

import android.text.SpannableStringBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NoteListNoteTextConverterTest {

    private NoteListNoteTextConverter converter;

    @Before
    public void setUp() {
        // Ensure common notes are initialized before tests
        Note.getCommonNotes();
        converter = new NoteListNoteTextConverter();
    }

    // ========== Basic Conversion Tests ==========

    @Test
    public void convertToTarget_returnsSpannableStringBuilder() {
        Note note = Note.findNote("C", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result instanceof SpannableStringBuilder);
    }

    // ========== Natural Note Tests ==========

    @Test
    public void convertToTarget_naturalNote_containsNameAndOctave() {
        Note note = Note.findNote("C", Accidental.Natural, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("C"));
        assertTrue(text.contains("4")); // Octave should be present
    }

    @Test
    public void convertToTarget_c4Natural_correctFormat() {
        Note note = Note.findNote("C", Accidental.Natural, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        assertEquals("C4", result.toString());
    }

    @Test
    public void convertToTarget_a4Natural_correctFormat() {
        Note note = Note.findNote("A", Accidental.Natural, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        assertEquals("A4", result.toString());
    }

    @Test
    public void convertToTarget_g3Natural_correctFormat() {
        Note note = Note.findNote("G", Accidental.Natural, 3);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        assertEquals("G3", result.toString());
    }

    // ========== Sharp Note Tests ==========

    @Test
    public void convertToTarget_sharpNote_containsAccidentalSymbol() {
        Note note = Note.findNote("C", Accidental.Sharp, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("C"));
        assertTrue(text.length() > 2); // Name + accidental + octave
    }

    @Test
    public void convertToTarget_fSharp4_hasCorrectNameAndOctave() {
        Note note = Note.findNote("F", Accidental.Sharp, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("F"));
        assertTrue(text.endsWith("4"));
    }

    @Test
    public void convertToTarget_gSharp3_hasCorrectNameAndOctave() {
        Note note = Note.findNote("G", Accidental.Sharp, 3);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("G"));
        assertTrue(text.endsWith("3"));
    }

    // ========== Flat Note Tests ==========

    @Test
    public void convertToTarget_flatNote_containsAccidentalSymbol() {
        Note note = Note.findNote("B", Accidental.Flat, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("B"));
        assertTrue(text.length() > 2); // Name + accidental + octave
    }

    @Test
    public void convertToTarget_bFlat4_hasCorrectNameAndOctave() {
        Note note = Note.findNote("B", Accidental.Flat, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("B"));
        assertTrue(text.endsWith("4"));
    }

    @Test
    public void convertToTarget_eFlat3_hasCorrectNameAndOctave() {
        Note note = Note.findNote("E", Accidental.Flat, 3);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("E"));
        assertTrue(text.endsWith("3"));
    }

    @Test
    public void convertToTarget_aFlat5_hasCorrectNameAndOctave() {
        Note note = Note.findNote("A", Accidental.Flat, 5);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.startsWith("A"));
        assertTrue(text.endsWith("5"));
    }

    // ========== Alternate Note Tests ==========

    @Test
    public void convertToTarget_noteWithAlternate_containsSeparator() {
        // Get pruned notes which have alternates set
        Note noteWithAlternate = null;
        for (Note n : Note.getPrunedNotes()) {
            if (n.getAlternate() != null) {
                noteWithAlternate = n;
                break;
            }
        }
        
        if (noteWithAlternate != null) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(noteWithAlternate, CharSequence.class);
            
            assertNotNull(result);
            String text = result.toString();
            assertTrue("Note with alternate should contain separator", text.contains(" / "));
        }
    }

    @Test
    public void convertToTarget_noteWithAlternate_containsBothNotes() {
        // Get pruned notes which have alternates set
        Note noteWithAlternate = null;
        for (Note n : Note.getPrunedNotes()) {
            if (n.getAlternate() != null) {
                noteWithAlternate = n;
                break;
            }
        }
        
        if (noteWithAlternate != null) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(noteWithAlternate, CharSequence.class);
            
            assertNotNull(result);
            String text = result.toString();
            
            // Should contain the main note name
            assertTrue("Should contain main note name", 
                text.startsWith(noteWithAlternate.getFriendlyName()));
            
            // Should contain the alternate note name
            Note alternate = noteWithAlternate.getAlternate();
            assertTrue("Should contain alternate note name",
                text.contains(alternate.getFriendlyName()));
        }
    }

    // ========== Different Octave Tests ==========

    @Test
    public void convertToTarget_differentOctaves_showCorrectOctave() {
        for (int octave = 1; octave <= 7; octave++) {
            Note note = Note.findNote("C", Accidental.Natural, octave);
            if (note != null) {
                SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
                
                assertNotNull("Result should not be null for octave " + octave, result);
                String text = result.toString();
                assertEquals("C" + octave, text);
            }
        }
    }

    @Test
    public void convertToTarget_lowOctave_showsCorrectOctave() {
        Note note = Note.findNote("C", Accidental.Natural, 1);
        if (note != null) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
            
            assertNotNull(result);
            assertTrue(result.toString().contains("1"));
        }
    }

    @Test
    public void convertToTarget_highOctave_showsCorrectOctave() {
        Note note = Note.findNote("C", Accidental.Natural, 7);
        if (note != null) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
            
            assertNotNull(result);
            assertTrue(result.toString().contains("7"));
        }
    }

    // ========== All Common Notes Tests ==========

    @Test
    public void convertToTarget_allCommonNotes_produceValidResults() {
        for (Note note : Note.getCommonNotes()) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
            
            assertNotNull("Result should not be null for note: " + note.toString(), result);
            assertTrue("Result should have content for note: " + note.toString(), result.length() > 0);
            
            // Should start with note name
            String text = result.toString();
            assertTrue("Should start with note name for: " + note.toString(),
                text.startsWith(note.getFriendlyName()));
        }
    }

    // ========== All Pruned Notes Tests ==========

    @Test
    public void convertToTarget_allPrunedNotes_produceValidResults() {
        for (Note note : Note.getPrunedNotes()) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
            
            assertNotNull("Result should not be null for note: " + note.toString(), result);
            assertTrue("Result should have content for note: " + note.toString(), result.length() > 0);
        }
    }

    // ========== Note Display Format Tests ==========

    @Test
    public void convertToTarget_naturalNote_hasMinimalLength() {
        Note note = Note.findNote("A", Accidental.Natural, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(note, CharSequence.class);
        
        // Natural note: Name + Octave = 2 characters (e.g., "A4")
        assertEquals(2, result.length());
    }

    @Test
    public void convertToTarget_accidentalNote_hasLongerLength() {
        Note naturalNote = Note.findNote("C", Accidental.Natural, 4);
        Note sharpNote = Note.findNote("C", Accidental.Sharp, 4);
        
        SpannableStringBuilder naturalResult = (SpannableStringBuilder) converter.convertToTarget(naturalNote, CharSequence.class);
        SpannableStringBuilder sharpResult = (SpannableStringBuilder) converter.convertToTarget(sharpNote, CharSequence.class);
        
        // Sharp note should be longer than natural note
        assertTrue(sharpResult.length() > naturalResult.length());
    }

    @Test
    public void convertToTarget_sharpAndFlatNotes_haveSameFormatLength() {
        Note sharpNote = Note.findNote("C", Accidental.Sharp, 4);
        Note flatNote = Note.findNote("D", Accidental.Flat, 4);
        
        // Clear any alternates that might have been set by getPrunedNotes() in other tests
        // This ensures we're testing the base format length without alternate note suffixes
        sharpNote.setAlternate(null);
        flatNote.setAlternate(null);
        
        SpannableStringBuilder sharpResult = (SpannableStringBuilder) converter.convertToTarget(sharpNote, CharSequence.class);
        SpannableStringBuilder flatResult = (SpannableStringBuilder) converter.convertToTarget(flatNote, CharSequence.class);
        
        // Both should have same format length (Name + Accidental + Octave)
        assertEquals(sharpResult.length(), flatResult.length());
    }

    // ========== Note C4 (Middle C) Tests ==========

    @Test
    public void convertToTarget_middleC_correctFormat() {
        Note middleC = Note.getC4();
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(middleC, CharSequence.class);
        
        assertNotNull(result);
        assertEquals("C4", result.toString());
    }
}
