package com.example.starborn.feature.exploration.viewmodel

internal object RuntimeLog { fun debug(tag: String, message: String) { java.util.logging.Logger.getLogger(tag).fine(message) } }
