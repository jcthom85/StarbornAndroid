package com.example.starborn.feature.exploration.viewmodel.helpers

import com.example.starborn.domain.model.ShopDefinition
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueLineUi
import com.example.starborn.feature.exploration.viewmodel.ShopDialogueAction
import com.example.starborn.feature.exploration.viewmodel.ShopGreetingUi
import java.util.Locale

data class ShopDialogueSession(
    val shopId: String,
    val baseLines: List<ShopDialogueLineUi>,
    val topics: Map<String, ShopDialogueTopicState>,
    val tradeLabel: String,
    val leaveLabel: String,
    val visitedTopics: MutableSet<String> = mutableSetOf()
)

data class ShopDialogueTopicState(
    val id: String,
    val label: String,
    val responseLines: List<ShopDialogueLineUi>,
    val voiceCue: String?
)

object ShopDialogueSessionManager {
    fun createSession(shop: ShopDefinition, fallbackGreeting: String): ShopDialogueSession {
        val dialogue = shop.dialogue
        val baseLines = if (dialogue?.preface.isNullOrEmpty()) {
            listOf(
                ShopDialogueLineUi(
                    id = "${shop.id}_preface_0",
                    speaker = shop.name.takeIf { it.isNotBlank() } ?: "Shopkeeper",
                    text = fallbackGreeting,
                    voiceCue = shop.voCue
                )
            )
        } else {
            dialogue.preface.mapIndexed { index, line ->
                ShopDialogueLineUi(
                    id = "${shop.id}_preface_$index",
                    speaker = line.speaker?.takeIf { it.isNotBlank() } ?: shop.name.takeIf { it.isNotBlank() },
                    text = line.text,
                    voiceCue = line.voiceCue
                )
            }
        }
        val topics = dialogue?.smalltalk.orEmpty().associate { topic ->
            topic.id to ShopDialogueTopicState(
                id = topic.id,
                label = topic.label,
                responseLines = topic.response.mapIndexed { idx, line ->
                    ShopDialogueLineUi(
                        id = "${shop.id}_${topic.id}_$idx",
                        speaker = line.speaker?.takeIf { it.isNotBlank() } ?: shop.name.takeIf { it.isNotBlank() },
                        text = line.text,
                        voiceCue = line.voiceCue
                    )
                },
                voiceCue = topic.voiceCue
            )
        }
        val tradeLabel = dialogue?.tradeLabel?.takeIf { it.isNotBlank() } ?: "Browse stock"
        val leaveLabel = dialogue?.leaveLabel?.takeIf { it.isNotBlank() } ?: "Not now"
        return ShopDialogueSession(
            shopId = shop.id,
            baseLines = baseLines,
            topics = topics,
            tradeLabel = tradeLabel,
            leaveLabel = leaveLabel
        )
    }

    fun buildGreetingUi(session: ShopDialogueSession, shop: ShopDefinition): ShopGreetingUi {
        return ShopGreetingUi(
            shopId = session.shopId,
            shopName = shop.name.ifBlank { "Shopkeeper" },
            portraitPath = shop.portrait,
            lines = session.baseLines,
            choices = buildShopChoices(session)
        )
    }

    fun buildShopChoices(session: ShopDialogueSession): List<ShopDialogueChoiceUi> {
        val tradeChoice = ShopDialogueChoiceUi(
            id = "enter_shop_${session.shopId}",
            label = session.tradeLabel,
            action = ShopDialogueAction.ENTER_SHOP
        )
        val topicChoices = session.topics.values
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .map { topic ->
                ShopDialogueChoiceUi(
                    id = topic.id,
                    label = topic.label,
                    action = ShopDialogueAction.SMALLTALK,
                    enabled = topic.id !in session.visitedTopics
                )
            }
        val leaveChoice = ShopDialogueChoiceUi(
            id = "leave_shop_${session.shopId}",
            label = session.leaveLabel,
            action = ShopDialogueAction.LEAVE
        )
        return buildList {
            add(tradeChoice)
            addAll(topicChoices)
            add(leaveChoice)
        }
    }
}
