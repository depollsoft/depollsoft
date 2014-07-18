package depollsoft.pitchperfect;

import com.bindroid.trackable.Trackable;

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

  public static void setToggleNotes(boolean value) {
    Preferences.set(SettingsModel.ToggleNoteKey, value);
    SettingsModel.toggleNoteTrackable.updateTrackers();
  }

  public static void setWakeLock(boolean value) {
    Preferences.set(SettingsModel.WakeLockKey, value);
    SettingsModel.wakeLockTrackable.updateTrackers();
  }
}
