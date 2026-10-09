package com.example.starborn.domain.environment

import com.example.starborn.data.local.UserSettings
import com.example.starborn.data.local.EnvironmentalEffectsQuality
import com.example.starborn.domain.model.Room
import com.squareup.moshi.Json

/** Authoring units are normalized image coordinates, reference heights/sec, and seconds. */
data class EffectRegion(val x: Float = 0f, val y: Float = 0f, val width: Float = 1f, val height: Float = 1f)
data class EffectEmitter(val x: Float = .5f, val y: Float = .75f,
    @Json(name = "angle_degrees") val angleDegrees: Float = -90f, val radius: Float = .03f)
data class RoomEffectBinding(val id: String = "", val preset: String = "", val intensity: Float? = null,
    val space: String? = null, val region: EffectRegion? = null, val emitters: List<EffectEmitter>? = null,
    @Json(name = "requires_state") val requiresState: Map<String, Boolean> = emptyMap(),
    @Json(name = "forbidden_state") val forbiddenState: Map<String, Boolean> = emptyMap(),
    @Json(name = "requires_milestones") val requiresMilestones: List<String> = emptyList(),
    @Json(name = "forbidden_milestones") val forbiddenMilestones: List<String> = emptyList(),
    @Json(name = "in_combat") val inCombat: Boolean = true,
    @Json(name = "dark_visibility") val darkVisibility: String = "dim")
data class EnvironmentalPreset(val primitive: String = "", val style: String = "", val category: String = "atmosphere",
    val intensity: Float = .25f, val space: String = "viewport", val color: List<Float> = listOf(.8f,.9f,1f,.5f),
    val size: List<Float> = listOf(.001f,.003f), val speed: List<Float> = listOf(.01f,.04f),
    val rate: Float = 10f, val lifetime: List<Float> = listOf(4f,8f), val angle: Float = -90f,
    @Json(name = "burst_interval_seconds") val burstInterval: List<Float>? = null,
    @Json(name = "burst_duration_seconds") val burstDuration: Float = .8f,
    val emitters: List<EffectEmitter> = emptyList(), val priority: String = "ambient",
    @Json(name = "frame_asset_prefix") val frameAssetPrefix: String? = null,
    @Json(name = "frame_count") val frameCount: Int = 0)
data class EnvironmentalEffectCatalog(@Json(name = "schema_version") val schemaVersion: Int = 1,
    val presets: Map<String, EnvironmentalPreset> = emptyMap()) {
    fun validate(): List<String> = buildList {
        if (schemaVersion != 1) add("Unsupported environmental catalog version $schemaVersion")
        presets.forEach { (id, preset) -> presetErrors(preset).forEach { add("$id: $it") } }
    }
    companion object {
        val primitives = setOf("precipitation", "motes", "haze", "jet", "sparks", "light", "field")
        private val styles=mapOf("precipitation" to setOf("drip","rain","snow","storm"),"motes" to setOf("ash","dust","embers","gravity","spores","starfall"),
            "haze" to setOf("fog","gas","smoke"),"sparks" to setOf("arc","sparks"),"jet" to setOf("steam"),
            "light" to setOf("flicker","pulse","scanner","shaft"),"field" to setOf("resonance"))
        private fun rangeValid(v: List<Float>, positive: Boolean = false) = v.size == 2 && v.all { it.isFinite() } && v[0] >= 0f && v[1] >= v[0] && (!positive || v[0] > 0f)
        fun presetErrors(p: EnvironmentalPreset): List<String> = buildList {
            if(p.primitive !in primitives) add("unknown primitive ${p.primitive}")
            if(p.style !in styles[p.primitive].orEmpty()) add("unknown style ${p.style}")
            if(p.space !in setOf("scene","viewport")) add("invalid space")
            if(!p.intensity.isFinite() || p.intensity !in 0f..1f) add("invalid intensity")
            if(p.color.size != 4 || p.color.any { !it.isFinite() || it !in 0f..1f }) add("invalid RGBA")
            if(!rangeValid(p.size,true) || !rangeValid(p.speed) || !rangeValid(p.lifetime,true)) add("invalid size/speed/lifetime range")
            if(!p.rate.isFinite() || p.rate !in 0f..200f || !p.angle.isFinite()) add("invalid emission rate/direction")
            if(p.burstInterval != null && !rangeValid(p.burstInterval,true)) add("invalid burst interval")
            if(!p.burstDuration.isFinite() || p.burstDuration <= 0f) add("invalid burst duration")
            if(p.emitters.any { !emitterValid(it) }) add("invalid emitter")
            if(p.frameCount !in 0..64 || (p.primitive=="jet" && (p.frameAssetPrefix==null || p.frameCount<=0))) add("invalid frame assets")
            if(p.priority !in setOf("ambient","accent")) add("invalid priority")
        }
        fun emitterValid(e: EffectEmitter) = e.x.isFinite() && e.y.isFinite() && e.x in 0f..1f && e.y in 0f..1f && e.angleDegrees.isFinite() && e.radius.isFinite() && e.radius in 0f..1f
        fun regionValid(r: EffectRegion) = listOf(r.x,r.y,r.width,r.height).all { it.isFinite() } && r.x>=0f && r.y>=0f && r.width>0f && r.height>0f && r.x+r.width<=1.001f && r.y+r.height<=1.001f
    }
}
data class ResolvedEnvironmentalEffect(val id: String, val presetId: String, val preset: EnvironmentalPreset,
    val intensity: Float, val space: String, val region: EffectRegion, val emitters: List<EffectEmitter>,
    val alpha: Float, val speedScale: Float, val seed: Int, val flashes: Boolean, val fieldMotion: Boolean)

