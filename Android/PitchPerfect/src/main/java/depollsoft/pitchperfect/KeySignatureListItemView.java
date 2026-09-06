package depollsoft.pitchperfect;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bindroid.BindingMode;
import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.BoundUi;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.ReflectedProperty;

import depollsoft.pitchperfect.converters.KeyNameConverter;
import depollsoft.pitchperfect.converters.KeySignatureConverter;
import depollsoft.pitchperfect.lib.Key;

public class KeySignatureListItemView extends LinearLayout implements BoundUi<Key> {
  private TrackableField<Key> key = new TrackableField<Key>();

  public KeySignatureListItemView(Context context) {
    super(context);
    this.init();
  }

  public KeySignatureListItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  @Override
  public void bind(Key dataSource) {
    this.setKey(dataSource);
  }

  public Key getKey() {
    return this.key.get();
  }

  protected void init() {
    View.inflate(this.getContext(), R.layout.keysignatureitemview, this);
    this.setBackgroundDrawable(this.getContext().getDrawable(R.drawable.row_lit));
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    UiBinder.bind(this, R.id.keySignatureTextView, "Text", "Key", new KeySignatureConverter());

    UiBinder.bind(this, R.id.keyNameTextView, "Text", "Key", new KeyNameConverter());

    UiBinder.bind(this, new ReflectedProperty(this, "Pressed"), "Key.Note.IsPlaying",
        BindingMode.ONE_WAY, BoolConverter.get());
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (this.getKey() != null) {
      switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (SettingsModel.getToggleNotes())
          this.getKey().getNote().setIsPlaying(!this.getKey().getNote().getIsPlaying());
        else
          this.getKey().getNote().play();
        this.setPressed(this.getKey().getNote().getIsPlaying());
        return true;
      case MotionEvent.ACTION_MOVE:
        if (SettingsModel.getToggleNotes() && event.getEventTime() - event.getDownTime() > 100)
          this.getKey().getNote().stop();
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_OUTSIDE:
      case MotionEvent.ACTION_CANCEL:
        if (!SettingsModel.getToggleNotes())
          this.getKey().getNote().stop();
        this.setPressed(this.getKey().getNote().getIsPlaying());
        return true;
      }
    }
    return super.onTouchEvent(event);
  }

  public void setKey(Key value) {
    this.key.set(value);
  }

}
