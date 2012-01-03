package depollsoft.tagmaster;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.Trackable;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.Tracker;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.Preferences;

@SuppressWarnings("unchecked")
public class FavoritesModel {
  private static final String FavoritesPreference = "tagmaster.Favorites";
  private static TrackableField<ObservableCollection<Integer>> favoriteIds = new TrackableField<ObservableCollection<Integer>>();

  static {
    if (Preferences.get(FavoritesModel.FavoritesPreference) == null)
      FavoritesModel.setFavoriteIds(new ObservableCollection<Integer>());
    else
      FavoritesModel.setFavoriteIds((ObservableCollection<Integer>) Preferences
          .get(FavoritesModel.FavoritesPreference));
    ObservableCollection<?> objects = FavoritesModel.getFavoriteIds();
    for (int x = 0; x < objects.size(); x++) {
      if (!(objects.get(x).getClass() == Integer.TYPE || objects.get(x) instanceof Integer)) {
        try {
          ((ObservableCollection<Integer>) objects).set(x, new Integer(objects
              .get(x).toString()));
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

  public static ObservableCollection<Integer> getFavoriteIds() {
    return FavoritesModel.favoriteIds.getValue();
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
    FavoritesModel.getFavoriteIds().remove(new Integer(id));
  }

  public static void resetFavorites() {
    FavoritesModel.getFavoriteIds().clear();
  }

  public static void setFavoriteIds(ObservableCollection<Integer> value) {
    FavoritesModel.favoriteIds.setValue(value);
  }

  private static void storeValue() {
    Preferences.set(FavoritesModel.FavoritesPreference,
        FavoritesModel.getFavoriteIds());
  }
}
