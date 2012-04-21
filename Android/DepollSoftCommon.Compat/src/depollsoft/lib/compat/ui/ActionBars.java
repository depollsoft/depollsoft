package depollsoft.lib.compat.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.view.View;
import depollsoft.lib.compat.Compatibility;
import depollsoft.lib.compat.RunnableFactory;

@TargetApi(11)
public final class ActionBars {
  public static final int HOME_MENU_ITEM_ID = 16908332;

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

  public static boolean setCustomTitle(final Activity activity, final View view) {
    return Compatibility.tryWithFallback(new RunnableFactory() {
      @Override
      public Runnable create() {
        return new Runnable() {
          @Override
          public void run() {
            activity.getActionBar().setDisplayShowCustomEnabled(true);
            activity.getActionBar().setDisplayShowTitleEnabled(false);
            activity.getActionBar().setCustomView(view);
          }
        };
      }
    });
  }

  public static boolean setCustomTitle(final Activity activity, final int viewId) {
    return Compatibility.tryWithFallback(new RunnableFactory() {
      @Override
      public Runnable create() {
        return new Runnable() {
          @Override
          public void run() {
            activity.getActionBar().setDisplayShowCustomEnabled(true);
            activity.getActionBar().setDisplayShowTitleEnabled(false);
            activity.getActionBar().setCustomView(viewId);
          }
        };
      }
    });
  }
}
