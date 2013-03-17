package depollsoft.lib.util;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.bindroid.trackable.Trackable;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.json.JsonSerializer;

public class Preferences {
  private static SharedPreferences preferences;
  private static Map<String, Trackable> trackableMap;
  static {
    Preferences.preferences = RichApplication.getAppContext()
        .getSharedPreferences("depollsoft.lib.Preferences",
            Context.MODE_PRIVATE);
    Preferences.trackableMap = new HashMap<String, Trackable>();
    Preferences.preferences
        .registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
          public void onSharedPreferenceChanged(
              SharedPreferences sharedPreferences, String key) {
            if (Preferences.trackableMap.containsKey(key))
              Preferences.trackableMap.remove(key).updateTrackers();
          }
        });
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(String key) {
    if (!Preferences.trackableMap.containsKey(key))
      Preferences.trackableMap.put(key, new Trackable());
    Preferences.trackableMap.get(key).track();
    String stringValue = Preferences.preferences.getString(key, null);
    if (stringValue == null)
      return null;
    return (T) JsonSerializer.deserialize(stringValue);
  }

  public static void initialize(String key, Object value) {
    Preferences.initialize(key, value, Object.class);
  }

  public static void initialize(String key, Object value, Class<?> type) {
    if (Preferences.preferences.contains(key)) {
      try {
        Object initialValue = Preferences.get(key);
        type.cast(initialValue);
        return;
      }
      catch (Exception e) {
      }
    }
    Preferences.set(key, value);
  }

  public static boolean set(String key, Object value) {
    return Preferences.preferences.edit()
        .putString(key, JsonSerializer.serialize(value).toString()).commit();
  }
}
