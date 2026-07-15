# World 2 Storybeat Packet

**Status:** Build Prep
**Purpose:** Working bridge between World 2 Story Bible and game data implementation (`quests.json`, `events.json`, `dialogue.json`).

---

## 1. Player Experience Target

World 2 is the assembly of the crew. Nova is no longer in a structured society; she is in the wild, carrying an unstable energy source (the Echo) that burns her nervous system whenever she uses it. 

**Entry State:**
- Party: Nova solo.
- Items: Starter cutter, Tuning Fork, Chime.
- Status: Suffering from Erosion.

**Exit State:**
- Party: Nova, Zeke, Orion, Gh0st.
- Artifacts: Tuning Fork (Blast), Orion's Chime (identity/pairing), separate Bridge Echo (Link).
- Status: Repaired *Astra* locked under the Planetary Shield.

---

## 2. Non-Negotiable Story Beats

These are the primary narrative anchors for World 2.

| Order | Beat | Function | Required Data |
|---|---|---|---|
| 1 | Waking at the Crash | Onboard survival pacing, Erosion tutorial, Zeke's injury. | `w2_mq01`, `landing_core`, `landing_pod`. |
| 2 | Razor-Vine Gauntlet | First wilderness combat; introduction to wildlife threat. | `wilds_trailhead`, `wilds_thickets`. |
| 3 | The Choice | Gh0st confrontation; Source Lock pauses one action; Beast ambush; Gh0st freely protects. | `w2_mq04_confront`, `w2_mq04_beast_ambush`, Canopy Ridge state gates. |
| 4 | Awakening the Ancient | Reconnecting with the past; Orion wakes up to a dead world. | `EVT_W2_02`, `stasis_pod_deck` ring alignment. |
| 5 | The Anchor Drill | Grounded, willing Bridge training; unlock Link only after Beast victory and the cinematic callback. | `w2_mq04_anchor_drill`, `scene_anchor_drill`. |
| 6 | Stasis Observations | Reading light murals; learning about "Harmony" (Great Silence). | `EVT_W2_01`, `hall_main_gallery`. |
| 7 | Hitting the Ceiling | Launch sequence fails when Planetary Shield discharges. | `EVT_W2_11`, launch cinematic in the mesosphere. |

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
**Scene:** The ring array aligns. Orion's pod vents coolant and opens. Orion falls forward, blue fluid on his chin, gasping.

```
ORION:
(coughing, eyes searching)
The Chime... where is the Chime?

NOVA:
(holding up the crystal casing)
Here. We found it in the mine.

ORION:
(takes it, slotting it into the console)
It is still ringing. But the signal... it is hollow. Where are the others? Why is the sky so quiet?

ZEKE:
Uh, buddy, we’ve got some bad news. The colony above us? It’s a Dominion quarry. Your people... they aren't around.

ORION:
(staring at his hands, breathing heavily)
Twenty years... I went to sleep to escape the breach. I thought... I thought we would wake to a chorus. There is only static.

NOVA:
(reaches out to touch his shoulder)
Orion, wait—

(The Echo marks flare on Nova’s arm. A painful shriek of static fills the air. Nova staggers back, clutching her head.)

ORION:
Stop! You are burning the wire. The current is too strong for a single spark.
```

---

### EVT_W2_04: Anchor Drill (First Rest after Orion joins)
**Location:** Campsite (e.g., `sector9_beach_cove` or `sector9_temple_plaza`)
**Scene:** Evening. Spores drift down like glowing ash. Nova is shaking, her fingers glowing with blue static.

```
ORION:
(setting a pebble in Nova’s palm)
Physics is simple. If you try to hold the ocean in a cup, it will overflow. You must build a circuit.

NOVA:
I can handle it. I just need to push harder.

ORION:
Pushing is what melts the wire, child. Look at them.

(Orion points to Zeke.)

ORION:
He is the Ground. When the static rises, you push the excess to him. He is solid. He dissipates it.

ZEKE:
Wait, I’m the what now?

(Orion points to Gh0st.)

ORION:
He is the Shield. He holds the heat so it does not leak into the air.

GH0ST:
(adjusting his shoulder guard)
Thermal dampening systems standing by.

ORION:
And I am the Tuner. I make sure the noise becomes a chord. Close your eyes, Nova. Do not hold the spark. Link them.

NOVA:
(closing her eyes, exhaling)
Linking...

(A soft blue thread of light connects Nova, Zeke, and Gh0st. The pebble in Nova’s hand glows cool and steady.)

ZEKE:
Whoa. Tingly. Like drinking carbonated water through your fingers.

GH0ST:
Static discharge absorbed. Vector stable.

ORION:
Better. Again tomorrow.
```

---

### EVT_W2_11: The Hard Deck (The Ceiling)
**Location:** `sector9_the_sky`
**Scene:** *The Astra* breaks through the thick jungle clouds. The sky turns deep purple. Suddenly, a blinding flash of yellow-white lightning from the Planetary Shield strikes the cockpit.

```
CONSOLE:
WARNING. HARD DECK BREACH. ENERGY SHIELD DEFENSIVE DISCHARGE IMMINENT.

ZEKE:
Pull back! Pull back! It's not a wall, Nova, it's a zapper!

NOVA:
(fighting the steering yoke)
I can’t override it! It’s locking onto our heat signature!

ORION:
The Shield is a Phase Curtain. It matches the frequency of the sector. Without the Lens to see the gap, we will disintegrate!

(Nova slams the yoke forward. The ship dives back into the canopy just as a second blast vaporizes the clouds above them.)

ZEKE:
(panting, sliding out of his chair)
Okay. So we are inside a giant fishbowl, and the lid is electrified. Fantastic.

NOVA:
Then we need a key. Or a map.

ZEKE:
The Administrator’s Spire. World 3. The Lens is kept in the central vault. It monitors the frequency gaps.

NOVA:
Then we have our next job. We heist the Lens.
```
