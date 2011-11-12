package depollsoft.pitchperfect;

import java.util.Collections;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.Trackable;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.Tracker;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Preferences;
import depollsoft.pitchperfect.lib.PitchedSong;

public class SongsModel
{
   private static final String SongsKey = "depollsoft.pitchperfect.SongsModel";
   private TrackableField<ObservableCollection<PitchedSong>> songs = new TrackableField<ObservableCollection<PitchedSong>>();

   private static SongsModel instance;

   public static SongsModel get()
   {
      if (SongsModel.instance == null)
         SongsModel.instance = new SongsModel();
      return SongsModel.instance;
   }

   @SuppressWarnings("unchecked")
   private SongsModel()
   {
      if (Preferences.get(SongsModel.SongsKey) == null)
         this.setSongs(new ObservableCollection<PitchedSong>());
      else
         this.setSongs((ObservableCollection<PitchedSong>) Preferences
               .get(SongsModel.SongsKey));
      Trackable.track(new Tracker()
      {

         public void update()
         {
            SongsModel.this.storeValue();
            Trackable.track(this, new Action<Void>()
            {

               public void invoke(Void parameter)
               {
                  SongsModel.this.getSongs().track();
               }
            });
         }
      }, new Action<Void>()
      {

         public void invoke(Void parameter)
         {
            SongsModel.this.getSongs().track();
         }
      });
   }

   public void addSong(PitchedSong song)
   {
      if (!this.getSongs().contains(song))
         this.getSongs().add(song);
   }

   public boolean canMoveDown(PitchedSong s)
   {
      int index = this.getSongs().indexOf(s);
      return index < this.getSongs().size() - 1;
   }

   public boolean canMoveUp(PitchedSong s)
   {
      int index = this.getSongs().indexOf(s);
      return index > 0;
   }

   public ObservableCollection<PitchedSong> getSongs()
   {
      return this.songs.getValue();
   }
   
   public void sortSongs()
   {
      Collections.sort(getSongs());
      getSongs().updateTrackers();
   }

   public void moveDown(PitchedSong s)
   {
      if (!this.canMoveDown(s))
         return;
      int index = this.getSongs().indexOf(s);
      this.getSongs().remove(index);
      this.getSongs().add(index + 1, s);
   }

   public void moveUp(PitchedSong s)
   {
      if (!this.canMoveUp(s))
         return;
      int index = this.getSongs().indexOf(s);
      this.getSongs().remove(index);
      this.getSongs().add(index - 1, s);
   }

   public void removeSong(PitchedSong song)
   {
      this.getSongs().remove(song);
   }

   public void resetSongs()
   {
      this.getSongs().clear();
   }

   public void setSongs(ObservableCollection<PitchedSong> value)
   {
      this.songs.setValue(value);
   }

   private void storeValue()
   {
      Preferences.set(SongsModel.SongsKey, this.getSongs());
   }
}
