# Gemini review exports

Run `python scripts/export_gemini_review.py` from the main checkout. It generates
unique `full-source` and `dialogue-autosave` folders under `output/gemini-review/`.
Upload one using Gemini's **Import Code > Upload folder**, then submit the included
`REVIEW_PROMPT.md`. The focused package starts with dialogue conditions and Android
autosave coverage; request the full folder if missing dependencies matter.

Exports use tracked working-tree files and explicit source/text allowlists.
Credentials/local configuration, save-slot JSON, binary media, archives, build
outputs, and caches are excluded. Symlinks are rejected. Known credential
patterns block generation rather than printing secret values. Every final file
is scanned again. These heuristic checks cannot detect every possible secret.

The manifest records the commit, branch, tracked changes, included-file hashes,
exclusions, and scan result. `ASSET_PATHS.json` preserves omitted media paths and
sizes. Each folder must stay below 5,000 files and 90 MiB. Exports are review
snapshots rather than compilable checkouts: the Gradle wrapper JAR and game media
are intentionally absent.

The exporter never deletes or overwrites previous exports. Re-run after changes
and use the newest timestamp. Add intended new source files to Git before
exporting; untracked files never enter a package. No second remote repository is
needed. Review Gemini's proposed diff against the main checkout, run meaningful
tests, and send actual results back for follow-up review.

Do not reuse `starborn_source_clean.zip`: the older exporter included the local
service-account private key. Replace/revoke that key in Google Cloud. This exporter
does not modify account credentials or existing archives.
