package com.example.starborn.desktop

import com.example.starborn.core.platform.DesktopAssetProvider
import org.junit.Assert.*
import org.junit.Test

class DesktopPackagedAssetsTest {
    @Test fun packagedAssetsLoadWithoutFilesystemFallbacks() {
        val assets = DesktopAssetProvider(devAssetDirs = emptyList())
        listOf("rooms.json", "events.json", "items.json", "images/rooms/world_1/pit_nova_bunk_v5.webp", "images/arcade/deep_mine/mine_shaft_v1.webp").forEach { path ->
            assertTrue("Packaged asset missing: $path", assets.exists(path))
            assets.open(path).use { assertTrue(it != null && it.read() >= 0) }
        }
        assertTrue(assets.list("images/rooms/world_1").isNotEmpty())
        assertNull(assets.open("../rooms.json"))
    }
}
