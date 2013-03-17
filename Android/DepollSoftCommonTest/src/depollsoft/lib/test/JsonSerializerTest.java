package depollsoft.lib.test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

import com.bindroid.trackable.TrackableField;

import depollsoft.lib.json.JsonSerializer;

public class JsonSerializerTest extends TestCase {
  public static class SpecialObject {

    private TrackableField<Integer> intValue = new TrackableField<Integer>();

    private TrackableField<Double> doubleValue = new TrackableField<Double>();

    private TrackableField<String> stringValue = new TrackableField<String>();

    private TrackableField<SpecialObject> objectValue = new TrackableField<SpecialObject>();

    private TrackableField<List<Object>> listValue = new TrackableField<List<Object>>();

    public Double getDoubleValue() {
      return this.doubleValue.get();
    }

    public int getIntValue() {
      return this.intValue.get();
    }

    public List<Object> getListValue() {
      return this.listValue.get();
    }

    public SpecialObject getObjectValue() {
      return this.objectValue.get();
    }

    public String getStringValue() {
      return this.stringValue.get();
    }

    public void setDoubleValue(Double value) {
      this.doubleValue.set(value);
    }

    public void setIntValue(int value) {
      this.intValue.set(value);
    }

    public void setListValue(List<Object> value) {
      this.listValue.set(value);
    }

    public void setObjectValue(SpecialObject value) {
      this.objectValue.set(value);
    }

    public void setStringValue(String value) {
      this.stringValue.set(value);
    }
  }

  public void testComplexDeserialize() throws JSONException {
    SpecialObject so = new SpecialObject();
    so.setDoubleValue(1.234);
    so.setIntValue(123);
    so.setStringValue("Hello, world!");
    SpecialObject so2 = new SpecialObject();
    so.setObjectValue(so2);
    so2.setStringValue("Bonjour, monde!");
    so.setListValue(new ArrayList<Object>());
    so.getListValue().add(so2);
    so.getListValue().add(123);
    so.getListValue().add("Hello!");
    so.getListValue().add(new ArrayList<Object>());
    JSONObject obj = JsonSerializer.serialize(so);
    Assert.assertEquals(1.234, obj.getDouble("DoubleValue"), Double.MIN_VALUE);
    Assert.assertEquals(123, obj.getInt("IntValue"));
    Assert.assertEquals("Hello, world!", obj.getString("StringValue"));
    Assert.assertTrue(obj.get("ObjectValue") instanceof JSONObject);
    Assert.assertEquals("Bonjour, monde!", obj.getJSONObject("ObjectValue")
        .getString("StringValue"));
    Assert.assertEquals(4, obj.getJSONObject("ListValue").getJSONArray("*items").length());
    SpecialObject result = (SpecialObject) JsonSerializer.deserialize(obj);
    Assert.assertEquals(1.234, result.getDoubleValue(), Double.MIN_VALUE);
    Assert.assertEquals(123, result.getIntValue());
    Assert.assertEquals("Hello, world!", result.getStringValue());
    Assert.assertEquals("Bonjour, monde!", result.getObjectValue().getStringValue());
    Assert.assertEquals(4, result.getListValue().size());
    Assert.assertEquals("Bonjour, monde!",
        ((SpecialObject) result.getListValue().get(0)).getStringValue());
    Assert.assertEquals("Hello!", result.getListValue().get(2));
    Assert.assertTrue(result.getListValue().get(3) instanceof ArrayList);
  }

  public void testComplexSerialize() throws JSONException {
    SpecialObject so = new SpecialObject();
    so.setDoubleValue(1.234);
    so.setIntValue(123);
    so.setStringValue("Hello, world!");
    SpecialObject so2 = new SpecialObject();
    so.setObjectValue(so2);
    so2.setStringValue("Bonjour, monde!");
    so.setListValue(new ArrayList<Object>());
    so.getListValue().add(so2);
    so.getListValue().add(123);
    so.getListValue().add("Hello!");
    so.getListValue().add(new ArrayList<Object>());
    JSONObject obj = JsonSerializer.serialize(so);
    Assert.assertEquals(1.234, obj.getDouble("DoubleValue"), Double.MIN_VALUE);
    Assert.assertEquals(123, obj.getInt("IntValue"));
    Assert.assertEquals("Hello, world!", obj.getString("StringValue"));
    Assert.assertTrue(obj.get("ObjectValue") instanceof JSONObject);
    Assert.assertEquals("Bonjour, monde!", obj.getJSONObject("ObjectValue")
        .getString("StringValue"));
    Assert.assertEquals(4, obj.getJSONObject("ListValue").getJSONArray("*items").length());
  }

  public void testSimpleDeserialize() throws JSONException {
    SpecialObject so = new SpecialObject();
    so.setDoubleValue(1.234);
    so.setIntValue(123);
    JSONObject obj = JsonSerializer.serialize(so);
    Assert.assertEquals(1.234, obj.getDouble("DoubleValue"), Double.MIN_VALUE);
    Assert.assertEquals(123, obj.getInt("IntValue"));
    SpecialObject result = (SpecialObject) JsonSerializer.deserialize(obj);
    Assert.assertEquals(1.234, result.getDoubleValue(), Double.MIN_VALUE);
    Assert.assertEquals(123, result.getIntValue());
  }

  public void testSimpleSerialize() throws JSONException {
    SpecialObject so = new SpecialObject();
    so.setDoubleValue(1.234);
    so.setIntValue(123);
    JSONObject obj = JsonSerializer.serialize(so);
    Assert.assertEquals(1.234, obj.getDouble("DoubleValue"), Double.MIN_VALUE);
    Assert.assertEquals(123, obj.getInt("IntValue"));
  }
}
