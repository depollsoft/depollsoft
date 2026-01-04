package depollsoft.lib.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the Wrapper generic utility class.
 */
public class WrapperTest {

    // =====================================================================
    // Constructor tests
    // =====================================================================

    @Test
    public void constructor_withStringValue_storesValue() {
        Wrapper<String> wrapper = new Wrapper<>("test");
        assertEquals("test", wrapper.getValue());
    }

    @Test
    public void constructor_withIntegerValue_storesValue() {
        Wrapper<Integer> wrapper = new Wrapper<>(42);
        assertEquals(Integer.valueOf(42), wrapper.getValue());
    }

    @Test
    public void constructor_withNullValue_storesNull() {
        Wrapper<String> wrapper = new Wrapper<>(null);
        assertNull(wrapper.getValue());
    }

    @Test
    public void constructor_withDoubleValue_storesValue() {
        Wrapper<Double> wrapper = new Wrapper<>(3.14159);
        assertEquals(Double.valueOf(3.14159), wrapper.getValue());
    }

    @Test
    public void constructor_withBooleanValue_storesValue() {
        Wrapper<Boolean> wrapper = new Wrapper<>(true);
        assertEquals(Boolean.TRUE, wrapper.getValue());
    }

    // =====================================================================
    // getValue tests
    // =====================================================================

    @Test
    public void getValue_afterConstruction_returnsInitialValue() {
        Wrapper<String> wrapper = new Wrapper<>("initial");
        assertEquals("initial", wrapper.getValue());
    }

    @Test
    public void getValue_withComplexObject_returnsExactObject() {
        Object customObject = new Object();
        Wrapper<Object> wrapper = new Wrapper<>(customObject);
        assertSame(customObject, wrapper.getValue());
    }

    // =====================================================================
    // setValue tests
    // =====================================================================

    @Test
    public void setValue_updatesValue() {
        Wrapper<String> wrapper = new Wrapper<>("initial");
        wrapper.setValue("updated");
        assertEquals("updated", wrapper.getValue());
    }

    @Test
    public void setValue_withNull_setsNull() {
        Wrapper<String> wrapper = new Wrapper<>("initial");
        wrapper.setValue(null);
        assertNull(wrapper.getValue());
    }

    @Test
    public void setValue_multipleUpdates_retainsLastValue() {
        Wrapper<Integer> wrapper = new Wrapper<>(1);
        wrapper.setValue(2);
        wrapper.setValue(3);
        wrapper.setValue(4);
        assertEquals(Integer.valueOf(4), wrapper.getValue());
    }

    @Test
    public void setValue_fromNullToValue_works() {
        Wrapper<String> wrapper = new Wrapper<>(null);
        wrapper.setValue("notNull");
        assertEquals("notNull", wrapper.getValue());
    }

    @Test
    public void setValue_withSameValue_keepsValue() {
        Wrapper<String> wrapper = new Wrapper<>("same");
        wrapper.setValue("same");
        assertEquals("same", wrapper.getValue());
    }

    // =====================================================================
    // Type-specific tests
    // =====================================================================

    @Test
    public void wrapper_withListType_storesCorrectly() {
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("item1");
        list.add("item2");
        
        Wrapper<java.util.List<String>> wrapper = new Wrapper<>(list);
        assertEquals(2, wrapper.getValue().size());
        assertEquals("item1", wrapper.getValue().get(0));
        assertEquals("item2", wrapper.getValue().get(1));
    }

    @Test
    public void wrapper_withMapType_storesCorrectly() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        
        Wrapper<java.util.Map<String, Integer>> wrapper = new Wrapper<>(map);
        assertEquals(2, wrapper.getValue().size());
        assertEquals(Integer.valueOf(1), wrapper.getValue().get("a"));
    }

    @Test
    public void wrapper_withNestedWrapper_storesCorrectly() {
        Wrapper<Integer> inner = new Wrapper<>(10);
        Wrapper<Wrapper<Integer>> outer = new Wrapper<>(inner);
        
        assertEquals(inner, outer.getValue());
        assertEquals(Integer.valueOf(10), outer.getValue().getValue());
    }

    // =====================================================================
    // Mutability tests (ensuring Wrapper allows value modification)
    // =====================================================================

    @Test
    public void wrapper_allowsMutatingWrappedObject() {
        java.util.List<String> list = new java.util.ArrayList<>();
        Wrapper<java.util.List<String>> wrapper = new Wrapper<>(list);
        
        // Mutate through the wrapper
        wrapper.getValue().add("added");
        assertEquals(1, wrapper.getValue().size());
        assertEquals("added", wrapper.getValue().get(0));
    }

    @Test
    public void wrapper_withPrimitiveWrapper_handlesBoxing() {
        Wrapper<Integer> wrapper = new Wrapper<>(5);
        int primitiveValue = wrapper.getValue(); // auto-unboxing
        assertEquals(5, primitiveValue);
    }

    // =====================================================================
    // Edge case tests
    // =====================================================================

    @Test
    public void wrapper_withEmptyString_storesCorrectly() {
        Wrapper<String> wrapper = new Wrapper<>("");
        assertEquals("", wrapper.getValue());
        assertNotNull(wrapper.getValue());
    }

    @Test
    public void wrapper_withZeroInteger_storesCorrectly() {
        Wrapper<Integer> wrapper = new Wrapper<>(0);
        assertEquals(Integer.valueOf(0), wrapper.getValue());
    }

    @Test
    public void wrapper_withNegativeNumber_storesCorrectly() {
        Wrapper<Integer> wrapper = new Wrapper<>(-100);
        assertEquals(Integer.valueOf(-100), wrapper.getValue());
    }

    @Test
    public void wrapper_withMaxIntegerValue_storesCorrectly() {
        Wrapper<Integer> wrapper = new Wrapper<>(Integer.MAX_VALUE);
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), wrapper.getValue());
    }

    @Test
    public void wrapper_withMinIntegerValue_storesCorrectly() {
        Wrapper<Integer> wrapper = new Wrapper<>(Integer.MIN_VALUE);
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), wrapper.getValue());
    }

    @Test
    public void wrapper_withLongValue_storesCorrectly() {
        Wrapper<Long> wrapper = new Wrapper<>(Long.MAX_VALUE);
        assertEquals(Long.valueOf(Long.MAX_VALUE), wrapper.getValue());
    }
}
