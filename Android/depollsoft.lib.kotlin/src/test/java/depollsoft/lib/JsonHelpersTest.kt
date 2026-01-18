package depollsoft.lib

import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JsonHelpersTest {

    @Test
    fun toJsonElement_primitives_and_collections() {
        assertEquals(JsonNull, null.toJsonElement())
        assertEquals(JsonPrimitive(1), 1.toJsonElement())
        assertEquals(JsonPrimitive(true), true.toJsonElement())
        assertEquals(JsonPrimitive("s"), "s".toJsonElement())

        val arr = arrayOf(1, 2, 3)
        assertEquals(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))), arr.toJsonElement())

        val list = listOf("a", "b")
        assertEquals(JsonArray(list.map { JsonPrimitive(it) }), list.toJsonElement())

        val set = setOf(false, true)
        val setElem = set.toJsonElement()
        assertTrue(setElem is JsonArray)
        assertEquals(2, (setElem as JsonArray).size)

        val map = mapOf("x" to 1, "y" to 2)
        val mapElem = map.toJsonElement()
        assertTrue(mapElem is JsonObject)
        assertEquals(JsonPrimitive(1), (mapElem as JsonObject)["x"])
        assertEquals(JsonPrimitive(2), mapElem["y"])

        // Passing through JsonElement should be identity
        val element: JsonElement = JsonPrimitive("z")
        assertEquals(element, element.toJsonElement())
    }

    @Test
    fun toJsonString_serializes_nested() {
        val obj: Any? = mapOf(
            "a" to listOf(1, 2),
            "b" to mapOf("c" to true)
        )
        val json = obj.toJsonString()
        val parsed = Json.parseToJsonElement(json)
        require(parsed is JsonObject)
        assertEquals(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))), parsed["a"])
        assertEquals(JsonObject(mapOf("c" to JsonPrimitive(true))), parsed["b"])
    }

    @Test
    fun jsonObjectAndArray_conversion_helpers() {
        val jo = org.json.JSONObject()
        jo.put("a", 1)
        jo.put("b", org.json.JSONObject(mapOf("c" to 2)))
        jo.put("n", org.json.JSONObject.NULL)
        val out = jo.toMap()
        assertEquals(1, out["a"])
        @Suppress("UNCHECKED_CAST")
        val nested = out["b"] as Map<String, Any?>
        assertEquals(2, nested["c"])
        // NULL becomes null
        org.junit.Assert.assertEquals(null, out["n"])

        val ja = org.json.JSONArray(listOf(1, 2, 3))
        val list = ja.toList()
        assertEquals(listOf(1,2,3), list)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toJsonElement_unsupported_type_throws() {
        class Unsupported
        Unsupported().toJsonElement()
    }
}

