package com.example.starborn.domain.narrative

import com.example.starborn.domain.session.GameSessionState

/** Optional rest copy. Nothing here awards progress or supplies essential plot information. */
object RestBanter {
    data class Line(val id: String, val text: String)
    data class Selection(val id: String, val text: String, val heardIds: Set<String>)

    fun select(state: GameSessionState, roomId: String, message: String?): Selection {
        val lines = eligible(state, roomId, message)
        val unheard = lines.filter { it.id !in state.heardRestBanter }
        val pool = unheard.ifEmpty { lines }
        val line = pool.firstOrNull { it.id != state.lastRestBanterId } ?: pool.first()
        // Clear only the currently eligible cycle. Absent companions keep their history,
        // and newly eligible lines get a turn before already heard ones repeat.
        val heard = if (unheard.isEmpty()) state.heardRestBanter - lines.map { it.id }.toSet()
            else state.heardRestBanter
        return Selection(line.id, line.text, heard + line.id)
    }

    fun eligible(state: GameSessionState, roomId: String, message: String?): List<Line> = buildList {
        val room = roomId.lowercase(java.util.Locale.ROOT)
        val party = state.partyMembers.ifEmpty { listOfNotNull(state.playerId) }.toSet()
        val milestones = state.completedMilestones
        fun addLine(id: String, text: String, vararg members: String) {
            if (members.all { it in party }) add(Line(id, text))
        }
        message?.takeIf { it.isNotBlank() }?.let { add(Line("place:$room", it)) }
        when {
            room.startsWith("astra") -> {
                addLine("astra:tea", "Zeke sets a cup beside Nova. “No sugar?”\n“Still no sugar.”\n“Right. That one’s mine.” He swaps the cups.", "nova", "zeke")
                addLine("astra:seat", "Orion pauses at the table. “Is this someone’s place?”\n“Yours, if you sit in it,” Nova says. He pulls the chair closer to the window.", "nova", "orion")
                addLine("astra:watch", "Gh0st checks the hatch again. Nova puts a cup on the table beside him. “You can watch it from here.” He sits where he can still see the door.", "nova", "gh0st")
                if ("ms_all_arcade_cabinets_restored" in milestones) {
                    addLine("astra:arcade_all", "The restored cabinets hum from the recreation deck. “One game,” Zeke says. Gh0st pulls up a second chair.", "zeke", "gh0st")
                } else if ("ms_arcade_cabinet_01_repaired" in milestones) {
                    addLine("astra:arcade_first", "Nova stretches out on the couch. A restored cabinet hums on the recreation deck; for once, she leaves a working machine alone.", "nova")
                }
            }
            room.startsWith("sector9") || room.startsWith("swamp") -> {
                addLine("w2:boots", "Zeke puts his boots on a dry root. “Keep the seals off the moss. Mine are already starting to peel.”", "zeke")
                addLine("w2:reeds", "Orion watches the reeds light up. “They used to do that before the tide came in. We have time for a short rest.”", "orion")
                addLine("w2:watch", "Gh0st settles against a root. “The mist’s too thick for thermal. I’ll listen. Get some sleep.”", "gh0st")
                addLine("w2:dry_seat", "Nova shifts over on the dry stone. Orion starts to sit in the mud instead. “Here,” she says. “I moved for a reason.” He sits beside her.", "nova", "orion")
                if ("ms_w2_mq04_complete" in milestones) {
                    addLine("w2:boot_repair", "“That seal won’t hold,” Gh0st says. Zeke lifts his boot. “You could have told me before I patched it.”\n“You hadn’t finished.” Gh0st holds the loose edge while Zeke presses it flat.", "zeke", "gh0st")
                }
            }
            room.startsWith("spire") -> {
                addLine("w3:badge", "Gh0st props himself against the wall. “I cleared our badge pings. I’ll wake you before the next sweep.”", "gh0st")
                addLine("w3:freight", "Orion listens to the traffic overhead. “That’s a freight car. It took me a while to stop getting up for it.”", "orion")
                if ("ms_w3_mq14_complete" !in milestones) {
                    addLine("w3:reflection", "Zeke tilts his dark datapad toward the nearest light. Nova watches him. “Checking for cameras?”\n“Checking whether I still look like someone they’d let upstairs.”", "nova", "zeke")
                }
                addLine("w3:light", "Nova reaches to dim the light. “Leave a little,” Orion says. “I’d like to see the door.” She shades the glare with her sleeve and leaves it on.", "nova", "orion")
            }
            room.startsWith("foundry") -> {
                addLine("w4:water", "Zeke unscrews the canteen and passes it to Nova. “Drink. I’ll get another when the pipes cool.”", "nova", "zeke")
                addLine("w4:exhaust", "Gh0st checks the vent noise. “The exhaust will hide us while it’s running. Get some sleep.”", "gh0st")
                if ("ms_w4_phantom_records_found" !in milestones) {
                    addLine("w4:rail", "Orion stops Nova resting her arm on the rail. “That carries the press vibration.” She moves her bedroll. “You could hear that?”\n“Your teeth were chattering.”", "nova", "orion")
                }
                if ("ms_w4_mq17_complete" in milestones) {
                    addLine("w4:seat", "Zeke leaves a space beside him. Gh0st stays standing. “You don’t have to talk,” Zeke says. Gh0st sits; they listen to the pipes cool.", "zeke", "gh0st")
                }
            }
            room.startsWith("orbital") || room.startsWith("deep_") || room.startsWith("zenith") -> {
                addLine("w5:hatch", "Zeke rubs his eyes. “I can still taste the slag dust. Try to sleep. I’ll watch the hatch.”", "zeke")
                if (room.startsWith("orbital") || room.startsWith("zenith")) {
                    addLine("w5:tide", "Orion rests a hand against the viewport. “I can see where the tide turns from here. I used to wait all evening to see that from the shore.”", "orion")
                    if ("ms_w5_thorne_found" !in milestones) {
                        addLine("w5:carpet", "Zeke presses one boot against the drifting carpet. “They could afford a window this size. Not a floor that stays put.” Nova catches its edge under the bench.", "nova", "zeke")
                    }
                }
                addLine("w5:rest", "Nova starts checking the next route. Gh0st taps the seat beside her. “You said five minutes.” She puts the map down. “Keep count, then.”", "nova", "gh0st")
                if ("ms_w5_thorne_found" in milestones) {
                    addLine("w5:names", "Zeke starts to fold away the list. “I’ll keep a copy,” Gh0st says. Zeke sends it over without asking what he intends to do with it.", "zeke", "gh0st")
                }
            }
            room.startsWith("source") -> {
                addLine("w6:sound", "Orion sits beside Nova. “Close your eyes. I’ll tell you if the sound changes.”", "nova", "orion")
                addLine("w6:watch", "Gh0st checks his scanner. “Nothing moving close to us. I’ll stay up.”", "gh0st")
                addLine("w6:glove", "Zeke leaves his spare glove beside Nova’s hand. She pulls it on without sitting up.", "nova", "zeke")
            }
            else -> {
                // Keep the opening's authored wake-up/shift text; no new humor near Jed's loss.
                if (isEmpty()) {
                    addLine("w1:alarm", "Zeke sets his datapad face down. “I keep thinking I ought to set the shift alarm. Go ahead and sleep, Nova.”", "nova", "zeke")
                }
            }
        }
        if (isEmpty()) add(Line("quiet:$room", "Nova settles into the shelter and rests until her breathing steadies."))
    }
}
