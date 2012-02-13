package depollsoft.lib.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import depollsoft.lib.util.Versioning;

public class ChangelogViewer extends AlertDialog {
  private static boolean hasBeenShown;
  private String changelogText;

  public ChangelogViewer(Context context, boolean cancelable, OnCancelListener cancelListener,
      String changelogText) {
    super(context, cancelable, cancelListener);
    this.changelogText = changelogText;
  }

  public ChangelogViewer(Context context, int theme, String changelogText) {
    super(context, theme);
    this.changelogText = changelogText;
  }

  public ChangelogViewer(Context context, String changelogText) {
    super(context);
    this.changelogText = changelogText;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setButton("OK", new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dismiss();
      }
    });
    setMessage(Html.fromHtml(changelogText));
    super.onCreate(savedInstanceState);
  }

  public void showIfAppropriate() {
    if (Versioning.isFirstRunOfVersion() && !hasBeenShown) {
      show();
      hasBeenShown = true;
    }
  }
}
