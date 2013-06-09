package depollsoft.tagmaster;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

import com.bindroid.converters.BoolConverter;
import com.bindroid.converters.ToStringConverter;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;

public class TagMiscActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagmiscview);

    UiBinder.bind(this, R.id.titleTextView, "Text", "Parent.Tag.Title");

    UiBinder.bind(this, R.id.tagIdTextView, "Text", "Parent.Tag.Id", new ToStringConverter());

    UiBinder.bind(this, R.id.lastRefreshedTextView, "Text", "Parent.Tag.LastRefreshed",
        new ToStringConverter("%1$tD %1$tr"));

    UiBinder.bind(this, R.id.downloadsTextView, "Text", "Parent.Tag.DownloadCount",
        new ToStringConverter());

    UiBinder.bind(this, R.id.linkHyperlink, "HyperlinkUri", "Parent.Tag.TagUri");

    UiBinder.bind(this, R.id.postedByTextView, "Text", "Parent.Tag.Provider");
    UiBinder.bind(this, R.id.postedByTextView, "HyperlinkUri", "Parent.Tag.ProviderWebsite");
    UiBinder.bind(this, R.id.postedByRow, "Visibility", "Parent.Tag.Provider", BoolConverter.get());

    UiBinder.bind(this, R.id.postedTextView, "Text", "Parent.Tag.Posted", new ToStringConverter(
        "%1$tA, %1$tB %1$te, %1$tY"));

    UiBinder.bind(this, R.id.arrangedByTextView, "Text", "Parent.Tag.Arranger");
    UiBinder.bind(this, R.id.arrangedByTextView, "HyperlinkUri", "ArrangerWebsite");
    UiBinder.bind(this, R.id.arrangedByRow, "Visibility", "Parent.Tag.Arranger",
        BoolConverter.get());

    UiBinder.bind(this, R.id.yearArrangedTextView, "Text", "Parent.Tag.YearArranged",
        new ToStringConverter());
    UiBinder.bind(this, R.id.yearArrangedRow, "Visibility", "Parent.Tag.YearArranged",
        BoolConverter.get());

    UiBinder.bind(this, R.id.sungByTextView, "Text", "Parent.Tag.SungBy");
    UiBinder.bind(this, R.id.sungByTextView, "HyperlinkUri", "Parent.Tag.SungByWebsite");
    UiBinder.bind(this, R.id.sungByRow, "Visibility", "Parent.Tag.SungBy", BoolConverter.get());

    UiBinder.bind(this, R.id.yearSungTextView, "Text", "Parent.Tag.SungYear",
        new ToStringConverter());
    UiBinder.bind(this, R.id.yearSungRow, "Visibility", "Parent.Tag.SungYear", BoolConverter.get());

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
