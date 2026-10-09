package com.example.starborn.ui.environment

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.starborn.data.local.*
import com.example.starborn.domain.environment.*
import com.example.starborn.domain.model.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.math.*

/** Debug authoring only. Uses the production resolver, geometry, simulator and renderer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentalEffectsPreview(room:Room,catalog:EnvironmentalEffectCatalog,painter:Painter,
    steamFrames:List<ImageBitmap>,initialState:Map<String,Boolean>,initialMilestones:Set<String>,
    onCopy:(String)->Unit,onClose:()->Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    var intensity by remember { mutableFloatStateOf(.25f) }
    var space by remember { mutableStateOf("scene") }
    var dark by remember { mutableStateOf(false) };var combat by remember { mutableStateOf(false) }
    var flashes by remember { mutableStateOf(true) };var motion by remember { mutableStateOf(true) }
    var contrast by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf(EnvironmentalEffectsQuality.FULL) }
    var paused by remember { mutableStateOf(true) };var time by remember { mutableFloatStateOf(12f) }
    var replay by remember { mutableIntStateOf(0) }
    var placement by remember { mutableStateOf("Emitter") }
    var emitters by remember { mutableStateOf(listOf(EffectEmitter())) }
    var region by remember { mutableStateOf(EffectRegion()) }
    var firstCorner by remember { mutableStateOf<Offset?>(null) }
    var state by remember { mutableStateOf(initialState) }
    var milestones by remember { mutableStateOf(initialMilestones.joinToString(",")) }
    var layout by remember { mutableStateOf("Desktop") }
    var dropdown by remember { mutableStateOf(false) }
    var particles by remember { mutableIntStateOf(0) };var jets by remember { mutableIntStateOf(0) }
    var simulationMicros by remember { mutableLongStateOf(0L) }
    var bursts by remember { mutableIntStateOf(0) };var copied by remember { mutableStateOf(false) }
    val binding=RoomEffectBinding("preview",selected.orEmpty(),intensity,space,region,emitters)
    val previewRoom=remember(room,selected,binding) { if(selected==null) room else room.copy(environmentalEffects=listOf(binding)) }
    val settings=UserSettings(environmentalEffectsQuality=quality,disableFlashes=!flashes,disableScreenshake=!motion,highContrastMode=contrast)
    val resolver=remember(catalog) { EnvironmentalEffectResolver(catalog) }
    val effects=remember(previewRoom,state,milestones,settings,dark,combat) {
        resolver.resolve(previewRoom,state,milestones.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),settings,dark,combat)
    }
    val json=remember(binding) { Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        .adapter(RoomEffectBinding::class.java).indent("  ").toJson(binding) }
    val controls: @Composable ()->Unit = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Environmental effects",style=MaterialTheme.typography.titleLarge)
            Text(room.title,style=MaterialTheme.typography.bodySmall)
            Box {
                OutlinedButton(onClick={ dropdown=true }) { Text(selected ?: "Authored room effects") }
                DropdownMenu(dropdown,{dropdown=false},Modifier.heightIn(max=320.dp)) {
                    DropdownMenuItem(text={Text("Authored room effects")},onClick={selected=null;dropdown=false})
                    catalog.presets.keys.sorted().forEach { id -> DropdownMenuItem(text={Text(id)},onClick={
                        selected=id;dropdown=false;val p=catalog.presets.getValue(id);intensity=p.intensity;space=p.space
                        emitters=p.emitters.ifEmpty { listOf(EffectEmitter()) };region=EffectRegion();copied=false
                    }) }
                }
            }
            Text("Intensity: %.2f".format(intensity))
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Whisper" to .1f,"Present" to .5f,"Extreme" to .9f).forEach { (label,value) ->
                    TextButton(onClick={intensity=value}) { Text(label) }
                }
            }
            Slider(intensity,{intensity=it},valueRange=0f..1f)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("scene","viewport").forEach { value -> FilterChip(space==value,{space=value},label={Text(value)}) }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Android","Desktop","Compact").forEach { value -> FilterChip(layout==value,{layout=value},label={Text(value)}) }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("Emitter","Region").forEach { value -> FilterChip(placement==value,{placement=value;firstCorner=null},label={Text(value)}) }
            }
            Text(if(placement=="Emitter") "Tap artwork to add a source." else "Tap two corners to set a region.",style=MaterialTheme.typography.bodySmall)
            emitters.forEachIndexed { i,e -> Text("Source ${i+1}: %.3f, %.3f / %.0f?".format(e.x,e.y,e.angleDegrees),style=MaterialTheme.typography.bodySmall) }
            Row {
                TextButton(onClick={emitters=emptyList()}) {Text("Clear sources")}
                TextButton(onClick={region=EffectRegion();firstCorner=null}) {Text("Reset region")}
            }
            if(emitters.isNotEmpty()) {
                Text("Direction: %.0f?".format(emitters.last().angleDegrees),style=MaterialTheme.typography.bodySmall)
                Slider(emitters.last().angleDegrees,{angle->emitters=emitters.dropLast(1)+emitters.last().copy(angleDegrees=angle)},valueRange=-180f..180f)
            }
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                EnvironmentalEffectsQuality.entries.forEach { q->FilterChip(quality==q,{quality=q},label={Text(q.label)}) }
            }
            Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(dark,{dark=it});Text("Dark");Checkbox(combat,{combat=it});Text("Combat") }
            Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(flashes,{flashes=it});Text("Flashes");Checkbox(motion,{motion=it});Text("Field motion") }
            Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(contrast,{contrast=it});Text("High contrast") }
            state.keys.sorted().forEach { name -> Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(state[name]==true,{state=state+(name to it)});Text(name,style=MaterialTheme.typography.bodySmall)
            } }
            OutlinedTextField(milestones,{milestones=it},label={Text("Milestones (comma separated)")},modifier=Modifier.fillMaxWidth())
            Row {
                TextButton(onClick={paused=!paused}) {Text(if(paused) "Play" else "Pause")}
                TextButton(onClick={time=0f;replay++;paused=false}) {Text("Replay")}
            }
            Text("Fixed seed / time %.2fs".format(time),style=MaterialTheme.typography.bodySmall)
            Slider(time,{time=it;paused=true},valueRange=0f..60f)
            Text("$particles particles ? $jets jets ? $bursts bursts / ${effects.size} layers",style=MaterialTheme.typography.bodySmall)
            Text("Simulation CPU: ${simulationMicros}?s (live). Haze layers: ${effects.count {it.preset.primitive=="haze"}}",style=MaterialTheme.typography.bodySmall)
            Text("Budget: ${if(layout=="Android") 240 else 360} / 8 jets / 2 bursts. Haze uses bounded radial layers.",style=MaterialTheme.typography.bodySmall)
            resolver.validateRoom(previewRoom).forEach { Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
            Button(onClick={onCopy(json);copied=true},enabled=selected!=null) {Text(if(copied) "Copied JSON" else "Copy binding JSON")}
            OutlinedButton(onClick=onClose) {Text("Close preview")}
        }
    }
    val artwork: @Composable ()->Unit = {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF050911)),contentAlignment=Alignment.Center) {
          val aspect=when(layout) { "Android"->9f/16f;"Compact"->900f/650f;else->16f/10f }
          val previewWidth=minOf(maxWidth,maxHeight*aspect)
          BoxWithConstraints(Modifier.width(previewWidth).height(previewWidth/aspect)) {
            val density=LocalDensity.current
            val width=with(density) { maxWidth.toPx() };val height=with(density) { maxHeight.toPx() }
            val geometry=EnvironmentalGeometry.fit(width,height,painter.intrinsicSize.width,painter.intrinsicSize.height,crop=layout=="Android")
            Image(painter,null,Modifier.fillMaxSize(),contentScale=if(layout=="Android") ContentScale.Crop else ContentScale.Fit)
            if(dark) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.85f)))
            key(replay,layout) {
                if(quality!=EnvironmentalEffectsQuality.OFF) EnvironmentalEffectsOverlay(room.id,effects,geometry,particleCap=(if(layout=="Android") 240 else 360)/(if(quality==EnvironmentalEffectsQuality.REDUCED) 2 else 1),
                    steamFrames=steamFrames,previewTimeSeconds=if(paused) time else null,
                    onDiagnostics={p,j,b->particles=p;jets=j;bursts=b},initialTimeSeconds=time,onTime={time=it.coerceIn(0f,60f)},onSimulationCost={simulationMicros=it/1000})
                else SideEffect { particles=0;jets=0;bursts=0;simulationMicros=0L }
            }
            Canvas(Modifier.fillMaxSize().pointerInput(geometry,space,placement,emitters,region) {
                detectTapGestures { point->
                    val x=if(space=="scene") (point.x-geometry.imageX)/geometry.imageWidth else point.x/width
                    val y=if(space=="scene") (point.y-geometry.imageY)/geometry.imageHeight else point.y/height
                    if(x !in 0f..1f || y !in 0f..1f) return@detectTapGestures
                    if(placement=="Emitter") emitters=(emitters+EffectEmitter(x,y)).takeLast(8)
                    else if(firstCorner==null) firstCorner=Offset(x,y) else {
                        val first=firstCorner!!;val w=abs(x-first.x);val h=abs(y-first.y)
                        if(w>.001f && h>.001f) region=EffectRegion(min(x,first.x),min(y,first.y),w,h)
                        firstCorner=null
                    }
                }
            }) {
                if(selected!=null) {
                    emitters.forEach {e->val p=Offset(geometry.x(e.x,space),geometry.y(e.y,space))
                        drawCircle(Color.Cyan,7f,p,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
                        drawLine(Color.Cyan,p-Offset(10f,0f),p+Offset(10f,0f));drawLine(Color.Cyan,p-Offset(0f,10f),p+Offset(0f,10f)) }
                    val r=region;drawRect(Color.Cyan.copy(alpha=.45f),Offset(geometry.x(r.x,space),geometry.y(r.y,space)),
                        androidx.compose.ui.geometry.Size(geometry.spaceWidth(space)*r.width,geometry.spaceHeight(space)*r.height),style=androidx.compose.ui.graphics.drawscope.Stroke(1f))
                }
            }
          }
        }
    }
    Surface(Modifier.fillMaxSize(),color=Color(0xFF08111B)) {
        BoxWithConstraints {
            if(maxWidth>=850.dp) Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) {artwork()};Box(Modifier.width(380.dp).fillMaxHeight()) {controls()}
            } else Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(.52f)) {artwork()};Box(Modifier.fillMaxWidth().weight(.48f)) {controls()}
            }
        }
    }
}
