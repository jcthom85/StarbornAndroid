package com.example.starborn.desktop.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import com.example.starborn.desktop.DesktopAppServices

/** Original game fonts shared by desktop menus and exploration. */
@Composable
internal fun desktopStarbornTypography(services: DesktopAppServices): Typography {
    val fonts = remember(services.assetProvider) {
        fun family(file: String, weight: FontWeight) = services.assetProvider.open("font/$file.ttf")?.use {
            FontFamily(Font("starborn-$file", it.readBytes(), weight = weight))
        } ?: FontFamily.Default
        Triple(family("oxanium_bold", FontWeight.Bold), family("russo_one_regular", FontWeight.Bold),
            services.assetProvider.open("font/source_sans3_regular.ttf")?.use { regular ->
                services.assetProvider.open("font/source_sans3_medium.ttf")?.use { medium ->
                    FontFamily(Font("starborn-source-regular", regular.readBytes(), weight = FontWeight.Normal),
                        Font("starborn-source-medium", medium.readBytes(), weight = FontWeight.Medium))
                }
            } ?: FontFamily.Default)
    }
    val base = MaterialTheme.typography
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = fonts.second), displayMedium = base.displayMedium.copy(fontFamily = fonts.second),
        headlineLarge = base.headlineLarge.copy(fontFamily = fonts.first, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = fonts.first, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = fonts.first, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = fonts.second, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = fonts.first, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = fonts.first, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = fonts.third), bodyMedium = base.bodyMedium.copy(fontFamily = fonts.third),
        bodySmall = base.bodySmall.copy(fontFamily = fonts.third), labelLarge = base.labelLarge.copy(fontFamily = fonts.third),
        labelMedium = base.labelMedium.copy(fontFamily = fonts.third), labelSmall = base.labelSmall.copy(fontFamily = fonts.third))
}
