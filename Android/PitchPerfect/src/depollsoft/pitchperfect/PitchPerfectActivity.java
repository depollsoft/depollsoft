package depollsoft.pitchperfect;

import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.Window;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.Toast;

import com.bindroid.converters.BoolConverter;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import java.util.concurrent.Callable;

import bolts.Task;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.Activities;
import depollsoft.lib.compat.ui.CompatTabHostWrapper;
import depollsoft.lib.ui.ChangelogViewer;
import depollsoft.lib.util.RunUtils;

@SuppressWarnings("deprecation")
public class PitchPerfectActivity extends TabActivity {

  private WakeLock wakeLock;
  private CompatTabHostWrapper tabHost;
  private boolean preparingMenu;
  static boolean handlingResult;

  public boolean getAdsShouldShow() {
    if (SettingsModel.getAreAdsRemoved()) {
      return false;
    }
    if (SettingsModel.getLicensed()) {
      return false;
    }
    return true;
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
        } else {
          title.setVisibility(View.GONE);
        }
        break;
    }
  }

  /**
   * Called when the activity is first created.
   */
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
    UiBinder.bind(this, R.id.removeAds, "Visibility", "AdsShouldShow", BoolConverter.get());

    findViewById(R.id.removeAds).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (PurchaseService.isSubscriptionBillingAvailable(PitchPerfectActivity.this)) {
          PurchaseService.beginRemoveAds(PitchPerfectActivity.this, 666);
        }
      }
    });

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

    boolean isHoomiLogin = ParseUser.getCurrentUser() != null && !ParseFacebookUtils.isLinked(ParseUser.getCurrentUser());
    if (isHoomiLogin) {
      ParseUser.logOutInBackground();
    }

    if (isHoomiLogin || RunUtils.runOnce("loginDialog") && ParseUser.getCurrentUser() == null) {
      LoginPrompt.buildDialog(this, isHoomiLogin).show();
    } else {
      ChangelogViewer viewer = new ChangelogViewer(this, this.getString(R.string.Changelog));
      viewer.setTitle("Pitch Perfect Changelog");
      viewer.setIcon(this.getResources().getDrawable(R.drawable.icon));
      viewer.showIfAppropriate();
    }

    AdView adView = (AdView) findViewById(R.id.adView);

    AdRequest adRequest = new AdRequest.Builder()
            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
            .build();
    adView.loadAd(adRequest);

    PurchaseService.bind(this, new Runnable() {
      @Override
      public void run() {
        SettingsModel.setAreAdsRemoved(PurchaseService.areAdsRemoved(PitchPerfectActivity.this));
      }
    });
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
    } finally {
      preparingMenu = false;
    }
  }

  @Override
  protected void onDestroy() {
    PurchaseService.unbind(this);
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
    if (SettingsModel.getWakeLock()) {
      this.wakeLock = ((PowerManager) this.getSystemService(Context.POWER_SERVICE)).newWakeLock(
              PowerManager.SCREEN_DIM_WAKE_LOCK, "PitchPerfectActivity");
      this.wakeLock.acquire();
    }

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (handlingResult) {
          handlingResult = false;
          return;
        }
        PitchPerfectApplication.startupRefreshFromParse();
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 666) {
      Task.callInBackground(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          SettingsModel.setAreAdsRemoved(PurchaseService.areAdsRemoved(PitchPerfectActivity.this));
          return null;
        }
      });
    } else {
      LoginPrompt.FACEBOOK_CALLBACK_MANAGER.onActivityResult(requestCode, resultCode, data);
      handlingResult = true;
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    FlurryAgent.onStartSession(this, "B8F71MSD6E6KWMAK479A");
  }

  @Override
  protected void onStop() {
    super.onStop();
    FlurryAgent.onEndSession(this);
  }

  @Override
  public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
    super.onSaveInstanceState(outState, outPersistentState);
  }
}