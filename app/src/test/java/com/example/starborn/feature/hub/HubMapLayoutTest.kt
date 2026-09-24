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
                assertTrue(site.y > layout.cropTop && site.y < layout.cropBottom)
                assertTrue(site.artworkWidth in 0f..0.4f)
            }
        }
    }

    @Test fun `map fit preserves image proportions at compact phone and tablet sizes`() {
        listOf(320f to 340f, 400f to 540f, 800f to 900f).forEach { (width, height) ->
            HubMapLayouts.all.values.forEach { layout ->
                val fit = HubMapTransform.fit(width, height, 9f / 16f, layout.cropTop, layout.cropBottom)
                assertTrue(fit.width <= width + .01f)
                assertTrue(fit.y(layout.cropBottom) <= height + .01f)
                assertEquals(0f, fit.y(layout.cropTop), .001f)
                assertEquals(9f / 16f, fit.width / fit.imageHeight, .001f)
            }
        }
    }
}
