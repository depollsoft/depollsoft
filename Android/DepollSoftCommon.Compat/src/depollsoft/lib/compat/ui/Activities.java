package depollsoft.lib.compat.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import depollsoft.lib.compat.Compatibility;
import depollsoft.lib.compat.RunnableFactory;

public final class Activities {
  private Activities() {
  }

  @TargetApi(11)
  public static void invalidateOptionsMenu(final Activity activity) {
    Compatibility.tryWithFallback(new RunnableFactory() {
      @Override
      public Runnable create() {
        return new Runnable() {
          @Override
          public void run() {
            activity.invalidateOptionsMenu();
          }
        };
      }
    });
  }
}
