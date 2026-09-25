package depollsoft.pitchperfect;

import depollsoft.lib.licensing.LicenseChecker;
import depollsoft.lib.util.Preferences;

public class SettingsModel {
  private static final String ToggleNoteKey = "depollsoft.pitchperfect.ToggleNote";
  private static final String WakeLockKey = "depollsoft.pitchperfect.WakeLock";

  static {
    Preferences.initialize(SettingsModel.ToggleNoteKey, false);
    Preferences.initialize(SettingsModel.WakeLockKey, false);
  }

  public static boolean getLicensed() {
    return LicenseChecker.isLicensed();
  }

  public static boolean getToggleNotes() {
    return Preferences.<Boolean>get(SettingsModel.ToggleNoteKey);
  }

  public static boolean getWakeLock() {
    return Preferences.<Boolean>get(SettingsModel.WakeLockKey);
  }

  public static void setToggleNotes(boolean value) {
    Preferences.set(SettingsModel.ToggleNoteKey, value);
  }

  public static void setWakeLock(boolean value) {
    Preferences.set(SettingsModel.WakeLockKey, value);
  }
}
