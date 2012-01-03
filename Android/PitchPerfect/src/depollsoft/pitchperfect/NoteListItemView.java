package depollsoft.pitchperfect;

import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.ToStringConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.ui.BoundUi;
import depollsoft.lib.util.ReflectedProperty;
import depollsoft.pitchperfect.converters.NoteListNoteTextConverter;
import depollsoft.pitchperfect.lib.Note;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

public class NoteListItemView extends LinearLayout implements BoundUi<Note> {
  private TrackableField<Note> note = new TrackableField<Note>();

  public NoteListItemView(Context context) {
    super(context);
    this.init();
  }

  public NoteListItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  @Override
  public void bind(Note dataSource) {
    this.setNote(dataSource);
  }

  public Note getNote() {
    return this.note.getValue();
  }

  private void init() {
    View.inflate(this.getContext(), R.layout.notelistitemview, this);
    this.setBackgroundDrawable(new ListView(this.getContext()).getSelector());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    UiBinder.bind(this, R.id.noteNameTextView, "Text", "Note",
        new NoteListNoteTextConverter());
    UiBinder.bind(this, R.id.noteFrequencyTextView, "Text", "Note.Frequency",
        new ToStringConverter("%1.2f Hz"));

    UiBinder.bind(this, new ReflectedProperty(this, "Pressed"),
        "Note.IsPlaying", BindingMode.OneWay);
  }

  @Override
  protected void onDetachedFromWindow() {
    UiBinder.unbind(this);
    super.onDetachedFromWindow();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (this.getNote() != null) {
      switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (SettingsModel.getToggleNotes())
          this.getNote().setIsPlaying(!this.getNote().getIsPlaying());
        else
          this.getNote().play();
        this.setPressed(this.getNote().getIsPlaying());
        return true;
      case MotionEvent.ACTION_MOVE:
        if (SettingsModel.getToggleNotes()
            && event.getEventTime() - event.getDownTime() > 100)
          this.getNote().stop();
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_OUTSIDE:
      case MotionEvent.ACTION_CANCEL:
        if (!SettingsModel.getToggleNotes())
          this.getNote().stop();
        this.setPressed(this.getNote().getIsPlaying());
        return true;
      }
    }
    return super.onTouchEvent(event);
  }

  public void setNote(Note value) {
    this.note.setValue(value);
  }

}
