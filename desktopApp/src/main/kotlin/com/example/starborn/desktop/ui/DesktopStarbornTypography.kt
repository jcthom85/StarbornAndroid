package com.example.starborn.desktop.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import com.example.starborn.core.platform.AssetProvider
import com.example.starborn.desktop.DesktopAppServices

data class StarbornFontBundle(
    val oxanium: FontFamily,
    val russoOne: FontFamily,
    val orbitron: FontFamily,
    val pressStart: FontFamily,
    val body: FontFamily
)

val LocalStarbornFonts = staticCompositionLocalOf {
    StarbornFontBundle(
        oxanium = FontFamily.Default,
        russoOne = FontFamily.Default,
        orbitron = FontFamily.Monospace,
        pressStart = FontFamily.Monospace,
        body = FontFamily.Default
    )
}

@Composable
fun rememberStarbornFonts(assetProvider: AssetProvider): StarbornFontBundle {
    return remember(assetProvider) {
        fun family(file: String, weight: FontWeight): FontFamily = assetProvider.open("font/$file.ttf")?.use {
            try {
                FontFamily(Font("starborn-$file", it.readBytes(), weight = weight))
            } catch (_: Throwable) {
                FontFamily.Default
            }
        } ?: FontFamily.Default

        val oxanium = family("oxanium_bold", FontWeight.Bold)
        val russo = family("russo_one_regular", FontWeight.Bold)
        val orbitron = family("orbitron_medium", FontWeight.Medium).takeIf { it != FontFamily.Default } ?: oxanium
        val pressStart = family("press_start_2p_regular", FontWeight.Normal).takeIf { it != FontFamily.Default } ?: FontFamily.Monospace
        val body = assetProvider.open("font/source_sans3_regular.ttf")?.use { regular ->
            assetProvider.open("font/source_sans3_medium.ttf")?.use { medium ->
                try {
                    FontFamily(
                        Font("starborn-source-regular", regular.readBytes(), weight = FontWeight.Normal),
                        Font("starborn-source-medium", medium.readBytes(), weight = FontWeight.Medium)
                    )
                } catch (_: Throwable) {
                    FontFamily.Default
                }
            }
        } ?: FontFamily.Default

        StarbornFontBundle(
            oxanium = oxanium,
            russoOne = russo,
            orbitron = orbitron,
            pressStart = pressStart,
            body = body
        )
    }
}

/** Original game fonts shared by desktop menus and exploration. */
@Composable
internal fun desktopStarbornTypography(services: DesktopAppServices): Typography {
    val fonts = rememberStarbornFonts(services.assetProvider)
    val base = MaterialTheme.typography
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = fonts.russoOne),
        displayMedium = base.displayMedium.copy(fontFamily = fonts.russoOne),
        headlineLarge = base.headlineLarge.copy(fontFamily = fonts.oxanium, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = fonts.oxanium, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = fonts.oxanium, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = fonts.russoOne, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = fonts.oxanium, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = fonts.oxanium, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = fonts.body),
        bodyMedium = base.bodyMedium.copy(fontFamily = fonts.body),
        bodySmall = base.bodySmall.copy(fontFamily = fonts.body),
        labelLarge = base.labelLarge.copy(fontFamily = fonts.body),
        labelMedium = base.labelMedium.copy(fontFamily = fonts.body),
        labelSmall = base.labelSmall.copy(fontFamily = fonts.body)
    )
}

