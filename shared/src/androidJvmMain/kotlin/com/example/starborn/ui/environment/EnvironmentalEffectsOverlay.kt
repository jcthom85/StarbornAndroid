package com.example.starborn.ui.environment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.starborn.domain.environment.*
import kotlin.math.*

/** The same drawing source is compiled for Android and JVM; no platform graphics APIs. */
@Composable
fun EnvironmentalEffectsOverlay(roomId: String, effects: List<ResolvedEnvironmentalEffect>,
    geometry: EnvironmentalGeometry, modifier: Modifier = Modifier, particleCap: Int = 240,
    paused: Boolean = false, steamFrames: List<ImageBitmap> = emptyList(),
    previewTimeSeconds: Float? = null, opacity: Float = 1f,
    onDiagnostics: ((Int,Int,Int)->Unit)? = null, sceneMemory:EnvironmentalSceneMemory?=null,initialTimeSeconds:Float=0f,onTime:((Float)->Unit)?=null,onSimulationCost:((Long)->Unit)?=null,maxJets:Int=8,maxBursts:Int=2) {
    val isTestEnv = remember {
        System.getProperty("org.gradle.test.worker") != null ||
        System.getProperty("org.gradle.internal.worker.tmpdir") != null
    }
    val effectivePreviewTime = previewTimeSeconds ?: if (isTestEnv) 0.1f else null
    val simulation=if(effectivePreviewTime!=null) remember(roomId,particleCap,effectivePreviewTime,effects) { EnvironmentalEffectSimulation(particleCap).apply { update(effects) } }
        else remember(roomId,sceneMemory,if(onTime!=null) effects else null) { sceneMemory?.scene(roomId,particleCap) ?: EnvironmentalEffectSimulation(360).apply { update(effects);budget=particleCap } }
    SideEffect { simulation.budget=particleCap;simulation.jetLimit=maxJets;simulation.burstLimit=maxBursts }
    val currentGeometry by rememberUpdatedState(geometry)
    val diagnostics by rememberUpdatedState(onDiagnostics)
    val timeCallback by rememberUpdatedState(onTime)
    val costCallback by rememberUpdatedState(onSimulationCost)
    var drawTick by remember { mutableLongStateOf(0L) }
    val brushes=remember(roomId) { mutableMapOf<EnvironmentalPreset,Brush>() }
    SideEffect {
        effects.forEach { e -> if(e.preset !in brushes) brushes[e.preset]=Brush.radialGradient(
            listOf(Color(e.preset.color[0],e.preset.color[1],e.preset.color[2],.5f),Color.Transparent),Offset.Zero,1f) }
    }
    val path=remember { Path() }
    LaunchedEffect(simulation,effects,paused) {
        simulation.update(effects)
        if(effectivePreviewTime!=null) {
            repeat((effectivePreviewTime.coerceIn(0f,60f)/.025f).toInt()) { simulation.advance(.025f,currentGeometry) }
            drawTick++;diagnostics?.invoke(simulation.activeParticles,simulation.activeJets,simulation.activeBursts)
        } else if(!paused) {
            if(simulation.layers.all { it.time==0f } && initialTimeSeconds>0f) {
                repeat((initialTimeSeconds.coerceIn(0f,60f)/.025f).toInt()) {simulation.advance(.025f,currentGeometry)}
            }
            var previous=withFrameNanos { it }
            while(simulation.layers.isNotEmpty()) {
                withFrameNanos { now ->
                    if(now-previous>=16_000_000L) {
                        val started=if(costCallback!=null) System.nanoTime() else 0L
                        simulation.advance((now-previous)/1_000_000_000f,currentGeometry)
                        if(started!=0L) costCallback?.invoke(System.nanoTime()-started)
                        previous=now;drawTick++
                        timeCallback?.invoke(simulation.elapsedSeconds)
                        diagnostics?.invoke(simulation.activeParticles,simulation.activeJets,simulation.activeBursts)
                    }
                }
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawTick // State is read in drawing, not in composition for each moving particle.
        var jets=0
        simulation.layers.forEach { layer ->
            val effect=layer.effect;val preset=effect.preset
            val color=Color(preset.color[0],preset.color[1],preset.color[2],preset.color[3])
            val alpha=effect.alpha*layer.opacity*opacity
            val height=geometry.imageHeight
            val glow=brushes[preset]
            fun DrawScope.veil(center:Offset,width:Float,height:Float,strength:Float) {
                if(glow!=null) withTransform({ translate(center.x,center.y);scale(width,height,Offset.Zero) }) {
                    drawCircle(glow,radius=1f,center=Offset.Zero,alpha=strength*alpha)
                }
            }
            fun DrawScope.drawLayer() {
                for(index in 0 until layer.count) {
                    val p=layer.particles[index]
                    val at=Offset(geometry.x(p.x,effect.space),geometry.y(p.y,effect.space))
                    val radius=(p.size*height).coerceAtLeast(.45f)
                    val a=(p.alpha*alpha).coerceIn(0f,1f)
                    when(preset.primitive) {
                        "precipitation" -> if(preset.style=="snow") {
                            drawCircle(color.copy(alpha=color.alpha*a),radius,at)
                        } else {
                            val trail=Offset(p.vx,p.vy)*radius*1.8f
                            drawLine(color.copy(alpha=color.alpha*a),at-trail,at,strokeWidth=(height*.0009f).coerceAtLeast(.6f),cap=StrokeCap.Round)
                            if(preset.style=="drip" && p.y>.85f) drawOval(color.copy(alpha=.15f*a),Offset(at.x-radius,at.y),androidx.compose.ui.geometry.Size(radius*2,radius*.4f),style=Stroke(.6f))
                        }
                        "haze" -> veil(at,radius*2.5f,radius,.12f*p.alpha*(.3f+effect.intensity))
                        "sparks" -> if(preset.style!="arc") {
                            drawLine(color.copy(alpha=color.alpha*a),at-Offset(p.vx,p.vy)*height*.025f,at,(height*.001f).coerceAtLeast(.6f),cap=StrokeCap.Round)
                            drawCircle(color.copy(alpha=.15f*a),radius*2,at)
                        }
                        else -> {
                            val luminous=preset.primitive=="field" || preset.style in setOf("spores","starfall","embers","gravity")
                            val moteColor=if(preset.primitive=="field" && index%3==0) Color(.72f,.56f,1f,color.alpha) else color
                            val shimmer=if(preset.primitive=="field") .7f+.3f*sin(layer.time*.6f+p.phase) else 1f
                            if(luminous) drawCircle(moteColor.copy(alpha=.065f*a*shimmer),radius*5f,at)
                            drawCircle(moteColor.copy(alpha=moteColor.alpha*a*shimmer),radius,at)
                            if(preset.style=="starfall") drawLine(color.copy(alpha=.2f*a),at,at+Offset(-radius*5,radius*8),(height*.0005f).coerceAtLeast(.4f))
                        }
                    }
                }
                when(preset.primitive) {
                    "field" -> {
                        // Suspended motes and very faint, vertical light veils; no oscilloscope waves.
                        for(index in 0..1) {
                            val phase=(effect.seed%31)*.1f+index*2.1f
                            val motion=if(effect.fieldMotion) layer.time*.07f else 0f
                            val x=effect.region.x+effect.region.width*(.28f+index*.44f+sin(motion+phase)*.045f)
                            val y=effect.region.y+effect.region.height*(.35f+index*.16f)
                            val center=Offset(geometry.x(x,effect.space),geometry.y(y,effect.space))
                            veil(center,height*.05f,height*.16f,.035f+effect.intensity*.06f)
                            if(effect.fieldMotion) {
                                val r=height*.06f
                                path.reset();path.moveTo(center.x+r*.5f,center.y-r*2)
                                path.cubicTo(center.x-r,center.y-r,center.x+r,center.y+r,center.x-r*.2f,center.y+r*2)
                                drawPath(path,color.copy(alpha=.025f*alpha*effect.intensity),style=Stroke((height*.001f).coerceAtLeast(.6f)))
                            }
                        }
                    }
                    "jet" -> {
                        effect.emitters.forEachIndexed { index,source ->
                            if(jets>=maxJets) return@forEachIndexed
                            jets++
                            val envelope=if(preset.burstInterval!=null) (layer.burst/preset.burstDuration).coerceIn(0f,1f) else .7f+.3f*sin(layer.time*.8f+index*1.7f)
                            if(envelope<=0f) return@forEachIndexed
                            val sourcePoint=Offset(geometry.x(source.x,effect.space),geometry.y(source.y,effect.space))
                            val width=height*(preset.size[0]+(preset.size[1]-preset.size[0])*.5f)
                            val frame=steamFrames.getOrNull(((layer.time*20f+index*11).toInt()).mod(steamFrames.size.coerceAtLeast(1)))
                            if(frame!=null) rotate(source.angleDegrees+90f,sourcePoint) {
                                drawImage(frame,srcOffset=IntOffset.Zero,srcSize=IntSize(frame.width,frame.height),
                                    dstOffset=IntOffset((sourcePoint.x-width/2).toInt(),(sourcePoint.y-width*1.7f).toInt()),
                                    dstSize=IntSize(width.toInt().coerceAtLeast(1),(width*1.7f).toInt().coerceAtLeast(1)),
                                    alpha=(alpha*envelope*(.2f+effect.intensity*.65f)).coerceIn(0f,1f))
                            } else veil(sourcePoint-Offset(0f,width*.7f),width*.45f,width*.8f,.15f*envelope*effect.intensity)
                        }
                    }
                    "light" -> {
                        val r=effect.region
                        val source=effect.emitters.firstOrNull()
                        val point=Offset(geometry.x(source?.x ?: r.x+r.width/2,effect.space),geometry.y(source?.y ?: r.y+r.height/2,effect.space))
                        val wave=if(!effect.flashes) .65f else if(preset.style=="flicker") {
                            // Irregular, softened power variation rather than an on/off strobe.
                            (.62f+.17f*sin(layer.time*2.1f+effect.seed%13)+.12f*sin(layer.time*3.7f)).coerceIn(.3f,.9f)
                        } else .6f+.4f*sin(layer.time*.8f+effect.seed%13)
                        if(preset.style=="scanner") {
                            val t=if(effect.fieldMotion) (layer.time*.04f)%1f else .5f
                            val x=geometry.x(r.x+r.width*t,effect.space)
                            veil(Offset(x,point.y),height*.025f,geometry.spaceHeight(effect.space)*r.height*.6f,.12f*effect.intensity)
                        } else if(preset.style=="shaft") {
                            rotate((source?.angleDegrees ?: -90f)+90f,point) {
                                veil(point-Offset(0f,height*.12f),height*.04f,height*.32f,effect.intensity*.24f)
                            }
                        } else veil(point,geometry.spaceWidth(effect.space)*r.width*.55f,geometry.spaceHeight(effect.space)*r.height*.5f,
                            effect.intensity*.18f*wave)
                    }
                    "sparks" -> if(preset.style=="arc" && effect.flashes && layer.burst>0f) {
                        effect.emitters.take(2).forEach { source ->
                            val start=Offset(geometry.x(source.x,effect.space),geometry.y(source.y,effect.space))
                            val angle=source.angleDegrees*PI.toFloat()/180
                            val step=Offset(cos(angle),sin(angle))*height*.012f
                            path.reset();path.moveTo(start.x,start.y)
                            for(i in 1..8) { val jitter=sin(i*2.4f+floor(layer.time*8)) * height*.008f
                                val at=start+step*i.toFloat()+Offset(-sin(angle)*jitter,cos(angle)*jitter);path.lineTo(at.x,at.y) }
                            drawPath(path,color.copy(alpha=.45f*alpha),style=Stroke(height*.004f))
                            drawPath(path,Color.White.copy(alpha=.75f*alpha),style=Stroke((height*.001f).coerceAtLeast(.7f)))
                        }
                    }
                }
                if(preset.style=="storm" && effect.flashes && layer.burst>0f) {
                    val flash=sin((layer.burst/preset.burstDuration).coerceIn(0f,1f)*PI.toFloat()).coerceAtLeast(0f)
                    veil(Offset(geometry.width*.65f,0f),geometry.width,geometry.height,.12f*flash*effect.intensity)
                    val boltX=geometry.width*(.35f+(effect.seed.mod(7))*.065f)
                    path.reset();path.moveTo(boltX,0f);path.lineTo(boltX-height*.025f,height*.07f)
                    path.lineTo(boltX+height*.018f,height*.09f);path.lineTo(boltX-height*.04f,height*.18f)
                    drawPath(path,color.copy(alpha=.3f*flash*alpha*effect.intensity),style=Stroke((height*.0014f).coerceAtLeast(.7f)))
                }
            }
            val region=effect.region
            if(region!=EffectRegion()) clipRect(geometry.x(region.x,effect.space),geometry.y(region.y,effect.space),
                geometry.x(region.x+region.width,effect.space),geometry.y(region.y+region.height,effect.space)) { drawLayer() }
            else drawLayer()
        }
    }
}
