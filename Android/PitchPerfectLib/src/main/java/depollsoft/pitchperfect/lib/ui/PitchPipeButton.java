package depollsoft.pitchperfect.lib.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

import com.bindroid.BindingMode;
import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.ReflectedProperty;

import depollsoft.pitchperfect.lib.Note;

public class PitchPipeButton extends Button {

  private TrackableField<Note> note = new TrackableField<Note>();

  private TrackableField<Boolean> isToggle = new TrackableField<Boolean>(false);

  private boolean touchInProgress;
  private Note clickNote;
  // Only stop notes activated by this button, not a note sounding elsewhere.
  private Note activeNote;
  private final Runnable clearTouchState = new Runnable() {
    @Override public void run() { touchInProgress = false; }
  };
  private final Runnable stopClickNote = new Runnable() {
    @Override public void run() {
      if (clickNote != null) {
        clickNote.stop();
        if (activeNote == clickNote) activeNote = null;
        clickNote = null;
      }
    }
  };
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
        "Note.IsPlaying"), BindingMode.ONE_WAY, BoolConverter.get());
  }

  @Override
  protected void onDetachedFromWindow() {
    removeCallbacks(stopClickNote);
    removeCallbacks(clearTouchState);
    stopActiveNote();
    touchInProgress = false;
    super.onDetachedFromWindow();
  }

  @Override
  public boolean performClick() {
    boolean activated = isEnabled() && getNote() != null && !touchInProgress;
    if (activated) {
      if (getIsToggle()) {
        toggleNote();
      } else {
        stopActiveNote();
        clickNote = activeNote = getNote();
        clickNote.play();
        postDelayed(stopClickNote, 1500);
      }
    }
    return super.performClick() || activated;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!isEnabled()) return super.onTouchEvent(event);
    final int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN) {
      removeCallbacks(clearTouchState);
      removeCallbacks(stopClickNote);
      stopClickNote.run();
      touchInProgress = true;
    }
    final boolean ending = action == MotionEvent.ACTION_UP
        || action == MotionEvent.ACTION_OUTSIDE || action == MotionEvent.ACTION_CANCEL;
    boolean handledByToggle = false;
    if (getNote() != null) {
      switch (action) {
      case MotionEvent.ACTION_DOWN:
        if (getIsToggle()) {
          toggleNote();
          handledByToggle = true;
        } else {
          activeNote = getNote();
          activeNote.play();
        }
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_OUTSIDE:
      case MotionEvent.ACTION_CANCEL:
        if (!getIsToggle()) stopActiveNote();
        else handledByToggle = true;
        break;
      }
    }
    boolean handled = handledByToggle || super.onTouchEvent(event);
    // View may post performClick from ACTION_UP. Keep the flag through that queued click.
    if (ending) post(clearTouchState);
    return handled;
  }

  public void setIsToggle(boolean value) {
    this.isToggle.set(value);
  }

  private void toggleNote() {
    Note n = getNote();
    boolean play = !n.getIsPlaying();
    n.setIsPlaying(play);
    activeNote = play ? n : null;
  }

  private void stopActiveNote() {
    removeCallbacks(stopClickNote);
    if (activeNote != null) activeNote.stop();
    activeNote = null;
    clickNote = null;
  }

  public void setNote(Note value) {
    if (getNote() != value) stopActiveNote();
    this.note.set(value);
    this.updateAccessibilityDescription();
  }

  private void updateAccessibilityDescription() {
    Note n = this.getNote();
    if (n == null) {
      this.setContentDescription(null);
      return;
    }
    StringBuilder description = new StringBuilder();
    if (n.getAccidental() == depollsoft.pitchperfect.lib.Accidental.Natural) {
      description.append(n.getFriendlyName());
    } else if (n.getAccidental() == depollsoft.pitchperfect.lib.Accidental.Sharp) {
      description.append(n.getFriendlyName()).append(" sharp");
      if (n.getAlternate() != null) {
        description.append(", ").append(n.getAlternate().getFriendlyName()).append(" flat");
      }
    } else {
      description.append(n.getFriendlyName()).append(" flat");
      if (n.getAlternate() != null) {
        description.append(", ").append(n.getAlternate().getFriendlyName()).append(" sharp");
      }
    }
    description.append(", octave ").append(n.getOctave());
    this.setContentDescription(description.toString());
  }

}
