package depollsoft.pitchperfect;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.ListView;

import com.bindroid.converters.AdapterConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.pitchperfect.lib.Note;

public class NoteListActivity extends Activity {

  private TrackableField<NoteListModel> model = new TrackableField<NoteListModel>();

  public NoteListActivity() {
    this.setModel(new NoteListModel());
  }

  public NoteListModel getModel() {
    return this.model.get();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    this.setContentView(R.layout.notelistview);

    UiBinder.bind(this, R.id.noteListView, "Adapter", "Model.Notes", new AdapterConverter(
        NoteListItemView.class));

    final ListView list = (ListView) this.findViewById(R.id.noteListView);
    list.post(new Runnable() {

      @Override
      public void run() {

        int totalVisible = list.getLastVisiblePosition() - list.getFirstVisiblePosition();
        list.setSelection(list.getCount() / 2 - totalVisible / 2);
      }
    });
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
    FlurryAgent.endTimedEvent("NoteListActivity");
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.runOnUiThread(new Runnable() {
      public void run() {
        GoogleAnalyticsTracker.getInstance().trackPageView("NoteListActivity");
        FlurryAgent.logEvent("NoteListActivity", true);
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    FlurryAgent.onStartSession(this, "B8F71MSD6E6KWMAK479A");
  }

  @Override
  protected void onStop() {
    super.onStop();
    FlurryAgent.onEndSession(this);
  }

  public void setModel(NoteListModel value) {
    this.model.set(value);
  }

}
