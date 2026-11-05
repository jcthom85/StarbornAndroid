# Starborn playtest companion

A dependency-free static reader for the Worlds 1, 2 and 3 completionist guides.
The source guides live in `docs/playtest/walkthroughs/`, the same files bundled by
the Android app. Never edit the generated guide text in `dist/index.html`.

From the repository root, rebuild after editing the guides or reader:

```powershell
node sites/starborn-playtest/build.mjs
```

Open `dist/index.html` directly in Chrome for a self-contained offline copy, or
serve `dist/` using any static host. The hosted version caches all three worlds for
offline use after the first successful visit. Search, checkmarks, theme and saved
reading position work without a backend. Progress belongs to the browser and
origin; it does not sync to another device or the game. Clearing site data removes it.

The walkthrough text retains its authored review date and game version. Publishing
the companion does not mean its gameplay instructions have been revalidated.

## Publication

Live reader: https://jcthom85.github.io/StarbornAndroid/#world-2/

Pushing a walkthrough change to `main` does **not** update the live reader. Rebuild and publish `gh-pages` as well. The November 4 refresh adds World 3's current main route, eleven quest checklists and seventy-room reference, and refreshes Worlds 1 and 2 from their maintained sources. Direct `#world-1/`, `#world-2/` and `#world-3/` links select that world even on a fresh browser visit.

The November 5 World 3 refresh follows the NSEW navigation repair: all seventy room references, the heist and rooftop routes, optional-quest returns and Lens puzzle guidance match that build. Older game installations can still have different paths. Direct link: https://jcthom85.github.io/StarbornAndroid/#world-3/

Before publishing, serve `dist/` locally and run `node sites/starborn-playtest/check.mjs <url>`. It checks direct World 2 entry, current guide data, saved progress, search, mobile layout and offline reading. Chrome is detected for macOS/Windows; set `CHROME_PATH` to override its executable.


`publish.mjs` rebuilds the companion, commits only the generated static files to
the repository's `gh-pages` branch, pushes that branch and configures GitHub Pages
if needed. It requires Node, Git and an authenticated `gh` CLI. It never stages or
commits the main working tree. Run it only when intending to publish publicly:

```powershell
node sites/starborn-playtest/publish.mjs
```
