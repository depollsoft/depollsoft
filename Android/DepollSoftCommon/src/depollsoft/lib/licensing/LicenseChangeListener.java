package depollsoft.lib.licensing;

import depollsoft.lib.activity.RichApplication;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

class LicenseChangeListener extends BroadcastReceiver {
  private static LicenseChangeListener commonInstance;

  public static void initialize() {
    if (LicenseChangeListener.commonInstance == null) {
      LicenseChangeListener.commonInstance = new LicenseChangeListener();
      IntentFilter broadcastFilter = new IntentFilter();
      broadcastFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
      broadcastFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
      broadcastFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
      broadcastFilter.addDataScheme("package");
      RichApplication.getAppContext().registerReceiver(
          LicenseChangeListener.commonInstance, broadcastFilter);
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    LicenseChecker.notifyLicenseChange();
  }

}
