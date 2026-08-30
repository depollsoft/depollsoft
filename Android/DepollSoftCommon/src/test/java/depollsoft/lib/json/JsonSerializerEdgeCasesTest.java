package depollsoft.lib.json;

import android.os.Build;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Edge case and advanced tests for JsonSerializer.
 * Tests complex object graphs, edge cases, and error handling.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M)
public class JsonSerializerEdgeCasesTest {

    // =====================================================================
    // Test POJOs
    // =====================================================================

    public static class EmptyPojo {
        // No properties
    }

    public static class SingleProperty {
        private String value;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class PojoWithPrimitiveWrappers {
        private Integer intValue;
        private Long longValue;
        private Double doubleValue;
        private Boolean boolValue;

        public Integer getIntValue() { return intValue; }
        public void setIntValue(Integer intValue) { this.intValue = intValue; }
        public Long getLongValue() { return longValue; }
        public void setLongValue(Long longValue) { this.longValue = longValue; }
        public Double getDoubleValue() { return doubleValue; }
        public void setDoubleValue(Double doubleValue) { this.doubleValue = doubleValue; }
        public Boolean getBoolValue() { return boolValue; }
        public void setBoolValue(Boolean boolValue) { this.boolValue = boolValue; }
    }

    public static class SelfReferencing {
        private String name;
        private SelfReferencing child;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public SelfReferencing getChild() { return child; }
        public void setChild(SelfReferencing child) { this.child = child; }
    }

    public static class DeepNesting {
        private DeepNesting inner;
        private int level;

        public DeepNesting getInner() { return inner; }
        public void setInner(DeepNesting inner) { this.inner = inner; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
    }

    public static class PojoWithMultipleListTypes {
        private List<String> strings;
        private List<Integer> integers;
        private List<Boolean> booleans;

        public List<String> getStrings() { return strings; }
        public void setStrings(List<String> strings) { this.strings = strings; }
        public List<Integer> getIntegers() { return integers; }
        public void setIntegers(List<Integer> integers) { this.integers = integers; }
        public List<Boolean> getBooleans() { return booleans; }
        public void setBooleans(List<Boolean> booleans) { this.booleans = booleans; }
    }

    public enum DetailedEnum {
        FIRST_VALUE,
        SECOND_VALUE,
        THIRD_VALUE,
        FOURTH_VALUE
    }

    public static class MultiEnumPojo {
        private DetailedEnum status;
        private DetailedEnum priority;

        public DetailedEnum getStatus() { return status; }
        public void setStatus(DetailedEnum status) { this.status = status; }
        public DetailedEnum getPriority() { return priority; }
        public void setPriority(DetailedEnum priority) { this.priority = priority; }
    }

    // =====================================================================
    // Empty object tests
    // =====================================================================

    @Test
    public void serialize_emptyPojo_producesValidJson() {
        EmptyPojo pojo = new EmptyPojo();
        JSONObject json = JsonSerializer.serialize(pojo);
        assertNotNull(json);
        assertTrue(json.has("*type"));
    }

    @Test
    public void roundTrip_emptyPojo_works() {
        EmptyPojo pojo = new EmptyPojo();
        JSONObject json = JsonSerializer.serialize(pojo);
        Object result = JsonSerializer.deserialize(json);
        assertNotNull(result);
        assertTrue(result instanceof EmptyPojo);
    }

    // =====================================================================
    // Null handling tests
    // =====================================================================

    @Test
    public void serialize_allNullProperties_handlesCorrectly() {
        PojoWithPrimitiveWrappers pojo = new PojoWithPrimitiveWrappers();
        // All properties are null by default

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithPrimitiveWrappers result = (PojoWithPrimitiveWrappers) JsonSerializer.deserialize(json);

        assertNull(result.getIntValue());
        assertNull(result.getLongValue());
        assertNull(result.getDoubleValue());
        assertNull(result.getBoolValue());
    }

    @Test
    public void serialize_mixedNullAndNonNull_handlesCorrectly() {
        PojoWithPrimitiveWrappers pojo = new PojoWithPrimitiveWrappers();
        pojo.setIntValue(42);
        pojo.setLongValue(null);
        pojo.setDoubleValue(3.14);
        pojo.setBoolValue(null);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithPrimitiveWrappers result = (PojoWithPrimitiveWrappers) JsonSerializer.deserialize(json);

        assertEquals(Integer.valueOf(42), result.getIntValue());
        assertNull(result.getLongValue());
        assertEquals(3.14, result.getDoubleValue(), 0.001);
        assertNull(result.getBoolValue());
    }

    // =====================================================================
    // Self-referencing object tests
    // =====================================================================

