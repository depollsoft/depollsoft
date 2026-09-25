package depollsoft.lib.activity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import depollsoft.lib.util.AppLog;

public class RichApplication extends Application {
  private static Context context;

  public static Context getAppContext() {
    return RichApplication.context;
  }

  /** Unit tests run with a plain Application; this gives the shared helpers its context. */
  @VisibleForTesting
  public static void setAppContextForTesting(Context context) {
    RichApplication.context = context;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    RichApplication.context = this.getApplicationContext();
    AppLog.initialize(this);
    registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
      @Override
      public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity.getClass().getName().startsWith("depollsoft.")) {
          applyWindowInsets(activity);
        }
      }

      @Override
      public void onActivityStarted(Activity activity) {
      }

      @Override
      public void onActivityResumed(Activity activity) {
      }

      @Override
      public void onActivityPaused(Activity activity) {
      }

      @Override
      public void onActivityStopped(Activity activity) {
      }

      @Override
      public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
      }

      @Override
      public void onActivityDestroyed(Activity activity) {
      }
    });
  }

  private static void applyWindowInsets(Activity activity) {
    // Android 16 enforces edge-to-edge. AppCompat includes the ActionBar height in
    // the system-bar inset it dispatches to the activity content view.
    View content = activity.findViewById(android.R.id.content);
    if (content == null) {
      return;
    }

    int initialLeft = content.getPaddingLeft();
    int initialTop = content.getPaddingTop();
    int initialRight = content.getPaddingRight();
    int initialBottom = content.getPaddingBottom();
    int insetTypes = WindowInsetsCompat.Type.systemBars()
        | WindowInsetsCompat.Type.displayCutout();

    ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
      Insets insets = windowInsets.getInsets(insetTypes);
      view.setPadding(
          initialLeft + insets.left,
          initialTop + insets.top,
          initialRight + insets.right,
          initialBottom + insets.bottom);

      // Prevent inset-aware children from adding the same system-bar padding again.
      // Other inset types, including the IME, continue through the view hierarchy.
      return new WindowInsetsCompat.Builder(windowInsets)
          .setInsets(insetTypes, Insets.NONE)
          .build();
    });
    ViewCompat.requestApplyInsets(content);
  }
}
