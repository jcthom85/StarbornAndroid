package com.example.starborn.shared.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class SampleData(
    val id: String,
    val count: Int,
    val active: Boolean,
    val tags: List<String>
)

class StarbornJsonTest {

    @Test
    fun testSerializationRoundTrip() {
        val sample = SampleData(id = "station_01", count = 42, active = true, tags = listOf("alpha", "beta"))
        val json = StarbornJson.encodeToString(sample)
        val decoded = StarbornJson.decodeFromString<SampleData>(json)

        assertEquals(sample.id, decoded.id)
        assertEquals(sample.count, decoded.count)
        assertEquals(sample.active, decoded.active)
        assertEquals(sample.tags, decoded.tags)
    }

    @Test
    fun testAnyKSerializerWithDynamicMap() {
        val json = """{"door_unlocked": true, "attempts": 3, "label": "secure_vault"}"""
        val map = StarbornJson.decodeFromString(
            kotlinx.serialization.builtins.MapSerializer(
                kotlinx.serialization.serializer<String>(),
                AnyKSerializer
            ),
            json
        )

        assertEquals(true, map["door_unlocked"])
        assertEquals(3L, (map["attempts"] as Number).toLong())
        assertEquals("secure_vault", map["label"])
    }
}
