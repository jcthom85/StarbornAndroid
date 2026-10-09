package com.example.starborn.domain.model

/** Keep canonical combat IDs and stats while distinguishing drill equipment. */
fun Enemy.forEncounterRoom(roomId: String?): Enemy =
    if (id == "acoustic_bulwark" && roomId in setOf("admin_security", "workshop_dock")) {
        copy(
            name = "Shield Trainer",
            flavor = "A Dominion drill frame braces behind a powered riot shield. It records each completed certification run.",
            description = "Certification drill frame with an active barrier. Use Hydraulic Kick to break its Guard before committing attacks."
        )
    } else this
