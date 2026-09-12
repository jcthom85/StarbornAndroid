# Route and narrative review

## Astra checkpoint

Boarding saves the return location and warps to `astra_bridge` in
`ExplorationViewModel.boardAstraFromWorld`. The bridge leads south to the common
room, which leads north to bridge, west to cargo, east to quarters and south to
simulation. Each spoke has a reciprocal return connection. All five rooms are
reachable in at most two authored edges from boarding. This is static route
evidence, not an end-to-end navigation playtest.

Facility terminology and behavior:

- Navigation console opens navigation UI; available destinations are progression-dependent.
- Great Frontier film archive opens the tape deck; distinct from the simulation archive console.
- Cargo repair bench is an inspection, not a crafting service. Its current message
  describes equipment without promising a repair operation; retain this distinction.
- Storage terminal explicitly says storage is offline; relic array says synchronization
  is future functionality. Neither should be counted as a functioning service.
- Quarters bed invokes party rest. Simulation console opens simulation UI, but its
  fallback message still says it awaits runtime. Review this stale wording against
  the full simulation flow before editing it.

The all-arcades common-room variant exceeded the 45-word room-copy limit and was
shortened while retaining every inline reference. Other Astra descriptions remain
under that limit.

## Remaining review

The strict narrative validator currently fails (first reported room:
`spire_night_market`, 47 words). Run and inventory all findings; earlier
discoverability additions may exceed copy limits. Do not treat the 22 classified
static action pairs as narrative acceptance. Next review World 1 route hints and
quest terminology, then Worlds 2–6. Preserve action references when shortening
copy and distinguish static graph checks from player route discovery.
