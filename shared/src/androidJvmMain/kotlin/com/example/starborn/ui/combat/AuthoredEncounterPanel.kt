package com.example.starborn.ui.combat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.model.Enemy

private val ThreatAmber = Color(0xFFFFC46B)

fun hasAuthoredPreparation(state: CombatState?): Boolean = state?.outcome == null &&
    state?.authored?.preparations?.isNotEmpty() == true

/** Fits a preallocated message slot; callers must never add it to the formation's measured content. */
@Composable
fun AuthoredEncounterPanel(state: CombatState?, enemies: List<Enemy>, onTarget: (String) -> Unit,
                           modifier: Modifier = Modifier, compact: Boolean = false,
                           skillName: (String) -> String? = { null }) {
    if (!hasAuthoredPreparation(state)) return
    val battle = requireNotNull(state)
    BoxWithConstraints(modifier.fillMaxWidth().testTag("combat-authored-warnings")
        .semantics { liveRegion = LiveRegionMode.Polite }) {
        val width = maxWidth
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            val preparations = battle.authored.preparations.values.toList()
            val groups = if (compact) preparations.map { listOf(it) } else preparations.chunked(2)
            groups.forEach { group ->
                val groupWidth = if (compact && groups.size == 2) (width - 6.dp) / 2 else width
                Column(Modifier.width(groupWidth), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    group.forEach { intent ->
                        val source = battle.combatants[intent.sourceId]?.combatant?.name
                            ?: enemies.firstOrNull { it.id == intent.sourceId.substringBefore('#') }?.name ?: "Enemy"
                        val targets = intent.targetIds.mapNotNull { battle.combatants[it]?.takeIf { actor -> actor.isAlive } }
                        val names = targets.map { it.combatant.name }
                        val party = battle.combatants.values.filter { it.isAlive && it.combatant.side == CombatSide.PLAYER }
                            .map { it.combatant.id }.toSet()
                        val target = when {
                            targets.singleOrNull()?.combatant?.id == intent.actorId -> ""
                            names.size > 1 && targets.map { it.combatant.id }.toSet() == party -> "Party"
                            names.size > 2 -> "${names.first()} +${names.size - 1}"
                            else -> names.joinToString(" + ")
                        }
                        val action = skillName(intent.skillId) ?: intent.label.substringBefore(" — ")
                        val label = if (intent.label.startsWith("Frenzy ")) {
                            intent.label.substringBefore(" — ") + " · " + action.substringAfter(": ")
                        } else action
                        val full = "$source: ${intent.label}" + if (target.isEmpty()) "" else " → ${names.joinToString()}"
                        val slim = compact || preparations.size > 1
                        Surface(
                            Modifier.widthIn(max = groupWidth).height(if (slim) 24.dp else 38.dp)
                                .clickable { onTarget(intent.sourceId) }
                                .testTag("combat-preparation-${intent.actorId}")
                                .semantics { contentDescription = full },
                            color = Color(0xED101A23), shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ThreatAmber.copy(alpha = .38f))
                        ) {
                            Row(
                                Modifier.padding(horizontal = if (slim) 8.dp else 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(Icons.Rounded.GpsFixed, null, Modifier.size(if (slim) 12.dp else 15.dp), tint = ThreatAmber)
                                Text(label, Modifier.weight(1f, fill = false), color = ThreatAmber,
                                    style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (target.isNotEmpty()) {
                                    Text("→", color = ThreatAmber, style = MaterialTheme.typography.labelMedium)
                                    Text(target, Modifier.widthIn(max = 104.dp), color = Color.White,
                                        style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Phase labels share the fixed message slot with attack warnings. */
@Composable
fun AuthoredEncounterStatus(state: CombatState?, modifier: Modifier = Modifier) {
    if (state == null || state.outcome != null) return
    val labels = state.authored.phases.values.map { it.substringBefore(" — ") }
    if (labels.isEmpty()) return
    Text(labels.joinToString(" · "), modifier.widthIn(max = 164.dp).testTag("combat-authored-status"),
        color = ThreatAmber.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall,
        maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Static reticle corners identify the threatened portrait without changing its layout or scale. */
@Composable
fun AuthoredTargetMarker(state: CombatState?, id: String, modifier: Modifier = Modifier) {
    if (!hasAuthoredPreparation(state) || state?.combatants?.get(id)?.isAlive != true ||
        state.authored.preparations.values.none { id in it.targetIds }) return
    Canvas(modifier.testTag("combat-threat-target-$id").semantics { contentDescription = "Targeted attack preparing" }) {
        val inset = 2.dp.toPx(); val arm = size.minDimension * .20f; val stroke = 2.dp.toPx()
        for (right in listOf(false,true)) for (bottom in listOf(false,true)) {
            val x = if (right) size.width-inset else inset; val y = if (bottom) size.height-inset else inset
            drawLine(ThreatAmber, Offset(x,y), Offset(x+if(right) -arm else arm,y),stroke)
            drawLine(ThreatAmber, Offset(x,y), Offset(x,y+if(bottom) -arm else arm),stroke)
        }
        drawCircle(ThreatAmber, 3.dp.toPx(), Offset(size.width/2,inset+2.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    }
}

/** Unscaled target buttons keep a multipart sprite's controls readable on short phone screens. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinkedEnemyTargets(state: CombatState, ids: List<String>, selected: Set<String>, onTarget: (String) -> Unit,
                       modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth().testTag("combat-linked-targets"),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 3) {
        ids.mapNotNull(state.combatants::get).filter { it.combatant.side == CombatSide.ENEMY }.forEach { actor ->
            val preparing = state.authored.preparations.values.any { it.sourceId == actor.combatant.id }
            Surface(Modifier.weight(1f).widthIn(min = 90.dp).heightIn(min = 44.dp)
                .clickable(enabled = actor.isAlive) { onTarget(actor.combatant.id) }
                .testTag("combat-linked-target-${actor.combatant.id}")
                .semantics { if (preparing) stateDescription = "Preparing an action" },
                color = if (actor.combatant.id in selected) Color(0xFF334938) else Color(0xDD0A1721),
                shape = RoundedCornerShape(6.dp), border = BorderStroke(if (preparing) 2.dp else 1.dp, when {
                    preparing -> ThreatAmber
                    actor.isAlive -> Color(0xFF6C899C)
                    else -> Color(0xFF35424D)
                })) {
                Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(actor.combatant.name, color = if (actor.isAlive) Color.White else Color(0xFF9AA8AF),
                        style = MaterialTheme.typography.labelMedium)
                    Text(if (actor.isAlive) "${actor.hp} / ${actor.combatant.stats.maxHp} HP" else "Disabled",
                        color = Color(0xFFB7CDD9), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
