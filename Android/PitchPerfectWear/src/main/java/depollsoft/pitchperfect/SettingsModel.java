package depollsoft.pitchperfect;

import depollsoft.lib.state.ChangeSignal;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.licensing.LicenseChecker;
import depollsoft.lib.util.Preferences;

public class SettingsModel {
  private static final String ToggleNoteKey = "depollsoft.pitchperfect.ToggleNote";
  private static final String WakeLockKey = "depollsoft.pitchperfect.WakeLock";

  private static final ChangeSignal toggleNoteSignal = new ChangeSignal();
  private static final ChangeSignal wakeLockSignal = new ChangeSignal();

  static {
    Preferences.initialize(SettingsModel.ToggleNoteKey, false);
    Preferences.initialize(SettingsModel.WakeLockKey, false);
  }


  public static boolean getLicensed() {
    return LicenseChecker.isLicensed();
  }

  public static boolean getToggleNotes() {
    SettingsModel.toggleNoteSignal.read();
    return Preferences.<Boolean>get(SettingsModel.ToggleNoteKey);
  }

  public static boolean getWakeLock() {
    SettingsModel.wakeLockSignal.read();
    return Preferences.<Boolean>get(SettingsModel.WakeLockKey);
  }

  public static void setToggleNotes(boolean value) {
    Preferences.set(SettingsModel.ToggleNoteKey, value);
    SettingsModel.toggleNoteSignal.changed();
  }

  public static void setWakeLock(boolean value) {
    Preferences.set(SettingsModel.WakeLockKey, value);
    SettingsModel.wakeLockSignal.changed();
  }
}
