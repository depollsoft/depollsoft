package depollsoft.tagmaster;

import org.json.JSONArray;
import org.json.JSONException;

import com.bindroid.trackable.Trackable;
import com.bindroid.trackable.TrackableCollection;
import com.bindroid.trackable.TrackableField;
import com.bindroid.trackable.Tracker;
import com.bindroid.utils.Action;
import com.parse.ParseUser;

import depollsoft.lib.util.Preferences;

@SuppressWarnings("unchecked")
public class TeachableTagsModel {
  private static final String TeachableTagsPreference = "tagmaster.TeachableTags";
  private static TrackableField<TrackableCollection<Integer>> teachableTagIds = new TrackableField<TrackableCollection<Integer>>();

  static {
    if (Preferences.get(TeachableTagsModel.TeachableTagsPreference) == null)
      TeachableTagsModel.setTeachableTagIds(new TrackableCollection<Integer>());
    else
      TeachableTagsModel.setTeachableTagIds((TrackableCollection<Integer>) Preferences
          .get(TeachableTagsModel.TeachableTagsPreference));
    Trackable.track(new Tracker() {

      public void update() {
        TeachableTagsModel.storeValue();
        Trackable.track(this, new Action<Void>() {

          public void invoke(Void parameter) {
            TeachableTagsModel.getTeachableTagIds().track();
          }
        });
      }
    }, new Action<Void>() {

      public void invoke(Void parameter) {
        TeachableTagsModel.getTeachableTagIds().track();
      }
    });
  }

  public static void addTeachableTag(int id) {
    if (!TeachableTagsModel.getTeachableTagIds().contains(id))
      TeachableTagsModel.getTeachableTagIds().add(id);
  }

  public static boolean canMoveDown(int id) {
    int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
    return index < TeachableTagsModel.getTeachableTagIds().size() - 1;
  }

  public static boolean canMoveUp(int id) {
    int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
    return index > 0;
  }

  public static boolean getIsTeachableTag(int id) {
    return TeachableTagsModel.getTeachableTagIds().contains(id);
  }

  public static TrackableCollection<Integer> getTeachableTagIds() {
    return TeachableTagsModel.teachableTagIds.get();
  }

  public static void moveDown(int id) {
    int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
    TeachableTagsModel.getTeachableTagIds().remove(index);
    TeachableTagsModel.getTeachableTagIds().add(index + 1, id);
  }

  public static void moveUp(int id) {
    int index = TeachableTagsModel.getTeachableTagIds().indexOf(id);
    TeachableTagsModel.getTeachableTagIds().remove(index);
    TeachableTagsModel.getTeachableTagIds().add(index - 1, id);
  }

  public static void removeTeachableTag(int id) {
    TeachableTagsModel.getTeachableTagIds().remove(Integer.valueOf(id));
  }

  public static void resetTeachableTags() {
    TeachableTagsModel.getTeachableTagIds().clear();
  }

  public static void restoreFromUser() {
    if (ParseUser.getCurrentUser() != null) {
      JSONArray ids = ParseUser.getCurrentUser().getJSONArray("TeachableIds");
      if (ids == null)
        return;
      TrackableCollection<Integer> newIds = new TrackableCollection<Integer>();
      for (int i = 0; i < ids.length(); i++) {
        try {
          newIds.add(ids.getInt(i));
        } catch (JSONException e) {
        }
      }
      TeachableTagsModel.setTeachableTagIds(newIds);
    }
  }

  public static void setTeachableTagIds(TrackableCollection<Integer> value) {
    TeachableTagsModel.teachableTagIds.set(value);
  }

  public static void storeToUser() {
    if (ParseUser.getCurrentUser() != null) {
      try {
        JSONArray ids = new JSONArray(TeachableTagsModel.getTeachableTagIds());
        ParseUser.getCurrentUser().put("TeachableIds", ids);
      } catch (Exception e) {
      }
    }
  }

  private static void storeValue() {
    Preferences.set(TeachableTagsModel.TeachableTagsPreference,
        TeachableTagsModel.getTeachableTagIds());
    if (ParseUser.getCurrentUser() != null) {
      TeachableTagsModel.storeToUser();
      ParseUser.getCurrentUser().saveEventually();
    }
  }
}
