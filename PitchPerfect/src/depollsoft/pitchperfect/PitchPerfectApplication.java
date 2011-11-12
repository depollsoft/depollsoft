package depollsoft.pitchperfect;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.activity.RichApplication;

public class PitchPerfectApplication extends RichApplication
{
   public PitchPerfectApplication()
   {
   }

   @Override
   public void onCreate()
   {
      super.onCreate();
      GoogleAnalyticsTracker.getInstance().start("UA-24315533-2", 10, this);
      GoogleAnalyticsTracker.getInstance().setProductVersion("TagMaster",
            this.getString(R.string.app_version));
      GoogleAnalyticsTracker.getInstance().setCustomVar(1, "Version",
            this.getString(R.string.app_version), 2);
   }

   @Override
   public void onTerminate()
   {
      GoogleAnalyticsTracker.getInstance().dispatch();
      GoogleAnalyticsTracker.getInstance().stop();
      super.onTerminate();
   }
}
