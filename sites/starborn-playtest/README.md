# Starborn playtest companion

A dependency-free static reader for the Worlds 1–6 completionist guides.
Edit the source guides in `docs/playtest/walkthroughs/`, then rebuild:

```sh
node sites/starborn-playtest/build.mjs
```

The output is `dist/`. Open its index directly or serve the directory with a static
host. The hosted reader caches all six worlds for offline use after a successful
visit. Search, checkmarks, theme and reading position work locally. Progress belongs
to the browser and origin; clearing site data removes it.

## Rebuilt Worlds 3 and 4

The November 7 rebuild makes both canonical guides continuous completionist routes:
World 3 has 181 numbered steps, 70 rooms and 11 quests; World 4 has 236 numbered
steps, 74 rooms and 11 quests. Journal checks are additional checklist entries.
The older source editions are archived under
`docs/playtest/walkthroughs/archive/2026-11-07/`.

Worlds 1 and 2 retain their sources and saved progress. The rebuilt main World 3
uses a fresh progress namespace for a restart; legacy World 3 data remains stored.
Guide introductions distinguish source checks from actual device playtesting and
identify local game fixes that are not yet installed.

## Verification and publication

Live reader: https://jcthom85.github.io/StarbornAndroid/

Direct links `#world-1/` through `#world-6/` select a world on a fresh visit.
Pushing source changes to `main` does not publish the reader: rebuild and update
`gh-pages` too.

Serve `dist/` locally and run:

```sh
node sites/starborn-playtest/check.mjs http://127.0.0.1:4175/
```

Checks cover direct links, complete room references, saved checklists, preservation
of older progress, search, theme, mobile layout and offline reading. Chrome is
detected on macOS/Windows; `CHROME_PATH` overrides its executable.

The publisher requires Node, Git and an authenticated `gh` CLI. It rebuilds and
publishes only root static files to `gh-pages`, without committing the main tree:

```sh
node sites/starborn-playtest/publish.mjs
```

## Separate World 3 edition

The previously published independent edition remains available at
https://jcthom85.github.io/StarbornAndroid/world-3-integrated/#world-3/.
Its source is `WORLD_3_INTEGRATED_COMPLETIONIST_WALKTHROUGH.md`; rebuild with:

```sh
node sites/starborn-playtest/build.mjs --integrated-world3
```

Output goes to `dist/world-3-integrated/`, with independent saved progress and cache.
The root publisher preserves this subdirectory and does not publish its updates.

## World 5

Added November 12, 2026: 235 ordered route steps through all 81 rooms, all ten
Journal quests, optional discoveries and a separate saved checklist. Available at
https://jcthom85.github.io/StarbornAndroid/#world-5/. Earlier worlds retain their
progress keys and chapter links. Publish to gh-pages after rebuilding; updating
the Markdown source on main alone does not update the hosted reader.

## World 6

Added November 12, 2026: 187 numbered route steps through all 65 rooms, all ten
Journal quests, the two-form finale, ending callbacks and the postgame shore.
Link: https://jcthom85.github.io/StarbornAndroid/#world-6/.

Publication preserves the currently hosted Worlds 1–4 blocks and IDs exactly while
adding Worlds 5–6, because those older hosted editions differ from current source.
For matching local QA, pass `--preserve-existing /absolute/path/to/published-index.html`
to build.mjs. Explicitly updating the older editions requires a separate reviewed
publication; do not silently replace their content and invalidate saved progress.

## Visible-interaction audit

Before publishing Worlds 5 or 6, run
`./gradlew :desktopApp:test --tests '*World56WalkthroughReachabilityTest'`.
This checks the numbered inspection/use/open instructions with the game’s real
inline-link renderer, including explicit aliases and description variants. A raw
action definition is not proof of a tappable link. Room-only visits remain numbered
steps; dormant, unlinked passive actions are excluded from the room reference.
The inspection correction map retains the existing checklist IDs for these same
visits without preserving IDs for unrelated future instruction changes.
