# NPC presence and overworld access

Desktop now uses the Android presence presentation: named NPC portrait chips with circular portraits and cyan borders, instead of generic Talk to buttons. Portraits and display names come from the shared exploration UI data. All present NPCs appear in the rail, including those mentioned in room prose.

The Android-style OVERWORLD MAP gateway is now under the room information, visible in the default room view and compact layout. It uses the original shared controller gate/callback. Pit Landing becomes eligible after Jed's opening conversation; players return from Jed's bunk to Pit Landing to exit the Pit. Other node entrances and the Astra disembark route use the same control. Removed the easy-to-miss right HUD Hub map link.

Validation: desktop Kotlin compilation succeeded. Native playthrough remains outstanding.

Merchant pilot: Market Strip has Shopkeep at the Colony Commissary; Scrapper remains in his stall to the north. Their portrait chips and explicit NPC references in room prose open the same interaction. These rooms no longer expose separate shop actions. Weapon, armor, and accessory shop catalogs remain defined but are no longer placed in Market Strip.

NPCs opt into this flow with `trade_after_dialogue: true` and an interaction containing `type: shop` and `shop_id`. Authored dialogue runs normally, including choices, quest triggers, rewards, and dialogue-closed events. Once it finishes (or immediately if there is no authored dialogue), the merchant menu offers Trade, shop conversation topics, and Leave. Rooms with an existing standalone shop action retain that action's access rules. Other NPCs retain their previous interaction behavior.

Merchant menus show portraits on both platforms. Android's shop portrait remains visible even without a dialogue preface; desktop retains its persistent portrait header.

Merchant validation: Android and desktop Kotlin compilation, 37 targeted merchant/dialogue/shop/map tests, and desktop asset parity checks passed. The Scrapper regression covers quest acceptance, turn-in, completion rewards, and repeat conversations without duplicate rewards. Visual playthrough on a device remains outstanding.
