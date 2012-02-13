package depollsoft.pitchperfect;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.widget.TabHost.OnTabChangeListener;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.Activities;
import depollsoft.lib.compat.ui.CompatTabHostWrapper;
import depollsoft.lib.ui.ChangelogViewer;

public class PitchPerfectActivity extends TabActivity {
  private WakeLock wakeLock;
  private CompatTabHostWrapper tabHost;
  private boolean preparingMenu;

  public boolean getAdsShouldShow() {
    return !SettingsModel.getLicensed();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    View title = this.findViewById(R.id.titleLayout);
    switch (newConfig.orientation) {
    case Configuration.ORIENTATION_SQUARE:
    case Configuration.ORIENTATION_LANDSCAPE:
      title.setVisibility(View.GONE);
      break;
    case Configuration.ORIENTATION_PORTRAIT:
    case Configuration.ORIENTATION_UNDEFINED:
      if (!ActionBars.hasActionBar(this)) {
        title.setVisibility(View.VISIBLE);
      }
      else {
        title.setVisibility(View.GONE);
      }
      break;
    }
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!ActionBars.hasActionBar(this)) {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    this.setContentView(R.layout.pitchperfectview);

    tabHost = new CompatTabHostWrapper(this, getTabHost());

    UiBinder.bind(this, R.id.adView, "Visibility", "AdsShouldShow", BoolConverter.get());

    tabHost.addTab(tabHost.newTabSpec("PitchPipe")
        .setIndicator("Quick Pitch", this.getResources().getDrawable(R.drawable.ic_tab_pitchpipe))
        .setContent(new Intent(this, PitchPipeActivity.class)));

    tabHost.addTab(tabHost.newTabSpec("NoteList")
        .setIndicator("Notes", this.getResources().getDrawable(R.drawable.ic_tab_octaves))
        .setContent(new Intent(this, NoteListActivity.class)));

    tabHost.addTab(tabHost.newTabSpec("KeySignatures")
        .setIndicator("Keys", this.getResources().getDrawable(R.drawable.ic_tab_keys))
        .setContent(new Intent(this, KeySignatureActivity.class)));

    tabHost.addTab(tabHost.newTabSpec("Songs")
        .setIndicator("Songs", this.getResources().getDrawable(R.drawable.ic_tab_songs))
        .setContent(new Intent(this, SongListActivity.class)));

    this.onConfigurationChanged(Resources.getSystem().getConfiguration());

    getTabHost().setOnTabChangedListener(new OnTabChangeListener() {
      @Override
      public void onTabChanged(String tabId) {
        Activities.invalidateOptionsMenu(PitchPerfectActivity.this);
      }
    });

    ChangelogViewer viewer = new ChangelogViewer(this, this.getString(R.string.Changelog));
    viewer.setTitle("Pitch Perfect Changelog");
    viewer.setIcon(this.getResources().getDrawable(R.drawable.icon));
    viewer.showIfAppropriate();
  }

  @Override
  protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);
    tabHost.restoreInstanceState("tabs", state);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    tabHost.saveInstanceState("tabs", outState);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    if (preparingMenu)
      return false;
    preparingMenu = true;
    try {
      menu.clear();
      MenuInflater mi = new MenuInflater(this);
      mi.inflate(R.menu.mainmenu, menu);

      menu.findItem(R.id.settingsMenuItem).setOnMenuItemClickListener(
          new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
              Intent i = new Intent(PitchPerfectActivity.this, SettingsActivity.class);
              PitchPerfectActivity.this.startActivity(i);
              return true;
            }
          });
      getLocalActivityManager().getCurrentActivity().onPrepareOptionsMenu(menu);
      return super.onPrepareOptionsMenu(menu);
    }
    finally {
      preparingMenu = false;
    }
  }

  @Override
  protected void onDestroy() {
    UiBinder.unbind(this);
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    if (this.wakeLock != null) {
      this.wakeLock.release();
      this.wakeLock = null;
    }
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().trackPageView("PitchPerfectActivity");
    if (SettingsModel.getWakeLock()) {
      this.wakeLock = ((PowerManager) this.getSystemService(Context.POWER_SERVICE)).newWakeLock(
          PowerManager.SCREEN_DIM_WAKE_LOCK, "PitchPerfectActivity");
      this.wakeLock.acquire();
    }
  }
}