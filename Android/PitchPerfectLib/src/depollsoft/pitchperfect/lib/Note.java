package depollsoft.pitchperfect.lib;

import java.util.ArrayList;
import java.util.List;

import android.media.AudioFormat;
import android.media.AudioTrack;

import depollsoft.lib.binding.TrackableField;

public class Note
{
   private static List<Note> commonNotes;

   private static List<Note> prunedNotes;

   private static Note c4;

   public static Note findNote(String name, Accidental accidental, int octave)
   {
      for (Note n : Note.getCommonNotes())
      {
         if (n.getAccidental() == accidental && n.getOctave() == octave
               && n.getFriendlyName().equals(name))
            return n;
      }
      return null;
   }

   public static Note getC4()
   {
      if (Note.c4 == null)
         Note.getCommonNotes();
      return Note.c4;
   }

   public static List<Note> getCommonNotes()
   {
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
         for (int i = 0; i < originalCount; i++)
         {
            Note cur = Note.commonNotes.get(i);
            Note.commonNotes.add(new Note(cur.getFriendlyName(), octave, cur
                  .getAccidental(), cur.getFrequency() * Math.pow(2, octave)));
            if (octave == 4
                  && Note.commonNotes.get(Note.commonNotes.size() - 1)
                        .getFriendlyName().equals("C")
                  && Note.commonNotes.get(Note.commonNotes.size() - 1)
                        .getAccidental() == Accidental.Natural)
               Note.c4 = Note.commonNotes.get(Note.commonNotes.size() - 1);
         }

      return Note.commonNotes;
   }

   private static double getNoteFrequency(int number)
   {
      return 440 * Math.pow(2, (number - 49) / 12.0);
   }

   public static List<Note> getPrunedNotes()
   {
      if (Note.prunedNotes != null)
         return Note.prunedNotes;
      List<Note> notes = new ArrayList<Note>(Note.getCommonNotes());
      for (int x = 1; x < notes.size(); x++)
      {
         if (notes.get(x).getFrequency() == notes.get(x - 1).getFrequency())
         {
            notes.get(x - 1).setAlternate(notes.get(x));
            notes.remove(x);
            x--;
         }
      }
      Note.prunedNotes = notes;
      return Note.prunedNotes;
   }

   private AudioTrack track;

   private TrackableField<String> friendlyName = new TrackableField<String>();

   private TrackableField<Integer> octave = new TrackableField<Integer>(0);

   private TrackableField<Accidental> accidental = new TrackableField<Accidental>();

   private TrackableField<Double> frequency = new TrackableField<Double>(0d);

   private TrackableField<Integer> keyNumber = new TrackableField<Integer>();

   private TrackableField<Boolean> isPlaying = new TrackableField<Boolean>(
         false);

   private TrackableField<Note> alternate = new TrackableField<Note>();
   private boolean isAttemptingToPlay;

   private Object synchronizer = new Object();

   public Note()
   {
   }

   public Note(String friendlyName, int octave, Accidental accidental,
         double frequency)
   {
      this.setFriendlyName(friendlyName);
      this.setOctave(octave);
      this.setAccidental(accidental);
      this.setFrequency(frequency);
   }

   public Note(String friendlyName, int octave, Accidental accidental,
         int keyNumber)
   {
      this.setFriendlyName(friendlyName);
      this.setOctave(octave);
      this.setAccidental(accidental);
      this.setKeyNumber(keyNumber);
   }

   @Override
   public boolean equals(Object o)
   {
      if (!(o instanceof Note))
         return false;
      Note n = (Note) o;
      return n.getFriendlyName().equals(this.getFriendlyName())
            && n.getAccidental() == this.getAccidental()
            && n.getOctave() == this.getOctave();
   }

   public Accidental getAccidental()
   {
      return this.accidental.getValue();
   }

   public Note getAlternate()
   {
      return this.alternate.getValue();
   }

   public double getFrequency()
   {
      return this.frequency.getValue();
   }

   public String getFriendlyName()
   {
      return this.friendlyName.getValue();
   }

   public boolean getIsPlaying()
   {
      return this.isPlaying.getValue();
   }

   public Integer getKeyNumber()
   {
      return this.keyNumber.getValue();
   }

   public int getOctave()
   {
      return this.octave.getValue();
   }

   public void play()
   {
      synchronized (this.synchronizer)
      {
         if (this.isAttemptingToPlay)
            return;
         this.isAttemptingToPlay = true;
         if (this.track == null)
            this.track = PitchAudioTrackGenerator.getPitchAudioTrack(
                  this.getFrequency(), 8000,
                  AudioFormat.CHANNEL_CONFIGURATION_MONO, 2000);
         this.track.play();
         this.setIsPlaying(true);
         this.isAttemptingToPlay = false;
      }
   }

   public void setAccidental(Accidental value)
   {
      this.accidental.setValue(value);
   }

   public void setAlternate(Note value)
   {
      this.alternate.setValue(value);
   }

   public void setFrequency(double value)
   {
      this.frequency.setValue(value);
   }

   public void setFriendlyName(String value)
   {
      this.friendlyName.setValue(value);
   }

   public void setIsPlaying(boolean value)
   {
      if (value)
         this.play();
      else
         this.stop();
      this.isPlaying.setValue(value);
   }

   public void setKeyNumber(Integer value)
   {
      this.keyNumber.setValue(value);
      if (value != null)
         this.setFrequency(Note.getNoteFrequency(value));
   }

   public void setOctave(int value)
   {
      this.octave.setValue(value);
   }

   public void stop()
   {
      synchronized (this.synchronizer)
      {
         if (this.track != null && !this.isAttemptingToPlay
               && this.track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
         {
            PitchAudioTrackGenerator.stop(this.track);
            this.track = null;
            this.setIsPlaying(false);
         }
      }
   }

   @Override
   public String toString()
   {
      switch (this.getAccidental())
      {
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
