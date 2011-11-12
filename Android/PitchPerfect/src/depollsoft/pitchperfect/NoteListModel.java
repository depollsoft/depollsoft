package depollsoft.pitchperfect;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.TrackableField;
import depollsoft.pitchperfect.lib.Note;

public class NoteListModel
{
   private TrackableField<ObservableCollection<Note>> notes = new TrackableField<ObservableCollection<Note>>();

   public NoteListModel()
   {
      this.setNotes(new ObservableCollection<Note>(Note.getPrunedNotes()));
   }

   public ObservableCollection<Note> getNotes()
   {
      return this.notes.getValue();
   }

   public void setNotes(ObservableCollection<Note> value)
   {
      this.notes.setValue(value);
   }
}
