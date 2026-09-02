package com.example.starborn.shared.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Universal JSON serializer instance for Starborn across Android, Desktop JVM, and iOS.
 */
val StarbornJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
    prettyPrint = false
}

/**
 * Polymorphic serializer for dynamic untyped values (primitives, maps, lists)
 * commonly found in Starborn room state and script actions.
 */
object AnyKSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("AnyKSerializer can only be used with Json")
        return jsonDecoder.decodeJsonElement().toAny() ?: ""
    }

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("AnyKSerializer can only be used with Json")
        jsonEncoder.encodeJsonElement(value.toJsonElement())
    }
}

fun JsonElement.toAny(): Any? = when (this) {
    is JsonPrimitive -> {
        if (isString) content
        else booleanOrNull ?: intOrNull ?: longOrNull ?: doubleOrNull ?: content
    }
    is JsonArray -> map { it.toAny() }
    is JsonObject -> mapValues { it.value.toAny() }
    JsonNull -> null
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> buildJsonObject {
        forEach { (k, v) ->
            if (k is String) put(k, v.toJsonElement())
        }
    }
    is Iterable<*> -> buildJsonArray {
        forEach { add(it.toJsonElement()) }
    }
    is JsonElement -> this
    else -> JsonPrimitive(toString())
}
