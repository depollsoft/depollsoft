package depollsoft.pitchperfect.lib;

import org.junit.Test;

import static org.junit.Assert.*;

public class NoteTest {
    static class DummyPlayer implements Note.NotePlayer {
        int plays = 0; int stops = 0;
        @Override public void play(Note n) { plays++; }
        @Override public void stop(Note n) { stops++; }
    }

    @Test
    public void find_and_toString_and_play_stop() {
        DummyPlayer p = new DummyPlayer();
        Note.setPlayer(p);

        Note cSharp = Note.findNote("C", Accidental.Sharp, 4);
        assertNotNull(cSharp);
        assertEquals("C#", cSharp.toString());

        Note bFlat = Note.findNote("B", Accidental.Flat, 4);
        assertNotNull(bFlat);
        assertEquals("Bb", bFlat.toString());

        // exercise play/stop paths with guard
        cSharp.play();
        cSharp.stop();
        assertTrue(p.plays >= 1);
        assertTrue(p.stops >= 1);

        assertFalse(Note.getCommonNotes().isEmpty());
    }
}

