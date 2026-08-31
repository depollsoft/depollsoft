package depollsoft.lib.activity;

import android.app.Application;
import android.content.Context;

import depollsoft.lib.util.AppLog;

public class RichApplication extends Application {
  private static Context context;

  public static Context getAppContext() {
    return RichApplication.context;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    RichApplication.context = this.getApplicationContext();
    AppLog.initialize(this);
  }
}
