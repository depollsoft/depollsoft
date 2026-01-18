package depollsoft.lib.util;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import depollsoft.lib.activity.RichApplication;

public class Versioning {
  private static final String LAST_VERSION_SEEN_KEY = "depollsoft.lib.LastVersionSeen";
  private static int lastVersion;
  private static int curVersion;

  static {
    Preferences.initialize(LAST_VERSION_SEEN_KEY, Integer.MIN_VALUE);
    lastVersion = Preferences.<Integer> get(LAST_VERSION_SEEN_KEY);
    try {
      curVersion = RichApplication
          .getAppContext()
          .getPackageManager()
          .getPackageInfo(RichApplication.getAppContext().getPackageName(),
              PackageManager.GET_META_DATA).versionCode;
      Preferences.set(LAST_VERSION_SEEN_KEY, curVersion);
    }
    catch (NameNotFoundException e) {
      Log.e("depollsoft.lib", "Unable to set depollsoft.lib.LastVersionSeen", e);
    }
    Log.d("depollsoft.lib", "Current Version Code: " + curVersion);
    Log.d("depollsoft.lib", "Last Seen Version Code: " + lastVersion);
  }

  public static int getLastVersionSeen() {
    return lastVersion;
  }

  public static int getCurrentVersion() {
    return curVersion;
  }

  public static boolean isFirstRun() {
    return lastVersion == Integer.MIN_VALUE;
  }

  public static boolean isFirstRunOfVersion() {
    return curVersion != lastVersion;
  }
}
