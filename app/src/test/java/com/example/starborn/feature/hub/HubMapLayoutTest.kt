package com.example.starborn.feature.hub

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.feature.hub.ui.HubMapLayouts
import com.example.starborn.feature.hub.ui.HubMapTransform
import org.junit.Assert.*
import org.junit.Test

class HubMapLayoutTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))

    @Test fun `every hub and destination has an authored visible ground anchor`() {
        assertEquals(assets.loadHubs().map { it.id }.toSet(), HubMapLayouts.all.keys)
        assets.loadHubs().forEach { hub ->
            val layout = HubMapLayouts.all.getValue(hub.id)
            val expected = assets.loadHubNodes().filter { it.hubId == hub.id }.map { it.id }.toSet() +
                if (hub.id == "hub_astra") setOf("astra_disembark") else emptySet()
            assertEquals(hub.id, expected, layout.sites.keys)
            (layout.sites.values + listOfNotNull(layout.astraDock)).forEach { site ->
                assertTrue(site.x in 0f..1f)
                assertTrue(site.y in .24f.. .81f)
                assertTrue(site.artworkWidth in 0f..0.4f)
            }
            if (hub.id != "hub_astra") layout.sites.filterKeys { it != "hangar_bay" }.values.forEach {
                assertTrue("${hub.id} must display destination artwork", it.artworkWidth > 0f)
            }
        }
    }

    @Test fun `portrait map fills the screen with no letterboxing and matching anchor transform`() {
        listOf(320f to 640f, 400f to 890f, 800f to 1280f).forEach { (width, height) ->
            val fit = HubMapTransform.cover(width, height, 1088f / 1920f)
            assertTrue(fit.x(0f) <= 0f && fit.x(1f) >= width)
            assertTrue(fit.y(0f) <= 0f && fit.y(1f) >= height)
            assertEquals(width / 2, fit.x(.5f), .001f)
            assertEquals(height / 2, fit.y(.5f), .001f)
            assertEquals(1088f / 1920f, fit.width / fit.imageHeight, .001f)
        }
    }
}
