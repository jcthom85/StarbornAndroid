package com.example.starborn.domain.theme

/**
 * Authored campaign rooms must opt into environmental overlays explicitly.
 *
 * A single environment contains sealed interiors, sheltered routes, and exposed
 * spaces, so inferring weather here placed particles over physically protected
 * rooms.
 */
fun defaultWeatherForEnvironment(
    @Suppress("UNUSED_PARAMETER") environmentId: String?
): String? = null
