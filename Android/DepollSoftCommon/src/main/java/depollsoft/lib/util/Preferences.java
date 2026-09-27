package depollsoft.lib.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import depollsoft.lib.state.ChangeSignal;

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
  private static Map<String, ChangeSignal> trackableMap;
  private static boolean initialized = false;

  // Test mode support
  private static boolean testMode = false;
  private static Map<String, Object> testValues = new HashMap<>();
  /** Per-key change signals for the test store, so observers behave as they do on a device. */
  private static final Map<String, ChangeSignal> testSignals = new HashMap<>();

  private static ChangeSignal testSignal(String key) {
    ChangeSignal signal = testSignals.get(key);
    if (signal == null) {
      signal = new ChangeSignal();
      testSignals.put(key, signal);
    }
    return signal;
  }

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
    Preferences.trackableMap = new HashMap<String, ChangeSignal>();
    Preferences.preferences
        .registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
          ChangeSignal signal = Preferences.trackableMap.get(key);
          if (signal != null) {
            signal.changed();
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
      testSignal(key).read();
      return (T) testValues.get(key);
    }

    ensureInitialized();
    if (Preferences.trackableMap == null) {
      return null;
    }
    if (!Preferences.trackableMap.containsKey(key))
      Preferences.trackableMap.put(key, new ChangeSignal());
    Preferences.trackableMap.get(key).read();
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
        // As on a device, where initialize stores through set and the preference listener fires.
        testSignal(key).changed();
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

  /** Writes synchronously and returns whether the disk write succeeded. */
  public static boolean set(String key, Object value) {
    return set(key, value, true);
  }

  /** Updates in-memory preferences immediately and schedules the disk write. */
  public static void setAsync(String key, Object value) {
    set(key, value, false);
  }

  private static boolean set(String key, Object value, boolean synchronous) {
    if (testMode) {
      Object previous = value == null ? testValues.remove(key) : testValues.put(key, value);
      // Like SharedPreferences' listener, an equal value is no change. The same collection or
      // object set again may have been edited in place, so it counts: on a device its stored
      // string would differ.
      boolean editedInPlace = previous == value && value != null && !(value instanceof String
          || value instanceof Number || value instanceof Boolean);
      if (editedInPlace || !java.util.Objects.equals(previous, value)) {
        testSignal(key).changed();
      }
      return true;
    }

    ensureInitialized();
    if (Preferences.preferences == null) {
      return false;
    }
    SharedPreferences.Editor editor = Preferences.preferences.edit();
    if (value == null) {
      editor.remove(key);
    } else {
      if (value instanceof Map) {
        value = fromMap((Map<?, ?>)value);
      }
      editor.putString(key, JsonSerializer.serialize(value).toString());
    }
    if (synchronous) {
      return editor.commit();
    }
    editor.apply();
    return true;
  }
}
