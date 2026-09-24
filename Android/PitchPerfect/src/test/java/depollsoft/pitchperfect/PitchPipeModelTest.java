package depollsoft.pitchperfect;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.ArrayList;

import depollsoft.lib.util.Preferences;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PitchPipeModelTest {

    private static final String IS_FROM_F_TO_F_KEY = "depollsoft.pitchperfect.PitchPipeModel.IsFromFToF";

    @Before
    public void setUp() {
        // Ensure common notes are initialized before tests
        Note.getCommonNotes();
        // Enable test mode for Preferences to avoid SharedPreferences issues
        Preferences.setTestMode(true);
        Preferences.clearTestValues();
    }

    @After
    public void tearDown() {
        // Disable test mode after each test
        Preferences.setTestMode(false);
    }

    // ========== C-to-C vs F-to-F Range Switching Tests ==========

    @Test
    public void constructor_defaultsToCtoCRange_whenPreferenceIsFalse() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            // Set preference to false (C-to-C range)
            Preferences.set(IS_FROM_F_TO_F_KEY, false);
            
            PitchPipeModel model = new PitchPipeModel();
            List<Note> notes = model.getNotes();
            
            assertNotNull(notes);
            assertEquals(13, notes.size());
            // C-to-C range starts with C4
            assertEquals("C", notes.get(0).getFriendlyName());
            assertEquals(Accidental.Natural, notes.get(0).getAccidental());
            assertEquals(4, notes.get(0).getOctave());
        }
    }

    @Test
    public void constructor_usesFToFRange_whenPreferenceIsTrue() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            // Set preference to true (F-to-F range)
            Preferences.set(IS_FROM_F_TO_F_KEY, true);
            
            PitchPipeModel model = new PitchPipeModel();
            List<Note> notes = model.getNotes();
            
            assertNotNull(notes);
            assertEquals(13, notes.size());
            // F-to-F range starts with F4
            assertEquals("F", notes.get(0).getFriendlyName());
            assertEquals(Accidental.Natural, notes.get(0).getAccidental());
            assertEquals(4, notes.get(0).getOctave());
        }
    }

    @Test
    public void setIsFromFToF_true_switchesToFToFRange() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            // Start with C-to-C
            Preferences.set(IS_FROM_F_TO_F_KEY, false);
            
            PitchPipeModel model = new PitchPipeModel();
            
            // Switch to F-to-F
            model.setFromFToF(true);
            
            List<Note> notes = model.getNotes();
            assertEquals("F", notes.get(0).getFriendlyName());
            assertEquals(4, notes.get(0).getOctave());
            
            // Verify preference was saved
            assertTrue((Boolean) Preferences.get(IS_FROM_F_TO_F_KEY));
            
            // Verify widget update was called
            widgetMock.verify(() -> PitchPipeAppWidget.updateWidgets());
        }
    }

    @Test
    public void setIsFromFToF_false_switchesToCtoCRange() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            // Start with F-to-F
            Preferences.set(IS_FROM_F_TO_F_KEY, true);
            
            PitchPipeModel model = new PitchPipeModel();
            
            // Switch to C-to-C
            model.setFromFToF(false);
            
            List<Note> notes = model.getNotes();
            assertEquals("C", notes.get(0).getFriendlyName());
            assertEquals(4, notes.get(0).getOctave());
            
            // Verify preference was saved
            assertFalse((Boolean) Preferences.get(IS_FROM_F_TO_F_KEY));
        }
    }

    @Test
    public void getIsFromFToF_returnsPreferenceValue() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            Preferences.set(IS_FROM_F_TO_F_KEY, true);
            
            PitchPipeModel model = new PitchPipeModel();
            assertTrue(model.isFromFToF());
        }
    }

    // ========== Note Collection Tests ==========

    @Test
    public void ctoCRange_containsCorrectNotes() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            Preferences.set(IS_FROM_F_TO_F_KEY, false);
            
            PitchPipeModel model = new PitchPipeModel();
            List<Note> notes = model.getNotes();
            
            // Full inclusive chromatic octave: C4 through C5.
            String[] expectedNotes = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B", "C"};
            Accidental[] expectedAccidentals = {
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Sharp,
                Accidental.Natural, Accidental.Natural
            };
            int[] expectedOctaves = {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5};

            for (int i = 0; i < expectedNotes.length; i++) {
                assertEquals("Note " + i + " name mismatch", expectedNotes[i], notes.get(i).getFriendlyName());
                assertEquals("Note " + i + " accidental mismatch", expectedAccidentals[i], notes.get(i).getAccidental());
                assertEquals("Note " + i + " octave mismatch", expectedOctaves[i], notes.get(i).getOctave());
            }
        }
    }

    @Test
    public void fToFRange_containsCorrectNotes() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            Preferences.set(IS_FROM_F_TO_F_KEY, true);
            
            PitchPipeModel model = new PitchPipeModel();
            List<Note> notes = model.getNotes();
            
            // Full inclusive chromatic octave: F4 through F5.
            String[] expectedNotes = {"F", "F", "G", "G", "A", "A", "B", "C", "C", "D", "D", "E", "F"};
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

            for (int i = 0; i < expectedNotes.length; i++) {
                assertEquals("Note " + i + " name mismatch", expectedNotes[i], notes.get(i).getFriendlyName());
                assertEquals("Note " + i + " accidental mismatch", expectedAccidentals[i], notes.get(i).getAccidental());
                assertEquals("Note " + i + " octave mismatch", expectedOctaves[i], notes.get(i).getOctave());
            }
        }
    }

    @Test
    public void fToFRange_endsWithF5() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            Preferences.set(IS_FROM_F_TO_F_KEY, true);
            
            PitchPipeModel model = new PitchPipeModel();
            List<Note> notes = model.getNotes();
            
            Note lastNote = notes.get(12);
            assertEquals("F", lastNote.getFriendlyName());
            assertEquals(Accidental.Natural, lastNote.getAccidental());
            assertEquals(5, lastNote.getOctave());
        }
    }

    @Test
    public void setNotes_updatesNotesCollection() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            Preferences.set(IS_FROM_F_TO_F_KEY, false);
            
            PitchPipeModel model = new PitchPipeModel();
            
            List<Note> customNotes = new ArrayList<>();
            customNotes.add(Note.findNote("A", Accidental.Natural, 4));
            
            model.setNotes(customNotes);
            
            assertEquals(1, model.getNotes().size());
            assertEquals("A", model.getNotes().get(0).getFriendlyName());
        }
    }

    @Test
    public void bothRanges_haveExactly13Notes() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            // Test C-to-C
            Preferences.set(IS_FROM_F_TO_F_KEY, false);
            PitchPipeModel modelCtoC = new PitchPipeModel();
            assertEquals(13, modelCtoC.getNotes().size());
            
            // Test F-to-F
            Preferences.clearTestValues();
            Preferences.set(IS_FROM_F_TO_F_KEY, true);
            PitchPipeModel modelFtoF = new PitchPipeModel();
            assertEquals(13, modelFtoF.getNotes().size());
        }
    }

    @Test
    public void rangeSwitching_preservesNoteCount() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            Preferences.set(IS_FROM_F_TO_F_KEY, false);
            
            PitchPipeModel model = new PitchPipeModel();
            assertEquals(13, model.getNotes().size());

            model.setFromFToF(true);
            assertEquals(13, model.getNotes().size());

            model.setFromFToF(false);
            assertEquals(13, model.getNotes().size());
        }
    }

    @Test
    public void constructor_initializesPreference_whenNotSet() {
        try (MockedStatic<PitchPipeAppWidget> widgetMock = Mockito.mockStatic(PitchPipeAppWidget.class)) {
            // Don't set any preference value - it should be initialized to false
            new PitchPipeModel();
            
            // Verify the preference was initialized to false
            Boolean value = Preferences.get(IS_FROM_F_TO_F_KEY);
            assertNotNull("Preference should be initialized", value);
            assertFalse("Preference should default to false", value);
        }
    }
}
