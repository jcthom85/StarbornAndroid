"""Regression checks for review-export boundaries and credential rejection."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import export_gemini_review as exporter


class ReviewExportTest(unittest.TestCase):
    def test_credentials_and_personal_saves_are_excluded(self):
        for path in ("play-service-account.json", "app/google-services.json",
                     "app/src/main/assets/autosave.json", "app/src/main/assets/save3.json",
                     "app/.env.production", "app/upload.jks", "app/local.properties"):
            with self.subTest(path=path):
                self.assertIsNotNone(exporter.exclusion(path))

    def test_code_names_and_gameplay_data_are_preserved(self):
        for path in ("app/src/main/java/GameSaveRepository.kt",
                     "desktopApp/src/main/kotlin/DesktopKeyBindings.kt",
                     "app/src/main/assets/events.json", "gradle/libs.versions.toml"):
            with self.subTest(path=path):
                self.assertIsNone(exporter.exclusion(path))

    def test_generated_binary_and_escaping_paths_are_excluded(self):
        for path in ("../outside.kt", "/outside.kt", "app/build/Generated.kt",
                     "app/src/main/assets/backups/rooms.json", "app/src/main/assets/room.webp"):
            with self.subTest(path=path):
                self.assertIsNotNone(exporter.exclusion(path))

    def test_secret_patterns_detect_embedded_keys_without_flagging_references(self):
        self.assertTrue(exporter.secret_findings("-----BEGIN " + "PRIVATE KEY-----"))
        self.assertTrue(exporter.secret_findings("gh" + "p_" + "a" * 36))
        self.assertTrue(exporter.secret_findings('api_key = "' + "a" * 32 + '"'))
        self.assertEqual([], exporter.secret_findings("os.environ['API_KEY']; credentials_path"))

    def fake_git(self, *args):
        if args[0] == "ls-files":
            return b"app/src/main/assets/events.json\0app/google-services.json\0"
        if args[0] == "rev-parse":
            return b"fixture-branch\n" if "--abbrev-ref" in args else b"fixture-commit\n"
        return b""

    def test_export_uses_tracked_allowlist_and_preserves_manifest_hashes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            events = root / "app/src/main/assets/events.json"
            events.parent.mkdir(parents=True)
            events.write_text("[]", encoding="utf-8")
            (root / "app/google-services.json").write_text("{}", encoding="utf-8")
            (root / "untracked.json").write_text("{}", encoding="utf-8")
            with patch.object(exporter, "ROOT", root), patch.object(exporter, "git", self.fake_git), \
                    patch("sys.argv", ["export", "--output-parent", str(root / "output")]), \
                    contextlib.redirect_stdout(io.StringIO()):
                exporter.main()
            package = next((root / "output").glob("*/full-source"))
            self.assertEqual("[]", (package / "app/src/main/assets/events.json").read_text())
            self.assertFalse((package / "app/google-services.json").exists())
            self.assertFalse((package / "untracked.json").exists())
            manifest = json.loads((package / "REVIEW_MANIFEST.json").read_text())
            self.assertEqual("fixture-commit", manifest["commit"])
            self.assertFalse(manifest["untracked_files_included"])
            self.assertEqual(64, len(manifest["included_files"][0]["sha256"]))

    def test_embedded_secret_blocks_export_before_publication(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            events = root / "app/src/main/assets/events.json"
            events.parent.mkdir(parents=True)
            secret = "gh" + "p_" + "a" * 36
            events.write_text(json.dumps({"token": secret}), encoding="utf-8")
            with patch.object(exporter, "ROOT", root), patch.object(exporter, "git", self.fake_git), \
                    patch("sys.argv", ["export", "--output-parent", str(root / "output")]):
                with self.assertRaises(SystemExit) as error:
                    exporter.main()
            self.assertNotIn(secret, str(error.exception))
            self.assertFalse((root / "output").exists())

    def test_text_upload_preserves_complete_source_with_original_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            output = Path(tmp) / "focused"
            sources = {"app/src/File.kt": b"fun example() = 42\n",
                       "app/src/main/assets/events.json": b"[]"}
            with contextlib.redirect_stdout(io.StringIO()):
                exporter.write_text_uploads(output, sources,
                                            {"branch": "fixture", "commit": "abc"}, "review")
            parts = list(output.glob("starborn-*.txt"))
            self.assertEqual(1, len(parts))
            text = parts[0].read_text(encoding="utf-8")
            for path, source in sources.items():
                self.assertIn(f"=== FILE BEGIN: {path} ===", text)
                self.assertIn(source.decode("utf-8"), text)
                self.assertIn(f"=== FILE END: {path} ===", text)


if __name__ == "__main__":
    unittest.main()
