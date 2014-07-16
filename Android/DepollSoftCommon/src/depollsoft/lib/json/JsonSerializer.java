package depollsoft.lib.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bindroid.utils.Pair;

@SuppressWarnings("ALL")
public class JsonSerializer {
  private static class JsonPrimitive {
    private Object value;
    private String type;

    public JsonPrimitive() {
    }

    public Object getTrueValue() {
      if (this.getValue() == null)
        return null;
      try {
        Class<?> trueType = JsonSerializer.getClassForName(this.type);
        if (this.getValue().getClass().equals(trueType))
          return this.getValue();
        return trueType.getConstructor(String.class).newInstance("" + this.getValue());
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @SuppressWarnings("unused")
    public String getType() {
      return this.type;
    }

    public Object getValue() {
      return this.value;
    }

    public void setType(String value) {
      this.type = value;
    }

    public void setValue(Object value) {
      this.value = value;
    }
  }

  public static Class<?> PRIMITIVE_CLASS = JsonPrimitive.class;

  private static Map<Class<?>, List<Pair<Method, Method>>> properties;
  private static Map<Pair<Class<?>, String>, Pair<Method, Method>> revProperties;
  private static Map<Class<?>, String> typeAliases;
  private static Map<String, Class<?>> revTypeAliases;

  static {
    JsonSerializer.properties = new HashMap<Class<?>, List<Pair<Method, Method>>>();
    JsonSerializer.revProperties = new HashMap<Pair<Class<?>, String>, Pair<Method, Method>>();
    JsonSerializer.typeAliases = new HashMap<Class<?>, String>();
    JsonSerializer.revTypeAliases = new HashMap<String, Class<?>>();
  }

  public static Object deserialize(JSONObject object) {
    try {
      Object result = null;
      String typeName = (String) object.get("*type");
      Class<?> type = JsonSerializer.getClassForName(typeName);
      JSONArray items = (JSONArray) object.opt("*items");
      if (items == null) {
        result = JsonSerializer.deserializeObject(object, type);
        if (result instanceof JsonPrimitive)
          result = ((JsonPrimitive) result).getTrueValue();
      }
      else
        result = JsonSerializer.deserializeArray(items, type);
      return result;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Object deserialize(String serializedString) {
    try {
      return JsonSerializer.deserialize(new JSONObject(serializedString));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static Object deserializeArray(JSONArray items, Class<?> type) throws SecurityException,
      NoSuchMethodException, IllegalArgumentException, InstantiationException,
      IllegalAccessException, InvocationTargetException, JSONException {
    Constructor<?> c = type.getConstructor();
    Collection<?> collection = (Collection<?>) c.newInstance();
    for (int x = 0; x < items.length(); x++) {
      Object item = items.get(x);
      if (item instanceof JSONObject)
        item = JsonSerializer.deserialize((JSONObject) item);
      ((Collection<Object>) collection).add(item);
    }
    return collection;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static Object deserializeObject(JSONObject object, Class<?> type)
      throws SecurityException, NoSuchMethodException, IllegalArgumentException,
      InstantiationException, IllegalAccessException, InvocationTargetException, JSONException {
    Object result;
    if (Enum.class.isAssignableFrom(type))
      result = Enum.valueOf((Class) type, object.getString("*name"));
    else {
      Constructor<?> c = type.getConstructor();
      result = c.newInstance();
    }
    Iterator<String> keys = object.keys();
    while (keys.hasNext()) {
      try {
        String curKey = keys.next();
        Object cur = object.get(curKey);
        if (cur == JSONObject.NULL)
          cur = null;
        else if (cur instanceof JSONObject)
          cur = JsonSerializer.deserialize((JSONObject) cur);
        Pair<Method, Method> propPair = JsonSerializer.getProperty(type, curKey);
        propPair.getRight().invoke(result, cur);
      }
      catch (Exception e) {
      }
    }
    return result;
  }

  private static Class<?> getClassForName(String typeName) throws ClassNotFoundException {
    return JsonSerializer.revTypeAliases.containsKey(typeName) ? JsonSerializer.revTypeAliases
        .get(typeName) : Class.forName(typeName);
  }

  private static String getNameForClass(Class<?> cls) {
    return JsonSerializer.typeAliases.containsKey(cls) ? JsonSerializer.typeAliases.get(cls) : cls
        .getName();
  }

  private static List<Pair<Method, Method>> getProperties(Class<?> type) {
    if (JsonSerializer.properties.containsKey(type))
      return JsonSerializer.properties.get(type);
    List<Pair<Method, Method>> props = new ArrayList<Pair<Method, Method>>();
    Method[] methods = type.getMethods();
    for (Method m : methods) {
      if (!((m.getModifiers() & Member.PUBLIC) == Member.PUBLIC && m.getName().startsWith("set") && m
          .getParameterTypes().length == 1))
        continue;
      String propName = m.getName().substring(3);
      Method getter;
      try {
        getter = type.getMethod("get" + propName);
      }
      catch (NoSuchMethodException nsme) {
        continue;
      }
      if (!(getter.getReturnType().equals(m.getParameterTypes()[0]) && (m.getModifiers() & Member.PUBLIC) == Member.PUBLIC))
        continue;
      Pair<Method, Method> propPair = new Pair<Method, Method>(getter, m);
      props.add(propPair);
      JsonSerializer.revProperties.put(new Pair<Class<?>, String>(type, propName), propPair);
    }
    JsonSerializer.properties.put(type, props);
    return props;
  }

  private static Pair<Method, Method> getProperty(Class<?> type, String propName) {
    if (!JsonSerializer.properties.containsKey(type))
      JsonSerializer.getProperties(type);
    return JsonSerializer.revProperties.get(new Pair<Class<?>, String>(type, propName));
  }

  public static void registerAlias(Class<?> cls, String alias) {
    if (JsonSerializer.typeAliases.containsKey(cls))
      return;
    if (JsonSerializer.revTypeAliases.containsKey(alias))
      return;
    JsonSerializer.typeAliases.put(cls, alias);
    JsonSerializer.revTypeAliases.put(alias, cls);
  }

  public static JSONObject serialize(Object obj) {
    if (obj == null || obj.getClass().equals(String.class) || obj.getClass().equals(Integer.class)
        || obj.getClass().equals(Boolean.class) || obj.getClass().equals(Long.class)
        || obj.getClass().equals(Double.class)) {
      JsonPrimitive prim = new JsonPrimitive();
      prim.setValue(obj);
      if (obj != null)
        prim.setType(JsonSerializer.getNameForClass(obj.getClass()));
      return JsonSerializer.serializeObject(prim);
    }
    if (obj instanceof Collection)
      return JsonSerializer.serializeArray((Collection<?>) obj);
    else
      return JsonSerializer.serializeObject(obj);
  }

  private static JSONObject serializeArray(Collection<?> array) {
    try {
      JSONObject result = new JSONObject();
      result.put("*type", JsonSerializer.getNameForClass(array.getClass()));
      JSONArray data = new JSONArray();
      for (Object o : array) {
        try {
          if (o == null) {
            data.put(JSONObject.NULL);
          }
          else if (o.getClass().equals(String.class)) {
            data.put(o);
          }
          else if (o.getClass().equals(Integer.class) || o.getClass().equals(Boolean.class)
              || o.getClass().equals(Long.class) || o.getClass().equals(Double.class)) {
            JsonPrimitive prim = new JsonPrimitive();
            prim.setType(JsonSerializer.getNameForClass(o.getClass()));
            prim.setValue(o);
            data.put(JsonSerializer.serializeObject(prim));
          }
          else if (o instanceof Collection) {
            data.put(JsonSerializer.serializeArray((Collection<?>) o));
          }
          else {
            data.put(JsonSerializer.serializeObject(o));
          }
        }
        catch (Exception e) {
        }
      }
      result.put("*items", data);
      return result;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("rawtypes")
  private static JSONObject serializeObject(Object obj) {
    try {
      JSONObject result = new JSONObject();
      result.put("*type", JsonSerializer.getNameForClass(obj.getClass()));
      if (obj instanceof Enum) {
        result.put("*name", ((Enum) obj).name());
      }
      List<Pair<Method, Method>> props = JsonSerializer.getProperties(obj.getClass());
      for (Pair<Method, Method> property : props) {
        Method getter = property.getLeft();
        String propName = getter.getName().substring(3);
        try {
          Object value = getter.invoke(obj);
          if (value == null) {
            result.put(propName, JSONObject.NULL);
          }
          else if (value.getClass().equals(String.class) || obj instanceof JsonPrimitive) {
            result.put(propName, value);
          }
          else if (value.getClass().equals(Integer.class) || value.getClass().equals(Boolean.class)
              || value.getClass().equals(Long.class) || value.getClass().equals(Double.class)) {
            JsonPrimitive prim = new JsonPrimitive();
            prim.setType(JsonSerializer.getNameForClass(value.getClass()));
            prim.setValue(value);
            result.put(propName, JsonSerializer.serializeObject(prim));
          }
          else if (value instanceof Collection) {
            result.put(propName, JsonSerializer.serializeArray((Collection<?>) value));
          }
          else {
            result.put(propName, JsonSerializer.serializeObject(value));
          }
        }
        catch (Exception e) {
        }
      }
      return result;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
