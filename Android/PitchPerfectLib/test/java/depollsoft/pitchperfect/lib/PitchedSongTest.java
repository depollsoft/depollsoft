package depollsoft.pitchperfect.lib;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the PitchedSong class.
 * Tests song creation, comparison, and playback state.
 */
public class PitchedSongTest {

    static class DummyPlayer implements Note.NotePlayer {
        int plays = 0;
        int stops = 0;

        @Override
        public void play(Note n) {
            plays++;
        }

        @Override
        public void stop(Note n) {
            stops++;
        }

        void reset() {
            plays = 0;
            stops = 0;
        }
    }

    private DummyPlayer player;

    @Before
    public void setUp() {
        player = new DummyPlayer();
        Note.setPlayer(player);
    }

    // ==================== Original Tests (Enhanced) ====================

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
        PitchedSong song = new PitchedSong();
        song.setName("C song");
        song.setKey(new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0));

        assertFalse(song.getIsPlaying());
        song.play();
        assertTrue(player.plays >= 1);

        // Note.getIsPlaying is toggled by Note.play(); which in turn calls into player
        // We only verify that stop path also executes without error and toggles state
        song.stop();
        assertTrue(player.stops >= 1);
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testDefaultConstructor_generatesUUID() {
        PitchedSong song = new PitchedSong();
        assertNotNull("UUID should be generated", song.getId());
        assertFalse("UUID should not be empty", song.getId().isEmpty());
    }

    @Test
    public void testDefaultConstructor_uniqueUUIDs() {
        PitchedSong song1 = new PitchedSong();
        PitchedSong song2 = new PitchedSong();
        assertNotEquals("Each song should have unique UUID", song1.getId(), song2.getId());
    }

    // ==================== Setters and Getters Tests ====================

    @Test
    public void testSetName() {
        PitchedSong song = new PitchedSong();
        song.setName("Hello World");
        assertEquals("Hello World", song.getName());
    }

    @Test
    public void testSetName_emptyString() {
        PitchedSong song = new PitchedSong();
        song.setName("");
        assertEquals("", song.getName());
    }

    @Test
    public void testSetId() {
        PitchedSong song = new PitchedSong();
        song.setId("custom-id-123");
        assertEquals("custom-id-123", song.getId());
    }

    @Test
    public void testSetKey() {
        PitchedSong song = new PitchedSong();
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key cMajor = new Key(cNote, KeyType.Major, 0);
        song.setKey(cMajor);
        assertEquals(cMajor, song.getKey());
    }

    @Test
    public void testSetKey_minorKey() {
        PitchedSong song = new PitchedSong();
        Note aNote = Note.findNote("A", Accidental.Natural, 4);
        Key aMinor = new Key(aNote, KeyType.Minor, 0);
        song.setKey(aMinor);
        assertEquals(KeyType.Minor, song.getKey().getKeyType());
    }

    // ==================== compareTo Tests ====================

    @Test
    public void testCompareTo_sameName() {
        PitchedSong song1 = new PitchedSong();
        song1.setName("Test Song");
        PitchedSong song2 = new PitchedSong();
        song2.setName("Test Song");
        assertEquals(0, song1.compareTo(song2));
    }

    @Test
    public void testCompareTo_differentName_ascending() {
        PitchedSong song1 = new PitchedSong();
        song1.setName("Alpha");
        PitchedSong song2 = new PitchedSong();
        song2.setName("Beta");
        assertTrue("Alpha should come before Beta", song1.compareTo(song2) < 0);
    }

    @Test
    public void testCompareTo_differentName_descending() {
        PitchedSong song1 = new PitchedSong();
        song1.setName("Zebra");
        PitchedSong song2 = new PitchedSong();
        song2.setName("Apple");
        assertTrue("Zebra should come after Apple", song1.compareTo(song2) > 0);
    }

    @Test
    public void testCompareTo_caseInsensitive() {
        PitchedSong song1 = new PitchedSong();
        song1.setName("apple");
        PitchedSong song2 = new PitchedSong();
        song2.setName("APPLE");
        assertEquals("Comparison should be case-insensitive", 0, song1.compareTo(song2));
    }

    @Test
    public void testCompareTo_caseInsensitiveOrdering() {
        PitchedSong song1 = new PitchedSong();
        song1.setName("apple");
        PitchedSong song2 = new PitchedSong();
        song2.setName("BANANA");
        assertTrue("apple should come before BANANA", song1.compareTo(song2) < 0);
    }

    // ==================== equals Tests ====================

    @Test
    public void testEquals_sameId() {
        PitchedSong song1 = new PitchedSong();
        song1.setId("same-id");
        song1.setName("Song 1");

        PitchedSong song2 = new PitchedSong();
        song2.setId("same-id");
        song2.setName("Song 2");

        assertTrue("Songs with same ID should be equal", song1.equals(song2));
    }

    @Test
    public void testEquals_differentId() {
        PitchedSong song1 = new PitchedSong();
        song1.setId("id-1");
        PitchedSong song2 = new PitchedSong();
        song2.setId("id-2");
        assertFalse("Songs with different IDs should not be equal", song1.equals(song2));
    }

    @Test
    public void testEquals_sameNameDifferentId() {
        PitchedSong song1 = new PitchedSong();
        song1.setName("Same Name");
        PitchedSong song2 = new PitchedSong();
        song2.setName("Same Name");
        // Different UUIDs generated, so should not be equal
        assertFalse("Songs with same name but different IDs should not be equal",
                song1.equals(song2));
    }

    @Test
    public void testEquals_withNull() {
        PitchedSong song = new PitchedSong();
        assertFalse("Song should not equal null", song.equals(null));
    }

    @Test
    public void testEquals_withDifferentType() {
        PitchedSong song = new PitchedSong();
        song.setName("Test");
        assertFalse("Song should not equal different type", song.equals("Test"));
    }

    // ==================== hashCode Tests ====================

    @Test
    public void testHashCode_basedOnId() {
        PitchedSong song = new PitchedSong();
        song.setId("test-id");
        assertEquals("test-id".hashCode(), song.hashCode());
    }

    @Test
    public void testHashCode_equalSongsHaveSameHash() {
        PitchedSong song1 = new PitchedSong();
        song1.setId("same-id");
        PitchedSong song2 = new PitchedSong();
        song2.setId("same-id");
        assertEquals("Equal songs should have same hash code",
                song1.hashCode(), song2.hashCode());
    }

    @Test
    public void testHashCode_consistentWithEquals() {
        PitchedSong song1 = new PitchedSong();
        song1.setId("id-123");
        PitchedSong song2 = new PitchedSong();
        song2.setId("id-123");

        if (song1.equals(song2)) {
            assertEquals("If equals, hashCodes must match",
                    song1.hashCode(), song2.hashCode());
        }
    }

    // ==================== Play/Stop State Tests ====================

    @Test
    public void testPlay_callsNotePlay() {
        player.reset();
        PitchedSong song = new PitchedSong();
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key cMajor = new Key(cNote, KeyType.Major, 0);
        song.setKey(cMajor);
        song.play();
        assertTrue("Note play should be called", player.plays >= 1);
    }

    @Test
    public void testStop_callsNoteStop() {
        player.reset();
        PitchedSong song = new PitchedSong();
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        Key cMajor = new Key(cNote, KeyType.Major, 0);
        song.setKey(cMajor);
        song.play();
        song.stop();
        assertTrue("Note stop should be called", player.stops >= 1);
    }

    // ==================== Integration Tests ====================

    @Test
    public void testSongWithDifferentKeys() {
        PitchedSong song1 = new PitchedSong();
        song1.setName("Song in C Major");
        Note cNote = Note.findNote("C", Accidental.Natural, 4);
        song1.setKey(new Key(cNote, KeyType.Major, 0));

        PitchedSong song2 = new PitchedSong();
        song2.setName("Song in G Major");
        Note gNote = Note.findNote("G", Accidental.Natural, 4);
        song2.setKey(new Key(gNote, KeyType.Major, 1));

        assertNotEquals("Different keys should mean different notes",
                song1.getKey().getNote().getFriendlyName(),
                song2.getKey().getNote().getFriendlyName());
    }

    @Test
    public void testSongKeyProperties() {
        PitchedSong song = new PitchedSong();
        song.setName("My Song");
        Note fSharp = Note.findNote("F", Accidental.Sharp, 4);
        Key fSharpMajor = new Key(fSharp, KeyType.Major, 6);
        song.setKey(fSharpMajor);

        // Key.getFriendlyName returns just the letter; Note.toString includes accidental
        assertEquals("F", song.getKey().getFriendlyName());
        assertEquals("F#", song.getKey().getNote().toString());
        assertEquals(Accidental.Sharp, song.getKey().getAccidental());
        assertEquals(6, song.getKey().getNumAccidentals());
    }

    @Test
    public void testSongSorting_alphabetical() {
        PitchedSong[] songs = new PitchedSong[3];
        songs[0] = new PitchedSong();
        songs[0].setName("Zebra Song");
        songs[1] = new PitchedSong();
        songs[1].setName("Apple Song");
        songs[2] = new PitchedSong();
        songs[2].setName("Mango Song");

        java.util.Arrays.sort(songs);

        assertEquals("Apple Song", songs[0].getName());
        assertEquals("Mango Song", songs[1].getName());
        assertEquals("Zebra Song", songs[2].getName());
    }
}

