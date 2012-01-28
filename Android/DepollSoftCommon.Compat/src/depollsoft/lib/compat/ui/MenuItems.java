package depollsoft.lib.compat.ui;

import android.view.MenuItem;
import depollsoft.lib.compat.Compatibility;

public final class MenuItems {
  public static final int SHOW_AS_ACTION_ALWAYS = 2;
  public static final int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = 8;
  public static final int SHOW_AS_ACTION_IF_ROOM = 1;
  public static final int SHOW_AS_ACTION_NEVER = 0;
  public static final int SHOW_AS_ACTION_WITH_TEXT = 4;

  private MenuItems() {
  }

  public static boolean setShowAsAction(final MenuItem item, final int flags) {
    return Compatibility.tryWithFallback(new Runnable() {
      @Override
      public void run() {
        item.setShowAsAction(flags);
      }
    });
  }
}
