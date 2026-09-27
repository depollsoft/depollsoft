package depollsoft.lib.licensing;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import depollsoft.lib.state.ChangeSignal;

import depollsoft.lib.activity.RichApplication;

public class LicenseChecker {
  private static final ChangeSignal licenseCheckNotifier = new ChangeSignal();

  static {
    LicenseChangeListener.initialize();
  }

  public static boolean isLicensed() {
    LicenseChecker.licenseCheckNotifier.read();
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
    LicenseChecker.licenseCheckNotifier.changed();
  }
}
