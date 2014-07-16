package depollsoft.pitchperfect;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;

import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;

import depollsoft.pitchperfect.converters.PitchPipeNoteTextConverter;
import depollsoft.pitchperfect.lib.Note;

public class PitchPipeActivity extends Activity {

  private TrackableField<PitchPipeModel> model = new TrackableField<PitchPipeModel>();

  public PitchPipeActivity() {
    this.setModel(new PitchPipeModel());
  }

  public PitchPipeModel getModel() {
    return this.model.get();
  }

  public boolean getToggle() {
    return SettingsModel.getToggleNotes();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    this.setContentView(R.layout.activity_pitch_pipe);

    WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
    stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
      @Override
      public void onLayoutInflated(WatchViewStub watchViewStub) {
        setupAfterInflation();
      }
    });
  }

  private void setupAfterInflation() {
    UiBinder.bind(this, R.id.pitchButton0, "Note", "Model.Notes[0]");
    UiBinder.bind(this, R.id.pitchButton1, "Note", "Model.Notes[1]");
    UiBinder.bind(this, R.id.pitchButton2, "Note", "Model.Notes[2]");
    UiBinder.bind(this, R.id.pitchButton3, "Note", "Model.Notes[3]");
    UiBinder.bind(this, R.id.pitchButton4, "Note", "Model.Notes[4]");
    UiBinder.bind(this, R.id.pitchButton5, "Note", "Model.Notes[5]");
    UiBinder.bind(this, R.id.pitchButton6, "Note", "Model.Notes[6]");
    UiBinder.bind(this, R.id.pitchButton7, "Note", "Model.Notes[7]");
    UiBinder.bind(this, R.id.pitchButton8, "Note", "Model.Notes[8]");
    UiBinder.bind(this, R.id.pitchButton9, "Note", "Model.Notes[9]");
    UiBinder.bind(this, R.id.pitchButton10, "Note", "Model.Notes[10]");
    UiBinder.bind(this, R.id.pitchButton11, "Note", "Model.Notes[11]");

    UiBinder.bind(this, R.id.pitchButton0, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton1, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton2, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton3, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton4, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton5, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton6, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton7, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton8, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton9, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton10, "IsToggle", "Toggle");
    UiBinder.bind(this, R.id.pitchButton11, "IsToggle", "Toggle");

    UiBinder.bind(this, R.id.pitchButton0, "Text", "Model.Notes[0]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton1, "Text", "Model.Notes[1]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton2, "Text", "Model.Notes[2]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton3, "Text", "Model.Notes[3]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton4, "Text", "Model.Notes[4]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton5, "Text", "Model.Notes[5]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton6, "Text", "Model.Notes[6]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton7, "Text", "Model.Notes[7]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton8, "Text", "Model.Notes[8]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton9, "Text", "Model.Notes[9]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton10, "Text", "Model.Notes[10]",
        new PitchPipeNoteTextConverter());
    UiBinder.bind(this, R.id.pitchButton11, "Text", "Model.Notes[11]",
        new PitchPipeNoteTextConverter());

    final RadioButton fToF = (RadioButton) this.findViewById(R.id.fToFButton);
    final RadioButton cToC = (RadioButton) this.findViewById(R.id.cToCButton);
    fToF.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        cToC.setChecked(false);
        PitchPipeActivity.this.getModel().setIsFromFToF(true);
      }
    });
    cToC.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        fToF.setChecked(false);
        PitchPipeActivity.this.getModel().setIsFromFToF(false);
      }
    });

    if (this.getModel().getIsFromFToF())
      fToF.setChecked(true);
    else
      cToC.setChecked(true);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    for (Note n : this.getModel().getNotes())
      n.stop();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
  }

  public void setModel(PitchPipeModel value) {
    this.model.set(value);
  }
}
