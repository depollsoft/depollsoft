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
public class KeyNameConverterTest {

    private KeyNameConverter converter;

    @Before
    public void setUp() {
        // Ensure common notes are initialized before tests
        Note.getCommonNotes();
        converter = new KeyNameConverter();
    }

    // ========== Basic Conversion Tests ==========

    @Test
    public void convertToTarget_returnsSpannableStringBuilder() {
        Key key = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        Object result = converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result instanceof SpannableStringBuilder);
    }

    // ========== Major Key Name Tests (Uppercase) ==========

    @Test
    public void convertToTarget_cMajor_returnsUppercaseC() {
        Key key = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("C"));
    }

    @Test
    public void convertToTarget_gMajor_returnsUppercaseG() {
        Key key = new Key(Note.findNote("G", Accidental.Natural, 4), KeyType.Major, 1);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("G"));
    }

    @Test
    public void convertToTarget_fSharpMajor_startsWithUppercaseF() {
        Key key = new Key(Note.findNote("F", Accidental.Sharp, 4), KeyType.Major, 6);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("F"));
        assertTrue(result.length() > 1); // Should have sharp symbol
    }

    @Test
    public void convertToTarget_bFlatMajor_startsWithUppercaseB() {
        Key key = new Key(Note.findNote("B", Accidental.Flat, 4), KeyType.Major, -2);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("B"));
        assertTrue(result.length() > 1); // Should have flat symbol
    }

    // ========== Minor Key Name Tests (Lowercase) ==========

    @Test
    public void convertToTarget_aMinor_returnsLowercaseA() {
        Key key = new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Minor, 0);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("a"));
    }

    @Test
    public void convertToTarget_eMinor_returnsLowercaseE() {
        Key key = new Key(Note.findNote("E", Accidental.Natural, 4), KeyType.Minor, 1);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("e"));
    }

    @Test
    public void convertToTarget_fSharpMinor_startsWithLowercaseF() {
        Key key = new Key(Note.findNote("F", Accidental.Sharp, 4), KeyType.Minor, 3);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("f"));
        assertTrue(result.length() > 1); // Should have sharp symbol
    }

    @Test
    public void convertToTarget_bFlatMinor_startsWithLowercaseB() {
        Key key = new Key(Note.findNote("B", Accidental.Flat, 4), KeyType.Minor, -5);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("b"));
        assertTrue(result.length() > 1); // Should have flat symbol
    }

    // ========== Natural Keys Tests (No Accidental Symbol) ==========

    @Test
    public void convertToTarget_naturalKey_hasOnlyLetterName() {
        Key key = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertEquals(1, result.length()); // Just "C", no accidental symbol
        assertEquals("C", result.toString());
    }

    @Test
    public void convertToTarget_aMinorNatural_hasOnlyLetterName() {
        Key key = new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Minor, 0);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertEquals(1, result.length()); // Just "a", no accidental symbol
        assertEquals("a", result.toString());
    }

    // ========== Flat Keys Tests ==========

    @Test
    public void convertToTarget_flatKey_hasAccidentalSymbol() {
        Key key = new Key(Note.findNote("B", Accidental.Flat, 4), KeyType.Major, -2);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() > 1); // Letter + flat symbol
    }

    @Test
    public void convertToTarget_eFlatMajor_returnsValidResult() {
        Key key = new Key(Note.findNote("E", Accidental.Flat, 4), KeyType.Major, -3);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("E"));
        assertEquals(2, result.length()); // E + flat symbol
    }

    @Test
    public void convertToTarget_aFlatMajor_returnsValidResult() {
        Key key = new Key(Note.findNote("A", Accidental.Flat, 4), KeyType.Major, -4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("A"));
    }

    @Test
    public void convertToTarget_dFlatMajor_returnsValidResult() {
        Key key = new Key(Note.findNote("D", Accidental.Flat, 4), KeyType.Major, -5);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("D"));
    }

    @Test
    public void convertToTarget_gFlatMajor_returnsValidResult() {
        Key key = new Key(Note.findNote("G", Accidental.Flat, 4), KeyType.Major, -6);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("G"));
    }

    // ========== Sharp Keys Tests ==========

    @Test
    public void convertToTarget_sharpKey_hasAccidentalSymbol() {
        Key key = new Key(Note.findNote("F", Accidental.Sharp, 4), KeyType.Major, 6);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.length() > 1); // Letter + sharp symbol
    }

    @Test
    public void convertToTarget_cSharpMinor_returnsValidResult() {
        Key key = new Key(Note.findNote("C", Accidental.Sharp, 4), KeyType.Minor, 4);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("c")); // lowercase for minor
    }

    @Test
    public void convertToTarget_gSharpMinor_returnsValidResult() {
        Key key = new Key(Note.findNote("G", Accidental.Sharp, 4), KeyType.Minor, 5);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("g")); // lowercase for minor
    }

    @Test
    public void convertToTarget_dSharpMinor_returnsValidResult() {
        Key key = new Key(Note.findNote("D", Accidental.Sharp, 4), KeyType.Minor, 6);
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertTrue(result.toString().startsWith("d")); // lowercase for minor
    }

    // ========== All Major Keys Test ==========

    @Test
    public void convertToTarget_allMajorKeys_produceValidResults() {
        for (Key key : Key.getMajorKeys()) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
            
            assertNotNull("Result should not be null for key: " + key.getFriendlyName(), result);
            assertTrue("Result should have content for key: " + key.getFriendlyName(), result.length() > 0);
            
            // Major keys should have uppercase first letter
            char firstChar = result.charAt(0);
            assertTrue("Major key should start with uppercase letter: " + key.getFriendlyName(),
                Character.isUpperCase(firstChar));
        }
    }

    // ========== All Minor Keys Test ==========

    @Test
    public void convertToTarget_allMinorKeys_produceValidResults() {
        for (Key key : Key.getMinorKeys()) {
            SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
            
            assertNotNull("Result should not be null for key: " + key.getFriendlyName(), result);
            assertTrue("Result should have content for key: " + key.getFriendlyName(), result.length() > 0);
            
            // Minor keys should have lowercase first letter
            char firstChar = result.charAt(0);
            assertTrue("Minor key should start with lowercase letter: " + key.getFriendlyName(),
                Character.isLowerCase(firstChar));
        }
    }

    // ========== Default Key Tests ==========

    @Test
    public void convertToTarget_defaultKey_returnsCMajor() {
        Key key = new Key(); // Default is C Major
        SpannableStringBuilder result = (SpannableStringBuilder) converter.convertToTarget(key, CharSequence.class);
        
        assertNotNull(result);
        assertEquals("C", result.toString());
    }

    // ========== Case Consistency Tests ==========

    @Test
    public void convertToTarget_majorVsMinor_differentCase() {
        Key cMajor = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        Key cMinor = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Minor, -3);
        
        SpannableStringBuilder majorResult = (SpannableStringBuilder) converter.convertToTarget(cMajor, CharSequence.class);
        SpannableStringBuilder minorResult = (SpannableStringBuilder) converter.convertToTarget(cMinor, CharSequence.class);
        
        assertEquals("C", majorResult.toString());
        assertEquals("c", minorResult.toString());
    }

    @Test
    public void convertToTarget_relativeKeys_differentCase() {
        // C Major and A minor are relative keys (both have 0 accidentals)
        Key cMajor = new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0);
        Key aMinor = new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Minor, 0);
        
        SpannableStringBuilder cMajorResult = (SpannableStringBuilder) converter.convertToTarget(cMajor, CharSequence.class);
        SpannableStringBuilder aMinorResult = (SpannableStringBuilder) converter.convertToTarget(aMinor, CharSequence.class);
        
        assertTrue(Character.isUpperCase(cMajorResult.charAt(0)));
        assertTrue(Character.isLowerCase(aMinorResult.charAt(0)));
    }
}
