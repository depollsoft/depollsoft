package depollsoft.pitchperfect;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.AdapterConverter;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.ToggleButtonCheckedProperty;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.pitchperfect.lib.Key;
import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ToggleButton;

public class KeySignatureActivity extends Activity {

  private TrackableField<KeySignatureModel> model = new TrackableField<KeySignatureModel>();
  private ListView majorView;
  private ListView minorView;

  public KeySignatureActivity() {
    this.setModel(new KeySignatureModel());
  }

  private void centerList(int id) {
    final ListView list = (ListView) this.findViewById(id);
    final int priorVisibility = list.getVisibility();
    list.post(new Runnable() {
      public void run() {
        list.setVisibility(View.INVISIBLE);

        int totalVisible = list.getLastVisiblePosition()
            - list.getFirstVisiblePosition();
        list.setSelection(list.getCount() / 2 - totalVisible / 2);
        list.setVisibility(priorVisibility);
      }
    });
  }

  public KeySignatureModel getModel() {
    return this.model.getValue();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    this.setContentView(R.layout.keysignatureview);

    UiBinder.bind(
        this,
        new ToggleButtonCheckedProperty((ToggleButton) this
            .findViewById(R.id.majorMinorToggleButton)), "Model.IsMajor",
        BindingMode.TwoWay);

    UiBinder
        .bind(this, R.id.majorKeySignatureListView, "Adapter",
            "Model.MajorKeys", new AdapterConverter(
                KeySignatureListItemView.class));
    UiBinder.bind(this, R.id.majorKeySignatureListView, "Visibility",
        "Model.IsMajor", new BoolConverter());
    UiBinder
        .bind(this, R.id.minorKeySignatureListView, "Adapter",
            "Model.MinorKeys", new AdapterConverter(
                KeySignatureListItemView.class));
    UiBinder.bind(this, R.id.minorKeySignatureListView, "Visibility",
        "Model.IsMajor", new BoolConverter(true));

    this.majorView = (ListView) this
        .findViewById(R.id.majorKeySignatureListView);
    this.minorView = (ListView) this
        .findViewById(R.id.minorKeySignatureListView);

    ((ToggleButton) this.findViewById(R.id.majorMinorToggleButton))
        .setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            if (KeySignatureActivity.this.getModel().getIsMajor())
              KeySignatureActivity.this.majorView
                  .setSelection(KeySignatureActivity.this.minorView
                      .getFirstVisiblePosition());
            else
              KeySignatureActivity.this.minorView
                  .setSelection(KeySignatureActivity.this.majorView
                      .getFirstVisiblePosition());
            for (Key k : KeySignatureActivity.this.getModel().getMajorKeys())
              k.getNote().stop();
            for (Key k : KeySignatureActivity.this.getModel().getMinorKeys())
              k.getNote().stop();
          }
        });

    this.centerList(R.id.majorKeySignatureListView);
    this.centerList(R.id.minorKeySignatureListView);
  }

  @Override
  protected void onDestroy() {
    UiBinder.unbind(this);
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    for (Key k : this.getModel().getMajorKeys())
      k.getNote().stop();
    for (Key k : this.getModel().getMinorKeys())
      k.getNote().stop();
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.runOnUiThread(new Runnable() {
      public void run() {
        GoogleAnalyticsTracker.getInstance().trackPageView(
            "KeySignatureActivity");
      }
    });
  }

  public void setModel(KeySignatureModel value) {
    this.model.setValue(value);
  }

}
