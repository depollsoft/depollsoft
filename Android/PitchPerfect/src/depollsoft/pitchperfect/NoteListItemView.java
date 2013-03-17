package depollsoft.pitchperfect;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bindroid.BindingMode;
import com.bindroid.converters.ToStringConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.BoundUi;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.ReflectedProperty;

import depollsoft.pitchperfect.converters.NoteListNoteTextConverter;
import depollsoft.pitchperfect.lib.Note;

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
    return this.note.get();
  }

  private void init() {
    View.inflate(this.getContext(), R.layout.notelistitemview, this);
    this.setBackgroundDrawable(new ListView(this.getContext()).getSelector());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    UiBinder.bind(this, R.id.noteNameTextView, "Text", "Note", new NoteListNoteTextConverter());
    UiBinder.bind(this, R.id.noteFrequencyTextView, "Text", "Note.Frequency",
        new ToStringConverter("%1.2f Hz"));

    UiBinder.bind(this, new ReflectedProperty(this, "Pressed"), "Note.IsPlaying",
        BindingMode.ONE_WAY);
  }

  @Override
  protected void onDetachedFromWindow() {
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
        if (SettingsModel.getToggleNotes() && event.getEventTime() - event.getDownTime() > 100)
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
    this.note.set(value);
  }

}
