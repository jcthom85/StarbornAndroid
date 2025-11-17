package com.example.starborn.domain.theme

import com.example.starborn.data.local.Theme
import com.example.starborn.data.local.ThemeStyle
import com.example.starborn.data.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EnvironmentThemeState(
    val environmentId: String? = null,
    val theme: Theme? = null,
    val style: ThemeStyle? = null,
    val weatherId: String? = null
)

class EnvironmentThemeManager(
    private val themeRepository: ThemeRepository
) {
    private val _state = MutableStateFlow(EnvironmentThemeState())
    val state: StateFlow<EnvironmentThemeState> = _state.asStateFlow()

    fun apply(environmentId: String?, weatherId: String? = null) {
        val theme = themeRepository.getTheme(environmentId)
        val style = themeRepository.getStyle(environmentId)
        _state.value = EnvironmentThemeState(
            environmentId = environmentId,
            theme = theme,
            style = style,
            weatherId = weatherId
        )
    }

    fun reset() {
        _state.value = EnvironmentThemeState()
    }
}
