# Astra: crew home and mobile hub

Status: Current supporting overview, checked against source October 14, 2026. [Functionality](01_functionality.md) owns the implementation summary; [locations](03_locations.md) owns room topology. [Master Story](../Starborn_Master_Story.md) owns ship history and campaign transitions.

The Astra is a derelict Dominion survey vessel recovered in Sector 9 and repaired into the crew's home. It should make recovery, preparation, relationships, and remembered discoveries easy to revisit.

Five connected rooms are sufficient. Prefer more meaningful use of these spaces over extra rooms containing menus. Keep the warm, patched, practical ship identity: CRTs, exposed wiring, repaired fittings, shared meals, and personal possessions.

The story progresses from atmospheric repair in Sector 9 through Spire work to the Foundry engine upgrade. Exact access, destinations, and facility gates come from `AstraTravel`, room actions, events, and `AstraCatalog`; an overview is not a universal unlock schedule.

The current implementation includes crew check-ins and downtime, cargo/relic/catch records, recovered films, restored arcade access, navigation, crafting, cooking, rest, and gated simulations. This does not establish a separate bank, automatic relic skill synchronization, or mandatory travel conversations.

Simulations launch real combat with normal costs and rewards, so the old claim that no combat occurs aboard the ship is retired. Source implementation has been inspected; full Android/desktop device acceptance is separate.

[The design review](04_design_review.md) records earlier gaps and subsequent changes. Its chronology is evidence, not a second current facility specification.
