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
  private static boolean initialized = false;

  // Test mode support
  private static boolean testMode = false;
  private static Map<String, Object> testValues = new HashMap<>();

  /**
   * Lazily initialize SharedPreferences. This is done lazily to allow test mode
   * to be set before any SharedPreferences access is attempted.
   */
  private static synchronized void ensureInitialized() {
    if (initialized || testMode) {
      return;
    }
    Context context = RichApplication.getAppContext();
    if (context == null) {
      // In test mode without proper context, just mark as initialized
      // and operations will fail gracefully or use test mode
      return;
    }
    Preferences.preferences = context
        .getSharedPreferences("depollsoft.lib.Preferences",
            Context.MODE_PRIVATE);
    Preferences.trackableMap = new HashMap<String, Trackable>();
    Preferences.preferences
        .registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
          Trackable trackable = Preferences.trackableMap.remove(key);
          if (trackable != null) {
            trackable.updateTrackers();
          }
        });
    initialized = true;
  }

  /**
   * Enable test mode to allow tests to run without requiring real SharedPreferences.
   * When in test mode, get/set/initialize operations use an in-memory map.
   */
  public static void setTestMode(boolean enabled) {
    testMode = enabled;
    if (!enabled) {
      testValues.clear();
    }
  }

  /**
   * Check if test mode is enabled.
   */
  public static boolean isTestMode() {
    return testMode;
  }

  /**
   * Clear all test values. Only effective in test mode.
   */
  public static void clearTestValues() {
    testValues.clear();
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(String key) {
    if (testMode) {
      return (T) testValues.get(key);
    }

    ensureInitialized();
    if (Preferences.trackableMap == null) {
      return null;
    }
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
    if (testMode) {
      if (value != null && !testValues.containsKey(key)) {
        testValues.put(key, value);
      }
      return;
    }

    ensureInitialized();
    if (Preferences.preferences == null) {
      return;
    }
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
    if (testMode) {
      if (value == null) {
        testValues.remove(key);
      } else {
        testValues.put(key, value);
      }
      return true;
    }

    ensureInitialized();
    if (Preferences.preferences == null) {
      return false;
    }
    if (value == null) {
      return Preferences.preferences.edit().remove(key).commit();
    }
    if (value instanceof Map) {
      value = fromMap((Map<?, ?>)value);
    }
    return Preferences.preferences.edit()
        .putString(key, JsonSerializer.serialize(value).toString()).commit();
  }
}
