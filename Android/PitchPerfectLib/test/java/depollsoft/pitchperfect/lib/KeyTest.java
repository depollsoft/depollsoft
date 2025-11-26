package depollsoft.pitchperfect.lib;

import org.junit.Test;

import static org.junit.Assert.*;

public class KeyTest {
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
}

