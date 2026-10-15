package com.example.starborn.feature.combat.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.model.Enemy
import com.example.starborn.feature.combat.presentation.weaknessLabel

@Composable
fun EnemyOverview(enemy: Enemy, state: CombatantState, knownWeaknesses: Set<String>,
    inspecting: Boolean, canInspect: Boolean, onInspect: () -> Unit, onClose: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(enemy.name, style = MaterialTheme.typography.titleSmall)
                if (inspecting) Text("Paused", style = MaterialTheme.typography.labelMedium)
            }
            Text("HP ${state.hp}/${state.combatant.stats.maxHp}  ?  Stability ${state.stability}/${state.combatant.stats.stability}", style = MaterialTheme.typography.bodySmall)
            Text(if (knownWeaknesses.isEmpty()) "Weaknesses: unknown" else
                "Known weaknesses: " + knownWeaknesses.sorted().joinToString(" ? ", transform = ::weaknessLabel),
                style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onInspect, enabled = canInspect, modifier = Modifier.heightIn(min = 48.dp)) { Text("Inspect") }
                if (inspecting) TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Close overview") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnemyInspectionSheet(enemy: Enemy, state: CombatantState, knownWeaknesses: Set<String>, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(enemy.name, style = MaterialTheme.typography.headlineSmall)
            Text("Combat paused", style = MaterialTheme.typography.labelMedium)
            Text("HP ${state.hp}/${state.combatant.stats.maxHp}")
            Text("Stability ${state.stability}/${state.combatant.stats.stability}")
            Text(if (state.breakTurns > 0) "Broken ? ${state.breakTurns} turns remaining" else "Deplete Stability to cause a Break and create an opening for follow-up attacks.")
            Text("Discovered weaknesses", style = MaterialTheme.typography.titleMedium)
            Text(if (knownWeaknesses.isEmpty()) "Unknown. Try different attacks to discover weaknesses." else
                knownWeaknesses.sorted().joinToString(" ? ", transform = ::weaknessLabel))
            Text("Undiscovered weaknesses and resistances are not revealed by inspection.", style = MaterialTheme.typography.bodySmall)
            val effects = statusChipsFor(state.statusEffects, state.buffs)
            Text("Active effects", style = MaterialTheme.typography.titleMedium)
            if (effects.isEmpty()) Text("None") else effects.forEach { Text(it.description) }
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Close") }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
