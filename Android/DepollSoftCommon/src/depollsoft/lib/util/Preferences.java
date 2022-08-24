package depollsoft.lib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.bindroid.trackable.Trackable;

import depollsoft.lib.activity.RichApplication;
import depollsoft.lib.json.JsonSerializer;

public class Preferences {
  public static class Mapping {
    private Object key;
    private Object value;

    public Object getKey() {
      return key;
    }

    public void setKey(Object key) {
      this.key = key;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }
  }
  public static class MappingList extends ArrayList<Mapping> {}
  private static SharedPreferences preferences;
  private static Map<String, Trackable> trackableMap;
  static {
    Preferences.preferences = RichApplication.getAppContext()
        .getSharedPreferences("depollsoft.lib.Preferences",
            Context.MODE_PRIVATE);
    Preferences.trackableMap = new HashMap<String, Trackable>();
    Preferences.preferences
        .registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
          if (Preferences.trackableMap.containsKey(key))
            Preferences.trackableMap.remove(key).updateTrackers();
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

    Object result = JsonSerializer.deserialize(stringValue);
    if (result instanceof MappingList) {
      result = fromMappingList((MappingList)result);
    }
    return (T) result;
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

  private static MappingList fromMap(Map<?, ?> map) {
    MappingList list = new MappingList();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Mapping m = new Mapping();
      m.setKey(entry.getKey());
      m.setValue(entry.getValue());
      list.add(m);
    }
    return list;
  }

  private static Map<?, ?> fromMappingList(MappingList list) {
    Map<Object, Object> map = new HashMap<>();
    for (Mapping m : list) {
      map.put(m.getKey(), m.getValue());
    }
    return map;
  }

  public static boolean set(String key, Object value) {
    if (value instanceof Map) {
      value = fromMap((Map<?, ?>)value);
    }
    return Preferences.preferences.edit()
        .putString(key, JsonSerializer.serialize(value).toString()).commit();
  }
}
