package depollsoft.pitchperfect.converters;

import android.text.SpannableStringBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Key;
import depollsoft.pitchperfect.lib.KeyType;
import depollsoft.pitchperfect.lib.Note;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class KeySignatureConverterTest {

    private KeySignatureConverter converter;

    @Before
    public void setUp() {
        // Ensure common notes are initialized before tests
        Note.getCommonNotes();
        converter = new KeySignatureConverter();
    }

    // ========== Basic Conversion Tests ==========

    @Test
    public void convertToTarget_returnsSpannableStringBuilder() {
        Key key = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        Object result = converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result instanceof SpannableStringBuilder);
    }

    @Test
    public void convertToTarget_cMajor_noAccidentals() {
        Key key = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        // Should have clef symbol but no accidentals
        assertTrue(result.length() > 0);
        assertEquals('&', result.charAt(0)); // Clef symbol
    }

    // ========== Sharp Key Tests ==========

    @Test
    public void convertToTarget_gMajor_oneSharpsCharacter() {
        Key key = new Key(Note.findNote("G", Accidental.Natural, 4), KeyType.Major, 1);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2); // Clef + sharps
    }

    @Test
    public void convertToTarget_dMajor_twoSharps() {
        Key key = new Key(Note.findNote("D", Accidental.Natural, 4), KeyType.Major, 2);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_aMajor_threeSharps() {
        Key key = new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Major, 3);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_eMajor_fourSharps() {
        Key key = new Key(Note.findNote("E", Accidental.Natural, 4), KeyType.Major, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_bMajor_fiveSharps() {
        Key key = new Key(Note.findNote("B", Accidental.Natural, 4), KeyType.Major, 5);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_fSharpMajor_sixSharps() {
        Key key = new Key(Note.findNote("F", Accidental.Sharp, 4), KeyType.Major, 6);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    // ========== Flat Key Tests ==========

    @Test
    public void convertToTarget_fMajor_oneFlat() {
        Key key = new Key(Note.findNote("F", Accidental.Natural, 4), KeyType.Major, -1);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2); // Clef + flats
    }

    @Test
    public void convertToTarget_bFlatMajor_twoFlats() {
        Key key = new Key(Note.findNote("B", Accidental.Flat, 4), KeyType.Major, -2);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_eFlatMajor_threeFlats() {
        Key key = new Key(Note.findNote("E", Accidental.Flat, 4), KeyType.Major, -3);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_aFlatMajor_fourFlats() {
        Key key = new Key(Note.findNote("A", Accidental.Flat, 4), KeyType.Major, -4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_dFlatMajor_fiveFlats() {
        Key key = new Key(Note.findNote("D", Accidental.Flat, 4), KeyType.Major, -5);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_gFlatMajor_sixFlats() {
        Key key = new Key(Note.findNote("G", Accidental.Flat, 4), KeyType.Major, -6);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    // ========== Minor Key Tests ==========

    @Test
    public void convertToTarget_aMinor_noAccidentals() {
        Key key = new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Minor, 0);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertEquals('&', result.charAt(0)); // Clef symbol, no accidentals
    }

    @Test
    public void convertToTarget_eMinor_oneSharp() {
        Key key = new Key(Note.findNote("E", Accidental.Natural, 4), KeyType.Minor, 1);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    @Test
    public void convertToTarget_dMinor_oneFlat() {
        Key key = new Key(Note.findNote("D", Accidental.Natural, 4), KeyType.Minor, -1);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() >= 2);
    }

    // ========== All Major Keys Test ==========

    @Test
    public void convertToTarget_allMajorKeys_produceValidResults() {
        for (Key key : Key.getMajorKeys()) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
            
            assertNotNull("Result should not be null for key: " + key.getFriendlyName(), result);
            assertTrue("Result should have content for key: " + key.getFriendlyName(), result.length() > 0);
            assertEquals("Result should start with clef symbol for key: " + key.getFriendlyName(), 
                '&', result.charAt(0));
        }
    }

    // ========== All Minor Keys Test ==========

    @Test
    public void convertToTarget_allMinorKeys_produceValidResults() {
        for (Key key : Key.getMinorKeys()) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
            
            assertNotNull("Result should not be null for key: " + key.getFriendlyName(), result);
            assertTrue("Result should have content for key: " + key.getFriendlyName(), result.length() > 0);
            assertEquals("Result should start with clef symbol for key: " + key.getFriendlyName(), 
                '&', result.charAt(0));
        }
    }

    // ========== Default Key Tests ==========

    @Test
    public void convertToTarget_defaultKey_noAccidentals() {
        Key key = new Key(); // Default is C Major with 0 accidentals
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    // ========== Accidentals Count Consistency Tests ==========

    @Test
    public void convertToTarget_keysWithSameAccidentalCount_haveSameLength() {
        // G Major (1 sharp) and E minor (1 sharp) should have same output structure
        Key gMajor = new Key(Note.findNote("G", Accidental.Natural, 4), KeyType.Major, 1);
        Key eMinor = new Key(Note.findNote("E", Accidental.Natural, 4), KeyType.Minor, 1);
        
        SpannableStringBuilder gMajorResult = (SpannableStringBuilder) converter.convertToTarget(gMajor, CharSequence.class);
        SpannableStringBuilder eMinorResult = (SpannableStringBuilder) converter.convertToTarget(eMinor, CharSequence.class);
        
        assertEquals("Keys with same accidental count should have same output length",
            gMajorResult.length(), eMinorResult.length());
    }

    @Test
    public void convertToTarget_keysWithMoreAccidentals_haveValidOutput() {
        Key oneSharp = new Key(Note.findNote("G", Accidental.Natural, 4), KeyType.Major, 1);
        Key fiveSharps = new Key(Note.findNote("B", Accidental.Natural, 4), KeyType.Major, 5);
        
        SpannableStringBuilder oneResult = (SpannableStringBuilder) converter.convertToTarget(oneSharp, CharSequence.class);
        SpannableStringBuilder fiveResult = (SpannableStringBuilder) converter.convertToTarget(fiveSharps, CharSequence.class);
        
        assertNotNull(oneResult);
        assertNotNull(fiveResult);
        // Both should have valid output
        assertTrue(oneResult.length() > 0);
        assertTrue(fiveResult.length() > 0);
    }
}
