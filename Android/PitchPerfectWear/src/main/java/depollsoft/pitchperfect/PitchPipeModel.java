package depollsoft.pitchperfect;

import depollsoft.lib.state.StateField;
import depollsoft.lib.state.StateList;

import depollsoft.lib.util.Preferences;
import depollsoft.pitchperfect.lib.Accidental;
import depollsoft.pitchperfect.lib.Note;

public class PitchPipeModel {
  private static final String PitchPipeModelKey = "depollsoft.pitchperfect.PitchPipeModel";
  private StateList<Note> cToC;
  private StateList<Note> fToF;

  private final StateField<StateList<Note>> notes = new StateField<>(null);

  // Mirrors the stored choice so the face redraws the selector when it changes.
  private final StateField<Boolean> isFromFToF = new StateField<>(false);

  public PitchPipeModel() {
    this.cToC = new StateList<Note>();
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

    this.fToF = new StateList<Note>();
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
    this.isFromFToF.set(Preferences.<Boolean>get(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF"));
    if (this.getIsFromFToF())
      this.setNotes(this.fToF);
    else
      this.setNotes(this.cToC);
  }

  public boolean getIsFromFToF() {
    return this.isFromFToF.get();
  }

  public StateList<Note> getNotes() {
    return this.notes.get();
  }

  public void setIsFromFToF(boolean value) {
    Preferences.set(PitchPipeModel.PitchPipeModelKey + ".IsFromFToF", value);
    this.isFromFToF.set(value);
    if (value)
      this.setNotes(this.fToF);
    else
      this.setNotes(this.cToC);
  }

  public void setNotes(StateList<Note> value) {
    this.notes.set(value);
  }
}
