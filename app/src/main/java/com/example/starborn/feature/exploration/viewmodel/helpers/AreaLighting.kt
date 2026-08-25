package com.example.starborn.feature.exploration.viewmodel.helpers

/**
 * Resolves which rooms an area power source is wired to.
 *
 * Area power lights the area it powers -- the node the generator sits in -- never every room that
 * happens to share the generator's art environment. World 1 themes almost all of its interiors as
 * env "mine" (the Pit, the workshop, the med-bay, the checkpoint), so env scoping reached far past
 * the mine and switched off darkness puzzles that had nothing to do with it.
 *
 * Only rooms that can actually be dark are returned; lighting a room that was never dark is a no-op
 * that would still churn saved room state.
 */
fun generatorLitRoomIds(
    generatorRoomId: String,
    nodeIdByRoomId: Map<String, String>,
    darkCapableRoomIds: Set<String>
): Set<String> {
    val generatorNodeId = nodeIdByRoomId[generatorRoomId] ?: return emptySet()
    return darkCapableRoomIds
        .filter { nodeIdByRoomId[it] == generatorNodeId }
        .toSet()
}
