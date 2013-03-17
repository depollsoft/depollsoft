package depollsoft.tagmaster;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Handler;
import android.os.Looper;

import com.bindroid.trackable.Trackable;
import com.bindroid.trackable.TrackableCollection;
import com.bindroid.trackable.TrackableField;
import com.bindroid.trackable.Tracker;
import com.bindroid.utils.Action;
import com.parse.ParseUser;

import depollsoft.lib.util.Preferences;

@SuppressWarnings("unchecked")
public class FavoritesModel {
  private static final String FavoritesPreference = "tagmaster.Favorites";
  private static TrackableField<TrackableCollection<Integer>> favoriteIds = new TrackableField<TrackableCollection<Integer>>();

  static {
    if (Preferences.get(FavoritesModel.FavoritesPreference) == null)
      FavoritesModel.setFavoriteIds(new TrackableCollection<Integer>());
    else
      FavoritesModel.setFavoriteIds((TrackableCollection<Integer>) Preferences
          .get(FavoritesModel.FavoritesPreference));
    TrackableCollection<?> objects = FavoritesModel.getFavoriteIds();
    for (int x = 0; x < objects.size(); x++) {
      if (!(objects.get(x).getClass() == Integer.TYPE || objects.get(x) instanceof Integer)) {
        try {
          ((TrackableCollection<Integer>) objects).set(x,
              Integer.valueOf(objects.get(x).toString()));
        }
        catch (Exception e) {
          // Oh well...
          objects.remove(x);
          x--;
        }
      }
    }
    Trackable.track(new Tracker() {

      public void update() {
        FavoritesModel.storeValue();
        Trackable.track(this, new Action<Void>() {

          public void invoke(Void parameter) {
            FavoritesModel.getFavoriteIds().track();
          }
        });
      }
    }, new Action<Void>() {

      public void invoke(Void parameter) {
        FavoritesModel.getFavoriteIds().track();
      }
    });
  }

  public static void addFavorite(int id) {
    if (!FavoritesModel.getFavoriteIds().contains(id))
      FavoritesModel.getFavoriteIds().add(id);
  }

  public static boolean canMoveDown(int id) {
    int index = FavoritesModel.getFavoriteIds().indexOf(id);
    return index < FavoritesModel.getFavoriteIds().size() - 1;
  }

  public static boolean canMoveUp(int id) {
    int index = FavoritesModel.getFavoriteIds().indexOf(id);
    return index > 0;
  }

  public static TrackableCollection<Integer> getFavoriteIds() {
    return FavoritesModel.favoriteIds.get();
  }

  public static boolean getIsFavorite(int id) {
    return FavoritesModel.getFavoriteIds().contains(id);
  }

  public static void moveDown(int id) {
    int index = FavoritesModel.getFavoriteIds().indexOf(id);
    FavoritesModel.getFavoriteIds().remove(index);
    FavoritesModel.getFavoriteIds().add(index + 1, id);
  }

  public static void moveUp(int id) {
    int index = FavoritesModel.getFavoriteIds().indexOf(id);
    FavoritesModel.getFavoriteIds().remove(index);
    FavoritesModel.getFavoriteIds().add(index - 1, id);
  }

  public static void removeFavorite(int id) {
    FavoritesModel.getFavoriteIds().remove(Integer.valueOf(id));
  }

  public static void resetFavorites() {
    FavoritesModel.getFavoriteIds().clear();
  }

  public static void restoreFromUser() {
    if (ParseUser.getCurrentUser() != null) {
      JSONArray ids = ParseUser.getCurrentUser().getJSONArray("FavoriteIds");
      if (ids == null)
        return;
      TrackableCollection<Integer> newIds = new TrackableCollection<Integer>();
      for (int i = 0; i < ids.length(); i++) {
        try {
          newIds.add(ids.getInt(i));
        }
        catch (JSONException e) {
        }
      }
      FavoritesModel.setFavoriteIds(newIds);
    }
  }

  public static void setFavoriteIds(TrackableCollection<Integer> value) {
    FavoritesModel.favoriteIds.set(value);
  }

  public static void storeToUser() {
    if (ParseUser.getCurrentUser() != null) {
      try {
        JSONArray ids = new JSONArray(FavoritesModel.getFavoriteIds());
        try {
          ParseUser.getCurrentUser().put("FavoriteIds", ids);
        }
        catch (Exception e) {
          new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

            public void run() {
              storeToUser();
            }
          }, 100);
        }
      }
      catch (Exception e) {
      }
    }
  }

  private static void storeValue() {
    Preferences.set(FavoritesModel.FavoritesPreference, FavoritesModel.getFavoriteIds());
    if (ParseUser.getCurrentUser() != null) {
      FavoritesModel.storeToUser();
      ParseUser.getCurrentUser().saveEventually();
    }
  }
}
