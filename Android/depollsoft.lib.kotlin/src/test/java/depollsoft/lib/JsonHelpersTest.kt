package depollsoft.lib

import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

class JsonHelpersTest {

    @Test
    fun testToJsonElement_Null() {
        // Act
        val result = null.toJsonElement()
        
        // Assert
        assertEquals("Null should convert to JsonNull", JsonNull, result)
    }

    @Test
    fun testToJsonElement_String() {
        // Arrange
        val input = "test string"
        
        // Act
        val result = input.toJsonElement()
        
        // Assert
        assertTrue("String should convert to JsonPrimitive", result is JsonPrimitive)
        assertEquals("String value should be preserved", input, (result as JsonPrimitive).content)
    }

    @Test
    fun testToJsonElement_Number() {
        // Arrange
        val intInput = 42
        val doubleInput = 3.14
        
        // Act
        val intResult = intInput.toJsonElement()
        val doubleResult = doubleInput.toJsonElement()
        
        // Assert
        assertTrue("Int should convert to JsonPrimitive", intResult is JsonPrimitive)
        assertTrue("Double should convert to JsonPrimitive", doubleResult is JsonPrimitive)
        assertEquals("Int value should be preserved", intInput, (intResult as JsonPrimitive).int)
        assertEquals("Double value should be preserved", doubleInput, (doubleResult as JsonPrimitive).double, 0.001)
    }

    @Test
    fun testToJsonElement_Boolean() {
        // Arrange
        val trueInput = true
        val falseInput = false
        
        // Act
        val trueResult = trueInput.toJsonElement()
        val falseResult = falseInput.toJsonElement()
        
        // Assert
        assertTrue("Boolean true should convert to JsonPrimitive", trueResult is JsonPrimitive)
        assertTrue("Boolean false should convert to JsonPrimitive", falseResult is JsonPrimitive)
        assertEquals("True value should be preserved", trueInput, (trueResult as JsonPrimitive).boolean)
        assertEquals("False value should be preserved", falseInput, (falseResult as JsonPrimitive).boolean)
    }

    @Test
    fun testToJsonElement_List() {
        // Arrange
        val input = listOf("a", "b", "c")
        
        // Act
        val result = input.toJsonElement()
        
        // Assert
        assertTrue("List should convert to JsonArray", result is JsonArray)
        val jsonArray = result as JsonArray
        assertEquals("Array should have same size", input.size, jsonArray.size)
        assertEquals("First element should match", "a", jsonArray[0].jsonPrimitive.content)
        assertEquals("Second element should match", "b", jsonArray[1].jsonPrimitive.content)
        assertEquals("Third element should match", "c", jsonArray[2].jsonPrimitive.content)
    }

    @Test
    fun testToJsonElement_Array() {
        // Arrange
        val input = arrayOf(1, 2, 3)
        
        // Act
        val result = input.toJsonElement()
        
        // Assert
        assertTrue("Array should convert to JsonArray", result is JsonArray)
        val jsonArray = result as JsonArray
        assertEquals("Array should have same size", input.size, jsonArray.size)
        assertEquals("First element should match", 1, jsonArray[0].jsonPrimitive.int)
        assertEquals("Second element should match", 2, jsonArray[1].jsonPrimitive.int)
        assertEquals("Third element should match", 3, jsonArray[2].jsonPrimitive.int)
    }

    @Test
    fun testToJsonElement_Map() {
        // Arrange
        val input = mapOf("key1" to "value1", "key2" to 42)
        
        // Act
        val result = input.toJsonElement()
        
        // Assert
        assertTrue("Map should convert to JsonObject", result is JsonObject)
        val jsonObject = result as JsonObject
        assertEquals("Object should have same size", input.size, jsonObject.size)
        assertEquals("String value should match", "value1", jsonObject["key1"]?.jsonPrimitive?.content)
        assertEquals("Int value should match", 42, jsonObject["key2"]?.jsonPrimitive?.int)
    }

    @Test
    fun testToJsonElement_Set() {
        // Arrange
        val input = setOf("a", "b", "c")
        
        // Act
        val result = input.toJsonElement()
        
        // Assert
        assertTrue("Set should convert to JsonArray", result is JsonArray)
        val jsonArray = result as JsonArray
        assertEquals("Array should have same size", input.size, jsonArray.size)
    }

