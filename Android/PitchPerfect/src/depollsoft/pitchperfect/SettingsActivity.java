package depollsoft.pitchperfect;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import com.facebook.login.LoginManager;
import com.parse.ParseUser;

import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.ui.ChangelogViewer;

public class SettingsActivity extends AppCompatActivity {
  private boolean loggingIn;

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
    LoginPrompt.FACEBOOK_CALLBACK_MANAGER.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setTitle("Pitch Perfect Settings");

    this.setContentView(R.layout.settingsview);

    UiBinder.bind(this,
        new CompoundButtonCheckedProperty((CheckBox) this.findViewById(R.id.toggleNoteCheckBox)),
        "ToggleNotes", BindingMode.TWO_WAY);

    UiBinder.bind(this,
        new CompoundButtonCheckedProperty((CheckBox) this.findViewById(R.id.wakeLockCheckBox)),
        "WakeLock", BindingMode.TWO_WAY);

    UiBinder.bind(this, R.id.rateReviewHyperlink, "Visibility", "ShowBuyLink", BoolConverter.get());
    UiBinder.bind(this, R.id.aboutPurchased, "Visibility", "Licensed", BoolConverter.get());

    UiBinder.bind(this, R.id.loginButton, "Visibility", "LoggedIn", BoolConverter.get(true));
    UiBinder.bind(this, R.id.logoutButton, "Visibility", "LoggedIn", BoolConverter.get());

    this.findViewById(R.id.loginButton).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(final View v) {
        Dialog dlg = LoginPrompt.buildDialog(SettingsActivity.this, false);
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
            LoginManager.getInstance().logOut();
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
        viewer.setIcon(R.mipmap.ic_launcher);
        viewer.show();
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    SettingsModel.refreshUser();
  }

  public void setToggleNotes(boolean value) {
    SettingsModel.setToggleNotes(value);
  }

  public void setWakeLock(boolean value) {
    SettingsModel.setWakeLock(value);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
  }
}