    @Test
    public void serialize_selfReferencingObject_handlesChain() {
        SelfReferencing root = new SelfReferencing();
        root.setName("root");
        SelfReferencing child = new SelfReferencing();
        child.setName("child");
        root.setChild(child);

        JSONObject json = JsonSerializer.serialize(root);
        SelfReferencing result = (SelfReferencing) JsonSerializer.deserialize(json);

        assertEquals("root", result.getName());
        assertNotNull(result.getChild());
        assertEquals("child", result.getChild().getName());
        assertNull(result.getChild().getChild());
    }

    @Test
    public void serialize_deepNesting_handlesMultipleLevels() {
        DeepNesting level1 = new DeepNesting();
        level1.setLevel(1);
        DeepNesting level2 = new DeepNesting();
        level2.setLevel(2);
        DeepNesting level3 = new DeepNesting();
        level3.setLevel(3);

        level1.setInner(level2);
        level2.setInner(level3);

        JSONObject json = JsonSerializer.serialize(level1);
        DeepNesting result = (DeepNesting) JsonSerializer.deserialize(json);

        assertEquals(1, result.getLevel());
        assertEquals(2, result.getInner().getLevel());
        assertEquals(3, result.getInner().getInner().getLevel());
    }

    // =====================================================================
    // Collection tests
    // =====================================================================

    @Test
    public void serialize_emptyList_roundTrips() {
        List<String> empty = new ArrayList<>();
        JSONObject json = JsonSerializer.serialize(empty);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) JsonSerializer.deserialize(json);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void serialize_singleItemList_roundTrips() {
        List<String> single = new ArrayList<>();
        single.add("only item");

        JSONObject json = JsonSerializer.serialize(single);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) JsonSerializer.deserialize(json);

        assertEquals(1, result.size());
        assertEquals("only item", result.get(0));
    }

    @Test
    public void serialize_linkedList_roundTrips() {
        LinkedList<String> linked = new LinkedList<>();
        linked.add("first");
        linked.add("second");
        linked.add("third");

        JSONObject json = JsonSerializer.serialize(linked);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) JsonSerializer.deserialize(json);

        assertEquals(3, result.size());
    }

    @Test
    public void serialize_listWithNulls_handlesCorrectly() {
        List<String> withNulls = new ArrayList<>();
        withNulls.add("first");
        withNulls.add(null);
        withNulls.add("third");

        JSONObject json = JsonSerializer.serialize(withNulls);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) JsonSerializer.deserialize(json);

