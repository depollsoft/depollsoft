package depollsoft.lib.ui;

import android.app.Activity;
import android.view.View;

public class ThreadSwitchContext {
  private Activity activity;
  private View view;

  public ThreadSwitchContext(Activity activity) {
    this.activity = activity;
  }

  public ThreadSwitchContext(View view) {
    this.view = view;
  }

  public void post(Runnable runnable) {
    if (this.activity != null)
      this.activity.runOnUiThread(runnable);
    else
      this.view.post(runnable);
  }
}
