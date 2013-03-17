package depollsoft.lib.licensing;

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
    return pm.checkSignatures(packageName, packageName + ".license") == PackageManager.SIGNATURE_MATCH;
  }

  static void notifyLicenseChange() {
    LicenseChecker.licenseCheckNotifier.updateTrackers();
  }
}
