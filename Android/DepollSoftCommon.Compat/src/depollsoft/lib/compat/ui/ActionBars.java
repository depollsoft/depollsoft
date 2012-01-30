package depollsoft.lib.compat.ui;

import android.app.Activity;
import depollsoft.lib.compat.Compatibility;
import depollsoft.lib.compat.RunnableFactory;

public final class ActionBars {

  private ActionBars() {
  }

  public static boolean hasActionBar(final Activity activity) {
    return Compatibility.tryWithFallback(new RunnableFactory() {
      @Override
      public Runnable create() {
        return new Runnable() {
          @Override
          public void run() {
            activity.getActionBar();
          }
        };
      }
    });
  }
}
