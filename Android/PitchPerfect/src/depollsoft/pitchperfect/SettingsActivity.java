package depollsoft.pitchperfect;

import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
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

import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseFacebookUtils.Permissions;
import com.parse.ParseUser;

import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.Trackable;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.CheckBoxCheckedProperty;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.ui.ChangelogViewer;

public class SettingsActivity extends Activity {

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
    ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!ActionBars.hasActionBar(this)) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    this.setContentView(R.layout.settingsview);

    UiBinder.bind(this,
        new CheckBoxCheckedProperty((CheckBox) this.findViewById(R.id.toggleNoteCheckBox)),
        "ToggleNotes", BindingMode.TwoWay);

    UiBinder.bind(this,
        new CheckBoxCheckedProperty((CheckBox) this.findViewById(R.id.wakeLockCheckBox)),
        "WakeLock", BindingMode.TwoWay);

    UiBinder.bind(this, R.id.removeAdsHyperlink, "Visibility", "ShowBuyLink", BoolConverter.get());
    UiBinder.bind(this, R.id.rateReviewHyperlink, "Visibility", "ShowBuyLink", BoolConverter.get());
    UiBinder.bind(this, R.id.aboutPurchased, "Visibility", "Licensed", BoolConverter.get());

    UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true));
    UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get());

    this.findViewById(R.id.loginButton).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(final View v) {
        v.setEnabled(false);
        final ProgressDialog progress = new ProgressDialog(SettingsActivity.this);
        progress.setMessage("Logging in...");
        loggingIn = true;
        progress.show();
        ParseFacebookUtils.logIn(Arrays.asList(Permissions.Extended.OFFLINE_ACCESS),
            SettingsActivity.this, new LogInCallback() {
              @Override
              public void done(ParseUser user, ParseException err) {
                try {
                  loggingIn = false;
                  progress.dismiss();
                  v.setEnabled(true);
                  if (err != null) {
                    Toast.makeText(SettingsActivity.this, "Facebook login failed.",
                        Toast.LENGTH_SHORT).show();
                    Log.d("Pitch Perfect", "Failed to log in.", err);
                    return;
                  }

                  if (user == null) {
                    Log.d("Pitch Perfect", "User cancelled login.");
                    return;
                  }
                  FlurryAgent.setUserId(user.getUsername());
                  SettingsActivity.this.loginTrackable.updateTrackers();
                  if (!user.isNew()) {
                    SettingsModel.restoreUser();
                    SongsModel.get().refreshFromParse();
                  }
                  else {
                    SettingsModel.refreshUser();
                    SongsModel.get().saveAllToParse(true);
                  }
                }
                catch (Exception e) {
                }
              }
            });
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
    UiBinder.unbind(this);
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    SettingsModel.refreshUser();
    FlurryAgent.endTimedEvent("SettingsActivity");
  }

  @Override
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().trackPageView("SettingsActivity");
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

}
