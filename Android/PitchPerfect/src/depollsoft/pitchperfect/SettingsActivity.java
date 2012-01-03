package depollsoft.pitchperfect;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.CheckBoxCheckedProperty;
import depollsoft.lib.binding.ui.UiBinder;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.CheckBox;

public class SettingsActivity extends Activity {

  public boolean getLicensed() {
    return SettingsModel.getLicensed();
  }

  public boolean getToggleNotes() {
    return SettingsModel.getToggleNotes();
  }

  public boolean getWakeLock() {
    return SettingsModel.getWakeLock();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.settingsview);

    UiBinder.bind(
        this,
        new CheckBoxCheckedProperty((CheckBox) this
            .findViewById(R.id.toggleNoteCheckBox)), "ToggleNotes",
        BindingMode.TwoWay);

    UiBinder.bind(
        this,
        new CheckBoxCheckedProperty((CheckBox) this
            .findViewById(R.id.wakeLockCheckBox)), "WakeLock",
        BindingMode.TwoWay);

    UiBinder.bind(this, R.id.removeAdsHyperlink, "Visibility", "Licensed",
        BoolConverter.get(true));
    UiBinder.bind(this, R.id.aboutPurchased, "Visibility", "Licensed",
        BoolConverter.get());

    View clearSongListButton = this.findViewById(R.id.clearSongListButton);
    clearSongListButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
            SettingsActivity.this);
        builder.setMessage("Are you sure you want to clear your song list?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
                SongsModel.get().resetSongs();
                Toast.makeText(SettingsActivity.this, "Song list cleared.",
                    Toast.LENGTH_SHORT).show();
              }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int which) {
              }
            }).show();
      }
    });
  }

  @Override
  protected void onDestroy() {
    UiBinder.unbind(this);
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().trackPageView("SettingsActivity");
  }

  public void setToggleNotes(boolean value) {
    SettingsModel.setToggleNotes(value);
  }

  public void setWakeLock(boolean value) {
    SettingsModel.setWakeLock(value);
  }

}
