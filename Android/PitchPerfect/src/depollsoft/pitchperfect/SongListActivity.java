package depollsoft.pitchperfect;

import java.util.Collections;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.bindroid.converters.AdapterConverter;
import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;
import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.parse.ParseUser;

import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.MenuItems;
import depollsoft.pitchperfect.lib.PitchedSong;

public class SongListActivity extends Activity {

  private TrackableField<SongsModel> model = new TrackableField<SongsModel>();

  private TrackableField<Boolean> editing = new TrackableField<Boolean>(true);

  private boolean preparingMenu;

  public SongListActivity() {
    this.setModel(SongsModel.get());
  }

  public boolean getEditing() {
    return this.editing.get();
  }

  public SongsModel getModel() {
    return this.model.get();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

    this.setContentView(R.layout.songlistview);

    UiBinder.bind(this, R.id.songListView, "Adapter", "Model.Songs", new AdapterConverter(
        SongListItemView.class));

    UiBinder.bind(this, R.id.sorryText, "Visibility", "Model.Songs[0]",
        BoolConverter.get(true, true));

    View addButton = this.findViewById(R.id.addSongButton);
    addButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(SongListActivity.this, AddSongActivity.class);
        SongListActivity.this.startActivityForResult(i, 1);
      }
    });
    if (ActionBars.hasActionBar(this)) {
      addButton.setVisibility(View.GONE);
    }

    View editButton = this.findViewById(R.id.editSongsButton);
    editButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        SongListActivity.this.setEditing(!SongListActivity.this.getEditing());
      }
    });
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    if (preparingMenu) {
      return true;
    }
    preparingMenu = true;
    try {
      super.onPrepareOptionsMenu(menu);
      MenuInflater mi = new MenuInflater(this);
      mi.inflate(R.menu.songsmenu, menu);

      MenuItems.setShowAsAction(menu.findItem(R.id.sortMenuItem), MenuItems.SHOW_AS_ACTION_IF_ROOM);
      menu.findItem(R.id.sortMenuItem).setOnMenuItemClickListener(new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
          SongsModel.get().sortSongs();
          return true;
        }
      });

      MenuItems.setShowAsAction(menu.findItem(R.id.addSongMenuItem),
          MenuItems.SHOW_AS_ACTION_IF_ROOM);
      menu.findItem(R.id.addSongMenuItem).setOnMenuItemClickListener(new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
          Intent i = new Intent(SongListActivity.this, AddSongActivity.class);
          SongListActivity.this.startActivityForResult(i, 2);
          return true;
        }
      });

      return true;
    } finally {
      preparingMenu = false;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    PitchPerfectActivity.handlingResult = true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    super.onPause();
    for (PitchedSong song : this.getModel().getSongs())
      song.stop();
    if (ParseUser.getCurrentUser() != null) {
      SongsModel.get().saveAllToParse();
    }
    FlurryAgent.endTimedEvent("SongListActivity");
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.runOnUiThread(new Runnable() {
      public void run() {
        GoogleAnalyticsTracker.getInstance().trackPageView("SongListActivity");
        FlurryAgent.logEvent("SongListActivity",
            Collections.singletonMap("SongCount", SongsModel.get().getSongs().size()), true);
      }
    });
  }

  public void setEditing(boolean value) {
    this.editing.set(value);
    String text;
    if (!value)
      text = this.getResources().getString(R.string.EditSongList);
    else
      text = this.getResources().getString(R.string.StopEditing);
    ((Button) this.findViewById(R.id.editSongsButton)).setText(text);
  }

  public void setModel(SongsModel value) {
    this.model.set(value);
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
}
