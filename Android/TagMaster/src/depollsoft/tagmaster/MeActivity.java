package depollsoft.tagmaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.ui.AdapterConverter;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.MenuItems;

public class MeActivity extends Activity {
  public MeActivity() {
  }

  public ObservableCollection<Integer> getFavoriteIds() {
    return FavoritesModel.getFavoriteIds();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.meview);

    UiBinder.bind(this, R.id.favoritesItemsControl, "Adapter", "FavoriteIds", new AdapterConverter(
        FavoriteTagItemView.class, false, true));

    UiBinder.bind(this, R.id.titleLayout, "Visibility", "HasActionBar", new BoolConverter(true));
    
    ActionBars.setCustomTitle(this, R.layout.titleview);
  }

  public boolean getHasActionBar() {
    return ActionBars.hasActionBar(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    this.getMenuInflater().inflate(R.menu.memenu, menu);

    MenuItems
        .setShowAsAction(menu.findItem(R.id.settingsMenuItem), MenuItems.SHOW_AS_ACTION_ALWAYS);
    return true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.settingsMenuItem) {
      Intent i = new Intent(this, SettingsActivity.class);
      this.startActivity(i);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().setCustomVar(2, "NumFavorites",
        "" + FavoritesModel.getFavoriteIds().size(), 1);
    GoogleAnalyticsTracker.getInstance().trackPageView("MeActivity");
  }

  @Override
  public boolean onSearchRequested() {
    Intent i = new Intent(this, TagSearchActivity.class);
    this.startActivity(i);
    return true;
  }

}
