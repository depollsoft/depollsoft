package depollsoft.pitchperfect;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.bindroid.trackable.TrackableCollection;

import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PitchPipeModelTest {

    private PitchPipeModel pitchPipeModel;

    @Before
    public void setUp() {
        pitchPipeModel = new PitchPipeModel();
    }

    @Test
    public void testPitchPipeModel_InitializesWithDefaultCToCNotes() {
        TrackableCollection<Note> notes = pitchPipeModel.getNotes();
        
        assertNotNull("Notes collection should not be null", notes);
        assertEquals("Should have 12 notes in C to C collection", 12, notes.size());
        
        // Verify first and last notes in C to C collection
        assertEquals("First note should be C natural", "C", notes.get(0).noteName);
        assertEquals("First note should be natural", Accidental.Natural, notes.get(0).accidental);
        assertEquals("First note should be octave 4", 4, notes.get(0).octave);
        
        assertEquals("Last note should be B natural", "B", notes.get(11).noteName);
        assertEquals("Last note should be natural", Accidental.Natural, notes.get(11).accidental);
        assertEquals("Last note should be octave 4", 4, notes.get(11).octave);
    }

    @Test
    public void testSetIsFromFToF_True_SwitchesToFToFCollection() {
        pitchPipeModel.setIsFromFToF(true);
        
        TrackableCollection<Note> notes = pitchPipeModel.getNotes();
        assertNotNull("Notes collection should not be null", notes);
        assertEquals("Should have 12 notes in F to F collection", 12, notes.size());
        
        // Verify first and last notes in F to F collection
        assertEquals("First note should be F natural", "F", notes.get(0).noteName);
        assertEquals("First note should be natural", Accidental.Natural, notes.get(0).accidental);
        assertEquals("First note should be octave 4", 4, notes.get(0).octave);
        
        assertEquals("Last note should be E natural", "E", notes.get(11).noteName);
        assertEquals("Last note should be natural", Accidental.Natural, notes.get(11).accidental);
        assertEquals("Last note should be octave 5", 5, notes.get(11).octave);
    }

    @Test
    public void testSetIsFromFToF_False_SwitchesToCToCCollection() {
        // First switch to F to F
        pitchPipeModel.setIsFromFToF(true);
        
        // Then switch back to C to C
        pitchPipeModel.setIsFromFToF(false);
        
        TrackableCollection<Note> notes = pitchPipeModel.getNotes();
        assertNotNull("Notes collection should not be null", notes);
        assertEquals("Should have 12 notes in C to C collection", 12, notes.size());
        
        assertEquals("First note should be C natural", "C", notes.get(0).noteName);
        assertEquals("Last note should be B natural", "B", notes.get(11).noteName);
    }

    @Test
    public void testGetIsFromFToF_ReturnsCorrectValue() {
        // Default should be false (C to C)
        assertFalse("Default should be C to C (false)", pitchPipeModel.getIsFromFToF());
        
        pitchPipeModel.setIsFromFToF(true);
        assertTrue("Should return true after setting to F to F", pitchPipeModel.getIsFromFToF());
        
        pitchPipeModel.setIsFromFToF(false);
        assertFalse("Should return false after setting to C to C", pitchPipeModel.getIsFromFToF());
    }

    @Test
    public void testCToCCollection_ContainsCorrectNotes() {
        // Ensure we're in C to C mode
        pitchPipeModel.setIsFromFToF(false);
        TrackableCollection<Note> notes = pitchPipeModel.getNotes();
        
        // Check all expected notes
        String[] expectedNotes = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"};
        Accidental[] expectedAccidentals = {
            Accidental.Natural, Accidental.Sharp, Accidental.Natural, Accidental.Sharp,
            Accidental.Natural, Accidental.Natural, Accidental.Sharp, Accidental.Natural,
            Accidental.Sharp, Accidental.Natural, Accidental.Sharp, Accidental.Natural
        };
        
        for (int i = 0; i < expectedNotes.length; i++) {
            assertEquals("Note " + i + " should match", expectedNotes[i], notes.get(i).noteName);
            assertEquals("Accidental " + i + " should match", expectedAccidentals[i], notes.get(i).accidental);
            assertEquals("All notes should be octave 4", 4, notes.get(i).octave);
        }
    }

    @Test
    public void testFToFCollection_ContainsCorrectNotes() {
        pitchPipeModel.setIsFromFToF(true);
        TrackableCollection<Note> notes = pitchPipeModel.getNotes();
        
        // Check first few and last few notes
        assertEquals("First note should be F natural", "F", notes.get(0).noteName);
        assertEquals("Second note should be F sharp", "F", notes.get(1).noteName);
        assertEquals("Second note should be sharp", Accidental.Sharp, notes.get(1).accidental);
        
        // Last note should be E natural in octave 5
        assertEquals("Last note should be E natural", "E", notes.get(11).noteName);
        assertEquals("Last note should be natural", Accidental.Natural, notes.get(11).accidental);
        assertEquals("Last note should be octave 5", 5, notes.get(11).octave);
    }

    @Test
    public void testSetNotes_UpdatesNotesCollection() {
        TrackableCollection<Note> customNotes = new TrackableCollection<Note>();
        customNotes.add(Note.findNote("A", Accidental.Natural, 4));
        customNotes.add(Note.findNote("B", Accidental.Natural, 4));
        
        pitchPipeModel.setNotes(customNotes);
        
        TrackableCollection<Note> retrievedNotes = pitchPipeModel.getNotes();
        assertEquals("Should have 2 custom notes", 2, retrievedNotes.size());
        assertEquals("First note should be A", "A", retrievedNotes.get(0).noteName);
        assertEquals("Second note should be B", "B", retrievedNotes.get(1).noteName);
    }

    @Test
    public void testSetNotes_WithNullCollection_HandlesGracefully() {
        pitchPipeModel.setNotes(null);
        
        // Should not crash and should return null or handle gracefully
        TrackableCollection<Note> notes = pitchPipeModel.getNotes();
        // The behavior depends on implementation - just ensure no crash
        assertNotNull("Test should complete without crashing", pitchPipeModel);
    }

    @Test
    public void testPitchPipeModel_MultipleInstances_Independent() {
        PitchPipeModel model1 = new PitchPipeModel();
        PitchPipeModel model2 = new PitchPipeModel();
        
        model1.setIsFromFToF(true);
        model2.setIsFromFToF(false);
        
        assertTrue("Model 1 should be F to F", model1.getIsFromFToF());
        assertFalse("Model 2 should be C to C", model2.getIsFromFToF());
        
        // Verify different note collections
        assertNotEquals("Models should have different note collections", 
                       model1.getNotes().get(0).noteName, 
                       model2.getNotes().get(0).noteName);
    }
}