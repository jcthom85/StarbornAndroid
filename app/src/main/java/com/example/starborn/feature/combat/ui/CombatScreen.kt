package com.example.starborn.feature.combat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.Player
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.CombatViewModelFactory
import com.example.starborn.ui.dialogs.SkillsDialog

@Composable
fun CombatScreen(
    navController: NavController,
    enemyId: String,
    viewModel: CombatViewModel = viewModel(
        factory = CombatViewModelFactory(LocalContext.current, enemyId)
    )
) {
    val player = viewModel.player
    val enemy = viewModel.enemy
    val skills = viewModel.skills
    val combatState = viewModel.combatState

    if (viewModel.checkCombatEnd()) {
        navController.popBackStack()
    }

    if (player != null && enemy != null && skills != null && combatState != null) {
        val showSkillsDialog = remember { mutableStateOf(false) }

        if (showSkillsDialog.value) {
            SkillsDialog(
                player = player,
                skills = skills,
                onDismiss = { showSkillsDialog.value = false },
                onSkillSelected = { skill ->
                    viewModel.useSkill(skill)
                    showSkillsDialog.value = false
                }
            )
        }

        LaunchedEffect(combatState.turnQueue.first()) {
            if (combatState.turnQueue.first() is Enemy) {
                viewModel.enemyAttack()
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val imageId = LocalContext.current.resources.getIdentifier(
                    enemy.portrait.substringAfterLast("/").substringBeforeLast("."),
                    "drawable",
                    LocalContext.current.packageName
                )
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = null,
                    modifier = Modifier.size(128.dp)
                )
                Text(text = enemy.name)
                LinearProgressIndicator(progress = { combatState.enemyHealth.toFloat() / enemy.hp.toFloat() })
                Text(text = "${combatState.enemyHealth}/${enemy.hp}")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val imageId = LocalContext.current.resources.getIdentifier(
                    player.miniIconPath.substringAfterLast("/").substringBeforeLast("."),
                    "drawable",
                    LocalContext.current.packageName
                )
                Image(
                    painter = painterResource(id = imageId),
                    contentDescription = null,
                    modifier = Modifier.size(128.dp)
                )
                Text(text = player.name)
                LinearProgressIndicator(progress = { combatState.playerHealth.toFloat() / player.hp.toFloat() })
                Text(text = "${combatState.playerHealth}/${player.hp}")
            }

            Row {
                Button(onClick = {
                    if (combatState.turnQueue.first() is Player) {
                        viewModel.playerAttack()
                    }
                }) {
                    Text(text = "Attack")
                }
                Button(onClick = { showSkillsDialog.value = true }) {
                    Text(text = "Skills")
                }
            }
        }
    }
}
