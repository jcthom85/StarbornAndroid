package com.example.starborn.domain.environment

import com.example.starborn.data.assets.*
import com.example.starborn.data.local.*
import com.example.starborn.core.platform.AssetProvider
import com.example.starborn.domain.model.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EnvironmentalEffectsTest {
    private val root=File("..").canonicalFile
    private val provider=object:AssetProvider {
        override fun open(path:String)=listOf("app/src/main/assets","world_assets/src/main/assets").map {File(root,"$it/$path")}.firstOrNull {it.isFile}?.inputStream()
    }
    private val data=WorldAssetDataSource(AssetJsonReader(provider,Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
    private val catalog get()=data.environmentalEffectsCatalog
    private val resolver get()=data.environmentalEffectResolver
    private val settings=UserSettings()
    private val room get()=data.loadRooms().first {it.id=="pit_nova_bunk"}
    private fun fixture(vararg bindings:RoomEffectBinding)=room.copy(environmentalEffects=bindings.toList())
    private fun effects(r:Room)=resolver.resolve(r,emptyMap(),emptySet(),settings)
    private val geometry=EnvironmentalGeometry.fit(1440f,900f,9f,16f)
    @Test fun allCampaignDefinitionsAndOriginalSteamAssetsValidate() {
        assertTrue(catalog.validate().joinToString(),catalog.validate().isEmpty())
        val rooms=data.loadRooms();assertEquals(477,rooms.size)
        val errors=rooms.flatMap(resolver::validateRoom);assertTrue(errors.joinToString(),errors.isEmpty())
        catalog.presets.values.filter {it.primitive=="jet"}.forEach {p->repeat(p.frameCount) {i->assertTrue(provider.exists("${p.frameAssetPrefix}%02d.png".format(i)))}}
    }
    @Test fun allLegacyFamiliesResolveInExplorationAndCombat() {
        listOf("rain","storm","snow","dust","fog","gas","steam","sparks","cave_drip","starfall","resonance","industrial").forEach {id->
            val r=room.copy(weather=id,environmentalEffects=null)
            assertEquals(id,1,effects(r).size)
            assertEquals(id,1,resolver.resolve(r,emptyMap(),emptySet(),settings,combat=true).size)
        }
    }
    @Test fun explicitEmptyOverridesWeatherAndNullUsesLegacy() {
        assertTrue(effects(room.copy(weather="rain",environmentalEffects=emptyList())).isEmpty())
        assertEquals("legacy.rain",effects(room.copy(weather="rain",environmentalEffects=null)).single().presetId)
    }
    @Test fun invalidBindingSkipsWithoutFallingBackToRain() {
        val r=fixture(RoomEffectBinding("bad","missing")).copy(weather="rain")
        assertTrue(effects(r).isEmpty());assertFalse(resolver.validateRoom(r).isEmpty())
    }
    @Test fun requiredStateFalseDoesNotMatchMissingKey() {
        val r=fixture(RoomEffectBinding("dust","interior.dust_motes",requiresState=mapOf("lamp" to false)))
        assertTrue(effects(r).isEmpty())
        assertEquals(1,resolver.resolve(r,mapOf("lamp" to false),emptySet(),settings).size)
    }
    @Test fun stateAndMilestonePredicatesMatchRoomVariants() {
        val r=fixture(RoomEffectBinding("dust","interior.dust_motes",requiresState=mapOf("lamp" to true),forbiddenState=mapOf("sealed" to true),requiresMilestones=listOf("ready"),forbiddenMilestones=listOf("gone")))
        assertTrue(resolver.resolve(r,mapOf("lamp" to true),emptySet(),settings).isEmpty())
        assertEquals(1,resolver.resolve(r,mapOf("lamp" to true),setOf("ready"),settings).size)
        assertTrue(resolver.resolve(r,mapOf("lamp" to true,"sealed" to true),setOf("ready"),settings).isEmpty())
        assertTrue(resolver.resolve(r,mapOf("lamp" to true),setOf("ready","gone"),settings).isEmpty())
    }
    @Test fun novaLightConditionAndSaveStateRemainAuthoritative() {
        assertTrue(effects(room).isEmpty())
        assertEquals(1,resolver.resolve(room,mapOf("light_on" to true),emptySet(),settings).size)
    }
    @Test fun qualityAndAccessibilityApplyTogether() {
        val r=fixture(RoomEffectBinding("field","field.resonance"),RoomEffectBinding("gas","atmosphere.gas"))
        assertTrue(resolver.resolve(r,emptyMap(),emptySet(),settings.copy(environmentalEffectsQuality=EnvironmentalEffectsQuality.OFF)).isEmpty())
        val reduced=resolver.resolve(r,emptyMap(),emptySet(),settings.copy(environmentalEffectsQuality=EnvironmentalEffectsQuality.REDUCED,highContrastMode=true))
        assertTrue(reduced.all {it.speedScale==.35f && !it.flashes && !it.fieldMotion})
        assertEquals(.5f,reduced.first {it.presetId=="atmosphere.gas"}.alpha,.0001f)
        val noMotion=resolver.resolve(r,emptyMap(),emptySet(),settings.copy(disableScreenshake=true,disableFlashes=true))
        assertTrue(noMotion.all {!it.flashes && !it.fieldMotion})
    }
    @Test fun darkVisibilityAndCombatContributionAreBounded() {
        val r=fixture(RoomEffectBinding("dim","interior.dust_motes"),RoomEffectBinding("glow","field.resonance",darkVisibility="emissive"),RoomEffectBinding("hidden","atmosphere.fog",darkVisibility="hidden"))
        val resolved=resolver.resolve(r,emptyMap(),emptySet(),settings,dark=true,combat=true)
        assertEquals(2,resolved.size);assertEquals(.15f*.65f,resolved.first {it.id=="dim"}.alpha,.0001f)
        assertEquals(.35f*.65f,resolved.first {it.id=="glow"}.alpha,.0001f)
    }
    @Test fun combatOptOutAndZeroIntensityDoNoWork() {
        assertTrue(effects(fixture(RoomEffectBinding("zero","atmosphere.fog",intensity=0f))).isEmpty())
        assertTrue(resolver.resolve(fixture(RoomEffectBinding("dust","interior.dust_motes",inCombat=false)),emptyMap(),emptySet(),settings,combat=true).isEmpty())
    }
    @Test fun sourceGeometryHandlesFitCropAndViewportIndependently() {
        assertEquals(506.25f,geometry.imageWidth,.001f)
        assertEquals(720f,geometry.x(.5f,"scene"),.001f)
        assertEquals(0f,geometry.x(0f,"viewport"),.001f)
        val crop=EnvironmentalGeometry.fit(1000f,1000f,9f,16f,crop=true)
        assertTrue(crop.imageY<0f);assertEquals(500f,crop.y(.5f,"scene"),.001f)
        assertTrue(EnvironmentalGeometry.fit(900f,650f,Float.NaN,0f).imageWidth.isFinite())
    }
    @Test fun fixedSeedAndTimeAreDeterministic() {
        val e=effects(fixture(RoomEffectBinding("field","field.resonance")))
        val a=EnvironmentalEffectSimulation();val b=EnvironmentalEffectSimulation();a.update(e);b.update(e)
        repeat(480){a.advance(.025f,geometry);b.advance(.025f,geometry)}
        assertEquals(a.activeParticles,b.activeParticles)
        a.layers.single().particles.zip(b.layers.single().particles).forEach {(x,y)->assertEquals(x.x,y.x,0f);assertEquals(x.y,y.y,0f)}
    }
    @Test fun resizeAndStateChangesKeepPhasesAndParticleSlots() {
        val e=effects(fixture(RoomEffectBinding("field","field.resonance")))
        val sim=EnvironmentalEffectSimulation();sim.update(e);sim.advance(.04f,geometry)
        val layer=sim.layers.single();val slots=layer.particles;val time=layer.time
        sim.update(e.map {it.copy(intensity=.4f)});sim.advance(.04f,EnvironmentalGeometry.fit(2560f,1080f,9f,16f))
        assertSame(layer,sim.layers.single());assertSame(slots,layer.particles);assertTrue(layer.time>time)
    }
    @Test fun deltaClampFadeAndRetirementWork() {
        val sim=EnvironmentalEffectSimulation();sim.update(effects(fixture(RoomEffectBinding("field","field.resonance"))))
        sim.advance(8f,geometry);assertEquals(.05f,sim.layers.single().time,0f)
        repeat(10){sim.advance(.05f,geometry)};sim.update(emptyList());repeat(8){sim.advance(.05f,geometry)}
        assertTrue(sim.layers.isEmpty())
    }
    @Test fun pooledBudgetAndBurstsNeverExceedCaps() {
        val e=effects(fixture(*Array(4) {RoomEffectBinding("burst$it","machinery.sparks",intensity=1f,emitters=listOf(EffectEmitter()))}))
        val sim=EnvironmentalEffectSimulation(240);sim.update(e)
        repeat(12000){sim.advance(.05f,geometry);assertTrue(sim.activeParticles<=240);assertTrue(sim.activeBursts<=2)}
        sim.budget=120;sim.advance(.05f,geometry);assertTrue(sim.activeParticles<=120)
        sim.budget=999;sim.advance(.05f,geometry);assertTrue(sim.activeParticles<=240)
    }
    @Test fun overlappingScenesShareParticleJetAndBurstBudgets() {
        val e=effects(fixture(RoomEffectBinding("steam","legacy.steam",emitters=List(8){EffectEmitter()}),RoomEffectBinding("sparks","machinery.sparks",intensity=1f,emitters=listOf(EffectEmitter()))))
        val outgoing=EnvironmentalEffectSimulation(360).apply {budget=180;jetLimit=4;burstLimit=1;update(e)}
        val incoming=EnvironmentalEffectSimulation(360).apply {budget=180;jetLimit=4;burstLimit=1;update(e)}
        repeat(4000) {
            outgoing.advance(.025f,geometry);incoming.advance(.025f,geometry)
            assertTrue(outgoing.activeParticles+incoming.activeParticles<=360)
            assertTrue(outgoing.activeJets+incoming.activeJets<=8)
            assertTrue(outgoing.activeBursts+incoming.activeBursts<=2)
        }
    }
    @Test fun rapidProfileChangesAndNavigationStayBounded() {
        val sim=EnvironmentalEffectSimulation();val e=effects(fixture(RoomEffectBinding("field","field.resonance"))).single()
        repeat(1000){sim.update(listOf(e.copy(presetId="variant$it")));assertTrue(sim.layers.size<=8);sim.advance(.005f,geometry)}
        val memory=EnvironmentalSceneMemory();repeat(100){memory.scene("room$it",120);assertTrue(memory.sceneCount<=2)}
    }
    @Test fun emitterRegionAndDuplicateValidationRejectBadData() {
        val r=fixture(RoomEffectBinding("same","machinery.vent_mist",emitters=listOf(EffectEmitter(x=2f))),RoomEffectBinding("same","field.resonance",region=EffectRegion(width=2f)))
        assertTrue(resolver.validateRoom(r).size>=3)
    }
    @Test fun missingCatalogStillSupportsLegacyWeather() {
        val source=object:AssetProvider {override fun open(path:String)=null}
        val fallback=WorldAssetDataSource(AssetJsonReader(source,Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        assertEquals(12,fallback.environmentalEffectsCatalog.presets.size)
        assertEquals(1,fallback.environmentalEffectResolver.resolve(room.copy(environmentalEffects=null,weather="steam"),emptyMap(),emptySet(),settings).size)
    }
    @Test fun compatibilityDefaultsMatchTheAuthoredLegacyCatalog() {
        assertEquals(catalog.presets.filterKeys {it.startsWith("legacy.")},LegacyEnvironmentalCatalog.catalog.presets)
    }
    @Test fun preferenceValueDefaultsAndRoundTrips() {
        EnvironmentalEffectsQuality.entries.forEach {assertEquals(it,EnvironmentalEffectsQuality.fromId(it.name))}
        assertEquals(EnvironmentalEffectsQuality.FULL,EnvironmentalEffectsQuality.fromId("future"))
    }
}
