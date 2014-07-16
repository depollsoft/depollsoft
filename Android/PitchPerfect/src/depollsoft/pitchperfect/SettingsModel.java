package depollsoft.pitchperfect;

import com.bindroid.trackable.Trackable;
import com.parse.ParseUser;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.licensing.LicenseChecker;
import depollsoft.lib.util.Preferences;

public class SettingsModel {
  private static final String ToggleNoteKey = "depollsoft.pitchperfect.ToggleNote";
  private static final String WakeLockKey = "depollsoft.pitchperfect.WakeLock";

  private static Trackable toggleNoteTrackable = new Trackable();
  private static Trackable wakeLockTrackable = new Trackable();
  static {
    Preferences.initialize(SettingsModel.ToggleNoteKey, false);
    Preferences.initialize(SettingsModel.WakeLockKey, false);
  }

  public static String getAppStore() {
    return RichApplication.getAppContext().getString(R.string.app_store);
  }

  public static boolean getLicensed() {
    return LicenseChecker.isLicensed();
  }

  public static boolean getToggleNotes() {
    SettingsModel.toggleNoteTrackable.track();
    return Preferences.<Boolean>get(SettingsModel.ToggleNoteKey);
  }

  public static boolean getWakeLock() {
    SettingsModel.wakeLockTrackable.track();
    return Preferences.<Boolean>get(SettingsModel.WakeLockKey);
  }

  public static void refreshUser() {
    if (ParseUser.getCurrentUser() != null) {
      ParseUser.getCurrentUser().put("ToggleNote", SettingsModel.getToggleNotes());
      ParseUser.getCurrentUser().put("WakeLock", SettingsModel.getWakeLock());
      ParseUser.getCurrentUser().saveEventually();
    }
  }

  public static void restoreUser() {
    if (ParseUser.getCurrentUser() != null) {
      if (ParseUser.getCurrentUser().containsKey("ToggleNote")) {
        SettingsModel.setToggleNotes(ParseUser.getCurrentUser().getBoolean("ToggleNote"));
      }
      if (ParseUser.getCurrentUser().containsKey("WakeLock")) {
        SettingsModel.setWakeLock(ParseUser.getCurrentUser().getBoolean("WakeLock"));
      }
    }
  }

  public static void setToggleNotes(boolean value) {
    Preferences.set(SettingsModel.ToggleNoteKey, value);
    SettingsModel.toggleNoteTrackable.updateTrackers();
  }

  public static void setWakeLock(boolean value) {
    Preferences.set(SettingsModel.WakeLockKey, value);
    SettingsModel.wakeLockTrackable.updateTrackers();
  }
}
