package depollsoft.tagmaster;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.tagmaster.barbershop.RemoteLocation;

public class TagTracksActivity extends Activity {

  private TrackableField<RemoteLocation> selectedTrack = new TrackableField<RemoteLocation>();

  public TagTracksActivity() {
  }

  public RemoteLocation getSelectedTrack() {
    return this.selectedTrack.get();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagtracksview);

    UiBinder.bind(this, R.id.allPartsButton, "Visibility", "Parent.Tag.AllPartsTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.allPartsButton, "Enabled", "Parent.Tag.AllPartsTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.allPartsButton, "Tag", "Parent.Tag.AllPartsTrackUri");

    UiBinder.bind(this, R.id.tenorButton, "Visibility", "Parent.Tag.TenorTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.tenorButton, "Enabled", "Parent.Tag.TenorTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.tenorButton, "Tag", "Parent.Tag.TenorTrackUri");

    UiBinder.bind(this, R.id.leadButton, "Visibility", "Parent.Tag.LeadTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.leadButton, "Enabled", "Parent.Tag.LeadTrackUri", BoolConverter.get());
    UiBinder.bind(this, R.id.leadButton, "Tag", "Parent.Tag.LeadTrackUri");

    UiBinder.bind(this, R.id.bariButton, "Visibility", "Parent.Tag.BaritoneTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.bariButton, "Enabled", "Parent.Tag.BaritoneTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.bariButton, "Tag", "Parent.Tag.BaritoneTrackUri");

    UiBinder.bind(this, R.id.bassButton, "Visibility", "Parent.Tag.BassTrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.bassButton, "Enabled", "Parent.Tag.BassTrackUri", BoolConverter.get());
    UiBinder.bind(this, R.id.bassButton, "Tag", "Parent.Tag.BassTrackUri");

    UiBinder.bind(this, R.id.other1Button, "Visibility", "Parent.Tag.Other1TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other1Button, "Enabled", "Parent.Tag.Other1TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other1Button, "Tag", "Parent.Tag.Other1TrackUri");

    UiBinder.bind(this, R.id.other2Button, "Visibility", "Parent.Tag.Other2TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other2Button, "Enabled", "Parent.Tag.Other2TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other2Button, "Tag", "Parent.Tag.Other2TrackUri");

    UiBinder.bind(this, R.id.other3Button, "Visibility", "Parent.Tag.Other3TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other3Button, "Enabled", "Parent.Tag.Other3TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other3Button, "Tag", "Parent.Tag.Other3TrackUri");

    UiBinder.bind(this, R.id.other4Button, "Visibility", "Parent.Tag.Other4TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other4Button, "Enabled", "Parent.Tag.Other4TrackUri",
        BoolConverter.get());
    UiBinder.bind(this, R.id.other4Button, "Tag", "Parent.Tag.Other4TrackUri");

    UiBinder.bind(this, R.id.trackNotesTextView, "Text", "Parent.Tag.RecordingMethod");
    UiBinder.bind(this, R.id.trackNotesLayout, "Visibility", "Parent.Tag.RecordingMethod",
        BoolConverter.get());

    UiBinder.bind(this, R.id.sorryTextView, "Visibility", "Parent.Tag.Tracks",
        BoolConverter.get(true, true));

    UiBinder.bind(this, R.id.mediaPlayer, "RemoteLocation", "SelectedTrack");
    UiBinder.bind(this, R.id.mediaPlayer, "Enabled", "SelectedTrack", BoolConverter.get());

    RadioGroup group = (RadioGroup) this.findViewById(R.id.partsRadioGroup);
    group.setOnCheckedChangeListener(new OnCheckedChangeListener() {

      public void onCheckedChanged(RadioGroup group, int checkedId) {
        RadioButton rb = (RadioButton) group.findViewById(checkedId);
        TagTracksActivity.this.setSelectedTrack((RemoteLocation) rb.getTag());
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (this.getParent() != null)
      return this.getParent().onMenuItemSelected(featureId, item);
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.runOnUiThread(new Runnable() {
      public void run() {
        GoogleAnalyticsTracker.getInstance().trackPageView(
            "TagDetailActivity/"
                + ((TagDetailActivity) TagTracksActivity.this.getParent()).getTagId() + "/tracks");
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    FlurryAgent.onStartSession(this, "V5L1948BNDQCKZFPARJ9");
  }

  @Override
  protected void onStop() {
    super.onStop();
    FlurryAgent.onEndSession(this);
    stopMedia();
  }

  public void setSelectedTrack(RemoteLocation value) {
    this.selectedTrack.set(value);
  }

  public void stopMedia() {
    ((MediaPlayerView) this.findViewById(R.id.mediaPlayer)).stop();
  }

}
