package depollsoft.pitchperfect;

import com.bindroid.trackable.TrackableCollection;
import com.bindroid.trackable.TrackableField;

import depollsoft.pitchperfect.lib.Note;

public class NoteListModel {
  private TrackableField<TrackableCollection<Note>> notes = new TrackableField<TrackableCollection<Note>>();

  public NoteListModel() {
    this.setNotes(new TrackableCollection<Note>(Note.getPrunedNotes()));
  }

  public TrackableCollection<Note> getNotes() {
    return this.notes.get();
  }

  public void setNotes(TrackableCollection<Note> value) {
    this.notes.set(value);
  }
}
