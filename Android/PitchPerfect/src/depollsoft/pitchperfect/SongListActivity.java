package depollsoft.pitchperfect;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.parse.ParseUser;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.AdapterConverter;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.MenuItems;
import depollsoft.pitchperfect.lib.PitchedSong;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SongListActivity extends Activity {

  private TrackableField<SongsModel> model = new TrackableField<SongsModel>();

  private TrackableField<Boolean> editing = new TrackableField<Boolean>(true);

  private boolean preparingMenu;

  public SongListActivity() {
    this.setModel(SongsModel.get());
  }

  public boolean getEditing() {
    return this.editing.getValue();
  }

  public SongsModel getModel() {
    return this.model.getValue();
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
        SongListActivity.this.startActivity(i);
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
          SongListActivity.this.startActivity(i);
          return true;
        }
      });

      return true;
    }
    finally {
      preparingMenu = false;
    }
  }

  @Override
  protected void onDestroy() {
    UiBinder.unbind(this);
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
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.runOnUiThread(new Runnable() {
      public void run() {
        GoogleAnalyticsTracker.getInstance().trackPageView("SongListActivity");
      }
    });
  }

  public void setEditing(boolean value) {
    this.editing.setValue(value);
    String text;
    if (!value)
      text = this.getResources().getString(R.string.EditSongList);
    else
      text = this.getResources().getString(R.string.StopEditing);
    ((Button) this.findViewById(R.id.editSongsButton)).setText(text);
  }

  public void setModel(SongsModel value) {
    this.model.setValue(value);
  }

}
