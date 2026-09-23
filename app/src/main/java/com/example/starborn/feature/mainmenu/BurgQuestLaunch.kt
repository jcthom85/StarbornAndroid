package com.example.starborn.feature.mainmenu

/** Visit-local intent; never persisted into campaign saves or scenario identifiers. */
data class BurgQuestLaunch(val scenario: DebugScenario, val sampler: Boolean = false) {
    val isHomecoming: Boolean get() = sampler && scenario.id == "burgfest_astra"

    companion object {
        fun sampler() = BurgQuestLaunch(scenario("burgfest_combat"), sampler = true)
        fun homecoming() = BurgQuestLaunch(scenario("burgfest_astra"), sampler = true)
        private fun scenario(id: String) = DebugScenarioCatalog.burgfestScenarios.single { it.id == id }
    }
}
