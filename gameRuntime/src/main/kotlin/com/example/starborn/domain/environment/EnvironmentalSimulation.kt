package com.example.starborn.domain.environment

import kotlin.math.*
import kotlin.random.Random

/** Slots are pooled: advancing a frame allocates no particle list or per-particle objects. */
class EnvironmentalParticle {
    var x=0f; var y=0f; var vx=0f; var vy=0f; var size=0f
    var life=0f; var lifetime=1f; var phase=0f; var alpha=0f
}
class EnvironmentalLayerFrame(var effect: ResolvedEnvironmentalEffect, capacity: Int) {
    val particles=Array(capacity) { EnvironmentalParticle() }
    var count=0; var opacity=0f; var time=0f; var burst=0f; var nextBurst=0f
    val random=Random(effect.seed)
    var wanted=true; var initialized=false
    fun between(range:List<Float>)=range[0]+random.nextFloat()*(range[1]-range[0])
}

/** At most two scenes survive navigation; revisiting does not accumulate simulations. */
class EnvironmentalSceneMemory {
    private val scenes=linkedMapOf<String,EnvironmentalEffectSimulation>()
    fun scene(roomId:String,cap:Int):EnvironmentalEffectSimulation {
        val scene=scenes.remove(roomId) ?: EnvironmentalEffectSimulation(360)
        scenes[roomId]=scene
        while(scenes.size>2) scenes.remove(scenes.keys.first())
        scene.budget=cap.coerceIn(0,360)
        return scene
    }
    val sceneCount get()=scenes.size
}
class EnvironmentalEffectSimulation(private val particleCap: Int = 240) {
    var budget=particleCap
        set(value) { field=value.coerceIn(0,particleCap) }
    val layers=mutableListOf<EnvironmentalLayerFrame>()
    var elapsedSeconds=0f; private set
    var jetLimit=8
    var burstLimit=2
    var activeParticles=0; private set
    var activeJets=0; private set
    var activeBursts=0; private set
    fun update(effects: List<ResolvedEnvironmentalEffect>) {
        layers.forEach { it.wanted=false }
        effects.forEach { effect ->
            val existing=layers.firstOrNull { it.effect.id==effect.id && it.effect.presetId==effect.presetId }
            if(existing!=null) { existing.effect=effect;existing.wanted=true }
            else {
                if(layers.size>=8) layers.firstOrNull { !it.wanted }?.let(layers::remove)
                layers.add(EnvironmentalLayerFrame(effect,particleCap.coerceAtLeast(0)))
            }
        }
    }
    fun advance(deltaSeconds: Float, geometry: EnvironmentalGeometry) {
        val dt=if(deltaSeconds.isFinite()) deltaSeconds.coerceIn(0f,.05f) else 0f
        elapsedSeconds+=dt
        activeParticles=0;activeJets=0;activeBursts=0
        var reservedBursts=0
        layers.forEach { layer -> if(layer.burst>0f) {
            if(reservedBursts<burstLimit) reservedBursts++ else layer.burst=0f
        } }
        val iterator=layers.iterator()
        while(iterator.hasNext()) {
            val f=iterator.next();val e=f.effect;val p=e.preset
            f.opacity=(f.opacity+if(f.wanted) dt/.35f else -dt/.35f).coerceIn(0f,1f)
            if(!f.wanted && f.opacity==0f) { iterator.remove();continue }
            f.time+=dt*e.speedScale
            if(p.burstInterval!=null) {
                if(!f.initialized) f.nextBurst=f.between(p.burstInterval)
                if(f.time>=f.nextBurst && f.burst<=0f && reservedBursts<burstLimit) {
                    f.burst=p.burstDuration;reservedBursts++;f.nextBurst=f.time+f.between(p.burstInterval)
                }
                if(f.burst>0f) { activeBursts++;f.burst=(f.burst-dt*e.speedScale).coerceAtLeast(0f) }
            }
            val expansion=(geometry.width*geometry.height/(geometry.imageWidth*geometry.imageHeight).coerceAtLeast(1f)).coerceIn(1f,1.5f)
            val averageLife=(p.lifetime[0]+p.lifetime[1])*.5f
            val desired=when(p.primitive) {
                "light","jet"->0
                "haze" -> (3f+e.intensity*5).toInt()
                else->(p.rate*averageLife*e.intensity*expansion).roundToInt()
            }.coerceAtLeast(0)
            val wantedCount=if(p.burstInterval!=null && f.burst<=0f && p.primitive=="sparks") 0 else minOf(desired,(budget-activeParticles).coerceAtLeast(0))
            if(p.primitive=="jet") activeJets+=minOf(e.emitters.size,(jetLimit-activeJets).coerceAtLeast(0))
            for(i in 0 until wantedCount) {
                val particle=f.particles[i]
                if(i>=f.count || particle.life<=0f) spawn(f,particle,!f.initialized)
                particle.life-=dt*e.speedScale
                val sw=geometry.spaceWidth(e.space).coerceAtLeast(1f);val sh=geometry.spaceHeight(e.space).coerceAtLeast(1f)
                particle.x+=particle.vx*dt*e.speedScale*geometry.imageHeight/sw
                particle.y+=particle.vy*dt*e.speedScale*geometry.imageHeight/sh
                if(p.primitive=="motes" || p.primitive=="field" || p.primitive=="haze") {
                    particle.x+=sin(f.time*.45f+particle.phase)*dt*.002f*e.speedScale
                }
                if(particle.x<e.region.x-.1f || particle.x>e.region.x+e.region.width+.1f || particle.y<e.region.y-.1f || particle.y>e.region.y+e.region.height+.1f) spawn(f,particle,false)
                val progress=(1f-particle.life/particle.lifetime).coerceIn(0f,1f)
                particle.alpha=minOf(progress*5f,(1f-progress)*4f,1f).coerceAtLeast(0f)
            }
            f.count=wantedCount;activeParticles+=wantedCount;f.initialized=true
        }
    }
    private fun spawn(f:EnvironmentalLayerFrame,particle:EnvironmentalParticle,warm:Boolean) {
        val e=f.effect;val p=e.preset;val random=f.random
        particle.lifetime=f.between(p.lifetime);particle.life=particle.lifetime*(if(warm) .1f+random.nextFloat()*.85f else 1f)
        particle.size=f.between(p.size);particle.phase=random.nextFloat()*6.283185f
        val source=e.emitters.takeIf { it.isNotEmpty() }?.let { it[random.nextInt(it.size)] }
        particle.x=source?.x ?: (e.region.x+random.nextFloat()*e.region.width)
        particle.y=source?.y ?: (e.region.y+random.nextFloat()*e.region.height)
        val angle=(source?.angleDegrees ?: p.angle)+(random.nextFloat()-.5f)*if(p.primitive=="sparks") 65f else 20f
        val speed=f.between(p.speed)
        particle.vx=cos(angle*PI.toFloat()/180)*speed;particle.vy=sin(angle*PI.toFloat()/180)*speed
        if(p.primitive=="precipitation" && !warm) particle.y=e.region.y-.02f
        if(source!=null && warm) { particle.x+=particle.vx*(particle.lifetime-particle.life);particle.y+=particle.vy*(particle.lifetime-particle.life) }
    }
}
