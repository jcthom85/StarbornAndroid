package com.example.starborn.feature.exploration.presentation

const val GLADE_BALL_ROOM = "sector9_landing_glade"

fun gladeBallTrust(milestones: Set<String>): Int = when {
    "ms_charlie_trust_3" in milestones -> 3
    "ms_charlie_trust_2" in milestones -> 2
    "ms_charlie_trust_1" in milestones -> 1
    else -> 0
}

fun gladeBallReaction(trust: Int, repeatIndex: Int = 0): String = when (trust) {
    0 -> "You roll the puffball toward the log and wait. A long pause. Then it rolls back. “Charlie?” Nova tries softly. A small chirp answers from the ferns."
    1 -> "You give the puffball a gentle toss. Something scrambles through the moss; it comes bouncing back, overshoots your boots, and settles. A pleased chirp follows."
    2 -> "“One more, Charlie.” You roll it back. This time it returns with a smooth stone nudged out of the moss beside it. A low purr comes from beneath the log."
    else -> when (repeatIndex % 3) {
        0 -> "The puffball disappears beneath the fronds, then comes tumbling back. You hear an eager rustle and a soft, contented purr."
        1 -> "A quick scuffle beneath the log. The puffball shoots past your boots, followed by a tiny, indignant chirp. “Good throw, Charlie.”"
        else -> "This time the puffball returns slowly, nudged a little at a time from the shelter of the ferns. A steady purr fills the hollow."
    }
}
