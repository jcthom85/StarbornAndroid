package com.example.starborn.desktop.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.environment.*
import com.example.starborn.data.local.*
import com.example.starborn.ui.environment.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
fun main():Unit=runBlocking {
 withContext(Dispatchers.Main) {
  val folder=File("build/reports/environmental-effects").apply {mkdirs()}
  val services=DesktopAppServices(Files.createTempDirectory("starborn-environment-review").toFile())
  services.userSettingsStore.setTutorialsEnabled(false)
  check(services.startDebugScenario("tut_npc_dialogue"))
  val steam=(0 until 48).map {i->services.assetProvider.open("images/vfx/steam_jet/steam_frame_%02d.png".format(i))!!.use(::loadImageBitmap)}
  val catalog=services.worldDataSource.environmentalEffectsCatalog
  val presets=catalog.presets.keys.filter {!it.startsWith("legacy.")}+listOf("legacy.dust","legacy.rain","legacy.storm","legacy.snow","legacy.cave_drip","legacy.starfall","legacy.steam","legacy.fog","legacy.gas","legacy.resonance","legacy.sparks","legacy.industrial")
  for(preset in if(System.getProperty("starborn.environmentReview.quick")=="true") presets.filter {it.contains("resonance")} else presets) {
   val roomId=when {preset.contains("rain") || preset.contains("storm")->"spire_night_market";preset.contains("gas")->"mine_gas";preset.contains("fog") || preset.contains("spore")->"sector9_landing_brush";preset.contains("steam") || preset.contains("vent") || preset.contains("pressure")->"pit_shaft";preset.contains("spark") || preset.contains("arc") || preset.contains("ember") || preset.contains("ash")->"foundry_subterranean_smelter";preset.contains("snow")->"deep_firewall_alpha";preset.contains("starfall")->"source_orion_nightmare";preset.contains("resonance") || preset.contains("gravity")->"source_campfire";else->"astra_bridge"}
   val original=services.roomDefinitions.getValue(roomId)
   for((level,intensity) in listOf("whisper" to .1f,"present" to .5f,"extreme" to .9f)) {
    var time by mutableFloatStateOf(12f)
    val definition=catalog.presets.getValue(preset)
    val room=original.copy(environmentalEffects=listOf(RoomEffectBinding("review",preset,intensity,emitters=definition.emitters.ifEmpty {listOf(EffectEmitter(.5f,.68f))})))
    val effects=services.worldDataSource.environmentalEffectResolver.resolve(room,emptyMap(),emptySet(),UserSettings())
    var motionStart=12f
    if(definition.burstInterval!=null) {
        val probe=EnvironmentalEffectSimulation(360);probe.update(effects)
        for(tick in 1..2400) {
            probe.advance(.025f,EnvironmentalGeometry.fit(300f,480f,9f,16f,true))
            if(probe.activeBursts>0) {motionStart=(tick*.025f-.25f).coerceAtLeast(0f);break}
        }
    }
    val scene=ImageComposeScene(300,480,coroutineContext=coroutineContext) {
     DesktopStarbornTheme {
      val painter=rememberDesktopAssetPainter(original.backgroundImage,services.assetProvider)
      Box { androidx.compose.foundation.Image(painter,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
       EnvironmentalEffectsOverlay(room.id,effects,EnvironmentalGeometry.fit(300f,480f,painter.intrinsicSize.width,painter.intrinsicSize.height,true),
         steamFrames=steam,previewTimeSeconds=time)
      }
     }
    }
    repeat(5){scene.render(it*100_000_000L).close();delay(15)}
    for(frame in 0 until 12) {
     time=motionStart+frame*(if(definition.burstInterval!=null) .2f else .5f)
     scene.render((frame*3+10)*100_000_000L).close();delay(2)
     scene.render((frame*3+11)*100_000_000L).close();delay(2)
     scene.render((frame*3+12)*100_000_000L).use {File(folder,"${preset}_$level-$frame.png").writeBytes(it.encodeToData()!!.bytes)}
    }
    scene.close()
   }
   println("Reviewed $preset")
  }
  for((width,height) in listOf(1440 to 900,1280 to 720,900 to 650,2560 to 1080,405 to 720)) {
   val room=services.roomDefinitions.getValue("source_campfire")
   val scene=ImageComposeScene(width,height,coroutineContext=coroutineContext) {
    DesktopStarbornTheme {
     val painter=rememberDesktopAssetPainter(room.backgroundImage,services.assetProvider)
     if(width==405) Box { Image(painter,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
      DesktopEnvironmentalEffects(services,room,EnvironmentalGeometry.fit(width.toFloat(),height.toFloat(),painter.intrinsicSize.width,painter.intrinsicSize.height,true),previewTimeSeconds=12f)
     } else DesktopPortraitBackdrop(painter,null) { layout->
      DesktopEnvironmentalEffects(services,room,EnvironmentalGeometry(width.toFloat(),height.toFloat(),layout.center.left,layout.center.top,layout.center.width,layout.center.height),previewTimeSeconds=12f)
     }
    }
   }
   repeat(12){scene.render(it*100_000_000L).close();delay(20)}
   scene.render(1_300_000_000L).use {File(folder,"resonance-$width-$height.png").writeBytes(it.encodeToData()!!.bytes)};scene.close()
  }
  for(id in listOf("pit_nova_bunk","pit_jed_bunk","pit_shaft","spire_night_market","mine_gas","source_campfire","astra_simulation_deck")) {
   services.sessionStore.setRoom(id)
   withTimeout(15000){services.exploration.uiState.first {it.currentRoom?.id==id && !it.isLoading}}
   if(id=="pit_nova_bunk") services.exploration.uiState.value.actions.firstOrNull {it.name.equals("bunk light",true)}?.let(services.exploration::onActionSelected)
   services.exploration.dismissNarration();services.questPresentations.clear()
   val scene=ImageComposeScene(1440,900,coroutineContext=coroutineContext) {DesktopStarbornTheme { DesktopExplorationScreen(services,{},{},{},{},{},{}) }}
   repeat(25){services.exploration.dismissNarration();services.questPresentations.clear();scene.render(it*100_000_000L).close();delay(20)}
   scene.render(2_600_000_000L).use {File(folder,"gameplay-$id.png").writeBytes(it.encodeToData()!!.bytes)};scene.close()
  }
  run {
   val room=services.roomDefinitions.getValue("pit_shaft")
   val scene=ImageComposeScene(1440,900,coroutineContext=coroutineContext) {DesktopStarbornTheme {
    EnvironmentalEffectsPreview(room,catalog,rememberDesktopAssetPainter(room.backgroundImage,services.assetProvider),steam,emptyMap(),emptySet(),{},{})
   }}
   repeat(12){scene.render(it*100_000_000L).close();delay(20)}
   scene.render(1_300_000_000L).use {File(folder,"authoring-preview.png").writeBytes(it.encodeToData()!!.bytes)};scene.close()
  }
  services.sessionStore.setRoom("mine_gas")
  val combatScene=ImageComposeScene(1440,900,coroutineContext=coroutineContext) {DesktopStarbornTheme {DesktopCombatScreen(services,listOf("faulted_loader"),{},{},{})}}
  repeat(20){combatScene.render(it*100_000_000L).close();delay(20)}
  combatScene.render(2_100_000_000L).use {File(folder,"combat-gas.png").writeBytes(it.encodeToData()!!.bytes)};combatScene.close()
  services.close()
 }
 exitProcess(0)
}
