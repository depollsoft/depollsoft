package depollsoft.tagmaster;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.ui.AdapterConverter;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

public class TagVideosActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagvideosview);

    UiBinder.bind(this, R.id.videoList, "Adapter", "Parent.Tag.Videos",
        new AdapterConverter(VideoDisplay.class));

    UiBinder.bind(this, R.id.teachingVideoRow, "Visibility",
        "Parent.Tag.TeachingVideo", BoolConverter.get());

    UiBinder.bind(this, R.id.sorryTextView, "Visibility", "Parent.Tag.Videos",
        BoolConverter.get(true, true));

    UiBinder.bind(this, R.id.teachingVideoDisplay, "Tag", "Parent.Tag");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (this.getParent() != null)
      return this.getParent().onMenuItemSelected(featureId, item);
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.runOnUiThread(new Runnable() {
      public void run() {
        GoogleAnalyticsTracker.getInstance().trackPageView(
            "TagDetailActivity/"
                + ((TagDetailActivity) TagVideosActivity.this.getParent())
                    .getTag().getId() + "/videos");
      }
    });
  }
}
