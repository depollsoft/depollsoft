package depollsoft.pitchperfect;

import java.util.Collections;
import java.util.Date;

import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.Trackable;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.Tracker;
import depollsoft.lib.json.JsonSerializer;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Preferences;
import depollsoft.pitchperfect.lib.PitchedSong;

public class SongsModel {
  private static final String SongsKey = "depollsoft.pitchperfect.SongsModel";
  private static final String SongsChangedKey = "depollsoft.pitchperfect.SongsChanged";
  private TrackableField<ObservableCollection<PitchedSong>> songs = new TrackableField<ObservableCollection<PitchedSong>>();
  private ParseObject serialized;
  private boolean suspendTimestamp;
  private boolean saving;
  private boolean refreshing;
  private boolean postponedSave;
  private boolean refreshPostponedSave;

  private static SongsModel instance;

  public static SongsModel get() {
    if (SongsModel.instance == null)
      SongsModel.instance = new SongsModel();
    return SongsModel.instance;
  }

  @SuppressWarnings("unchecked")
  private SongsModel() {
    this.suspendTimestamp = true;
    Preferences.initialize(SongsModel.SongsChangedKey, 0L, Long.TYPE);
    if (Preferences.get(SongsModel.SongsKey) == null)
      this.setSongs(new ObservableCollection<PitchedSong>());
    else
      this.setSongs((ObservableCollection<PitchedSong>) Preferences.get(SongsModel.SongsKey));
    this.suspendTimestamp = false;
    Trackable.track(new Tracker() {

      public void update() {
        SongsModel.this.storeValue();
        Trackable.track(this, new Action<Void>() {

          public void invoke(Void parameter) {
            SongsModel.this.getSongs().track();
          }
        });
      }
    }, new Action<Void>() {

      public void invoke(Void parameter) {
        SongsModel.this.getSongs().track();
      }
    });
  }

  public void addSong(PitchedSong song) {
    if (!this.getSongs().contains(song))
      this.getSongs().add(song);
  }

  public boolean canMoveDown(PitchedSong s) {
    int index = this.getSongs().indexOf(s);
    return index < this.getSongs().size() - 1;
  }

  public boolean canMoveUp(PitchedSong s) {
    int index = this.getSongs().indexOf(s);
    return index > 0;
  }

  @SuppressWarnings("unchecked")
  public void fromParseObject(ParseObject object) {
    this.suspendTimestamp = true;
    this.setSongs((ObservableCollection<PitchedSong>) JsonSerializer.deserialize(object
        .getJSONObject("songs")));
    this.suspendTimestamp = false;
    this.serialized = object;
  }

  private long getLastChangeTime() {
    return Preferences.get(SongsModel.SongsChangedKey);
  }

  public ObservableCollection<PitchedSong> getSongs() {
    return this.songs.getValue();
  }

  public void handleLogOut() {
    this.setLastChangeTime(0);
  }

  public void moveDown(PitchedSong s) {
    if (!this.canMoveDown(s))
      return;
    int index = this.getSongs().indexOf(s);
    this.getSongs().remove(index);
    this.getSongs().add(index + 1, s);
  }

  public void moveUp(PitchedSong s) {
    if (!this.canMoveUp(s))
      return;
    int index = this.getSongs().indexOf(s);
    this.getSongs().remove(index);
    this.getSongs().add(index - 1, s);
  }

  public void notifyOfChange() {
    this.songs.updateTrackers();
  }

  public void refreshFromParse() {
    refreshing = true;
    ParseQuery query = new ParseQuery("SongList");
    try {
      query.getFirstInBackground(new GetCallback() {

        @Override
        public void done(ParseObject main, ParseException ex) {
          refreshing = false;
          if (ex != null) {
            return;
          }
          if (main != null && SongsModel.this.getLastChangeTime() < main.getUpdatedAt().getTime()) {
            SongsModel.this.fromParseObject(main);
          }
          else if (refreshPostponedSave) {
            saveAllToParse();
          }
          refreshPostponedSave = false;
        }
      });
    }
    catch (Exception e) {
      // It's ok -- it just means that a query is already ongoing.
      refreshing = false;
    }
  }

  public void removeSong(PitchedSong song) {
    this.getSongs().remove(song);
  }

  public void resetSongs() {
    this.getSongs().clear();
  }

  public void saveAllToParse() {
    this.saveAllToParse(false);
  }

  public void saveAllToParse(boolean immediately) {
    if (ParseUser.getCurrentUser() == null) {
      return;
    }
    if (refreshing) {
      refreshPostponedSave = true;
      return;
    }
    if (this.saving) {
      this.postponedSave = true;
      return;
    }
    if (immediately) {
      this.saving = true;
      this.toParseObject().saveInBackground(new SaveCallback() {

        @Override
        public void done(ParseException ex) {
          SongsModel.this.saving = false;
          if (SongsModel.this.postponedSave) {
            SongsModel.this.saveAllToParse(false);
          }
          SongsModel.this.postponedSave = false;
        }
      });
    }
    else if (this.serialized == null || this.serialized.getUpdatedAt() == null
        || this.getLastChangeTime() > this.serialized.getUpdatedAt().getTime()) {
      this.toParseObject().saveEventually();
    }
  }

  private void setLastChangeTime(long time) {
    if (this.suspendTimestamp)
      return;
    if (this.serialized == null) {
      Preferences.set(SongsModel.SongsChangedKey, 0L);
    }
    else {
      Preferences.set(SongsModel.SongsChangedKey, time);
    }
  }

  public void setSongs(ObservableCollection<PitchedSong> value) {
    this.songs.setValue(value);
  }

  public void sortSongs() {
    Collections.sort(this.getSongs());
    this.getSongs().updateTrackers();
  }

  private void storeValue() {
    Preferences.set(SongsModel.SongsKey, this.getSongs());
    if (!suspendTimestamp) {
      this.saveAllToParse();
    }
    this.setLastChangeTime(new Date().getTime());
  }

  public ParseObject toParseObject() {
    if (this.serialized == null) {
      this.serialized = new ParseObject("SongList");
      this.serialized.setACL(new ParseACL(ParseUser.getCurrentUser()));
      this.serialized.put("name", "*default");
    }
    this.serialized.put("songs", JsonSerializer.serialize(this.getSongs()));
    return this.serialized;
  }
}
