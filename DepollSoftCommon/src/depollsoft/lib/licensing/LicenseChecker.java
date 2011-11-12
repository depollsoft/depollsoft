package depollsoft.lib.licensing;

import android.content.pm.PackageManager;
import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.binding.Trackable;

public class LicenseChecker
{
   private static Trackable licenseCheckNotifier = new Trackable();

   static
   {
      LicenseChangeListener.initialize();
   }

   public static boolean isLicensed()
   {
      LicenseChecker.licenseCheckNotifier.track();
      PackageManager pm = RichApplication.getAppContext().getPackageManager();
      String packageName = RichApplication.getAppContext().getPackageName();
      return pm.checkSignatures(packageName, packageName + ".license") == PackageManager.SIGNATURE_MATCH;
   }

   static void notifyLicenseChange()
   {
      LicenseChecker.licenseCheckNotifier.updateTrackers();
   }
}