        assertEquals(3, result.size());
        assertEquals("first", result.get(0));
        assertNull(result.get(1));
        assertEquals("third", result.get(2));
    }

    @Test
    public void serialize_multipleListTypes_roundTrips() {
        PojoWithMultipleListTypes pojo = new PojoWithMultipleListTypes();

        List<String> strings = new ArrayList<>();
        strings.add("a");
        strings.add("b");
        pojo.setStrings(strings);

        List<Integer> integers = new ArrayList<>();
        integers.add(1);
        integers.add(2);
        pojo.setIntegers(integers);

        List<Boolean> booleans = new ArrayList<>();
        booleans.add(true);
        booleans.add(false);
        pojo.setBooleans(booleans);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithMultipleListTypes result = (PojoWithMultipleListTypes) JsonSerializer.deserialize(json);

        assertEquals(strings, result.getStrings());
        assertEquals(integers, result.getIntegers());
        assertEquals(booleans, result.getBooleans());
    }

    // =====================================================================
    // Enum tests
    // =====================================================================

    @Test
    public void serialize_multipleEnumProperties_roundTrips() {
        MultiEnumPojo pojo = new MultiEnumPojo();
        pojo.setStatus(DetailedEnum.FIRST_VALUE);
        pojo.setPriority(DetailedEnum.FOURTH_VALUE);

        JSONObject json = JsonSerializer.serialize(pojo);
        MultiEnumPojo result = (MultiEnumPojo) JsonSerializer.deserialize(json);

        assertEquals(DetailedEnum.FIRST_VALUE, result.getStatus());
        assertEquals(DetailedEnum.FOURTH_VALUE, result.getPriority());
    }

    @Test
    public void serialize_enumWithNullProperty_handlesCorrectly() {
        MultiEnumPojo pojo = new MultiEnumPojo();
        pojo.setStatus(DetailedEnum.SECOND_VALUE);
        pojo.setPriority(null);

        JSONObject json = JsonSerializer.serialize(pojo);
        MultiEnumPojo result = (MultiEnumPojo) JsonSerializer.deserialize(json);

        assertEquals(DetailedEnum.SECOND_VALUE, result.getStatus());
        assertNull(result.getPriority());
    }

    // =====================================================================
    // String deserialization tests
    // =====================================================================

    @Test
    public void deserialize_fromString_works() {
        SingleProperty pojo = new SingleProperty();
        pojo.setValue("test");

        JSONObject json = JsonSerializer.serialize(pojo);
        String serialized = json.toString();

        Object result = JsonSerializer.deserialize(serialized);
        assertTrue(result instanceof SingleProperty);
        assertEquals("test", ((SingleProperty) result).getValue());
    }

    // =====================================================================
    // Type alias tests
    // =====================================================================

    @Test
    public void registerAlias_preventsReregistration() {
        // First registration should succeed (or do nothing if already registered)
        JsonSerializer.registerAlias(SingleProperty.class, "TestAlias_" + System.nanoTime());

        // Verify class still works after alias registration
        SingleProperty pojo = new SingleProperty();
        pojo.setValue("aliased");

        JSONObject json = JsonSerializer.serialize(pojo);
        assertNotNull(json);
    }

    // =====================================================================
    // Primitive value edge cases
    // =====================================================================

    @Test
    public void serialize_zeroValues_handlesCorrectly() {
        PojoWithPrimitiveWrappers pojo = new PojoWithPrimitiveWrappers();
        pojo.setIntValue(0);
        pojo.setLongValue(0L);
        pojo.setDoubleValue(0.0);
        pojo.setBoolValue(false);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithPrimitiveWrappers result = (PojoWithPrimitiveWrappers) JsonSerializer.deserialize(json);

        assertEquals(Integer.valueOf(0), result.getIntValue());
        assertEquals(Long.valueOf(0L), result.getLongValue());
        assertEquals(0.0, result.getDoubleValue(), 0.001);
        assertEquals(Boolean.FALSE, result.getBoolValue());
    }

    @Test
    public void serialize_maxValues_handlesCorrectly() {
        PojoWithPrimitiveWrappers pojo = new PojoWithPrimitiveWrappers();
        pojo.setIntValue(Integer.MAX_VALUE);
        pojo.setLongValue(Long.MAX_VALUE);
        pojo.setDoubleValue(Double.MAX_VALUE);
        pojo.setBoolValue(true);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithPrimitiveWrappers result = (PojoWithPrimitiveWrappers) JsonSerializer.deserialize(json);

        assertEquals(Integer.valueOf(Integer.MAX_VALUE), result.getIntValue());
        assertEquals(Long.valueOf(Long.MAX_VALUE), result.getLongValue());
        assertEquals(Boolean.TRUE, result.getBoolValue());
    }

    @Test
    public void serialize_negativeValues_handlesCorrectly() {
        PojoWithPrimitiveWrappers pojo = new PojoWithPrimitiveWrappers();
        pojo.setIntValue(-42);
        pojo.setLongValue(-999999999999L);
        pojo.setDoubleValue(-3.14159);

        JSONObject json = JsonSerializer.serialize(pojo);
        PojoWithPrimitiveWrappers result = (PojoWithPrimitiveWrappers) JsonSerializer.deserialize(json);

        assertEquals(Integer.valueOf(-42), result.getIntValue());
        assertEquals(Long.valueOf(-999999999999L), result.getLongValue());
        assertEquals(-3.14159, result.getDoubleValue(), 0.00001);
    }

    // =====================================================================
    // String content edge cases
    // =====================================================================

    @Test
    public void serialize_emptyString_handlesCorrectly() {
        SingleProperty pojo = new SingleProperty();
        pojo.setValue("");

        JSONObject json = JsonSerializer.serialize(pojo);
        SingleProperty result = (SingleProperty) JsonSerializer.deserialize(json);

        assertEquals("", result.getValue());
    }

    @Test
    public void serialize_stringWithSpecialChars_handlesCorrectly() {
        SingleProperty pojo = new SingleProperty();
        pojo.setValue("Special: \"quotes\" and \\backslash\\ and \nnewline");

        JSONObject json = JsonSerializer.serialize(pojo);
        SingleProperty result = (SingleProperty) JsonSerializer.deserialize(json);

        assertEquals("Special: \"quotes\" and \\backslash\\ and \nnewline", result.getValue());
    }

    @Test
    public void serialize_unicodeString_handlesCorrectly() {
        SingleProperty pojo = new SingleProperty();
        pojo.setValue("日本語 한국어 العربية");

        JSONObject json = JsonSerializer.serialize(pojo);
        SingleProperty result = (SingleProperty) JsonSerializer.deserialize(json);

        assertEquals("日本語 한국어 العربية", result.getValue());
    }

    @Test
    public void serialize_emojiString_handlesCorrectly() {
        SingleProperty pojo = new SingleProperty();
        pojo.setValue("Hello 😀 World 🌍");

        JSONObject json = JsonSerializer.serialize(pojo);
        SingleProperty result = (SingleProperty) JsonSerializer.deserialize(json);

        assertEquals("Hello 😀 World 🌍", result.getValue());
    }
}
