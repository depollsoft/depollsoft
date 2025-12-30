package depollsoft.lib

import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Additional edge case tests for JsonHelpers.kt
 */
@RunWith(RobolectricTestRunner::class)
class JsonHelpersEdgeCaseTest {

    // =====================
    // toJsonElement tests
    // =====================

    @Test
    fun toJsonElement_with_empty_array() {
        val emptyArray = arrayOf<Any>()
        val result = emptyArray.toJsonElement()
        assertTrue(result is JsonArray)
        assertEquals(0, (result as JsonArray).size)
    }

    @Test
    fun toJsonElement_with_empty_list() {
        val emptyList = emptyList<Any>()
        val result = emptyList.toJsonElement()
        assertTrue(result is JsonArray)
        assertEquals(0, (result as JsonArray).size)
    }

    @Test
    fun toJsonElement_with_empty_set() {
        val emptySet = emptySet<Any>()
        val result = emptySet.toJsonElement()
        assertTrue(result is JsonArray)
        assertEquals(0, (result as JsonArray).size)
    }

    @Test
    fun toJsonElement_with_empty_map() {
        val emptyMap = emptyMap<String, Any>()
        val result = emptyMap.toJsonElement()
        assertTrue(result is JsonObject)
        assertEquals(0, (result as JsonObject).size)
    }

    @Test
    fun toJsonElement_with_nested_nulls() {
        val listWithNulls = listOf(null, "value", null)
        val result = listWithNulls.toJsonElement()
        assertTrue(result is JsonArray)
        val arr = result as JsonArray
        assertEquals(JsonNull, arr[0])
        assertEquals(JsonPrimitive("value"), arr[1])
        assertEquals(JsonNull, arr[2])
    }

    @Test
    fun toJsonElement_with_double_values() {
        val doubleValue = 3.14159
        val result = doubleValue.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals(3.14159, (result as JsonPrimitive).double, 0.00001)
    }

    @Test
    fun toJsonElement_with_float_values() {
        val floatValue = 2.5f
        val result = floatValue.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals(2.5f, (result as JsonPrimitive).float, 0.001f)
    }

    @Test
    fun toJsonElement_with_long_values() {
        val longValue = 9223372036854775807L
        val result = longValue.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals(9223372036854775807L, (result as JsonPrimitive).long)
    }

    @Test
    fun toJsonElement_with_negative_numbers() {
        val negativeInt = -42
        val result = negativeInt.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals(-42, (result as JsonPrimitive).int)
    }

    @Test
    fun toJsonElement_with_zero() {
        val zero = 0
        val result = zero.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals(0, (result as JsonPrimitive).int)
    }

    @Test
    fun toJsonElement_with_empty_string() {
        val emptyString = ""
        val result = emptyString.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals("", (result as JsonPrimitive).content)
    }

    @Test
    fun toJsonElement_with_special_characters() {
        val specialChars = "Hello\n\t\"World\""
        val result = specialChars.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals(specialChars, (result as JsonPrimitive).content)
    }

    @Test
    fun toJsonElement_with_unicode_characters() {
        val unicodeString = "日本語 🎵 émojis"
        val result = unicodeString.toJsonElement()
        assertTrue(result is JsonPrimitive)
        assertEquals(unicodeString, (result as JsonPrimitive).content)
    }

    @Test
    fun toJsonElement_with_deeply_nested_structures() {
        val nested = mapOf(
            "level1" to mapOf(
                "level2" to mapOf(
                    "level3" to listOf(
                        mapOf("level4" to "deep")
                    )
                )
            )
        )
        val result = nested.toJsonElement()
        assertTrue(result is JsonObject)
        val obj = result as JsonObject
        val level1 = obj["level1"] as JsonObject
        val level2 = level1["level2"] as JsonObject
        val level3 = level2["level3"] as JsonArray
        val level4 = level3[0] as JsonObject
        assertEquals(JsonPrimitive("deep"), level4["level4"])
    }

    @Test
    fun toJsonElement_with_mixed_type_array() {
        val mixedArray = arrayOf(1, "two", true, 4.0, null)
        val result = mixedArray.toJsonElement()
        assertTrue(result is JsonArray)
        val arr = result as JsonArray
        assertEquals(JsonPrimitive(1), arr[0])
        assertEquals(JsonPrimitive("two"), arr[1])
        assertEquals(JsonPrimitive(true), arr[2])
        assertEquals(JsonPrimitive(4.0), arr[3])
        assertEquals(JsonNull, arr[4])
    }

    @Test
    fun toJsonElement_with_map_containing_null_value() {
        val mapWithNull = mapOf("key" to null)
        val result = mapWithNull.toJsonElement()
        assertTrue(result is JsonObject)
        assertEquals(JsonNull, (result as JsonObject)["key"])
    }

    @Test
    fun toJsonElement_with_map_non_string_keys() {
        val mapWithIntKeys = mapOf(1 to "one", 2 to "two")
        val result = mapWithIntKeys.toJsonElement()
        assertTrue(result is JsonObject)
        val obj = result as JsonObject
        assertEquals(JsonPrimitive("one"), obj["1"])
        assertEquals(JsonPrimitive("two"), obj["2"])
    }

    // =====================
    // toJsonString tests
    // =====================

