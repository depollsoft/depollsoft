package depollsoft.tagmaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.bindroid.converters.AdapterConverter;
import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableCollection;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.RefreshCallback;

import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.MenuItems;
import depollsoft.lib.ui.ChangelogViewer;

public class MeActivity extends Activity {
  public MeActivity() {
  }

  public TrackableCollection<Integer> getFavoriteIds() {
    return FavoritesModel.getFavoriteIds();
  }

  public boolean getHasActionBar() {
    return ActionBars.hasActionBar(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.meview);

    UiBinder.bind(this, R.id.favoritesItemsControl, "Adapter", "FavoriteIds", new AdapterConverter(
        FavoriteTagItemView.class, false, true));

    UiBinder.bind(this, R.id.titleLayout, "Visibility", "HasActionBar", new BoolConverter(true));

    ActionBars.setCustomTitle(this, R.layout.titleview);

    if (ParseUser.getCurrentUser() != null) {
      try {
        ParseUser.getCurrentUser().refreshInBackground(new RefreshCallback() {

          @Override
          public void done(ParseObject obj, ParseException err) {
            if (err != null) {
              return;
            }
            FavoritesModel.restoreFromUser();
            TeachableTagsModel.restoreFromUser();
          }
        });
      }
      catch (Exception e) {
      }
    }

    ChangelogViewer viewer = new ChangelogViewer(this, this.getString(R.string.Changelog));
    viewer.setTitle("Tag Master Changelog");
    viewer.setIcon(this.getResources().getDrawable(R.drawable.icon));
    viewer.showIfAppropriate();
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
  }

  @Override
  public boolean onSearchRequested() {
    Intent i = new Intent(this, TagSearchActivity.class);
    this.startActivity(i);
    return true;
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
