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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;

public class PitchPerfectActivity extends TabActivity {
  private WakeLock wakeLock;

  public boolean getAdsShouldShow() {
    return !SettingsModel.getLicensed();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    // TODO Auto-generated method stub
    super.onConfigurationChanged(newConfig);
    View title = this.findViewById(R.id.titleLayout);
    switch (newConfig.orientation) {
    case Configuration.ORIENTATION_SQUARE:
    case Configuration.ORIENTATION_LANDSCAPE:
      title.setVisibility(View.GONE);
      break;
    case Configuration.ORIENTATION_PORTRAIT:
    case Configuration.ORIENTATION_UNDEFINED:
      title.setVisibility(View.VISIBLE);
      break;
    }
  }

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    this.setContentView(R.layout.pitchperfectview);

    UiBinder.bind(this, R.id.adView, "Visibility", "AdsShouldShow",
        BoolConverter.get());

    this.getTabHost().addTab(
        this.getTabHost()
            .newTabSpec("PitchPipe")
            .setIndicator("Quick Pitch",
                this.getResources().getDrawable(R.drawable.ic_tab_pitchpipe))
            .setContent(new Intent(this, PitchPipeActivity.class)));

    this.getTabHost().addTab(
        this.getTabHost()
            .newTabSpec("NoteList")
            .setIndicator("Notes",
                this.getResources().getDrawable(R.drawable.ic_tab_octaves))
            .setContent(new Intent(this, NoteListActivity.class)));

    this.getTabHost().addTab(
        this.getTabHost()
            .newTabSpec("KeySignatures")
            .setIndicator("Keys",
                this.getResources().getDrawable(R.drawable.ic_tab_keys))
            .setContent(new Intent(this, KeySignatureActivity.class)));

    this.getTabHost().addTab(
        this.getTabHost()
            .newTabSpec("Songs")
            .setIndicator("Songs",
                this.getResources().getDrawable(R.drawable.ic_tab_songs))
            .setContent(new Intent(this, SongListActivity.class)));

    this.onConfigurationChanged(Resources.getSystem().getConfiguration());

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater mi = new MenuInflater(this);
    mi.inflate(R.menu.mainmenu, menu);

    menu.findItem(R.id.settingsMenuItem).setOnMenuItemClickListener(
        new OnMenuItemClickListener() {

          @Override
          public boolean onMenuItemClick(MenuItem item) {
            Intent i = new Intent(PitchPerfectActivity.this,
                SettingsActivity.class);
            PitchPerfectActivity.this.startActivity(i);
            return true;
          }
        });

    return super.onCreateOptionsMenu(menu);
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
      this.wakeLock = ((PowerManager) this
          .getSystemService(Context.POWER_SERVICE)).newWakeLock(
          PowerManager.SCREEN_DIM_WAKE_LOCK, "PitchPerfectActivity");
      this.wakeLock.acquire();
    }
  }
}