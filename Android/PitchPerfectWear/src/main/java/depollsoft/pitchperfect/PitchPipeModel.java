package depollsoft.pitchperfect;

import com.bindroid.trackable.TrackableCollection;
import com.bindroid.trackable.TrackableField;

import depollsoft.lib.util.Preferences;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class PitchPipeModel {
  private static final String PitchPipeModelKey = "depollsoft.pitchperfect.PitchPipeModel";
  private TrackableCollection<Note> cToC;
  private TrackableCollection<Note> fToF;

  private TrackableField<TrackableCollection<Note>> notes = new TrackableField<TrackableCollection<Note>>();

  public PitchPipeModel() {
    this.cToC = new TrackableCollection<Note>();
    this.cToC.add(Note.findNote("C", Accidental.Natural, 4));
    this.cToC.add(Note.findNote("C", Accidental.Sharp, 4));
    this.cToC.add(Note.findNote("D", Accidental.Natural, 4));
    this.cToC.add(Note.findNote("D", Accidental.Sharp, 4));
    this.cToC.add(Note.findNote("E", Accidental.Natural, 4));
    this.cToC.add(Note.findNote("F", Accidental.Natural, 4));
    this.cToC.add(Note.findNote("F", Accidental.Sharp, 4));
    this.cToC.add(Note.findNote("G", Accidental.Natural, 4));
    this.cToC.add(Note.findNote("G", Accidental.Sharp, 4));
    this.cToC.add(Note.findNote("A", Accidental.Natural, 4));
    this.cToC.add(Note.findNote("A", Accidental.Sharp, 4));
    this.cToC.add(Note.findNote("B", Accidental.Natural, 4));
    this.cToC.add(Note.findNote("C", Accidental.Natural, 5));

    this.fToF = new TrackableCollection<Note>();
    this.fToF.add(Note.findNote("F", Accidental.Natural, 4));
    this.fToF.add(Note.findNote("F", Accidental.Sharp, 4));
    this.fToF.add(Note.findNote("G", Accidental.Natural, 4));
    this.fToF.add(Note.findNote("G", Accidental.Sharp, 4));
    this.fToF.add(Note.findNote("A", Accidental.Natural, 4));
    this.fToF.add(Note.findNote("A", Accidental.Sharp, 4));
    this.fToF.add(Note.findNote("B", Accidental.Natural, 4));
    this.fToF.add(Note.findNote("C", Accidental.Natural, 5));
    this.fToF.add(Note.findNote("C", Accidental.Sharp, 5));
    this.fToF.add(Note.findNote("D", Accidental.Natural, 5));
    this.fToF.add(Note.findNote("D", Accidental.Sharp, 5));
    this.fToF.add(Note.findNote("E", Accidental.Natural, 5));
    this.fToF.add(Note.findNote("F", Accidental.Natural, 5));

    Preferences.initialize(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF", false);
    if (this.getIsFromFToF())
      this.setNotes(this.fToF);
    else
      this.setNotes(this.cToC);
  }

  public boolean getIsFromFToF() {
    return Preferences.<Boolean>get(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF");
  }

  public TrackableCollection<Note> getNotes() {
    return this.notes.get();
  }

  public void setIsFromFToF(boolean value) {
    Preferences.set(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF", value);
    if (value)
      this.setNotes(this.fToF);
    else
      this.setNotes(this.cToC);
  }

  public void setNotes(TrackableCollection<Note> value) {
    this.notes.set(value);
  }
}
