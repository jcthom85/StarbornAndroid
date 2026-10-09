package com.example.starborn.feature.exploration.presentation

import com.example.starborn.domain.model.Item
import java.util.Locale

/** Displays only supplied item fields, without inventing missing stats or effects. */
object ItemDetails {
    fun lines(item: Item, resolveName: (String) -> String = { it }): List<String> = buildList {
        fun label(value: String) = value.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.ROOT) }
        fun percent(value: Double) = "${(value * 100).toInt()}%"
        item.equipment?.let { gear ->
            add("Slot: ${label(gear.slot)}")
            gear.weaponType?.let { add("Weapon type: ${label(it)}") }
            gear.damageMin?.let { add("Damage: $it-${gear.damageMax ?: it}") }
            gear.defense?.let { add("Defense: $it") }
            gear.hpBonus?.let { add("HP bonus: $it") }
            gear.accuracy?.let { add("Accuracy: ${percent(it)}") }
            gear.critRate?.let { add("Critical chance: ${percent(it)}") }
            gear.attackStyle?.let { add("Attack style: ${label(it)}") }
            gear.attackPowerMultiplier?.let { add("Attack power: ${it}x") }
            gear.attackChargeTurns?.let { add("Charge: $it turns") }
            gear.attackSplashMultiplier?.let { add("Splash power: ${it}x") }
            gear.attackElement?.let { add("Element: ${label(it)}") }
            gear.statusOnHit?.let { add("On hit: ${resolveName(it)}${gear.statusChance?.let { chance -> " (${percent(chance)})" }.orEmpty()}") }
            gear.statMods.orEmpty().forEach { (stat, value) -> add("${label(stat)}: ${if (value > 0) "+" else ""}$value") }
        }
        item.effect?.let { effect ->
            effect.type?.let { add("Effect: ${label(it)}") }
            effect.amount?.let { add("Amount: $it") }
            effect.restoreHp?.let { add("Restore HP: $it") }
            effect.damage?.let { add("Damage: $it") }
            effect.status?.let { add("Status: ${resolveName(it)}") }
            effect.target?.let { add("Target: ${label(it)}") }
            effect.duration?.let { add("Duration: $it turns") }
            effect.learnSchematic?.let { add("Learn schematic: ${resolveName(it)}") }
            (listOfNotNull(effect.singleBuff) + effect.buffs.orEmpty()).forEach { buff ->
                add("${label(buff.stat)}: ${if (buff.value > 0) "+" else ""}${buff.value}${buff.duration?.let { " ($it turns)" }.orEmpty()}")
            }
            effect.cooldown?.let { add("Cooldown: $it turns") }
            effect.usesPerBattle?.let { add("Uses per battle: $it") }
        }
    }
}
