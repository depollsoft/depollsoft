package depollsoft.pitchperfect;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.util.Preferences;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class PitchPipeModel {
  private static final String PitchPipeModelKey = "depollsoft.pitchperfect.PitchPipeModel";
  private ObservableCollection<Note> cToC;
  private ObservableCollection<Note> fToF;

  private TrackableField<ObservableCollection<Note>> notes = new TrackableField<ObservableCollection<Note>>();

  public PitchPipeModel() {
    this.cToC = new ObservableCollection<Note>();
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

    this.fToF = new ObservableCollection<Note>();
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

    Preferences.initialize(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF",
        false);
    if (this.getIsFromFToF())
      this.setNotes(this.fToF);
    else
      this.setNotes(this.cToC);
  }

  public boolean getIsFromFToF() {
    return Preferences.get(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF");
  }

  public ObservableCollection<Note> getNotes() {
    return this.notes.getValue();
  }

  public void setIsFromFToF(boolean value) {
    Preferences.set(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF", value);
    if (value)
      this.setNotes(this.fToF);
    else
      this.setNotes(this.cToC);
  }

  public void setNotes(ObservableCollection<Note> value) {
    this.notes.setValue(value);
  }
}
