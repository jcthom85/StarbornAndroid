# Skill Trees

All playable characters have a skill tree defined in this folder. Each branch is a list of nodes that can be unlocked by spending Ability Points (AP).

A node may depend on other abilities or on completed milestones. Every string in the node's `requires` array is checked against the character's unlocked abilities and the set of completed milestones.

Here is an example referencing a milestone:

```json
{
  "id": "nova_drone_hunter",
  "name": "Drone Hunter",
  "cost_ap": 3,
  "requires": ["nova_skill_05", "beat_first_drone"],
  "effect": { "type": "damage", "mult": 1.5, "rp_cost": 10 }
}
```

The node above will only become available once Nova knows `nova_skill_05` **and** the milestone `beat_first_drone` from `milestones.json` has been achieved.