package com.example.starborn.domain.session

import com.example.starborn.domain.leveling.CrewBuildPrototype
import com.example.starborn.domain.leveling.NovaBuildPrototype

/** Frozen compatibility data, not a live purchase catalog. Never change historical credit costs. */
object LegacyCrewProgression {
    val nodeCosts: Map<String, Int> = mapOf(
        "nova_light_fingers" to 1,
        "nova_quiet_steps" to 1,
        "nova_keen_sight" to 1,
        "nova_lockpicking" to 1,
        "nova_distraction" to 2,
        "nova_hydraulic_kick" to 2,
        "nova_smoke_bomb" to 3,
        "nova_pickpocket_master" to 3,
        "nova_shadow_escape" to 3,
        "nova_master_thief" to 5,
        "nova_dark_sight" to 1,
        "nova_night_cloak" to 1,
        "nova_shadow_blade" to 1,
        "nova_umbral_step" to 1,
        "nova_fear_aura" to 2,
        "nova_soul_drain" to 2,
        "nova_dark_pact" to 3,
        "nova_plasma_burst" to 3,
        "nova_eclipse" to 3,
        "nova_lord_of_shadows" to 5,
        "nova_jury_rig" to 1,
        "nova_handy_tools" to 1,
        "nova_trap_setting" to 1,
        "nova_gear_upgrade" to 1,
        "nova_hacking" to 2,
        "nova_remote_mine" to 2,
        "nova_cryo_vent" to 3,
        "nova_overclock" to 3,
        "nova_drone_swarm" to 3,
        "nova_master_engineer" to 5,
        "zeke_team_meeting" to 1,
        "zeke_training_session" to 1,
        "zeke_motivational_speech" to 1,
        "zeke_conflict_resolution" to 1,
        "zeke_benefits_plan" to 2,
        "zeke_retention_bonus" to 2,
        "zeke_talent_acquisition" to 3,
        "zeke_employee_survey" to 3,
        "zeke_professional_development" to 3,
        "zeke_chief_people_officer" to 5,
        "zeke_filing_system" to 1,
        "zeke_budgeting" to 1,
        "zeke_paperwork_mastery" to 1,
        "zeke_resource_allocation" to 1,
        "zeke_policy_manual" to 2,
        "zeke_audit" to 2,
        "zeke_signal_jammer" to 3,
        "zeke_office_renovation" to 3,
        "zeke_process_optimization" to 3,
        "zeke_executive_order" to 5,
        "zeke_load_balancing" to 1,
        "zeke_workflow_charts" to 1,
        "zeke_standard_procedure" to 1,
        "zeke_supply_chain" to 1,
        "zeke_quality_assurance" to 2,
        "zeke_logistics" to 2,
        "zeke_bulwark_stance" to 3,
        "zeke_overload_fists" to 3,
        "zeke_emergency_protocol" to 3,
        "zeke_quake_slam" to 5,
        "orion_innovate" to 1,
        "orion_med_lab_efficiency" to 1,
        "orion_crafting_bench" to 1,
        "orion_advanced_materials" to 2,
        "orion_nano_repair" to 2,
        "orion_weapon_specialization" to 2,
        "orion_disruption_pulse" to 3,
        "orion_overcharge" to 3,
        "orion_stasis_field" to 3,
        "orion_master_engineer" to 5,
        "orion_relay_repair" to 1,
        "orion_triage" to 1,
        "orion_shield_protocol" to 1,
        "orion_revive" to 2,
        "orion_cleanse" to 2,
        "orion_mass_repair" to 2,
        "orion_aegis_protocol" to 3,
        "orion_regeneration_field" to 3,
        "orion_sanctuary" to 3,
        "orion_guardian_angel" to 5,
        "gh0st_scope" to 1,
        "gh0st_steady_aim" to 1,
        "gh0st_headshot" to 1,
        "gh0st_penetrating_shot" to 2,
        "gh0st_venom_edge" to 2,
        "gh0st_crippling_shot" to 2,
        "gh0st_system_crash" to 3,
        "gh0st_chain_shot" to 3,
        "gh0st_ghost_in_the_machine" to 3,
        "gh0st_shadow_flurry" to 5,
        "gh0st_dismantle" to 1,
        "gh0st_emp_grenade" to 1,
        "gh0st_viral_agent" to 1,
        "gh0st_system_shock" to 2,
        "gh0st_overload" to 2,
        "gh0st_logic_bomb" to 2,
        "gh0st_meltdown" to 3,
        "gh0st_cascade_failure" to 3,
        "gh0st_master_saboteur" to 3,
        "gh0st_total_system_failure" to 5
    )

    val retainedCombatSkills: Set<String> = setOf(
        "gh0st_headshot",
        "gh0st_shadow_flurry",
        "gh0st_system_crash",
        "gh0st_venom_edge",
        "nova_cryo_vent",
        "nova_hydraulic_kick",
        "nova_plasma_burst",
        "nova_smoke_bomb",
        "orion_disruption_pulse",
        "orion_nano_repair",
        "orion_stasis_field",
        "zeke_bulwark_stance",
        "zeke_overload_fists",
        "zeke_quake_slam",
        "zeke_signal_jammer"
    )
}

/**
 * One-time adoption of the crew build rules. Legacy saves did not record paid AP,
 * so historical unlock costs are a generous respec credit, not an exact refund.
 * Keep decoding defaults at version 0 so old files remain distinguishable.
 */
fun GameSessionState.migrateCrewProgressionState(): GameSessionState {
    val checkpoint = battleCheckpoint?.migrateCrewProgressionState()
    val original = if (checkpoint == battleCheckpoint) this else copy(battleCheckpoint = checkpoint)
    if (progressionPrototypeVersion != 0) return original
    // An empty DataStore/default state must remain empty for existing import detection.
    if (worldId == null && playerId == null && partyMembers.isEmpty()) return original

    val legacy = LegacyCrewProgression
    val credit = unlockedSkills.sumOf { legacy.nodeCosts[it] ?: 0 }
    var migrated = original.copy(
        progressionPrototypeVersion = CrewBuildPrototype.VERSION,
        playerAp = (playerAp.toLong().coerceAtLeast(0) + credit).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        unlockedSkills = unlockedSkills - (legacy.nodeCosts.keys - legacy.retainedCombatSkills),
        purchasedSkillNodes = purchasedSkillNodes.filterKeys { it in CrewBuildPrototype.nodes }
    )
    // Only new quest/recruit awards are backfilled. Existing boss AP is already in the save.
    migrated.completedQuests.forEach { migrated = NovaBuildPrototype.awardCompletedQuest(migrated, it) }
    val recruits = buildSet {
        addAll(migrated.partyMembers)
        if ("talk_to_orion" in questTasksCompleted["w2_mq03"].orEmpty() ||
            "ms_w2_orion_briefed" in completedMilestones || "w2_mq03" in completedQuests) add("orion")
        if ("defeat_the_beast" in questTasksCompleted["w2_mq04"].orEmpty() ||
            "ms_w2_beast_defeated" in completedMilestones || "w2_mq04" in completedQuests) add("gh0st")
    }
    recruits.forEach { migrated = NovaBuildPrototype.awardRecruitment(migrated, it) }
    if ("orion" in recruits) migrated = migrated.copy(unlockedSkills = migrated.unlockedSkills + CrewBuildPrototype.PATCH_SKILL)
    return migrated
}
