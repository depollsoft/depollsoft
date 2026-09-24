package depollsoft.pitchperfect.lib;

import android.media.AudioFormat;
import android.media.AudioTrack;

import depollsoft.lib.state.StateField;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class Note {
  public interface NotePlayer {
    public void play(Note n);

    public void stop(Note n);
  }

  private static List<Note> commonNotes;

  private static List<Note> prunedNotes;

  private static Note c4;

  public static final NotePlayer DEFAULT_PLAYER = new NotePlayer() {
    private WeakHashMap<Note, AudioTrack> tracks = new WeakHashMap<Note, AudioTrack>();

    @Override
    public void play(Note n) {
      AudioTrack track = tracks.get(n);
      if (track == null) {
        track = PitchAudioTrackGenerator.getPitchAudioTrack(n.getFrequency(), 8000,
            AudioFormat.CHANNEL_CONFIGURATION_MONO, 2000);
        tracks.put(n, track);
      }
      track.play();
    }

    @Override
    public void stop(Note n) {
      AudioTrack track = tracks.get(n);
      if (track != null && !n.isAttemptingToPlay
          && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
        PitchAudioTrackGenerator.stop(track);
        tracks.remove(n);
      }
    }
  };

  private static NotePlayer player = DEFAULT_PLAYER;

  public static void setPlayer(NotePlayer player) {
    Note.player = player;
  }

  public static Note findNote(String name, Accidental accidental, int octave) {
    for (Note n : Note.getCommonNotes()) {
      if (n.getAccidental() == accidental && n.getOctave() == octave
          && n.getFriendlyName().equals(name))
        return n;
    }
    return null;
  }

  public static Note getC4() {
    if (Note.c4 == null)
      Note.getCommonNotes();
    return Note.c4;
  }

  public static List<Note> getCommonNotes() {
    if (Note.commonNotes != null)
      return Note.commonNotes;
    Note.commonNotes = new ArrayList<Note>();
    Note.commonNotes.add(new Note("C", 0, Accidental.Natural, -8));
    Note.commonNotes.add(new Note("C", 0, Accidental.Sharp, -7));
    Note.commonNotes.add(new Note("D", 0, Accidental.Flat, -7));
    Note.commonNotes.add(new Note("D", 0, Accidental.Natural, -6));
    Note.commonNotes.add(new Note("D", 0, Accidental.Sharp, -5));
    Note.commonNotes.add(new Note("E", 0, Accidental.Flat, -5));
    Note.commonNotes.add(new Note("E", 0, Accidental.Natural, -4));
    Note.commonNotes.add(new Note("F", 0, Accidental.Natural, -3));
    Note.commonNotes.add(new Note("F", 0, Accidental.Sharp, -2));
    Note.commonNotes.add(new Note("G", 0, Accidental.Flat, -2));
    Note.commonNotes.add(new Note("G", 0, Accidental.Natural, -1));
    Note.commonNotes.add(new Note("G", 0, Accidental.Sharp, 0));
    Note.commonNotes.add(new Note("A", 0, Accidental.Flat, 0));
    Note.commonNotes.add(new Note("A", 0, Accidental.Natural, 1));
    Note.commonNotes.add(new Note("A", 0, Accidental.Sharp, 2));
    Note.commonNotes.add(new Note("B", 0, Accidental.Flat, 2));
    Note.commonNotes.add(new Note("B", 0, Accidental.Natural, 3));

    int originalCount = Note.commonNotes.size();
    for (int octave = 1; octave < 8; octave++)
      for (int i = 0; i < originalCount; i++) {
        Note cur = Note.commonNotes.get(i);
        Note.commonNotes.add(new Note(cur.getFriendlyName(), octave, cur.getAccidental(), cur
            .getFrequency() * Math.pow(2, octave)));
        if (octave == 4
            && Note.commonNotes.get(Note.commonNotes.size() - 1).getFriendlyName().equals("C")
            && Note.commonNotes.get(Note.commonNotes.size() - 1).getAccidental() == Accidental.Natural)
          Note.c4 = Note.commonNotes.get(Note.commonNotes.size() - 1);
      }

    return Note.commonNotes;
  }

  private static double getNoteFrequency(int number) {
    return 440 * Math.pow(2, (number - 49) / 12.0);
  }

  public static List<Note> getPrunedNotes() {
    if (Note.prunedNotes != null)
      return Note.prunedNotes;
    List<Note> notes = new ArrayList<Note>(Note.getCommonNotes());
    for (int x = 1; x < notes.size(); x++) {
      if (notes.get(x).getFrequency() == notes.get(x - 1).getFrequency()) {
        notes.get(x - 1).setAlternate(notes.get(x));
        notes.remove(x);
        x--;
      }
    }
    Note.prunedNotes = notes;
    return Note.prunedNotes;
  }

  private StateField<String> friendlyName = new StateField<>(null);

  private StateField<Integer> octave = new StateField<>(0);

  private StateField<Accidental> accidental = new StateField<>(null);

  private StateField<Double> frequency = new StateField<>(0d);

  private StateField<Integer> keyNumber = new StateField<>(null);

  private StateField<Boolean> isPlaying = new StateField<>(false);

  private StateField<Note> alternate = new StateField<>(null);
  private boolean isAttemptingToPlay;

  private Object synchronizer = new Object();

  public Note() {
  }

  public Note(String friendlyName, int octave, Accidental accidental, double frequency) {
    this.setFriendlyName(friendlyName);
    this.setOctave(octave);
    this.setAccidental(accidental);
    this.setFrequency(frequency);
  }

  public Note(String friendlyName, int octave, Accidental accidental, int keyNumber) {
    this.setFriendlyName(friendlyName);
    this.setOctave(octave);
    this.setAccidental(accidental);
    this.setKeyNumber(keyNumber);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Note))
      return false;
    Note n = (Note) o;
    return n.getFriendlyName().equals(this.getFriendlyName())
        && n.getAccidental() == this.getAccidental() && n.getOctave() == this.getOctave();
  }

  public Accidental getAccidental() {
    return this.accidental.get();
  }

  public Note getAlternate() {
    return this.alternate.get();
  }

  public double getFrequency() {
    return this.frequency.get();
  }

  public String getFriendlyName() {
    return this.friendlyName.get();
  }

  public boolean getIsPlaying() {
    return this.isPlaying.get();
  }

  public Integer getKeyNumber() {
    return this.keyNumber.get();
  }

  public int getOctave() {
    return this.octave.get();
  }

  public void play() {
    synchronized (this.synchronizer) {
      if (this.isAttemptingToPlay)
        return;
      this.isAttemptingToPlay = true;
      player.play(this);
      this.setIsPlaying(true);
      this.isAttemptingToPlay = false;
    }
  }

  public void setAccidental(Accidental value) {
    this.accidental.set(value);
  }

  public void setAlternate(Note value) {
    this.alternate.set(value);
  }

  public void setFrequency(double value) {
    this.frequency.set(value);
  }

  public void setFriendlyName(String value) {
    this.friendlyName.set(value);
  }

  public void setIsPlaying(boolean value) {
    if (this.getIsPlaying() != value) {
      this.isPlaying.set(value);

      if (value) {
        this.play();
      } else {
        this.stop();
      }
    }
  }

  public void setKeyNumber(Integer value) {
    this.keyNumber.set(value);
    if (value != null)
      this.setFrequency(Note.getNoteFrequency(value));
  }

  public void setOctave(int value) {
    this.octave.set(value);
  }

  public void stop() {
    synchronized (this.synchronizer) {
      player.stop(this);
      this.setIsPlaying(false);
    }
  }

  @Override
  public String toString() {
    switch (this.getAccidental()) {
      case Natural:
        return this.getFriendlyName();
      case Sharp:
        return this.getFriendlyName() + "#";
      case Flat:
        return this.getFriendlyName() + "b";
    }
    return this.getFriendlyName();
  }
}
