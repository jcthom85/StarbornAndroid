package com.example.starborn.ui.vfx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starborn.data.local.UserSettingsStore

import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.core.platform.AndroidAssetProvider
import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.EnvironmentalEffectsQuality
import com.example.starborn.domain.environment.*
import com.example.starborn.domain.model.Room
import com.example.starborn.ui.environment.EnvironmentalEffectsOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

private val steamMutex=kotlinx.coroutines.sync.Mutex()
private var steamCache:List<ImageBitmap>? = null
@Composable
fun rememberAndroidEnvironmentAssets(): WorldAssetDataSource {
    val context=LocalContext.current.applicationContext
    return remember(context) { WorldAssetDataSource(AssetJsonReader(AndroidAssetProvider(context),MoshiProvider.instance)) }
}
@Composable
fun rememberAndroidEnvironmentSteam(needed:Boolean):List<ImageBitmap> {
    val context=LocalContext.current.applicationContext
    val frames by produceState(steamCache.orEmpty(),needed,context) {
        if(needed && steamCache==null) value=withContext(Dispatchers.IO) {
            steamMutex.withLock { steamCache ?: (0 until 48).mapNotNull { index -> runCatching { context.assets.open("images/vfx/steam_jet/steam_frame_%02d.png".format(java.util.Locale.ROOT,index)).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull() }.also { steamCache=it } }
        }
    }
    return if(needed) frames else emptyList()
}
@Composable
fun AndroidEnvironmentalEffects(room:Room?,geometry:EnvironmentalGeometry,roomState:Map<String,Boolean>,
    milestones:Set<String>,settings:UserSettings,dark:Boolean=false,combat:Boolean=false,
    paused:Boolean=false,suppressAccents:Boolean=false,opacity:Float=1f,transition:Boolean=false,
    modifier:Modifier=Modifier,previewTimeSeconds:Float?=null,
    assets:WorldAssetDataSource=rememberAndroidEnvironmentAssets(),sceneMemory:EnvironmentalSceneMemory?=null) {
    if(room==null || settings.environmentalEffectsQuality==EnvironmentalEffectsQuality.OFF) return
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    LaunchedEffect(lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        foreground=true;try { awaitCancellation() } finally { foreground=false }
    } }
    val effects=remember(room,roomState,milestones,settings,dark,combat,suppressAccents) {
        assets.environmentalEffectResolver.resolve(room,roomState,milestones,settings,dark,combat,suppressAccents)
    }
    val frames=rememberAndroidEnvironmentSteam(effects.any { it.preset.primitive=="jet" })
    val cap=(if(settings.environmentalEffectsQuality==EnvironmentalEffectsQuality.REDUCED) 120 else 240)/(if(transition) 2 else 1)
    EnvironmentalEffectsOverlay(room.id,effects,geometry,modifier,cap,paused || !foreground,frames,previewTimeSeconds,opacity,sceneMemory=sceneMemory,maxJets=if(transition) 4 else 8,maxBursts=if(transition) 1 else 2)
}

@Composable
fun rememberEnvironmentalSettings(): UserSettings {
    val context=LocalContext.current.applicationContext
    val store=remember(context) { UserSettingsStore(context) }
    return store.settings.collectAsStateWithLifecycle(initialValue=UserSettings()).value
}

@Composable
fun AndroidRoomEnvironmentalEffects(room:Room?, artworkSize:Size, roomState:Map<String,Boolean>,
    milestones:Set<String>, settings:UserSettings, assets:WorldAssetDataSource,
    dark:Boolean=false, combat:Boolean=false, paused:Boolean=false, suppressAccents:Boolean=false,
    translationX:Float=0f, translationY:Float=0f, scale:Float=1f, opacity:Float=1f, transition:Boolean=false,
    sceneMemory:EnvironmentalSceneMemory?=null, layerTransform:(GraphicsLayerScope.()->Unit)?=null) {
    if (room == null || settings.environmentalEffectsQuality == EnvironmentalEffectsQuality.OFF) return
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val fallbackWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val fallbackHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val width = if (measuredSize.width > 0) measuredSize.width.toFloat() else fallbackWidth
    val height = if (measuredSize.height > 0) measuredSize.height.toFloat() else fallbackHeight
    val base = remember(width, height, artworkSize.width, artworkSize.height) {
        EnvironmentalGeometry.fit(width, height, artworkSize.width, artworkSize.height, crop = true)
    }
    val geometry = remember(base, width, height, translationX, translationY, scale) {
        if (translationX == 0f && translationY == 0f && scale == 1f) base
        else base.copy(
            imageX = base.imageX + base.imageWidth * (1 - scale) / 2 + width * translationX,
            imageY = base.imageY + base.imageHeight * (1 - scale) / 2 + height * translationY,
            imageWidth = base.imageWidth * scale,
            imageHeight = base.imageHeight * scale
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { if (it != measuredSize) measuredSize = it }
            .then(if (layerTransform != null) Modifier.graphicsLayer(block = layerTransform) else Modifier)
    ) {
        AndroidEnvironmentalEffects(room,geometry,roomState,milestones,settings,dark,combat,paused,
            suppressAccents,opacity,transition,assets=assets,sceneMemory=sceneMemory)
    }
}
