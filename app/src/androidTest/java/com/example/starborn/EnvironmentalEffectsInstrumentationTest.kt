package com.example.starborn
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.data.local.*
import com.example.starborn.domain.environment.*
import com.example.starborn.ui.vfx.*
import com.example.starborn.ui.theme.StarbornTheme
import com.example.starborn.ui.background.rememberRoomBackgroundPainter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File

class EnvironmentalEffectsInstrumentationTest {
 @get:Rule val compose=createComposeRule()
 @Test fun sharedEffectsRenderWithAndroidAssetsAndDoNotInterceptActions() {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val evidence=File(context.getExternalFilesDir(null),"environmental-effects").apply {mkdirs()}
  var family by mutableStateOf("resonance")
  var legacy by mutableStateOf(false)
  var dark by mutableStateOf(false)
  var clicks=0
  compose.setContent {
   StarbornTheme {
    val assets=rememberAndroidEnvironmentAssets()
    val rooms=remember {assets.loadRooms().associateBy {it.id}}
    val id=when(family) {"rain","storm"->"spire_night_market";"gas"->"mine_gas";"steam"->"pit_shaft";"sparks"->"foundry_subterranean_smelter";"snow"->"deep_firewall_alpha";"starfall"->"source_orion_nightmare";else->"source_campfire"}
    val room=rooms.getValue(id).copy(weather=family,environmentalEffects=null)
    val painter=rememberRoomBackgroundPainter(room.backgroundImage)
    BoxWithConstraints(Modifier.fillMaxSize()) {
     Image(painter,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
     if(dark) Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha=.85f)))
     Button(onClick={clicks++},modifier=Modifier.align(Alignment.BottomCenter).testTag("authored-action")) {Text("Bunk light")}
     key(family,legacy,dark) {
      if(legacy) WeatherOverlay(family,modifier=Modifier.fillMaxSize(),suppressFlashes=true)
      else {
       val density=LocalDensity.current
       AndroidEnvironmentalEffects(room,EnvironmentalGeometry.fit(with(density){maxWidth.toPx()},with(density){maxHeight.toPx()},painter.intrinsicSize.width,painter.intrinsicSize.height,true),
        emptyMap(),emptySet(),UserSettings(),dark=dark,previewTimeSeconds=12f,assets=assets)
      }
     }
    }
   }
  }
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  val actionBounds=compose.onNodeWithTag("authored-action").fetchSemanticsNode().boundsInRoot
  val contentOffset=IntArray(2)
  instrumentation.runOnMainSync {
   val activity=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
       .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).first()
   activity.findViewById<android.view.View>(android.R.id.content).getLocationOnScreen(contentOffset)
  }
  fun tapAuthoredAction() {
   val x=actionBounds.center.x+contentOffset[0];val y=actionBounds.center.y+contentOffset[1]
   val now=SystemClock.uptimeMillis()
   val down=android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,x,y,0)
   val up=android.view.MotionEvent.obtain(now,now+40,android.view.MotionEvent.ACTION_UP,x,y,0)
   down.source=android.view.InputDevice.SOURCE_TOUCHSCREEN;up.source=android.view.InputDevice.SOURCE_TOUCHSCREEN
   instrumentation.uiAutomation.injectInputEvent(down,true);instrumentation.uiAutomation.injectInputEvent(up,true)
   down.recycle();up.recycle();SystemClock.sleep(100)
  }
  val families=listOf("dust","rain","storm","snow","cave_drip","starfall","steam","fog","gas","resonance","sparks","industrial")
  val sceneColors=mutableMapOf<String,Int>()
  families.forEach {name->
   for(original in listOf(true,false)) {
    compose.runOnUiThread {family=name;legacy=original};compose.mainClock.advanceTimeBy(1000);SystemClock.sleep(700)
    tapAuthoredAction()
    val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot())
    if(!original) sceneColors[name]=bitmap.getPixel(bitmap.width/2,bitmap.height/2)
    File(evidence,"${if(original) "legacy" else "shared"}-$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
   }
  }
  assertEquals(24,clicks)
  assertNotEquals("Scene changes reached the Android renderer",sceneColors["steam"],sceneColors["resonance"])
  compose.runOnUiThread {legacy=false;family="resonance";dark=true};compose.mainClock.advanceTimeBy(1000);SystemClock.sleep(300)
  tapAuthoredAction();assertEquals(25,clicks)
 }
 @Test fun qualityPersistsAcrossSettingsConsumers()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>();val store=UserSettingsStore(context)
  val original=store.settings.first().environmentalEffectsQuality
  try {EnvironmentalEffectsQuality.entries.forEach {q->store.setEnvironmentalEffectsQuality(q);assertEquals(q,UserSettingsStore(context).settings.first().environmentalEffectsQuality)}}
  finally {store.setEnvironmentalEffectsQuality(original)}
 }
}
