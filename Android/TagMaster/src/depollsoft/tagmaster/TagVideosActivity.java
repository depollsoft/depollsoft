package depollsoft.tagmaster;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.bindroid.converters.AdapterConverter;
import com.bindroid.converters.BoolConverter;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;

public class TagVideosActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagvideosview);

    UiBinder.bind(this, R.id.videoList, "Adapter", "Parent.Tag.Videos", new AdapterConverter(
        VideoDisplay.class));

    UiBinder.bind(this, R.id.teachingVideoRow, "Visibility", "Parent.Tag.TeachingVideo",
        BoolConverter.get());

    UiBinder.bind(this, R.id.sorryTextView, "Visibility", "Parent.Tag.Videos",
        BoolConverter.get(true, true));

    UiBinder.bind(this, R.id.teachingVideoDisplay, "Tag", "Parent.Tag");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (this.getParent() != null)
      return this.getParent().onMenuItemSelected(featureId, item);
    return false;
  }

  @Override
  protected void onStart() {
    super.onStart();
    FlurryAgent.onStartSession(this, "V5L1948BNDQCKZFPARJ9");
  }

  @Override
  protected void onStop() {
    super.onStop();
    FlurryAgent.onEndSession(this);
  }
}
