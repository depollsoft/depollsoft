package depollsoft.pitchperfect;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

public class SongKeySignatureSelectedItemView extends
    SongKeySignatureListItemView {

  public SongKeySignatureSelectedItemView(Context context) {
    super(context);
  }

  public SongKeySignatureSelectedItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void init() {
    View.inflate(this.getContext(), R.layout.songkeysignatureselecteditemview,
        this);
    this.setBackgroundDrawable(new ListView(this.getContext()).getSelector());
  }

}
