package com.example.starborn.domain.environment

/** Emergency compatibility defaults. Campaign authoring lives in environmental_effects.json. */
object LegacyEnvironmentalCatalog {
    val catalog=EnvironmentalEffectCatalog(presets=mapOf(
        "legacy.dust" to EnvironmentalPreset(
            primitive="motes", style="dust", category="atmosphere",
            intensity=0.25f, space="viewport", color=listOf(1f,0.93f,0.75f,0.45f),
            size=listOf(0.0007f,0.0018f), speed=listOf(0.003f,0.01f), rate=8f,
            lifetime=listOf(8f,14f), angle=-70f
        ),
        "legacy.rain" to EnvironmentalPreset(
            primitive="precipitation", style="rain", category="weather",
            intensity=0.5f, space="viewport", color=listOf(0.8f,0.9f,1f,0.32f),
            size=listOf(0.008f,0.017f), speed=listOf(0.7f,1.05f), rate=90f,
            lifetime=listOf(1.2f,1.8f), angle=100f
        ),
        "legacy.storm" to EnvironmentalPreset(
            primitive="precipitation", style="storm", category="weather",
            intensity=0.5f, space="viewport", color=listOf(0.8f,0.9f,1f,0.32f),
            size=listOf(0.008f,0.017f), speed=listOf(0.7f,1.05f), rate=90f,
            lifetime=listOf(1.2f,1.8f), angle=100f, burstInterval=listOf(9f,20f),
            burstDuration=0.55f
        ),
        "legacy.snow" to EnvironmentalPreset(
            primitive="precipitation", style="snow", category="weather",
            intensity=0.5f, space="viewport", color=listOf(1f,1f,1f,0.65f),
            size=listOf(0.0012f,0.0035f), speed=listOf(0.025f,0.065f), rate=12f,
            lifetime=listOf(9f,16f), angle=100f
        ),
        "legacy.cave_drip" to EnvironmentalPreset(
            primitive="precipitation", style="drip", category="atmosphere",
            intensity=0.35f, space="viewport", color=listOf(0.6f,0.8f,1f,0.5f),
            size=listOf(0.004f,0.007f), speed=listOf(0.35f,0.6f), rate=6f,
            lifetime=listOf(1f,2f), angle=90f
        ),
        "legacy.starfall" to EnvironmentalPreset(
            primitive="motes", style="starfall", category="field",
            intensity=0.35f, space="viewport", color=listOf(0.9f,0.95f,1f,0.6f),
            size=listOf(0.001f,0.0022f), speed=listOf(0.015f,0.04f), rate=4f,
            lifetime=listOf(8f,14f), angle=-60f
        ),
        "legacy.steam" to EnvironmentalPreset(
            primitive="jet", style="steam", category="atmosphere",
            intensity=0.5f, space="scene", color=listOf(0.92f,0.92f,0.95f,0.5f),
            size=listOf(0.115f,0.155f), speed=listOf(0.025f,0.04f), rate=0f,
            lifetime=listOf(2f,4f), angle=-70f, emitters=listOf(EffectEmitter(x=0.385f,y=0.69f,angleDegrees=-76f),EffectEmitter(x=0.615f,y=0.69f,angleDegrees=-104f),EffectEmitter(x=0.36f,y=0.49f,angleDegrees=-78f),EffectEmitter(x=0.64f,y=0.49f,angleDegrees=-102f),EffectEmitter(x=0.5f,y=0.73f,angleDegrees=-90f)),
            frameAssetPrefix="images/vfx/steam_jet/steam_frame_", frameCount=48
        ),
        "legacy.fog" to EnvironmentalPreset(
            primitive="haze", style="fog", category="atmosphere",
            intensity=0.5f, space="viewport", color=listOf(0.85f,0.88f,0.92f,0.3f),
            size=listOf(0.1f,0.22f), speed=listOf(0.003f,0.01f), rate=1f,
            lifetime=listOf(14f,22f), angle=0f
        ),
        "legacy.gas" to EnvironmentalPreset(
            primitive="haze", style="gas", category="atmosphere",
            intensity=0.5f, space="viewport", color=listOf(0.42f,0.72f,0.28f,0.25f),
            size=listOf(0.08f,0.18f), speed=listOf(0.006f,0.018f), rate=1f,
            lifetime=listOf(8f,15f), angle=-60f
        ),
        "legacy.resonance" to EnvironmentalPreset(
            primitive="field", style="resonance", category="field",
            intensity=0.25f, space="viewport", color=listOf(0.48f,0.7f,1f,0.6f),
            size=listOf(0.001f,0.0023f), speed=listOf(0.002f,0.007f), rate=6f,
            lifetime=listOf(12f,20f), angle=-85f
        ),
        "legacy.sparks" to EnvironmentalPreset(
            primitive="sparks", style="sparks", category="machinery",
            intensity=0.5f, space="viewport", color=listOf(1f,0.65f,0.15f,0.8f),
            size=listOf(0.0009f,0.0018f), speed=listOf(0.035f,0.13f), rate=35f,
            lifetime=listOf(0.35f,0.9f), angle=80f, emitters=listOf(EffectEmitter(x=0.4f,y=0.5f,angleDegrees=55f),EffectEmitter(x=0.6f,y=0.65f,angleDegrees=125f)),
            burstInterval=listOf(3f,8f), burstDuration=0.8f
        ),
        "legacy.industrial" to EnvironmentalPreset(
            primitive="motes", style="dust", category="atmosphere",
            intensity=0.1f, space="scene", color=listOf(0.88f,0.82f,0.65f,0.3f),
            size=listOf(0.0007f,0.0018f), speed=listOf(0.003f,0.01f), rate=4f,
            lifetime=listOf(8f,14f), angle=-70f
        )
    ))
}
