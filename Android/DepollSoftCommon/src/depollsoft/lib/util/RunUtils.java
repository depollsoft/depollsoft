package depollsoft.lib.util;

public class RunUtils {
  public static boolean runOnce(String key) {
    String prefKey = "depollsoft.lib.RunOnce." + key;
    if (Preferences.get(prefKey) != null) {
      return false;
    }
    Preferences.set(prefKey, true);
    return true;
  }
}
