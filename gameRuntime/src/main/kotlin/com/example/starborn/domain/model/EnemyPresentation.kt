package com.example.starborn.domain.model

/** Keep canonical combat IDs and stats while distinguishing drill equipment. */
fun Enemy.forEncounterRoom(roomId: String?): Enemy =
    if (id == "acoustic_bulwark" && roomId in setOf("admin_security", "workshop_dock")) {
        copy(
            name = "Shield Trainer",
            flavor = "A Dominion drill frame braces behind a powered riot shield. It records each completed certification run.",
            description = "Certification drill frame with an active barrier. Use Hydraulic Kick to break its Guard before committing attacks."
        )
    } else if (roomId == "sector9_hall_acoustics_lab") {
        // One remixed opportunity keeps the old plant encounter's XP/credit/drop budget.
        when (id) {
            "sentinel_orb" -> copy(xpReward = 55, creditReward = 15, drops = emptyList())
            "spore_spitter" -> copy(xpReward = 0, creditReward = 0)
            else -> this
        }
    } else if (roomId == "spire_exec_lounge_scale_05" && id == "riot_guard") {
        // The guard joins the existing drone encounter without increasing its payout.
        copy(xpReward = 0, creditReward = 0, drops = emptyList())
    } else this
