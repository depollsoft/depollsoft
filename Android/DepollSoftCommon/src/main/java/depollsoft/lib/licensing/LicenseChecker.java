package depollsoft.lib.licensing;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.bindroid.trackable.Trackable;

import depollsoft.lib.activity.RichApplication;

public class LicenseChecker {
  private static Trackable licenseCheckNotifier = new Trackable();

  static {
    LicenseChangeListener.initialize();
  }

  public static boolean isLicensed() {
    LicenseChecker.licenseCheckNotifier.track();
    PackageManager pm = RichApplication.getAppContext().getPackageManager();
    String packageName = RichApplication.getAppContext().getPackageName();
    PackageInfo pkg = null;
    try {
      pkg = pm.getPackageInfo(packageName + ".license", 0);
    } catch (PackageManager.NameNotFoundException e) {
      return false;
    }
    return pkg != null;
  }

  static void notifyLicenseChange() {
    LicenseChecker.licenseCheckNotifier.updateTrackers();
  }
}
