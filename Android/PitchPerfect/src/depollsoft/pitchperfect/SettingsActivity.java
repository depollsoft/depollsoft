package depollsoft.pitchperfect;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.Toast;

import com.bindroid.BindingMode;
import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.Trackable;
import com.bindroid.ui.CompoundButtonCheckedProperty;
import com.bindroid.ui.UiBinder;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.flurry.android.FlurryAgent;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;

import co.hoomi.HoomiClient;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.ui.ChangelogViewer;

public class SettingsActivity extends Activity {
  private UiLifecycleHelper uiHelper;


  private boolean loggingIn;

  public static boolean getShowBuyLink() {
    return !SettingsModel.getLicensed() && !SettingsModel.getAppStore().equals("amazon")
        && !SettingsModel.getAppStore().equals("blackberry");
  }

  private Trackable loginTrackable = new Trackable();

  public boolean getLicensed() {
    return SettingsModel.getLicensed();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && loggingIn) {
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public boolean getLoggedIn() {
    this.loginTrackable.track();
    return ParseUser.getCurrentUser() != null;
  }

  public boolean getToggleNotes() {
    return SettingsModel.getToggleNotes();
  }

  public boolean getWakeLock() {
    return SettingsModel.getWakeLock();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    uiHelper.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    uiHelper = new UiLifecycleHelper(this, new Session.StatusCallback() {
      @Override
      public void call(Session session, SessionState sessionState, Exception e) {

      }
    });
    uiHelper.onCreate(savedInstanceState);

    if (!ActionBars.hasActionBar(this)) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    this.setContentView(R.layout.settingsview);

    UiBinder.bind(this,
        new CompoundButtonCheckedProperty((CheckBox) this.findViewById(R.id.toggleNoteCheckBox)),
        "ToggleNotes", BindingMode.TWO_WAY);

    UiBinder.bind(this,
        new CompoundButtonCheckedProperty((CheckBox) this.findViewById(R.id.wakeLockCheckBox)),
        "WakeLock", BindingMode.TWO_WAY);

    UiBinder.bind(this, R.id.removeAdsHyperlink, "Visibility", "ShowBuyLink", BoolConverter.get());
    UiBinder.bind(this, R.id.rateReviewHyperlink, "Visibility", "ShowBuyLink", BoolConverter.get());
    UiBinder.bind(this, R.id.aboutPurchased, "Visibility", "Licensed", BoolConverter.get());

    UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true));
    UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get());

    this.findViewById(R.id.loginButton).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(final View v) {
        Dialog dlg = LoginPrompt.buildDialog(SettingsActivity.this);
        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
            SettingsActivity.this.loginTrackable.updateTrackers();
          }
        });
        dlg.show();
      }
    });

    this.findViewById(R.id.logoutButton).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        AsyncTask<Void, Void, Void> logOutTask = new AsyncTask<Void, Void, Void>() {

          @Override
          protected Void doInBackground(Void... params) {
            ParseUser.logOut();
            SongsModel.get().handleLogOut();
            if (Session.getActiveSession() != null) {
              Session.getActiveSession().closeAndClearTokenInformation();
            }
            if (HoomiClient.getCurrentClient().getCurrentToken() != null) {
              HoomiClient.getCurrentClient().setCurrentToken(null);
            }
            return null;
          }

          @Override
          protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            SettingsActivity.this.loginTrackable.updateTrackers();
          }
        };
        logOutTask.execute();
      }
    });

    View clearSongListButton = this.findViewById(R.id.clearSongListButton);
    clearSongListButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
        builder.setMessage("Are you sure you want to clear your song list?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                SongsModel.get().resetSongs();
                Toast.makeText(SettingsActivity.this, "Song list cleared.", Toast.LENGTH_SHORT)
                    .show();
              }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
              }
            }).show();
      }
    });

    this.findViewById(R.id.changelogButton).setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        ChangelogViewer viewer = new ChangelogViewer(SettingsActivity.this, SettingsActivity.this
            .getString(R.string.Changelog));
        viewer.setTitle("Pitch Perfect Changelog");
        viewer.setIcon(SettingsActivity.this.getResources().getDrawable(R.drawable.icon));
        viewer.show();
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    uiHelper.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    uiHelper.onPause();
    SettingsModel.refreshUser();
    FlurryAgent.endTimedEvent("SettingsActivity");
  }

  @Override
  protected void onResume() {
    super.onResume();
    uiHelper.onResume();
    FlurryAgent.logEvent("SettingsActivity", true);
  }

  public void setToggleNotes(boolean value) {
    SettingsModel.setToggleNotes(value);
  }

  public void setWakeLock(boolean value) {
    SettingsModel.setWakeLock(value);
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
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    uiHelper.onSaveInstanceState(outState);
  }
}
