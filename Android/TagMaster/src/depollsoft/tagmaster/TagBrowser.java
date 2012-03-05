package depollsoft.tagmaster;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost.OnTabChangeListener;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.Activities;
import depollsoft.lib.compat.ui.CompatTabHostWrapper;
import depollsoft.lib.json.JsonSerializer;
import depollsoft.tagmaster.barbershop.TagCollection;
import depollsoft.tagmaster.barbershop.TagSortOptions;

public class TagBrowser extends TabActivity {
  private CompatTabHostWrapper tabHost;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagmasterview);

    this.tabHost = new CompatTabHostWrapper(this, this.getTabHost());

    Intent latest = new Intent(this, TagQueryActivity.class);
    QueryModel latestModel = new QueryModel();
    latestModel.setMaxResults(Integer.MAX_VALUE);
    latestModel.setSortBy(TagSortOptions.Posted);
    latest.putExtra(TagQueryActivity.QUERY_MODEL, JsonSerializer.serialize(latestModel).toString());
    this.tabHost
        .addTab(this.tabHost.newTabSpec("latest").setContent(latest).setIndicator("Latest"));

    Intent rating = new Intent(this, TagQueryActivity.class);
    QueryModel ratingModel = new QueryModel();
    ratingModel.setMaxResults(Integer.MAX_VALUE);
    ratingModel.setSortBy(TagSortOptions.Rating);
    rating.putExtra(TagQueryActivity.QUERY_MODEL, JsonSerializer.serialize(ratingModel).toString());
    this.tabHost
        .addTab(this.tabHost.newTabSpec("rating").setContent(rating).setIndicator("Rating"));

    Intent downloads = new Intent(this, TagQueryActivity.class);
    QueryModel downloadsModel = new QueryModel();
    downloadsModel.setMaxResults(Integer.MAX_VALUE);
    downloadsModel.setSortBy(TagSortOptions.Downloaded);
    downloads.putExtra(TagQueryActivity.QUERY_MODEL, JsonSerializer.serialize(downloadsModel)
        .toString());
    this.tabHost.addTab(this.tabHost.newTabSpec("downloads").setContent(downloads)
        .setIndicator("Downloads"));

    Intent classic = new Intent(this, TagQueryActivity.class);
    QueryModel classicModel = new QueryModel();
    classicModel.setSortBy(TagSortOptions.Classic);
    classicModel.setCollection(TagCollection.ClassicTags);
    classicModel.setMaxResults(400);
    classic.putExtra(TagQueryActivity.QUERY_MODEL, JsonSerializer.serialize(classicModel)
        .toString());
    this.tabHost.addTab(this.tabHost.newTabSpec("classic").setContent(classic)
        .setIndicator("Classic"));

    this.getTabHost().setOnTabChangedListener(new OnTabChangeListener() {

      public void onTabChanged(String tabId) {
        Activities.invalidateOptionsMenu(TagBrowser.this);
      }
    });

    ActionBars.setCustomTitle(this, R.layout.titleview);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return this.getLocalActivityManager().getCurrentActivity().onCreateOptionsMenu(menu);
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
  protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);
    this.tabHost.restoreInstanceState("tabs", state);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    this.tabHost.saveInstanceState("tabs", outState);
  }
}