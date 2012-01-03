package depollsoft.pitchperfect;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public class SongKeySignatureListItemView extends KeySignatureListItemView {

  public SongKeySignatureListItemView(Context context) {
    super(context);
  }

  public SongKeySignatureListItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  protected void init() {
    View.inflate(this.getContext(), R.layout.songkeysignatureitemview, this);
    this.setBackgroundDrawable(new ListView(this.getContext()).getSelector());
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return false;
  }

}
