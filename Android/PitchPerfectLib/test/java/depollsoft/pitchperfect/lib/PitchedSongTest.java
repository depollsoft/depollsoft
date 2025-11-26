package depollsoft.pitchperfect.lib;

import org.junit.Test;

import static org.junit.Assert.*;

public class PitchedSongTest {

    static class DummyPlayer implements Note.NotePlayer {
        int plays = 0; int stops = 0;
        @Override public void play(Note n) { plays++; }
        @Override public void stop(Note n) { stops++; }
    }

    @Test
    public void equality_hash_and_compareTo() {
        PitchedSong a = new PitchedSong();
        PitchedSong b = new PitchedSong();
        b.setId(a.getId());

        a.setName("Alpha");
        b.setName("beta");

        assertTrue(a.equals(b));
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.compareTo(b) < 0); // Alpha < beta (case-insensitive)
    }

    @Test
    public void play_and_stop_updates_state() {
        DummyPlayer p = new DummyPlayer();
        Note.setPlayer(p);

        PitchedSong song = new PitchedSong();
        song.setName("C song");
        song.setKey(new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0));

        assertFalse(song.getIsPlaying());
        song.play();
        assertTrue(p.plays >= 1);

        // Note.getIsPlaying is toggled by Note.play(); which in turn calls into player
        // We only verify that stop path also executes without error and toggles state
        song.stop();
        assertTrue(p.stops >= 1);
    }
}

