package depollsoft.pitchperfect;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.bindroid.trackable.TrackableCollection;

import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NoteListModelTest {

    @Before
    public void setUp() {
        // Ensure common notes are initialized before tests
        Note.getCommonNotes();
    }

    // ========== Note Collection Initialization Tests ==========

    @Test
    public void constructor_initializesNotesCollection() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        assertNotNull(notes);
        assertTrue(notes.size() > 0);
    }

    @Test
    public void constructor_usesPrunedNotes() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        // Pruned notes should have fewer entries than common notes
        // because enharmonic equivalents are merged
        assertTrue(notes.size() <= Note.getCommonNotes().size());
    }

    @Test
    public void notes_containsMultipleOctaves() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        // Verify notes span multiple octaves
        int minOctave = Integer.MAX_VALUE;
        int maxOctave = Integer.MIN_VALUE;
        
        for (int i = 0; i < notes.size(); i++) {
            int octave = notes.get(i).getOctave();
            minOctave = Math.min(minOctave, octave);
            maxOctave = Math.max(maxOctave, octave);
        }
        
        assertTrue("Should span multiple octaves", maxOctave > minOctave);
    }

    @Test
    public void notes_containsC4() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        boolean foundC4 = false;
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.getFriendlyName().equals("C") 
                && note.getAccidental() == Accidental.Natural
                && note.getOctave() == 4) {
                foundC4 = true;
                break;
            }
        }
        
        assertTrue("Notes should contain C4 (middle C)", foundC4);
    }

    @Test
    public void notes_containsAllNaturalNotes() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        String[] naturalNoteNames = {"C", "D", "E", "F", "G", "A", "B"};
        
        for (String noteName : naturalNoteNames) {
            boolean found = false;
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getFriendlyName().equals(noteName)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Should contain note: " + noteName, found);
        }
    }

    // ========== Note Collection Operations Tests ==========

    @Test
    public void setNotes_updatesCollection() {
        NoteListModel model = new NoteListModel();
        
        TrackableCollection<Note> customNotes = new TrackableCollection<>();
        customNotes.add(Note.findNote("A", Accidental.Natural, 4));
        customNotes.add(Note.findNote("B", Accidental.Natural, 4));
        
        model.setNotes(customNotes);
        
        assertEquals(2, model.getNotes().size());
    }

    @Test
    public void setNotes_replacesExistingCollection() {
        NoteListModel model = new NoteListModel();
        int originalSize = model.getNotes().size();
        
        TrackableCollection<Note> customNotes = new TrackableCollection<>();
        customNotes.add(Note.findNote("C", Accidental.Natural, 4));
        
        model.setNotes(customNotes);
        
        assertNotEquals(originalSize, model.getNotes().size());
        assertEquals(1, model.getNotes().size());
    }

    @Test
    public void getNotes_returnsSameInstance() {
        NoteListModel model = new NoteListModel();
        
        TrackableCollection<Note> notes1 = model.getNotes();
        TrackableCollection<Note> notes2 = model.getNotes();
        
        assertSame(notes1, notes2);
    }

    @Test
    public void setNotes_allowsEmptyCollection() {
        NoteListModel model = new NoteListModel();
        
        TrackableCollection<Note> emptyNotes = new TrackableCollection<>();
        model.setNotes(emptyNotes);
        
        assertEquals(0, model.getNotes().size());
    }

    @Test
    public void setNotes_allowsNull() {
        NoteListModel model = new NoteListModel();
        
        model.setNotes(null);
        
        assertNull(model.getNotes());
    }

    // ========== Pruned Notes Characteristics Tests ==========

    @Test
    public void prunedNotes_noConsecutiveDuplicateFrequencies() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        for (int i = 1; i < notes.size(); i++) {
            double prevFreq = notes.get(i - 1).getFrequency();
            double currFreq = notes.get(i).getFrequency();
            
            assertNotEquals("Notes at index " + (i-1) + " and " + i + " should have different frequencies",
                prevFreq, currFreq, 0.001);
        }
    }

    @Test
    public void prunedNotes_areInAscendingFrequencyOrder() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        for (int i = 1; i < notes.size(); i++) {
            double prevFreq = notes.get(i - 1).getFrequency();
            double currFreq = notes.get(i).getFrequency();
            
            assertTrue("Note at index " + i + " should have higher frequency than index " + (i-1),
                currFreq > prevFreq);
        }
    }

    @Test
    public void notes_frequencyDoublesPerOctave() {
        NoteListModel model = new NoteListModel();
        TrackableCollection<Note> notes = model.getNotes();
        
        // Find same note in two consecutive octaves
        Note c4 = null;
        Note c5 = null;
        
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.getFriendlyName().equals("C") && note.getAccidental() == Accidental.Natural) {
                if (note.getOctave() == 4) c4 = note;
                if (note.getOctave() == 5) c5 = note;
            }
        }
        
        if (c4 != null && c5 != null) {
            // C5 should be approximately double the frequency of C4
            double ratio = c5.getFrequency() / c4.getFrequency();
            assertEquals("Octave should double frequency", 2.0, ratio, 0.01);
        }
    }

    // ========== Multiple Instance Tests ==========

    @Test
    public void multipleInstances_haveIndependentCollections() {
        NoteListModel model1 = new NoteListModel();
        NoteListModel model2 = new NoteListModel();
        
        TrackableCollection<Note> customNotes = new TrackableCollection<>();
        customNotes.add(Note.findNote("A", Accidental.Natural, 4));
        model1.setNotes(customNotes);
        
        // model2 should still have original notes
        assertTrue(model2.getNotes().size() > 1);
    }
}
