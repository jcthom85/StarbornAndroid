#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import jsonschema


def load_json(path: Path):
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def load_schema(schema_path: Path, schemas_dir: Path):
    schema = load_json(schema_path)

    # Support local relative $ref like "quests.schema.json" in the bundle schema.
    store: dict[str, object] = {}
    try:
        for candidate in schemas_dir.glob("*.schema.json"):
            doc = load_json(candidate)
            store[candidate.name] = doc
            store[candidate.resolve().as_uri()] = doc
            doc_id = doc.get("$id") if isinstance(doc, dict) else None
            if isinstance(doc_id, str) and doc_id:
                store[doc_id] = doc
    except OSError:
        # Not fatal; we'll still attempt to resolve via base_uri.
        pass

    base_uri = schema_path.resolve().as_uri()
    resolver = jsonschema.RefResolver(base_uri=base_uri, referrer=schema, store=store)
    return schema, resolver


def validate(payload, schema, resolver: jsonschema.RefResolver, label: str) -> list[str]:
    validator = jsonschema.Draft202012Validator(schema, resolver=resolver)
    errors = sorted(validator.iter_errors(payload), key=lambda e: list(e.absolute_path))
    messages: list[str] = []
    for e in errors:
        loc = "$"
        if e.absolute_path:
            loc += "".join(
                f"[{p!r}]" if isinstance(p, str) else f"[{p}]"
                for p in e.absolute_path
            )
        messages.append(f"{label}: {loc}: {e.message}")
    return messages


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate AI-generated Starborn content JSON against strict runtime load contracts (JSON Schema)."
    )
    parser.add_argument(
        "input",
        type=Path,
        help="Path to JSON file to validate (either a bundle, or a single quests/events/dialogue file).",
    )
    parser.add_argument(
        "--schema",
        choices=["bundle", "quests", "events", "events_assets", "dialogue"],
        default="bundle",
        help="Which schema to validate against (default: bundle).",
    )
    parser.add_argument(
        "--schemas-dir",
        type=Path,
        default=Path("docs/schemas"),
        help="Directory containing the schema files (default: docs/schemas).",
    )
    args = parser.parse_args()

    schema_file = {
        "bundle": "ai_content_bundle.schema.json",
        "quests": "quests.schema.json",
        "events": "events.schema.json",
        "events_assets": "events.assets.schema.json",
        "dialogue": "dialogue.schema.json",
    }[args.schema]

    schema_path = (args.schemas_dir / schema_file).resolve()
    if not schema_path.exists():
        print(f"Schema not found: {schema_path}", file=sys.stderr)
        return 2

    input_path = args.input.resolve()
    if not input_path.exists():
        print(f"Input not found: {input_path}", file=sys.stderr)
        return 2

    payload = load_json(input_path)
    schema, resolver = load_schema(schema_path, schemas_dir=args.schemas_dir.resolve())
    errors = validate(payload, schema, resolver, label=str(input_path))
    if errors:
        for msg in errors:
            print(msg, file=sys.stderr)
        print(f"FAILED: {len(errors)} schema violation(s).", file=sys.stderr)
        return 1

    print(f"OK: {input_path} matches {schema_file}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
