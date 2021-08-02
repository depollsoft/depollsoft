package depollsoft.lib

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

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