package depollsoft.tagmaster;

import java.net.URLEncoder;

import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.json.JsonSerializer;
import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.widget.*;

public class TagSearchResultsActivity extends ActivityGroup {

  private TrackableField<QueryModel> model = new TrackableField<QueryModel>();

  public TagSearchResultsActivity() {
  }

  public QueryModel getModel() {
    return this.model.getValue();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagsearchresultsview);

    UiBinder.bind(this, R.id.queryTitleTextView, "Text", "Model.Query");

    this.setModel((QueryModel) JsonSerializer.deserialize(this.getIntent().getStringExtra(
        TagQueryActivity.QUERY_MODEL)));

    Intent queryActivity = new Intent(this, TagQueryActivity.class);
    queryActivity.putExtras(this.getIntent().getExtras());
    Window w = this.getLocalActivityManager().startActivity("query", queryActivity);

    ((FrameLayout) this.findViewById(R.id.contentFrame)).addView(w.getDecorView());

    ActionBars.setCustomTitle(this, R.layout.titleview);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == ActionBars.HOME_MENU_ITEM_ID) {
      Intent intent = new Intent(this, MeActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      this.startActivity(intent);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().trackPageView(
        "TagSearchResultsActivity/"
            + URLEncoder.encode(this.getIntent().getStringExtra(TagQueryActivity.QUERY_MODEL)));
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

  public void setModel(QueryModel value) {
    this.model.setValue(value);
  }

}
