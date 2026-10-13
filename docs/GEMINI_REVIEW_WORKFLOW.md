# Gemini review exports

Run `python scripts/export_gemini_review.py` from the main checkout. It generates
unique `full-source` and `dialogue-autosave` folders under `output/gemini-review/`.
Upload one using Gemini's **Import Code > Upload folder**, then submit the included
`REVIEW_PROMPT.md`. The focused package starts with dialogue conditions and Android
autosave coverage; request the full folder if missing dependencies matter.

If Gemini shows a red upload error for a folder, use `text-uploads/` in the
same timestamped export instead. Choose ordinary **Upload files** and attach
the numbered `.txt` files in `dialogue-autosave` (recommended first) or
`full-source`. Paste that package's `REVIEW_PROMPT.txt` into the message.
Original paths, hashes, and complete file boundaries are preserved, with no
source files split across parts. Each part is at most 3 MiB. Upload acceptance
does not guarantee all source fits the model context: narrow the task if warned.
The red icon's actual error text is needed to distinguish unsupported types,
the ordinary-folder file limit, and account/storage limits.

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
