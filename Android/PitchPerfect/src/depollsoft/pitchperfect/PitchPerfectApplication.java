package depollsoft.pitchperfect;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.parse.Parse;

import depollsoft.lib.activity.RichApplication;

public class PitchPerfectApplication extends RichApplication {
  public PitchPerfectApplication() {
  }

  @Override
  public void onCreate() {
    super.onCreate();
    GoogleAnalyticsTracker.getInstance().start("UA-24315533-2", 10, this);
    GoogleAnalyticsTracker.getInstance().setProductVersion("TagMaster",
        this.getString(R.string.app_version));
    GoogleAnalyticsTracker.getInstance().setCustomVar(1, "Version",
        this.getString(R.string.app_version), 2);
    Parse.initialize(this, "cXYwcCUUP2f78OBfMlXu7dk03f2JRMQYXpCnv7H9",
        "Y9ZIP3kLs1Jbh9Mpr2s8tRw9tjdGt6GuseuRHNdE");
  }

  @Override
  public void onTerminate() {
    GoogleAnalyticsTracker.getInstance().dispatch();
    GoogleAnalyticsTracker.getInstance().stop();
    super.onTerminate();
  }
}
