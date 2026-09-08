package depollsoft.tagmaster;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** A native, keyboard and accessibility-operable rating decision. */
public class RatingsPopup {
  private final AlertDialog dialog;
  private Integer rating;

  public RatingsPopup(Context context) {
    View content = LayoutInflater.from(context).inflate(R.layout.ratingview, null);
    RatingBar ratingBar = content.findViewById(R.id.ratingBar1);
    dialog = new MaterialAlertDialogBuilder(context)
        .setTitle(R.string.ChooseARating)
        .setView(content)
        .setPositiveButton(R.string.detail_submit, (d, which) -> rating = (int) ratingBar.getRating())
        .setNegativeButton(android.R.string.cancel, (d, which) -> rating = null)
        .create();
    dialog.setOnShowListener(d -> {
      dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(ratingBar.getRating() >= 1);
      ratingBar.setOnRatingBarChangeListener((bar, value, fromUser) ->
          dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(value >= 1));
    });
  }

  public Integer getRating() {
    return rating;
  }

  public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
    dialog.setOnDismissListener(listener);
  }

  public void show() {
    rating = null;
    dialog.show();
  }

  public void dismiss() {
    dialog.dismiss();
  }
}
