from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import wear


class WatchReadinessTests(unittest.TestCase):
    def test_killed_inspector_does_not_accept_stale_hierarchy(self):
        inspections = 0
        def run(*args, **kwargs):
            nonlocal inspections
            if 'uiautomator' in args:
                inspections += 1
                if inspections == 1:
                    raise subprocess.CalledProcessError(137, args)
        with patch.object(wear, 'run', side_effect=run), patch.object(wear.time, 'sleep'), \
                patch.object(wear, 'output', return_value='<node resource-id="pitchInstrument" /> octave 4') as output:
            wear.wait_for_instrument(['adb', '-s', 'emulator-5554'])
        self.assertEqual(inspections, 2)
        output.assert_called_once()  # Only the new, successful dump can supply readiness.

    def test_persistent_inspection_failure_is_bounded(self):
        with patch.object(wear, 'run', side_effect=subprocess.TimeoutExpired('uiautomator', 20)), \
                patch.object(wear.time, 'sleep'), \
                patch.object(wear.time, 'monotonic', side_effect=[0, 0, 121]), \
                self.assertRaisesRegex(ValueError, 'not visible'):
            wear.wait_for_instrument(['adb'])

    def test_view_era_id_alone_is_not_ready(self):
        # The Compose face has no View id; only its test tag counts.
        with patch.object(wear, 'run'), \
                patch.object(wear, 'output', return_value='resource-id="depollsoft.pitchperfect:id/other" octave 4'), \
                patch.object(wear.time, 'sleep'), \
                patch.object(wear.time, 'monotonic', side_effect=[0, 0, 121]), \
                self.assertRaisesRegex(ValueError, 'not visible'):
            wear.wait_for_instrument(['adb'])

    def test_non_instrument_screen_is_never_ready(self):
        with patch.object(wear, 'run'), patch.object(wear, 'output', return_value='Watch setup screen'), \
                patch.object(wear.time, 'sleep'), \
                patch.object(wear.time, 'monotonic', side_effect=[0, 0, 121]), \
                self.assertRaisesRegex(ValueError, 'Watch setup screen'):
            wear.wait_for_instrument(['adb'])