class EnvironmentalEffectResolver(private val catalog: EnvironmentalEffectCatalog, private val report: (String)->Unit = {}) {
    private val reported = mutableSetOf<String>()
    private fun warn(message: String) { if(reported.add(message)) report(message) }
    fun validateRoom(room: Room): List<String> = buildList {
        if(room.environmentalEffects == null) {
            room.weather?.let { if("legacy.${it.lowercase(java.util.Locale.ROOT).trim()}" !in catalog.presets) add("${room.id}: unknown legacy effect $it") }
        } else {
            if(room.environmentalEffects.size>4) add("${room.id}: maximum four bindings")
            room.environmentalEffects.groupBy { it.id }.filterValues { it.size>1 }.keys.forEach { add("${room.id}: duplicate binding $it") }
            room.environmentalEffects.forEach { b -> bindingErrors(b).forEach { add("${room.id}/${b.id}: $it") } }
        }
    }
    private fun bindingErrors(b: RoomEffectBinding): List<String> = buildList {
        if(b.id.isBlank()) add("missing binding ID")
        val p=catalog.presets[b.preset]
        if(p==null) add("unknown preset ${b.preset}") else addAll(EnvironmentalEffectCatalog.presetErrors(p))
        if(b.intensity != null && (!b.intensity.isFinite() || b.intensity !in 0f..1f)) add("invalid intensity")
        if(b.space != null && b.space !in setOf("scene","viewport")) add("invalid space")
        if(b.region != null && !EnvironmentalEffectCatalog.regionValid(b.region)) add("invalid region")
        if(b.emitters?.any { !EnvironmentalEffectCatalog.emitterValid(it) } == true) add("invalid emitters")
        if(p?.primitive in setOf("jet","sparks") && (b.emitters ?: p?.emitters).isNullOrEmpty()) add("source-bound effect needs emitters")
        if(b.darkVisibility !in setOf("dim","emissive","hidden")) add("invalid dark visibility")
    }
    fun resolve(room: Room?, roomState: Map<String,Boolean>, milestones: Set<String>, settings: UserSettings,
        dark: Boolean = false, combat: Boolean = false, suppressAccents: Boolean = false): List<ResolvedEnvironmentalEffect> {
        if(room==null || settings.environmentalEffectsQuality==EnvironmentalEffectsQuality.OFF) return emptyList()
        if(catalog.schemaVersion!=1) { warn("Unsupported environmental catalog version ${catalog.schemaVersion}");return emptyList() }
        val authoredBindings=room.environmentalEffects ?: room.weather?.let { listOf(RoomEffectBinding("legacy","legacy.${it.lowercase(java.util.Locale.ROOT).trim()}")) }.orEmpty()
        val bindings=authoredBindings.sortedBy { if(catalog.presets[it.preset]?.priority=="accent") 1 else 0 }
        if(bindings.size>4) warn("${room.id}: maximum four environmental bindings")
        val seen=mutableSetOf<String>()
        return bindings.take(4).mapNotNull { b ->
            val errors=bindingErrors(b)
            if(!seen.add(b.id) || errors.isNotEmpty()) { warn("${room.id}/${b.id}: ${errors.joinToString().ifEmpty { "duplicate binding" }}");return@mapNotNull null }
            if(combat && !b.inCombat || !b.requiresState.all { (k,v)->roomState[k]==v } || b.forbiddenState.any { (k,v)->roomState[k]==v } ||
                !b.requiresMilestones.all { it in milestones } || b.forbiddenMilestones.any { it in milestones }) return@mapNotNull null
            val p=catalog.presets.getValue(b.preset)
            val reduced=settings.environmentalEffectsQuality==EnvironmentalEffectsQuality.REDUCED
            val alpha=(if(dark) when(b.darkVisibility) { "hidden"->0f;"emissive"->.35f;else->.15f } else 1f)*
                (if(combat) .65f else 1f)*(if(settings.highContrastMode && p.primitive=="haze") .5f else 1f)
            val flashes=!settings.disableFlashes && !reduced && !suppressAccents
            if((b.intensity ?: p.intensity)==0f || alpha==0f || (p.style=="arc" && !flashes)) return@mapNotNull null
            ResolvedEnvironmentalEffect(b.id,b.preset,p,b.intensity ?: p.intensity,b.space ?: p.space,b.region ?: EffectRegion(),
                b.emitters ?: p.emitters,alpha,if(reduced) .35f else 1f,(room.id+":"+b.id+":"+catalog.schemaVersion).hashCode(),
                flashes,!settings.disableScreenshake && !reduced && !suppressAccents)
        }
    }
}

/** Pure float geometry; no UI dependency in the game runtime. */
data class EnvironmentalGeometry(val width: Float, val height: Float, val imageX: Float = 0f,
    val imageY: Float = 0f, val imageWidth: Float = width, val imageHeight: Float = height) {
    fun x(fraction: Float, space: String) = if(space=="scene") imageX+fraction*imageWidth else fraction*width
    fun y(fraction: Float, space: String) = if(space=="scene") imageY+fraction*imageHeight else fraction*height
    fun spaceWidth(space: String) = if(space=="scene") imageWidth else width
    fun spaceHeight(space: String) = if(space=="scene") imageHeight else height
    companion object {
        fun fit(width:Float,height:Float,artWidth:Float,artHeight:Float,crop:Boolean=false):EnvironmentalGeometry {
            val ratio=if(artWidth>0 && artHeight>0) artWidth/artHeight else 9f/16f
            val scaleHeight=if(crop) maxOf(height,width/ratio) else minOf(height,width/ratio)
            val scaledWidth=scaleHeight*ratio
            return EnvironmentalGeometry(width,height,(width-scaledWidth)/2,(height-scaleHeight)/2,scaledWidth,scaleHeight)
        }
    }
}