    @Test
    fun testToJsonElement_ExistingJsonElement() {
        // Arrange
        val input = JsonPrimitive("test")
        
        // Act
        val result = input.toJsonElement()
        
        // Assert
        assertSame("JsonElement should return itself", input, result)
    }

    @Test
    fun testToJsonElement_UnsupportedType() {
        // Arrange
        val input = object {}
        
        // Act & Assert
        try {
            input.toJsonElement()
            fail("Should throw IllegalArgumentException for unsupported type")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error message should mention type", e.message?.contains("Unable to serialize type") == true)
        }
    }

    @Test
    fun testToJsonString_SimpleValue() {
        // Arrange
        val input = "test"
        
        // Act
        val result = input.toJsonString()
        
        // Assert
        assertEquals("String should be JSON encoded", "\"test\"", result)
    }

    @Test
    fun testToJsonString_ComplexObject() {
        // Arrange
        val input = mapOf("name" to "John", "age" to 30)
        
        // Act
        val result = input.toJsonString()
        
        // Assert
        assertTrue("Result should be valid JSON", result.startsWith("{"))
        assertTrue("Result should contain name", result.contains("\"name\""))
        assertTrue("Result should contain age", result.contains("\"age\""))
    }

    @Test
    fun testJSONObjectToMap_SimpleObject() {
        // Arrange
        val jsonObject = JSONObject()
        jsonObject.put("string", "value")
        jsonObject.put("number", 42)
        jsonObject.put("boolean", true)
        
        // Act
        val result = jsonObject.toMap()
        
        // Assert
        assertEquals("Map should have correct size", 3, result.size)
        assertEquals("String value should match", "value", result["string"])
        assertEquals("Number value should match", 42, result["number"])
        assertEquals("Boolean value should match", true, result["boolean"])
    }

    @Test
    fun testJSONObjectToMap_WithNullValue() {
        // Arrange
        val jsonObject = JSONObject()
        jsonObject.put("nullValue", JSONObject.NULL)
        
        // Act
        val result = jsonObject.toMap()
        
        // Assert
        assertNull("Null value should be converted to null", result["nullValue"])
    }

    @Test
    fun testJSONObjectToMap_NestedObject() {
        // Arrange
        val nestedObject = JSONObject()
        nestedObject.put("nested", "value")
        val jsonObject = JSONObject()
        jsonObject.put("parent", nestedObject)
        
        // Act
        val result = jsonObject.toMap()
        
        // Assert
        assertTrue("Nested object should be converted to Map", result["parent"] is Map<*, *>)
        val nestedMap = result["parent"] as Map<*, *>
        assertEquals("Nested value should match", "value", nestedMap["nested"])
    }

    @Test
    fun testJSONArrayToList_SimpleArray() {
        // Arrange
        val jsonArray = JSONArray()
        jsonArray.put("string")
        jsonArray.put(42)
        jsonArray.put(true)
        
        // Act
        val result = jsonArray.toList()
        
        // Assert
        assertEquals("List should have correct size", 3, result.size)
        assertEquals("String value should match", "string", result[0])
        assertEquals("Number value should match", 42, result[1])
        assertEquals("Boolean value should match", true, result[2])
    }

    @Test
    fun testJSONArrayToList_NestedArray() {
        // Arrange
        val nestedArray = JSONArray()
        nestedArray.put("nested")
        val jsonArray = JSONArray()
        jsonArray.put(nestedArray)
        
        // Act
        val result = jsonArray.toList()
        
        // Assert
        assertTrue("Nested array should be converted to List", result[0] is List<*>)
        val nestedList = result[0] as List<*>
        assertEquals("Nested value should match", "nested", nestedList[0])
    }

    @Test
    fun testJSONArrayToList_EmptyArray() {
        // Arrange
        val jsonArray = JSONArray()
        
        // Act
        val result = jsonArray.toList()
        
        // Assert
        assertEquals("Empty array should convert to empty list", 0, result.size)
    }
}