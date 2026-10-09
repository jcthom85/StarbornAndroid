# NPC presence and overworld access

Desktop now uses the Android presence presentation: named NPC portrait chips with circular portraits and cyan borders, instead of generic Talk to buttons. Portraits and display names come from the shared exploration UI data. All present NPCs appear in the rail, including those mentioned in room prose.

The Android-style OVERWORLD MAP gateway is now under the room information, visible in the default room view and compact layout. It uses the original shared controller gate/callback. Pit Landing becomes eligible after Jed's opening conversation; players return from Jed's bunk to Pit Landing to exit the Pit. Other node entrances and the Astra disembark route use the same control. Removed the easy-to-miss right HUD Hub map link.

Validation: desktop Kotlin compilation succeeded. Native playthrough remains outstanding.
