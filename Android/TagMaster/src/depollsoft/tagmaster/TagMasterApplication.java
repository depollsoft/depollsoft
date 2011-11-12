package depollsoft.tagmaster;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.activity.RichApplication;

public class TagMasterApplication extends RichApplication
{

   @Override
   public void onCreate()
   {
      super.onCreate();
      GoogleAnalyticsTracker.getInstance().start("UA-24315533-3", 10, this);
      GoogleAnalyticsTracker.getInstance().setProductVersion("TagMaster",
            this.getString(R.string.VersionNumber));
      GoogleAnalyticsTracker.getInstance().setCustomVar(1, "Version",
            this.getString(R.string.VersionNumber), 2);
   }

   @Override
   public void onTerminate()
   {
      GoogleAnalyticsTracker.getInstance().dispatch();
      GoogleAnalyticsTracker.getInstance().stop();
      super.onTerminate();
   }

}
