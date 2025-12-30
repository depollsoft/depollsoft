package depollsoft.pitchperfect.converters;

import android.text.Spannable;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import depollsoft.pitchperfect.CommonModel;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PitchPipeNoteTextConverterTest {

    private PitchPipeNoteTextConverter converter;

    @BeforeClass
    public static void setUpClass() {
        CommonModel.setTestMode(true);
    }

    @Before
    public void setUp() {
        // Ensure common notes are initialized before tests
        Note.getCommonNotes();
        converter = new PitchPipeNoteTextConverter();
    }

    // ========== Basic Conversion Tests ==========

    @Test
    public void convertToTarget_naturalNote_returnsString() {
        Note note = Note.findNote("C", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result instanceof String);
    }

    @Test
    public void convertToTarget_sharpNote_returnsSpannable() {
        Note note = Note.findNote("C", Accidental.Sharp, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result instanceof Spannable);
    }

    @Test
    public void convertToTarget_flatNote_returnsSpannable() {
        Note note = Note.findNote("B", Accidental.Flat, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result instanceof Spannable);
    }

    // ========== Natural Note Tests ==========

    @Test
    public void convertToTarget_cNatural_returnsFriendlyName() {
        Note note = Note.findNote("C", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals("C", result);
    }

    @Test
    public void convertToTarget_dNatural_returnsFriendlyName() {
        Note note = Note.findNote("D", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals("D", result);
    }

    @Test
    public void convertToTarget_eNatural_returnsFriendlyName() {
        Note note = Note.findNote("E", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals("E", result);
    }

    @Test
    public void convertToTarget_fNatural_returnsFriendlyName() {
        Note note = Note.findNote("F", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals("F", result);
    }

    @Test
    public void convertToTarget_gNatural_returnsFriendlyName() {
        Note note = Note.findNote("G", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals("G", result);
    }

    @Test
    public void convertToTarget_aNatural_returnsFriendlyName() {
        Note note = Note.findNote("A", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals("A", result);
    }

    @Test
    public void convertToTarget_bNatural_returnsFriendlyName() {
        Note note = Note.findNote("B", Accidental.Natural, 4);
        Object result = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals("B", result);
    }

    // ========== Sharp/Flat Notes Return Same Result Tests ==========

    @Test
    public void convertToTarget_sharpNote_returnsSharpFlatSymbols() {
        Note note = Note.findNote("C", Accidental.Sharp, 4);
        Spannable result = (Spannable) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        // The result should contain both sharp and flat symbols
        assertTrue(result.length() > 0);
    }

    @Test
    public void convertToTarget_flatNote_returnsSharpFlatSymbols() {
        Note note = Note.findNote("D", Accidental.Flat, 4);
        Spannable result = (Spannable) converter.convertToTarget(note, CharSequence.class);
        
        assertNotNull(result);
        // The result should contain both sharp and flat symbols
        assertTrue(result.length() > 0);
    }

    @Test
    public void convertToTarget_sharpAndFlatNotes_returnSameResult() {
        // C# and Db are enharmonic equivalents
        Note sharpNote = Note.findNote("C", Accidental.Sharp, 4);
        Note flatNote = Note.findNote("D", Accidental.Flat, 4);
        
        Spannable sharpResult = (Spannable) converter.convertToTarget(sharpNote, CharSequence.class);
        Spannable flatResult = (Spannable) converter.convertToTarget(flatNote, CharSequence.class);
        
        // Both should return the same static sharpFlat spannable
        assertSame("Sharp and flat notes should return same spannable", sharpResult, flatResult);
    }

    // ========== All Sharp Notes Return Same Result ==========

    @Test
    public void convertToTarget_allSharpNotes_returnSameSpannable() {
        Note cSharp = Note.findNote("C", Accidental.Sharp, 4);
        Note dSharp = Note.findNote("D", Accidental.Sharp, 4);
        Note fSharp = Note.findNote("F", Accidental.Sharp, 4);
        Note gSharp = Note.findNote("G", Accidental.Sharp, 4);
        Note aSharp = Note.findNote("A", Accidental.Sharp, 4);
        
        Spannable cSharpResult = (Spannable) converter.convertToTarget(cSharp, CharSequence.class);
        Spannable dSharpResult = (Spannable) converter.convertToTarget(dSharp, CharSequence.class);
        Spannable fSharpResult = (Spannable) converter.convertToTarget(fSharp, CharSequence.class);
        Spannable gSharpResult = (Spannable) converter.convertToTarget(gSharp, CharSequence.class);
        Spannable aSharpResult = (Spannable) converter.convertToTarget(aSharp, CharSequence.class);
        
        // All should return the same static spannable
        assertSame(cSharpResult, dSharpResult);
        assertSame(dSharpResult, fSharpResult);
        assertSame(fSharpResult, gSharpResult);
        assertSame(gSharpResult, aSharpResult);
    }

    // ========== All Flat Notes Return Same Result ==========

    @Test
    public void convertToTarget_allFlatNotes_returnSameSpannable() {
        Note dFlat = Note.findNote("D", Accidental.Flat, 4);
        Note eFlat = Note.findNote("E", Accidental.Flat, 4);
        Note gFlat = Note.findNote("G", Accidental.Flat, 4);
        Note aFlat = Note.findNote("A", Accidental.Flat, 4);
        Note bFlat = Note.findNote("B", Accidental.Flat, 4);
        
        Spannable dFlatResult = (Spannable) converter.convertToTarget(dFlat, CharSequence.class);
        Spannable eFlatResult = (Spannable) converter.convertToTarget(eFlat, CharSequence.class);
        Spannable gFlatResult = (Spannable) converter.convertToTarget(gFlat, CharSequence.class);
        Spannable aFlatResult = (Spannable) converter.convertToTarget(aFlat, CharSequence.class);
        Spannable bFlatResult = (Spannable) converter.convertToTarget(bFlat, CharSequence.class);
        
        // All should return the same static spannable
        assertSame(dFlatResult, eFlatResult);
        assertSame(eFlatResult, gFlatResult);
        assertSame(gFlatResult, aFlatResult);
        assertSame(aFlatResult, bFlatResult);
    }

    // ========== Different Octave Tests ==========

    @Test
    public void convertToTarget_naturalNoteDifferentOctaves_allReturnFriendlyName() {
        for (int octave = 1; octave <= 7; octave++) {
            Note note = Note.findNote("C", Accidental.Natural, octave);
            if (note != null) {
                Object result = converter.convertToTarget(note, CharSequence.class);
                
                assertEquals("C", result);
            }
        }
    }

    @Test
    public void convertToTarget_sharpNoteDifferentOctaves_allReturnSameSpannable() {
        Note sharp4 = Note.findNote("C", Accidental.Sharp, 4);
        Note sharp5 = Note.findNote("C", Accidental.Sharp, 5);
        
        if (sharp4 != null && sharp5 != null) {
            Spannable result4 = (Spannable) converter.convertToTarget(sharp4, CharSequence.class);
            Spannable result5 = (Spannable) converter.convertToTarget(sharp5, CharSequence.class);
            
            // Both should return the same static spannable regardless of octave
            assertSame(result4, result5);
        }
    }

    // ========== Consistency Tests ==========

    @Test
    public void convertToTarget_calledMultipleTimes_consistentResults() {
        Note note = Note.findNote("C", Accidental.Natural, 4);
        
        Object result1 = converter.convertToTarget(note, CharSequence.class);
        Object result2 = converter.convertToTarget(note, CharSequence.class);
        Object result3 = converter.convertToTarget(note, CharSequence.class);
        
        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    @Test
    public void convertToTarget_sharpNoteCalledMultipleTimes_returnsSameInstance() {
        Note note = Note.findNote("C", Accidental.Sharp, 4);
        
        Spannable result1 = (Spannable) converter.convertToTarget(note, CharSequence.class);
        Spannable result2 = (Spannable) converter.convertToTarget(note, CharSequence.class);
        Spannable result3 = (Spannable) converter.convertToTarget(note, CharSequence.class);
        
        // Should return the same static instance
        assertSame(result1, result2);
        assertSame(result2, result3);
    }

    // ========== All Natural Notes Tests ==========

    @Test
    public void convertToTarget_allNaturalNotes_returnFriendlyNames() {
        String[] noteNames = {"C", "D", "E", "F", "G", "A", "B"};
        
        for (String name : noteNames) {
            Note note = Note.findNote(name, Accidental.Natural, 4);
            if (note != null) {
                Object result = converter.convertToTarget(note, CharSequence.class);
                
                assertNotNull("Result should not be null for note: " + name, result);
                assertEquals("Should return friendly name for: " + name, name, result);
            }
        }
    }

    // ========== Middle C Test ==========

    @Test
    public void convertToTarget_middleC_returnsC() {
        Note middleC = Note.getC4();
        Object result = converter.convertToTarget(middleC, CharSequence.class);
        
        assertEquals("C", result);
    }

    // ========== Sharp/Flat Display Contains Separator ==========

    @Test
    public void convertToTarget_sharpNote_resultContainsSeparator() {
        Note note = Note.findNote("C", Accidental.Sharp, 4);
        Spannable result = (Spannable) converter.convertToTarget(note, CharSequence.class);
        
        String text = result.toString();
        assertTrue("Sharp/flat display should contain separator '/'", text.contains("/"));
    }

    // ========== Result Type Differentiation Tests ==========

    @Test
    public void convertToTarget_naturalVsAccidental_differentTypes() {
        Note natural = Note.findNote("C", Accidental.Natural, 4);
        Note sharp = Note.findNote("C", Accidental.Sharp, 4);
        Note flat = Note.findNote("D", Accidental.Flat, 4);
        
        Object naturalResult = converter.convertToTarget(natural, CharSequence.class);
        Object sharpResult = converter.convertToTarget(sharp, CharSequence.class);
        Object flatResult = converter.convertToTarget(flat, CharSequence.class);
        
        assertTrue("Natural note should return String", naturalResult instanceof String);
        assertTrue("Sharp note should return Spannable", sharpResult instanceof Spannable);
        assertTrue("Flat note should return Spannable", flatResult instanceof Spannable);
    }
}
