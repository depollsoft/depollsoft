package depollsoft.pitchperfect;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;

import com.bindroid.trackable.TrackableCollection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.util.Preferences;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

/**
 * Unit tests for {@link PitchPipeModel}.
 * Tests pitch pipe functionality including note collections and F-to-F/C-to-C modes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 35)
public class PitchPipeModelTest {

    @BeforeClass
    public static void setUpClass() {
        // Enable test mode to use in-memory preferences
        Preferences.setTestMode(true);
    }

    @AfterClass
    public static void tearDownClass() {
        // Disable test mode after all tests
        Preferences.setTestMode(false);
    }

    @Before
    public void setUp() throws Exception {
        // Clear test values before each test for isolation
        Preferences.clearTestValues();
    }

    @Test
    public void constructor_initializesCToCNotes_whenIsFromFToFIsFalse() {
        PitchPipeModel model = new PitchPipeModel();

        TrackableCollection<Note> notes = model.getNotes();
        assertNotNull("Notes collection should not be null", notes);
        assertEquals("C-to-C scale should have 13 notes", 13, notes.size());

        // Verify first note is C4
        Note firstNote = notes.get(0);
        assertEquals("First note should be C", "C", firstNote.getFriendlyName());
        assertEquals("First note should be Natural", Accidental.Natural, firstNote.getAccidental());
        assertEquals("First note should be octave 4", 4, firstNote.getOctave());

        // Verify last note is C5
        Note lastNote = notes.get(12);
        assertEquals("Last note should be C", "C", lastNote.getFriendlyName());
        assertEquals("Last note should be Natural", Accidental.Natural, lastNote.getAccidental());
        assertEquals("Last note should be octave 5", 5, lastNote.getOctave());
    }

    @Test
    public void getNotes_returnsCToCScale_byDefault() {
        PitchPipeModel model = new PitchPipeModel();

        TrackableCollection<Note> notes = model.getNotes();

        // Verify chromatic C-to-C scale: C, C#, D, D#, E, F, F#, G, G#, A, A#, B, C
        String[] expectedNames = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B", "C"};
        Accidental[] expectedAccidentals = {
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Natural
        };

        for (int i = 0; i < 13; i++) {
            assertEquals("Note " + i + " name mismatch", expectedNames[i], notes.get(i).getFriendlyName());
            assertEquals("Note " + i + " accidental mismatch", expectedAccidentals[i], notes.get(i).getAccidental());
        }
    }

    @Test
    public void setIsFromFToF_true_switchesToFToFScale() {
        PitchPipeModel model = new PitchPipeModel();

        model.setIsFromFToF(true);

        TrackableCollection<Note> notes = model.getNotes();
        assertEquals("F-to-F scale should have 13 notes", 13, notes.size());

        // Verify first note is F4
        Note firstNote = notes.get(0);
        assertEquals("First note should be F", "F", firstNote.getFriendlyName());
        assertEquals("First note should be Natural", Accidental.Natural, firstNote.getAccidental());
        assertEquals("First note should be octave 4", 4, firstNote.getOctave());

        // Verify last note is F5
        Note lastNote = notes.get(12);
        assertEquals("Last note should be F", "F", lastNote.getFriendlyName());
        assertEquals("Last note should be Natural", Accidental.Natural, lastNote.getAccidental());
        assertEquals("Last note should be octave 5", 5, lastNote.getOctave());
    }

    @Test
    public void setIsFromFToF_false_switchesToCToCScale() {
        PitchPipeModel model = new PitchPipeModel();
        model.setIsFromFToF(true); // First switch to F-to-F

        model.setIsFromFToF(false); // Then switch back to C-to-C

        TrackableCollection<Note> notes = model.getNotes();

        // Verify first note is C4
        Note firstNote = notes.get(0);
        assertEquals("First note should be C", "C", firstNote.getFriendlyName());
        assertEquals("First note should be octave 4", 4, firstNote.getOctave());
    }

    @Test
    public void getIsFromFToF_returnsFalse_byDefault() {
        PitchPipeModel model = new PitchPipeModel();

        assertFalse("isFromFToF should be false by default", model.getIsFromFToF());
    }

    @Test
    public void getIsFromFToF_returnsTrue_afterSetToTrue() {
        PitchPipeModel model = new PitchPipeModel();

        model.setIsFromFToF(true);

        assertTrue("isFromFToF should be true after setting", model.getIsFromFToF());
    }

    @Test
    public void isFromFToF_persistsAcrossInstances() {
        PitchPipeModel model1 = new PitchPipeModel();
        model1.setIsFromFToF(true);

        // Create a new instance - it should read the persisted preference
        PitchPipeModel model2 = new PitchPipeModel();

        assertTrue("New instance should have persisted isFromFToF value", model2.getIsFromFToF());
    }

    @Test
    public void fToFScale_containsCorrectNotes() {
        PitchPipeModel model = new PitchPipeModel();
        model.setIsFromFToF(true);

        TrackableCollection<Note> notes = model.getNotes();

        // F-to-F scale: F, F#, G, G#, A, A#, B, C, C#, D, D#, E, F
        String[] expectedNames = {"F", "F", "G", "G", "A", "A", "B", "C", "C", "D", "D", "E", "F"};
        Accidental[] expectedAccidentals = {
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Natural
        };
        int[] expectedOctaves = {4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5};

        for (int i = 0; i < 13; i++) {
            assertEquals("Note " + i + " name mismatch", expectedNames[i], notes.get(i).getFriendlyName());
            assertEquals("Note " + i + " accidental mismatch", expectedAccidentals[i], notes.get(i).getAccidental());
            assertEquals("Note " + i + " octave mismatch", expectedOctaves[i], notes.get(i).getOctave());
        }
    }

    @Test
    public void setNotes_updatesNotesCollection() {
        PitchPipeModel model = new PitchPipeModel();
        TrackableCollection<Note> customNotes = new TrackableCollection<>();
        customNotes.add(Note.findNote("A", Accidental.Natural, 4));

        model.setNotes(customNotes);

        assertEquals("Notes should be updated to custom collection", 1, model.getNotes().size());
        assertEquals("Custom note should be A4", "A", model.getNotes().get(0).getFriendlyName());
    }

    @Test
    public void cToC_includesOctave5Root() {
        PitchPipeModel model = new PitchPipeModel();

        TrackableCollection<Note> notes = model.getNotes();

        for (int i = 0; i < notes.size(); i++) {
            assertEquals("C-to-C includes the upper C5", i == 12 ? 5 : 4, notes.get(i).getOctave());
        }
    }

    @Test
    public void toggleBetweenScales_switchesCorrectly() {
        PitchPipeModel model = new PitchPipeModel();

        // Start with C-to-C (default)
        assertEquals("C", model.getNotes().get(0).getFriendlyName());

        // Switch to F-to-F
        model.setIsFromFToF(true);
        assertEquals("F", model.getNotes().get(0).getFriendlyName());

        // Switch back to C-to-C
        model.setIsFromFToF(false);
        assertEquals("C", model.getNotes().get(0).getFriendlyName());

        // Switch to F-to-F again
        model.setIsFromFToF(true);
        assertEquals("F", model.getNotes().get(0).getFriendlyName());
    }
}
