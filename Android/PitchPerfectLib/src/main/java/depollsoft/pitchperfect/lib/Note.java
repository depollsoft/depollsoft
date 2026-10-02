package depollsoft.pitchperfect.lib;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import depollsoft.lib.json.NotStored;
import depollsoft.lib.state.StateField;
import depollsoft.pitchperfect.lib.sound.NoteSound;
import depollsoft.pitchperfect.lib.sound.NoteVoices;
import depollsoft.pitchperfect.lib.sound.SoundingNote;

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
    private WeakHashMap<Note, SoundingNote> voices = new WeakHashMap<Note, SoundingNote>();

    @Override
    public void play(Note n) {
      SoundingNote voice = voices.get(n);
      if (voice == null) {
        voice = NoteVoices.create(Note.getSound(), n.getFrequency(), Note.getReferencePitch(),
            () -> new Handler(Looper.getMainLooper()).post(() -> unlightIfSilent(n)));
        voices.put(n, voice);
      }
      try {
        voice.play();
      } catch (IllegalStateException e) {
        // The next press asks for a new voice rather than this one's dead track.
        voices.remove(n);
        throw e;
      }
    }

    /**
     * The voice gave up after play() returned (an instrument with no audio track to fall back on):
     * unlight the note, unless it has been played again since, so the next press plays it.
     */
    private void unlightIfSilent(Note n) {
      SoundingNote voice = voices.get(n);
      if (n.getIsPlaying() && (voice == null || !voice.isSounding()))
        n.setIsPlaying(false);
    }

    @Override
    public void stop(Note n) {
      SoundingNote voice = voices.get(n);
      if (voice != null && !n.isAttemptingToPlay && voice.isSounding()) {
        voice.stop();
        voices.remove(n);
      }
    }
  };

  private static NotePlayer player = DEFAULT_PLAYER;

  /** Standard concert pitch: the A4 every note's stored frequency is relative to. */
  public static final double STANDARD_A4 = 440;

  /** Common choices for A4, in Hz: historical, standard and orchestral pitches. */
  public static final int[] COMMON_A4_FREQUENCIES = {415, 430, 432, 435, 438, 440, 441, 442, 443, 444, 446};

  // Snapshot state, so a screen showing tuned frequencies redraws when the tuning changes.
  private static final StateField<Double> referencePitch = new StateField<>(STANDARD_A4);

  /** The A4 notes sound at, in Hz. */
  public static double getReferencePitch() {
    return Note.referencePitch.get();
  }

  /** Tunes every note to {@code value} Hz for A4; a note already sounding keeps its pitch until played again. */
  public static void setReferencePitch(double value) {
    Note.referencePitch.set(value);
  }

  // Snapshot state, so a screen showing the chosen sound redraws when it changes.
  private static final StateField<NoteSound> sound = new StateField<>(NoteSound.DEFAULT);

  /** The voice notes sound in. */
  public static NoteSound getSound() {
    return Note.sound.get();
  }

  /**
   * Voices every note in {@code value}; a note already sounding keeps its voice until played again.
   * An instrument's samples are made ready in the background.
   */
  public static void setSound(NoteSound value) {
    Note.sound.set(value);
    NoteVoices.prepare(value);
  }

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

  private String friendlyName = null;

  private Integer octave = 0;

  private Accidental accidental = null;

  private Double frequency = 0d;

  private Integer keyNumber = null;

  private StateField<Boolean> isPlaying = new StateField<>(false);

  private Note alternate = null;
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
    return this.accidental;
  }

  public Note getAlternate() {
    return this.alternate;
  }

  /** The note's frequency at A4 = 440 Hz, as stored with a song. */
  public double getFrequency() {
    return this.frequency;
  }

  /** The frequency the note sounds at, tuned to the chosen A4 (see {@link #setReferencePitch}). */
  public double getTunedFrequency() {
    return this.frequency * Note.getReferencePitch() / STANDARD_A4;
  }

  public String getFriendlyName() {
    return this.friendlyName;
  }

  // Whether the note sounds right now: never stored, or loading a saved song would start it.
  @NotStored
  public boolean getIsPlaying() {
    return this.isPlaying.get();
  }

  public Integer getKeyNumber() {
    return this.keyNumber;
  }

  public int getOctave() {
    return this.octave;
  }

  public void play() {
    synchronized (this.synchronizer) {
      if (this.isAttemptingToPlay)
        return;
      this.isAttemptingToPlay = true;
      try {
        player.play(this);
        this.setIsPlaying(true);
      } catch (IllegalStateException e) {
        // No audio track to be had right now (see PitchAudioTrackGenerator). The note stays
        // silent and unlit, so the next press tries again instead of "stopping" it.
        Log.w("Note", "Couldn't play " + this, e);
        this.isPlaying.set(false);
      } finally {
        this.isAttemptingToPlay = false;
      }
    }
  }

  public void setAccidental(Accidental value) {
    this.accidental = value;
  }

  public void setAlternate(Note value) {
    this.alternate = value;
  }

  public void setFrequency(double value) {
    this.frequency = value;
  }

  public void setFriendlyName(String value) {
    this.friendlyName = value;
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
    this.keyNumber = value;
    if (value != null)
      this.setFrequency(Note.getNoteFrequency(value));
  }

  public void setOctave(int value) {
    this.octave = value;
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
