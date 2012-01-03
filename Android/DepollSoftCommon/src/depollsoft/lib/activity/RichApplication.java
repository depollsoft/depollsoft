package depollsoft.lib.activity;

import android.app.Application;
import android.content.Context;

public class RichApplication extends Application {
  private static Context context;

  public static Context getAppContext() {
    return RichApplication.context;
  }

  @Override
  public void onCreate() {
    RichApplication.context = this.getApplicationContext();
  }
}
