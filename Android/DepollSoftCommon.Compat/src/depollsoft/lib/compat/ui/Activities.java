package depollsoft.lib.compat.ui;

import depollsoft.lib.compat.Compatibility;
import android.app.Activity;

public final class Activities {
  private Activities() {
  }

  public static void invalidateOptionsMenu(final Activity activity) {
    Compatibility.tryWithFallback(new Runnable() {
      @Override
      public void run() {
        activity.invalidateOptionsMenu();
      }
    });
  }
}
