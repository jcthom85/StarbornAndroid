"""Acceptance safeguards for the shared runner; no device or app data required."""
import os
import subprocess
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import maestro_runner as runner


class ReportAcceptanceTest(unittest.TestCase):
    def report(self, xml):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'report.xml'
            path.write_text(xml)
            return runner.report_passes(path)

    def test_accepts_nonempty_success(self):
        self.assertTrue(self.report('<testsuites><testsuite tests="1"><testcase name="opening"/></testsuite></testsuites>'))

    def test_rejects_empty_failure_error_and_partial_skip(self):
        for contents in ('', '<testcase><failure/></testcase>', '<testcase><error/></testcase>',
                         '<testcase name="passed"/><testcase name="not run"><skipped/></testcase>'):
            with self.subTest(contents=contents):
                self.assertFalse(self.report('<testsuite>' + contents + '</testsuite>'))

    def test_rejects_summary_failure_even_when_case_nodes_omit_it(self):
        for attribute in ('failures', 'errors', 'skipped'):
            self.assertFalse(self.report(f'<testsuite {attribute}="1"><testcase/></testsuite>'))


class FailureEvidenceTest(unittest.TestCase):
    @patch.object(runner.subprocess, 'run')
    def test_disconnected_device_does_not_block_failure_reporting(self, run):
        run.side_effect = subprocess.TimeoutExpired('adb logcat', 10)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'logcat.txt'
            runner.capture_failure_log('adb', 'disconnected', path)
            self.assertIn('timed out after device disconnect', path.read_text())
        self.assertEqual(10, run.call_args.kwargs['timeout'])


class DeviceSelectionTest(unittest.TestCase):
    @patch.dict(os.environ, {}, clear=True)
    @patch.object(runner, 'output')
    def test_refuses_ambiguous_devices(self, output):
        output.return_value = 'List of devices attached\none\tdevice\ntwo\tdevice'
        with self.assertRaisesRegex(RuntimeError, 'exactly one'):
            runner.device('adb', None)

    @patch.dict(os.environ, {}, clear=True)
    @patch.object(runner, 'output')
    def test_refuses_offline_explicit_device(self, output):
        output.return_value = 'List of devices attached\none\toffline'
        with self.assertRaisesRegex(RuntimeError, 'offline'):
            runner.device('adb', 'one')

    @patch.dict(os.environ, {}, clear=True)
    @patch.object(runner, 'output')
    def test_selects_requested_device_and_requires_completed_boot(self, output):
        output.side_effect = ['one\tdevice\ntwo\tdevice', '1']
        self.assertEqual('two', runner.device('adb', 'two'))
        output.side_effect = ['one\tdevice', '0']
        with self.assertRaisesRegex(RuntimeError, 'finish booting'):
            runner.device('adb', 'one')

    def test_child_environment_does_not_change_parent(self):
        original = os.environ.copy()
        env = runner.environment(Path('/jdk/bin/java'), Path('/sdk'))
        self.assertEqual(original, dict(os.environ))
        self.assertEqual(str(Path('/jdk')), env['JAVA_HOME'])
        self.assertEqual(str(Path('/sdk')), env['ANDROID_HOME'])
        self.assertEqual(original.get('HOME'), env.get('HOME'))
        self.assertEqual(original.get('USERPROFILE'), env.get('USERPROFILE'))

class PortableSpriteBoundsTest(unittest.TestCase):
    def test_all_png_filters_preserve_known_alpha_baseline(self):
        import struct
        import zlib
        from validate_enemy_sprite_bounds import bottom_padding
        def chunk(kind, data):
            return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
        # A 2x5 RGBA PNG with independently specified filtered alpha bytes.
        # Decoded alpha rows: [0,0], [0,200], [200,0], [0,200], [0,0].
        filtered_alpha = [(0, 0, 0), (1, 0, 200), (2, 200, 56), (3, 156, 200), (4, 0, 56)]
        raw = b''.join(bytes([mode, 0, 0, 0, a, 0, 0, 0, b]) for mode, a, b in filtered_alpha)
        png = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 2, 5, 8, 6, 0, 0, 0)) +
               chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b''))
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'combat.png'
            path.write_bytes(png)
            self.assertAlmostEqual(0.2, bottom_padding(path, 8))
            with self.assertRaisesRegex(ValueError, 'no visible pixels'):
                bottom_padding(path, 201)


if __name__ == '__main__':
    unittest.main()
