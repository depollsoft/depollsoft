package depollsoft.lib.util;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import depollsoft.lib.activity.RichApplication;

public class DebugTools {
  public static boolean isDebugSigned(String debugHash) {
    try {
      PackageInfo info = RichApplication
          .getAppContext()
          .getPackageManager()
          .getPackageInfo(RichApplication.getAppContext().getPackageName(),
              PackageManager.GET_SIGNATURES);
      return info.signatures[0].toCharsString().equals(debugHash);
    }
    catch (NameNotFoundException e) {
      return false;
    }
  }
}
