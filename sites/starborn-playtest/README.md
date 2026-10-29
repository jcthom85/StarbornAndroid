# Starborn playtest companion

A dependency-free static reader for the World 1 and World 2 completionist guides.
The source guides live in `docs/playtest/walkthroughs/`, the same files bundled by
the Android app. Never edit the generated guide text in `dist/index.html`.

From the repository root, rebuild after editing the guides or reader:

```powershell
node sites/starborn-playtest/build.mjs
```

Open `dist/index.html` directly in Chrome for a self-contained offline copy, or
serve `dist/` using any static host. The hosted version caches both worlds for
offline use after the first successful visit. Search, checkmarks, theme and saved
reading position work without a backend. Progress belongs to the browser and
origin; it does not sync to another device or the game. Clearing site data removes it.

The walkthrough text retains its authored review date and game version. Publishing
the companion does not mean its gameplay instructions have been revalidated.

## Publication

Live reader: https://jcthom85.github.io/StarbornAndroid/#world-2/

Pushing a walkthrough change to `main` does **not** update the live reader. Rebuild and publish `gh-pages` as well. The October 29 refresh includes World 2's corrected NPC/quest route and Botanist report task. Direct `#world-1/` and `#world-2/` links select that world even on a fresh browser visit.

Before publishing, serve `dist/` locally and run `node sites/starborn-playtest/check.mjs <url>`. It checks direct World 2 entry, current guide data, saved progress, search, mobile layout and offline reading. Chrome is detected for macOS/Windows; set `CHROME_PATH` to override its executable.


`publish.mjs` rebuilds the companion, commits only the generated static files to
the repository's `gh-pages` branch, pushes that branch and configures GitHub Pages
if needed. It requires Node, Git and an authenticated `gh` CLI. It never stages or
commits the main working tree. Run it only when intending to publish publicly:

```powershell
node sites/starborn-playtest/publish.mjs
```
