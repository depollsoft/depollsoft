package depollsoft.lib

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    is List<*> -> JsonArray(map { it.toJsonElement() })
    is Map<*, *> -> JsonObject(map { it.key.toString() to it.value.toJsonElement() }.toMap())
    is Set<*> -> JsonArray(map { it.toJsonElement() })
    else -> throw IllegalArgumentException("Unable to serialize type ${this::class}")
}

fun Any?.toJsonString(): String = Json.encodeToString(this.toJsonElement())

fun JSONObject.toMap(): Map<String, Any?> =
    this.keys().asSequence().associate { k ->
        when (val v = this.opt(k)) {
            is JSONObject -> Pair(k, v.toMap())
            is JSONArray -> Pair(k, v.toList())
            JSONObject.NULL -> Pair(k, null)
            else -> Pair(k, v)
        }
    }

fun JSONArray.toList(): List<Any?> =
    (0 until this.length()).map {
        when (val v = this.opt(it)) {
            is JSONObject -> v.toMap()
            is JSONArray -> v.toList()
            else -> v
        }
    }