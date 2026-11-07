# Starborn playtest companion

A dependency-free static reader for the Worlds 1–4 completionist guides.
Edit the source guides in `docs/playtest/walkthroughs/`, then rebuild:

```sh
node sites/starborn-playtest/build.mjs
```

The output is `dist/`. Open its index directly or serve the directory with a static
host. The hosted reader caches all four worlds for offline use after a successful
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

Direct links `#world-1/` through `#world-4/` select a world on a fresh visit.
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
