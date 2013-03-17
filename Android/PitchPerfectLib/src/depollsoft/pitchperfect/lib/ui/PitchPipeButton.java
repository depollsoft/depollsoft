package depollsoft.pitchperfect.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

import com.bindroid.BindingMode;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.ReflectedProperty;

import depollsoft.pitchperfect.lib.Note;

public class PitchPipeButton extends Button {

  private TrackableField<Note> note = new TrackableField<Note>();

  private TrackableField<Boolean> isToggle = new TrackableField<Boolean>(false);

  public PitchPipeButton(Context context) {
    super(context);
  }

  public PitchPipeButton(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public PitchPipeButton(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public boolean getIsToggle() {
    return this.isToggle.get();
  }

  public Note getNote() {
    return this.note.get();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    UiBinder.bind(new ReflectedProperty(this, "Pressed"), new ReflectedProperty(this,
        "Note.IsPlaying"), BindingMode.ONE_WAY);
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
        if (this.getIsToggle()) {
          this.getNote().setIsPlaying(!this.getNote().getIsPlaying());
          return true;
        } else
          this.getNote().play();
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_OUTSIDE:
      case MotionEvent.ACTION_CANCEL:
        if (!this.getIsToggle())
          this.getNote().stop();
        else
          return true;
        break;
      }
    }
    return super.onTouchEvent(event);
  }

  // @Override
  // public boolean onKeyDown(int keyCode, KeyEvent event)
  // {
  // if (this.getNote() != null)
  // this.getNote().play();
  // return super.onKeyDown(keyCode, event);
  // }
  //
  // @Override
  // public boolean onKeyUp(int keyCode, KeyEvent event)
  // {
  // if (this.getNote() != null)
  // this.getNote().stop();
  // return super.onKeyUp(keyCode, event);
  // }

  public void setIsToggle(boolean value) {
    this.isToggle.set(value);
  }

  public void setNote(Note value) {
    this.note.set(value);
  }

}