    @Test
    fun toJsonString_with_null() {
        val result = null.toJsonString()
        assertEquals("null", result)
    }

    @Test
    fun toJsonString_with_simple_string() {
        val result = "hello".toJsonString()
        assertEquals("\"hello\"", result)
    }

    @Test
    fun toJsonString_with_simple_number() {
        val result = 42.toJsonString()
        assertEquals("42", result)
    }

    @Test
    fun toJsonString_with_simple_boolean() {
        val result = true.toJsonString()
        assertEquals("true", result)
    }

    @Test
    fun toJsonString_with_empty_list() {
        val result = emptyList<Any>().toJsonString()
        assertEquals("[]", result)
    }

    @Test
    fun toJsonString_with_empty_map() {
        val result = emptyMap<String, Any>().toJsonString()
        assertEquals("{}", result)
    }

    // =====================
    // JSONObject.toMap tests
    // =====================

    @Test
    fun jsonObject_toMap_with_empty_object() {
        val jo = JSONObject()
        val result = jo.toMap()
        assertTrue(result.isEmpty())
    }

    @Test
    fun jsonObject_toMap_with_primitive_values() {
        val jo = JSONObject()
        jo.put("int", 42)
        jo.put("string", "hello")
        jo.put("boolean", true)
        jo.put("double", 3.14)

        val result = jo.toMap()
        assertEquals(42, result["int"])
        assertEquals("hello", result["string"])
        assertEquals(true, result["boolean"])
        assertEquals(3.14, result["double"])
    }

    @Test
    fun jsonObject_toMap_with_nested_objects() {
        val inner = JSONObject()
        inner.put("nested", "value")
        
        val outer = JSONObject()
        outer.put("inner", inner)
        
        val result = outer.toMap()
        @Suppress("UNCHECKED_CAST")
        val innerMap = result["inner"] as Map<String, Any?>
        assertEquals("value", innerMap["nested"])
    }

    @Test
    fun jsonObject_toMap_with_nested_arrays() {
        val arr = JSONArray(listOf(1, 2, 3))
        val jo = JSONObject()
        jo.put("array", arr)
        
        val result = jo.toMap()
        assertEquals(listOf(1, 2, 3), result["array"])
    }

    @Test
    fun jsonObject_toMap_with_null_values() {
        val jo = JSONObject()
        jo.put("nullKey", JSONObject.NULL)
        
        val result = jo.toMap()
        assertNull(result["nullKey"])
        assertTrue(result.containsKey("nullKey"))
    }

    @Test
    fun jsonObject_toMap_with_deeply_nested_structure() {
        val level3 = JSONObject().apply { put("deep", "value") }
        val level2 = JSONObject().apply { put("level3", level3) }
        val level1 = JSONObject().apply { put("level2", level2) }
        
        val result = level1.toMap()
        @Suppress("UNCHECKED_CAST")
        val l2 = result["level2"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val l3 = l2["level3"] as Map<String, Any?>
        assertEquals("value", l3["deep"])
    }

    // =====================
    // JSONArray.toList tests
    // =====================

    @Test
    fun jsonArray_toList_with_empty_array() {
        val ja = JSONArray()
        val result = ja.toList()
        assertTrue(result.isEmpty())
    }

    @Test
    fun jsonArray_toList_with_primitive_values() {
        val ja = JSONArray(listOf(1, "two", true, 4.0))
        val result = ja.toList()
        assertEquals(4, result.size)
        assertEquals(1, result[0])
        assertEquals("two", result[1])
        assertEquals(true, result[2])
        assertEquals(4.0, result[3])
    }

    @Test
    fun jsonArray_toList_with_nested_objects() {
        val jo = JSONObject().apply { put("key", "value") }
        val ja = JSONArray()
        ja.put(jo)
        
        val result = ja.toList()
        assertEquals(1, result.size)
        @Suppress("UNCHECKED_CAST")
        val map = result[0] as Map<String, Any?>
        assertEquals("value", map["key"])
    }

    @Test
    fun jsonArray_toList_with_nested_arrays() {
        val innerArray = JSONArray(listOf(1, 2))
        val outerArray = JSONArray()
        outerArray.put(innerArray)
        
        val result = outerArray.toList()
        assertEquals(1, result.size)
        assertEquals(listOf(1, 2), result[0])
    }

    @Test
    fun jsonArray_toList_with_mixed_types() {
        val jo = JSONObject().apply { put("obj", true) }
        val ja = JSONArray()
        ja.put(1)
        ja.put("string")
        ja.put(jo)
        ja.put(JSONArray(listOf("a", "b")))
        
        val result = ja.toList()
        assertEquals(4, result.size)
        assertEquals(1, result[0])
        assertEquals("string", result[1])
        @Suppress("UNCHECKED_CAST")
        assertEquals(true, (result[2] as Map<String, Any?>)["obj"])
        assertEquals(listOf("a", "b"), result[3])
    }

    @Test
    fun jsonArray_toList_preserves_order() {
        val ja = JSONArray()
        for (i in 0..9) {
            ja.put(i)
        }
        
        val result = ja.toList()
        assertEquals(10, result.size)
        for (i in 0..9) {
            assertEquals(i, result[i])
        }
    }
}
