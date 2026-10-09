package com.example.starborn.desktop
import com.example.starborn.data.local.EnvironmentalEffectsQuality
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
class DesktopEnvironmentalEffectsTest {
 @Test fun qualityPersistsAfterStoreRecreation()=runBlocking {
  val directory=Files.createTempDirectory("environment-settings").toFile()
  try {
   EnvironmentalEffectsQuality.entries.forEach {q->
    DesktopUserSettingsStore(directory).use {store->store.setEnvironmentalEffectsQuality(q);assertEquals(q,store.settings.first().environmentalEffectsQuality)}
    delay(100)
    DesktopUserSettingsStore(directory).use {store->assertEquals(q,store.settings.first().environmentalEffectsQuality)}
    delay(100)
   }
  } finally {directory.deleteRecursively()}
 }
}
