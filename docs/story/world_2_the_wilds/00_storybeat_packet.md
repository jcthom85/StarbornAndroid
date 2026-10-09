# World 2 Storybeat Packet

**Status:** Build Prep
**Purpose:** Working bridge between World 2 Story Bible and game data implementation (`quests.json`, `events.json`, `dialogue.json`).

---

## 1. Player Experience Target

World 2 is the assembly of the crew. Nova is no longer in a structured society; she is in the wild, carrying an unstable energy source (the Echo) that burns her nervous system whenever she uses it. 

**Entry State:**
- Party: Nova solo.
- Items: Starter cutter, Tuning Fork, Chime (Orion identity key + paired beacon; never a battery).
- Status: Suffering from Erosion.

**Exit State:**
- Party: Nova, Zeke, Orion, Gh0st.
- Echoes/Arts: Tuning Fork (Blast) and Bridge Echo (Link), with the Bridge installed aboard *The Astra*. The Chime remains a key/beacon, not a power-granting Echo.
- Status: Repaired *Astra* capable of low-altitude flight to the Spire; internal Phase Curtains and the outer Shield remain beyond its current systems.

---

## 2. Non-Negotiable Story Beats

These are the primary narrative anchors for World 2.

| Order | Beat | Function | Required Data |
|---|---|---|---|
| 1 | Waking at the Crash | Onboard survival pacing, Erosion tutorial, Zeke's injury. | `w2_mq01`, `landing_core`, `landing_pod`. |
| 2 | Razor-Vine Gauntlet | First wilderness combat; introduction to wildlife threat. | `wilds_trailhead`, `wilds_thickets`. |
| 3 | Beacon Authentication | The Chime proves Orion's identity to the Temple Gate; Sanctuary reserve cells wake. | `w2_mq02`, `sector9_temple_lock_chamber`. |
| 4 | Stasis Observations | Reading light murals; learning about "Harmony" (Great Silence). | `EVT_W2_01`, `hall_main_gallery`. |
| 5 | Awakening the Ancient | Reserve power and the ring array release Orion into a dead world; the crew recovers the separate Bridge Echo. | `EVT_W2_02`, `stasis_pod_deck` ring alignment. |
| 6 | The Ceasefire | Gh0st ambush; Nova's Fork mark trips a Source Lock failsafe but gives no order; Gh0st chooses to protect the party during the Source Beast attack. | `EVT_W2_03`, `ridge_arena` boss encounter. |
| 7 | The Anchor Drill | Learning to share the static; safely unlocking the Bridge-granted Source Art **Link**. | `EVT_W2_04`, first camp dialogue after Gh0st's truce. |
| 8 | First Liftoff | Salvaged conduits power *The Astra* and its newly installed Bridge Echo for a low-altitude flight to the Spire. The outer Shield is visible but never approached. | `EVT_W2_11`, launch cinematic over Sector 9. |

---

## 3. Playable Script & Dialogue Fragments

### EVT_W2_01: The Hall of Echoes
**Location:** `sector9_hall_of_echoes`
**Scene:** Nova touches the central light mural. Shifting waveforms of blue-white light dance across the smooth stone.

```
NOVA: (whispering)
It’s humming. It’s not just stone, Zeke. It’s vibrating.

ZEKE:
I can feel it in my fillings, Nova. And not the good kind of tingle. More like a generator about to blow its housing.

ORION:
They called it Harmony. But it was not peace. It was alignment. Every frequency forced to match one single master chord.

ZEKE:
So... a corporate board meeting, but with energy weapons?

ORION:
Dominion did not invent this. They are simply trying to strike the same note. They call it the Great Silence.
```

---

### EVT_W2_02: The Awakening
**Location:** `sector9_stasis_chamber`
**Scene:** The Temple Gate accepted the Chime as Orion's identity key and woke the Sanctuary's reserve cells. Nova and Zeke route that reserve power through the aligned ring array. Orion's pod vents coolant and opens; he falls forward, blue fluid on his chin, gasping.

```
ORION:
(coughing, eyes searching)
The Chime... where is the Chime?

NOVA:
(holding up the crystal casing)
Here. We found it in the mine.

ORION:
(checks its paired cadence against the console)
My key. My beacon. It found its way back. The reserve cells accepted me before I was awake.

ZEKE:
Uh, buddy, we’ve got some bad news. The colony above us? It’s a Dominion quarry. Your people... they aren't around.

ORION:
(staring at his hands, breathing heavily)
Twenty years... I went to sleep to escape the breach. I thought... I thought we would wake to a chorus. There is only static.

NOVA:
(reaches out to touch his shoulder)
Orion, wait—

(The Fork protocol mark on Nova's arm catches a burst of Architect telemetry. It translates the machine log into painful sound and image; there is no inherited memory in it. Nova staggers back, clutching her head.)

ORION:
Stop! You are burning the wire. The current is too strong for a single spark.

NOVA:
Why could I read that?

ORION:
Because an Echo wrote a machine protocol into you. Not blood. Not ancestry. A wound with permissions.

(Beyond the pod, Sanctuary reserves illuminate a sealed navigation cradle. Inside is the Bridge Echo—a separate Architect system. The crew recovers it here; its handshake makes the Source Art Link available, and it will be installed aboard *The Astra* later.)
```

---

### EVT_W2_04: Anchor Drill (First Rest after Gh0st's Truce)
**Location:** `sector9_canopy_ridge`
**Scene:** Night on the ridge. Zeke is bandaging a reopened cut when Nova tries to route the Bridge's Link into the wound by herself.

```
ORION:
Stop. You are burning the wire.

NOVA:
I can hold it.

ORION:
The Bridge connects; it does not make one body carry the circuit. Ground first. Then channel.

GH0ST:
No command attached. I am here. Route it.

(Zeke clips a grounding lead to Nova's bracer. Gh0st freely offers his armored hand. Orion holds one low tone. Nova opens Link; current passes among them without merging them, and Zeke's wound stabilizes.)

NOVA:
Link. Not merge. I get it.
```

---

### EVT_W2_11: First Liftoff
**Location:** `sector9_the_sky`
**Scene:** Salvaged conduits feed *The Astra's* power bus. The Bridge Echo wakes in its newly installed cradle and paints a low-altitude route from Sector 9 to the Spire. The ship rises just above the jungle; the Planetary Shield remains a distant lattice high overhead.

```
CONSOLE:
BRIDGE ECHO ONLINE. CONDUIT LOAD: EIGHTY-SEVEN PERCENT. LOW-ALTITUDE CORRIDOR LOCKED.

ZEKE:
Tell me that number gets friendlier once we're moving.

NOVA:
(steadying the steering yoke)
Tell the conduits to complain after we land.

ORION:
Keep her under the storm shelf. These conduits can carry us to the Spire, not through a containment curtain and not to the outer Shield.

(The Bridge Echo draws a stable line between the canopy and the cloud base. Nova follows it. The Spire emerges on the horizon.)

ZEKE:
(watching the conduit gauges)
Low and slow. I can work with low and slow.

NOVA:
And when we need to cross sectors?

ZEKE:
The Lens in the Spire reads frequency gaps. We steal it before we ask this ship for a miracle.

NOVA:
First we land. Then we heist the Lens.
```
