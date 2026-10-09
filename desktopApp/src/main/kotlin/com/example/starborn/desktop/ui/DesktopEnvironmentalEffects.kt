package com.example.starborn.desktop.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.example.starborn.desktop.DesktopAppServices
import com.example.starborn.domain.environment.*
import com.example.starborn.domain.model.Room
import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.EnvironmentalEffectsQuality
import com.example.starborn.ui.environment.EnvironmentalEffectsOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

private val steamMutex=kotlinx.coroutines.sync.Mutex()
private var steamCache: List<ImageBitmap>? = null
@Composable
internal fun rememberDesktopEnvironmentSteam(services: DesktopAppServices, needed: Boolean): List<ImageBitmap> {
    val frames by produceState(steamCache.orEmpty(),needed,services.assetProvider) {
        if(needed && steamCache==null) value=withContext(Dispatchers.IO) {
            steamMutex.withLock { steamCache ?: (0 until 48).mapNotNull { index -> services.assetProvider.open("images/vfx/steam_jet/steam_frame_%02d.png".format(java.util.Locale.ROOT,index))?.use { runCatching { loadImageBitmap(it) }.getOrNull() } }
                .also { steamCache=it } }
        }
    }
    return if(needed) frames else emptyList()
}

@Composable
internal fun DesktopEnvironmentalEffects(services:DesktopAppServices,room:Room?,geometry:EnvironmentalGeometry,
    roomState:Map<String,Boolean> = emptyMap(), milestones:Set<String> = emptySet(),
    settings:UserSettings = LocalExplorationSettings.current, dark:Boolean=false,combat:Boolean=false,
    paused:Boolean=false,suppressAccents:Boolean=false,opacity:Float=1f,transition:Boolean=false,
    modifier:Modifier=Modifier,previewTimeSeconds:Float?=null,sceneMemory:EnvironmentalSceneMemory?=null) {
    if(room==null || settings.environmentalEffectsQuality==EnvironmentalEffectsQuality.OFF) return
    val effects=remember(room,roomState,milestones,settings,dark,combat,suppressAccents) {
        services.worldDataSource.environmentalEffectResolver.resolve(room,roomState,milestones,settings,dark,combat,suppressAccents)
    }
    val frames=rememberDesktopEnvironmentSteam(services,effects.any { it.preset.primitive=="jet" })
    val cap=(if(settings.environmentalEffectsQuality==EnvironmentalEffectsQuality.REDUCED) 180 else 360)/(if(transition) 2 else 1)
    EnvironmentalEffectsOverlay(room.id,effects,geometry,modifier,cap,paused,frames,previewTimeSeconds,opacity,sceneMemory=sceneMemory,maxJets=if(transition) 4 else 8,maxBursts=if(transition) 1 else 2)
}
