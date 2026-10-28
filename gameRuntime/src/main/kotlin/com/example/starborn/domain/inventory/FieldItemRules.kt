package com.example.starborn.domain.inventory

import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect

/** The same combat-only rule drives field actions and their inventory presentation. */
object FieldItemRules {
    fun isCombatOnly(effect: ItemEffect?, preparedMeal: Boolean): Boolean = effect != null &&
        ((effect.damage ?: 0) > 0 || (!preparedMeal && (effect.singleBuff != null || !effect.buffs.isNullOrEmpty())))

    fun isCombatOnly(item: Item, preparedMeal: Boolean): Boolean =
        !item.type.equals("snack", true) && item.equipment?.slot?.equals("snack", true) != true &&
            isCombatOnly(item.effect, preparedMeal)
}
