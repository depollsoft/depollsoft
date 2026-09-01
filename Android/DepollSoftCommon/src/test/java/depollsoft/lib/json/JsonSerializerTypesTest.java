package depollsoft.lib.json;

import android.os.Build;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Additional comprehensive tests for JsonSerializer.
 * Supplements existing JsonSerializerTest and JsonSerializerExtraTest.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class JsonSerializerTypesTest {

    // =====================================================================
    // Test POJOs
    // =====================================================================

    public static class SimplePojo {
        private String name;
        private Integer count;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    public static class NestedPojo {
        private SimplePojo inner;
        private String label;

        public SimplePojo getInner() { return inner; }
        public void setInner(SimplePojo inner) { this.inner = inner; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    public static class PojoWithList {
        private List<String> items;
        private String title;

        public List<String> getItems() { return items; }
        public void setItems(List<String> items) { this.items = items; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    public static class PojoWithPrimitives {
        private Boolean enabled;
        private Long count;
        private Double value;

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
    }

    public enum TestEnum {
        VALUE_ONE,
        VALUE_TWO,
        VALUE_THREE
    }

    public static class PojoWithEnum {
        private TestEnum status;
        private String name;

        public TestEnum getStatus() { return status; }
        public void setStatus(TestEnum status) { this.status = status; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    // =====================================================================
    // Primitive type tests
    // =====================================================================

    @Test
    public void serialize_null_handlesCorrectly() {
        JSONObject result = JsonSerializer.serialize(null);
        assertNotNull(result);
    }

    @Test
    public void serialize_string_wrapsAsPrimitive() {
        JSONObject result = JsonSerializer.serialize("test string");
        assertNotNull(result);
        Object back = JsonSerializer.deserialize(result);
        assertEquals("test string", back);
    }

    @Test
    public void serialize_integer_wrapsAsPrimitive() {
        JSONObject result = JsonSerializer.serialize(42);
        assertNotNull(result);
        Object back = JsonSerializer.deserialize(result);
        assertEquals(42, back);
    }

    @Test
    public void serialize_boolean_wrapsAsPrimitive() {
        JSONObject result = JsonSerializer.serialize(true);
        assertNotNull(result);
        Object back = JsonSerializer.deserialize(result);
        assertEquals(true, back);
    }

    @Test
    public void serialize_long_wrapsAsPrimitive() {
        JSONObject result = JsonSerializer.serialize(Long.MAX_VALUE);
        assertNotNull(result);
        Object back = JsonSerializer.deserialize(result);
        assertEquals(Long.MAX_VALUE, back);
    }

    @Test
    public void serialize_double_wrapsAsPrimitive() {
        JSONObject result = JsonSerializer.serialize(3.14159);
        assertNotNull(result);
        Object back = JsonSerializer.deserialize(result);
        assertEquals(3.14159, (Double) back, 0.00001);
    }

    // =====================================================================
    // POJO tests
    // =====================================================================

    @Test
    public void serialize_simplePojo_roundTrip() {
        SimplePojo pojo = new SimplePojo();
        pojo.setName("test");
        pojo.setCount(100);

        JSONObject json = JsonSerializer.serialize(pojo);
        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(json);

        assertEquals("test", back.getName());
        assertEquals(Integer.valueOf(100), back.getCount());
    }

    @Test
    public void serialize_pojoWithNullProperties_handlesCorrectly() {
        SimplePojo pojo = new SimplePojo();
        pojo.setName(null);
        pojo.setCount(null);

        JSONObject json = JsonSerializer.serialize(pojo);
        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(json);

        assertNull(back.getName());
        assertNull(back.getCount());
    }

    @Test
    public void serialize_nestedPojo_roundTrip() {
        SimplePojo inner = new SimplePojo();
        inner.setName("innerName");
        inner.setCount(50);

        NestedPojo nested = new NestedPojo();
        nested.setInner(inner);
        nested.setLabel("outerLabel");

        JSONObject json = JsonSerializer.serialize(nested);
        NestedPojo back = (NestedPojo) JsonSerializer.deserialize(json);

        assertEquals("outerLabel", back.getLabel());
        assertNotNull(back.getInner());
        assertEquals("innerName", back.getInner().getName());
        assertEquals(Integer.valueOf(50), back.getInner().getCount());
    }

    @Test
    public void serialize_pojoWithPrimitiveTypes_roundTrip() {
        PojoWithPrimitives pojo = new PojoWithPrimitives();
        pojo.setEnabled(true);
        pojo.setCount(999L);
        pojo.setValue(2.718);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithPrimitives back = (PojoWithPrimitives) JsonSerializer.deserialize(json);

        assertEquals(Boolean.TRUE, back.getEnabled());
        assertEquals(Long.valueOf(999L), back.getCount());
        assertEquals(2.718, back.getValue(), 0.001);
    }

    // =====================================================================
    // Collection tests
    // =====================================================================

    @Test
    public void serialize_emptyList_roundTrip() {
        List<String> list = new ArrayList<>();

        JSONObject json = JsonSerializer.serialize(list);
        @SuppressWarnings("unchecked")
        List<String> back = (List<String>) JsonSerializer.deserialize(json);

        assertNotNull(back);
        assertEquals(0, back.size());
    }

    @Test
    public void serialize_listOfStrings_roundTrip() {
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        list.add("three");

        JSONObject json = JsonSerializer.serialize(list);
        @SuppressWarnings("unchecked")
        List<String> back = (List<String>) JsonSerializer.deserialize(json);

        assertEquals(3, back.size());
        assertEquals("one", back.get(0));
        assertEquals("two", back.get(1));
        assertEquals("three", back.get(2));
    }

    @Test
    public void serialize_listOfIntegers_roundTrip() {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        JSONObject json = JsonSerializer.serialize(list);
        @SuppressWarnings("unchecked")
        List<Integer> back = (List<Integer>) JsonSerializer.deserialize(json);

        assertEquals(3, back.size());
        assertEquals(Integer.valueOf(1), back.get(0));
        assertEquals(Integer.valueOf(2), back.get(1));
        assertEquals(Integer.valueOf(3), back.get(2));
    }

    @Test
    public void serialize_listWithNull_handlesCorrectly() {
        List<String> list = new ArrayList<>();
        list.add("before");
        list.add(null);
        list.add("after");

        JSONObject json = JsonSerializer.serialize(list);
        @SuppressWarnings("unchecked")
        List<String> back = (List<String>) JsonSerializer.deserialize(json);

        assertEquals(3, back.size());
        assertEquals("before", back.get(0));
        assertNull(back.get(1));
        assertEquals("after", back.get(2));
    }

    @Test
    public void serialize_pojoWithList_roundTrip() {
        PojoWithList pojo = new PojoWithList();
        pojo.setTitle("My List");
        List<String> items = new ArrayList<>();
        items.add("item1");
        items.add("item2");
        pojo.setItems(items);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithList back = (PojoWithList) JsonSerializer.deserialize(json);

        assertEquals("My List", back.getTitle());
        assertEquals(2, back.getItems().size());
        assertEquals("item1", back.getItems().get(0));
        assertEquals("item2", back.getItems().get(1));
    }

    @Test
    public void serialize_listOfPojos_roundTrip() {
        List<SimplePojo> list = new ArrayList<>();

        SimplePojo pojo1 = new SimplePojo();
        pojo1.setName("first");
        pojo1.setCount(1);
        list.add(pojo1);

        SimplePojo pojo2 = new SimplePojo();
        pojo2.setName("second");
        pojo2.setCount(2);
        list.add(pojo2);

        JSONObject json = JsonSerializer.serialize(list);
        @SuppressWarnings("unchecked")
        List<SimplePojo> back = (List<SimplePojo>) JsonSerializer.deserialize(json);

        assertEquals(2, back.size());
        assertEquals("first", back.get(0).getName());
        assertEquals(Integer.valueOf(1), back.get(0).getCount());
        assertEquals("second", back.get(1).getName());
        assertEquals(Integer.valueOf(2), back.get(1).getCount());
    }

    // =====================================================================
    // Enum tests
    // =====================================================================

    @Test
    public void serialize_pojoWithEnum_roundTrip() {
        PojoWithEnum pojo = new PojoWithEnum();
        pojo.setName("enumTest");
        pojo.setStatus(TestEnum.VALUE_TWO);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithEnum back = (PojoWithEnum) JsonSerializer.deserialize(json);

        assertEquals("enumTest", back.getName());
        assertEquals(TestEnum.VALUE_TWO, back.getStatus());
    }

    // =====================================================================
    // String deserialization tests
    // =====================================================================

    @Test
    public void deserialize_fromString_works() {
        SimplePojo pojo = new SimplePojo();
        pojo.setName("fromString");
        pojo.setCount(77);

        JSONObject json = JsonSerializer.serialize(pojo);
        String jsonString = json.toString();

        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(jsonString);

        assertEquals("fromString", back.getName());
        assertEquals(Integer.valueOf(77), back.getCount());
    }

    // =====================================================================
    // Type alias tests
    // =====================================================================

    @Test
    public void registerAlias_allowsShortTypeName() {
        JsonSerializer.registerAlias(SimplePojo.class, "simple");

        SimplePojo pojo = new SimplePojo();
        pojo.setName("aliased");
        pojo.setCount(123);

        JSONObject json = JsonSerializer.serialize(pojo);
        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(json);

        assertEquals("aliased", back.getName());
    }

    // =====================================================================
    // Edge case tests
    // =====================================================================

    @Test
    public void serialize_emptyStringProperty_roundTrip() {
        SimplePojo pojo = new SimplePojo();
        pojo.setName("");
        pojo.setCount(0);

        JSONObject json = JsonSerializer.serialize(pojo);
        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(json);

        assertEquals("", back.getName());
        assertEquals(Integer.valueOf(0), back.getCount());
    }

    @Test
    public void serialize_specialCharactersInString_roundTrip() {
        SimplePojo pojo = new SimplePojo();
        pojo.setName("special \"chars\" & <tags>");

        JSONObject json = JsonSerializer.serialize(pojo);
        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(json);

        assertEquals("special \"chars\" & <tags>", back.getName());
    }

    @Test
    public void serialize_unicodeInString_roundTrip() {
        SimplePojo pojo = new SimplePojo();
        pojo.setName("日本語テスト");

        JSONObject json = JsonSerializer.serialize(pojo);
        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(json);

        assertEquals("日本語テスト", back.getName());
    }

    @Test
    public void serialize_negativeNumbers_roundTrip() {
        SimplePojo pojo = new SimplePojo();
        pojo.setCount(-999);

        JSONObject json = JsonSerializer.serialize(pojo);
        SimplePojo back = (SimplePojo) JsonSerializer.deserialize(json);

        assertEquals(Integer.valueOf(-999), back.getCount());
    }

    @Test
    public void serialize_zeroValues_roundTrip() {
        PojoWithPrimitives pojo = new PojoWithPrimitives();
        pojo.setEnabled(false);
        pojo.setCount(0L);
        pojo.setValue(0.0);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithPrimitives back = (PojoWithPrimitives) JsonSerializer.deserialize(json);

        assertEquals(Boolean.FALSE, back.getEnabled());
        assertEquals(Long.valueOf(0L), back.getCount());
        assertEquals(0.0, back.getValue(), 0.001);
    }
}
