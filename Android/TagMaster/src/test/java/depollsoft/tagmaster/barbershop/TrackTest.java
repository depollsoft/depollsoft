package depollsoft.tagmaster.barbershop;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Track entity class.
 */
public class TrackTest {

    @Test
    public void constructor_default_createsEmptyTrack() {
        Track track = new Track();
        assertNull(track.getTitle());
        assertNull(track.getSource());
    }

    @Test
    public void constructor_withParameters_setsFieldsCorrectly() {
        RemoteLocation location = new RemoteLocation();
        location.setUri("http://example.com/track.mp3");
        location.setType("audio");

        Track track = new Track("Bass", location);

        assertEquals("Bass", track.getTitle());
        assertNotNull(track.getSource());
        assertEquals("http://example.com/track.mp3", track.getSource().getUri());
    }

    @Test
    public void setTitle_validTitle_getsCorrectTitle() {
        Track track = new Track();
        track.setTitle("Lead");
        assertEquals("Lead", track.getTitle());
    }

    @Test
    public void setTitle_emptyTitle_getsEmptyTitle() {
        Track track = new Track();
        track.setTitle("");
        assertEquals("", track.getTitle());
    }

    @Test
    public void setTitle_nullTitle_getsNullTitle() {
        Track track = new Track();
        track.setTitle(null);
        assertNull(track.getTitle());
    }

    @Test
    public void setSource_validSource_getsCorrectSource() {
        Track track = new Track();
        RemoteLocation location = new RemoteLocation();
        location.setUri("http://example.com/track.mp3");

        track.setSource(location);

        assertNotNull(track.getSource());
        assertEquals("http://example.com/track.mp3", track.getSource().getUri());
    }

    @Test
    public void setSource_nullSource_getsNullSource() {
        Track track = new Track();
        track.setSource(null);
        assertNull(track.getSource());
    }

    @Test
    public void toString_withTitle_returnsTitle() {
        Track track = new Track();
        track.setTitle("Tenor");
        assertEquals("Tenor", track.toString());
    }

    @Test
    public void toString_withNullTitle_returnsNull() {
        Track track = new Track();
        assertNull(track.toString());
    }

    @Test
    public void Equals_sameTrack_returnsTrue() {
        RemoteLocation location1 = new RemoteLocation();
        location1.setUri("http://example.com/track.mp3");
        location1.setType("audio");

        RemoteLocation location2 = new RemoteLocation();
        location2.setUri("http://example.com/track.mp3");
        location2.setType("audio");

        Track track1 = new Track("Bass", location1);
        Track track2 = new Track("Bass", location2);

        assertTrue(track1.Equals(track2));
    }

    @Test
    public void Equals_differentTitle_returnsFalse() {
        RemoteLocation location = new RemoteLocation();
        location.setUri("http://example.com/track.mp3");

        Track track1 = new Track("Bass", location);
        Track track2 = new Track("Lead", location);

        assertFalse(track1.Equals(track2));
    }

    @Test
    public void Equals_null_returnsFalse() {
        Track track = new Track("Bass", new RemoteLocation());
        assertFalse(track.Equals(null));
    }

    @Test
    public void setTitle_overwriteExistingTitle_getsNewTitle() {
        Track track = new Track();
        track.setTitle("Bass");
        assertEquals("Bass", track.getTitle());

        track.setTitle("Lead");
        assertEquals("Lead", track.getTitle());
    }

    @Test
    public void setSource_overwriteExistingSource_getsNewSource() {
        Track track = new Track();

        RemoteLocation location1 = new RemoteLocation();
        location1.setUri("http://example.com/track1.mp3");
        track.setSource(location1);
        assertEquals("http://example.com/track1.mp3", track.getSource().getUri());

        RemoteLocation location2 = new RemoteLocation();
        location2.setUri("http://example.com/track2.mp3");
        track.setSource(location2);
        assertEquals("http://example.com/track2.mp3", track.getSource().getUri());
    }

    @Test
    public void constructor_withNullTitle_setsNullTitle() {
        RemoteLocation location = new RemoteLocation();
        Track track = new Track(null, location);
        assertNull(track.getTitle());
    }

    @Test
    public void constructor_withNullSource_setsNullSource() {
        Track track = new Track("Bass", null);
        assertEquals("Bass", track.getTitle());
        assertNull(track.getSource());
    }

    @Test
    public void constructor_withBothNull_setsBothNull() {
        Track track = new Track(null, null);
        assertNull(track.getTitle());
        assertNull(track.getSource());
    }
}
