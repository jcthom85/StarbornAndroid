package com.example.starborn.desktop.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.data.local.*
import com.example.starborn.domain.environment.*
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

/** CPU submission/simulation probe; it is not a physical-device GPU frame-time claim. */
@OptIn(ExperimentalComposeUiApi::class)
fun main():Unit=runBlocking {
 withContext(Dispatchers.Main) {
  val folder=File("build/reports/environmental-effects").apply {mkdirs()}
  val services=DesktopAppServices(Files.createTempDirectory("environment-benchmark").toFile())
  val output=StringBuilder("scene,quality,p50_render_ms,p95_render_ms,p95_simulation_ms,allocated_bytes_per_simulation_frame\n")
  val allocation=ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean
  if(allocation?.isThreadAllocatedMemorySupported==true) allocation.isThreadAllocatedMemoryEnabled=true
  for(id in listOf("pit_jed_bunk","spire_night_market","mine_gas","source_campfire","pit_shaft")) {
   for(quality in listOf(EnvironmentalEffectsQuality.OFF,EnvironmentalEffectsQuality.FULL)) {
    val room=services.roomDefinitions.getValue(id)
    val settings=UserSettings(environmentalEffectsQuality=quality)
    val resolved=services.worldDataSource.environmentalEffectResolver.resolve(room,emptyMap(),emptySet(),settings)
    val geometry=EnvironmentalGeometry.fit(1440f,900f,9f,16f)
    val sim=EnvironmentalEffectSimulation(360);sim.update(resolved)
    repeat(1000){sim.advance(.016f,geometry)}
    val allocationStart=allocation?.getThreadAllocatedBytes(Thread.currentThread().id) ?: 0L
    val simulationTimes=LongArray(10000) {
     val start=System.nanoTime();sim.advance(.016f,geometry);System.nanoTime()-start
    }.sorted()
    val allocationEnd=allocation?.getThreadAllocatedBytes(Thread.currentThread().id) ?: 0L
    val scene=ImageComposeScene(1440,900,coroutineContext=coroutineContext) {DesktopStarbornTheme {
     DesktopPortraitBackdrop(rememberDesktopAssetPainter(room.backgroundImage,services.assetProvider),null) {layout->
      DesktopEnvironmentalEffects(services,room,EnvironmentalGeometry(1440f,900f,layout.center.left,layout.center.top,layout.center.width,layout.center.height),settings=settings)
     }
    }}
    repeat(30){scene.render(it*16_666_667L).close();delay(3)}
    val samples=LongArray(180) {i->
     val start=System.nanoTime();scene.render((i+30)*16_666_667L).close();val cost=System.nanoTime()-start
     delay(1);cost
    }.sorted()
    output.append("$id,${quality.name},${samples[90]/1e6},${samples[171]/1e6},${simulationTimes[9500]/1e6},${(allocationEnd-allocationStart)/10000}\n")
    scene.close()
   }
  }
  File(folder,"cpu-benchmark.csv").writeText(output.toString());println(output)
  services.close()
 }
 exitProcess(0)
}
