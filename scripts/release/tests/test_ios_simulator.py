from pathlib import Path
import json
import plistlib
import tempfile
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from ios_simulator import delete, clean_abandoned_captures


class SimulatorCleanupTests(unittest.TestCase):
    def test_delete_is_attempted_after_shutdown_hangs(self):
        with patch('ios_simulator.subprocess.run', side_effect=[
            subprocess.TimeoutExpired('simctl shutdown', 30), None,
        ]) as run:
            delete('owned-device')
        self.assertEqual([call.args[0][-2:] for call in run.call_args_list],
                         [['shutdown', 'owned-device'], ['delete', 'owned-device']])
        self.assertTrue(all(call.kwargs['timeout'] == 30 for call in run.call_args_list))

    def test_delete_failure_is_not_hidden(self):
        with patch('ios_simulator.subprocess.run', side_effect=[
            None, subprocess.TimeoutExpired('simctl delete', 30),
        ]), self.assertRaises(subprocess.TimeoutExpired):
            delete('owned-device')

    def test_abandoned_capture_cleanup_preserves_other_devices(self):
        names = ['Store-pitchperfect-iphone', 'Store-tagmaster-ipad',
                 'iPhone 16e', 'Depollsoft-CI-waiting-job', 'Store-personal-iphone',
                 'Store-tagmaster-iphone-personal']
        data = {'devices': {'runtime': [dict(name=name, udid=str(index), state='Booted')
                                        for index, name in enumerate(names)]}}
        with patch('ios_simulator.subprocess.check_output', return_value=json.dumps(data)), \
                patch('ios_simulator.delete') as remove:
            clean_abandoned_captures()
        self.assertEqual([call.args[0] for call in remove.call_args_list], ['0', '1'])

    def test_cleanup_recovers_names_from_metadata_when_discovery_hangs(self):
        with tempfile.TemporaryDirectory() as directory:
            home = Path(directory)
            devices = home / 'Library/Developer/CoreSimulator/Devices'
            for udid, name in [('owned', 'Store-tagmaster-iphone'), ('personal', 'iPhone 16e')]:
                path = devices / udid / 'device.plist'
                path.parent.mkdir(parents=True)
                path.write_bytes(plistlib.dumps({'UDID': udid, 'name': name}))
            with patch('ios_simulator.Path.home', return_value=home), \
                 patch('ios_simulator.subprocess.check_output',
                       side_effect=subprocess.TimeoutExpired('simctl list', 60)), \
                 patch('ios_simulator.delete') as remove:
                clean_abandoned_captures()
            remove.assert_called_once_with('owned')

    def test_cleanup_attempts_remaining_devices_after_one_failure(self):
        data = {'devices': {'runtime': [dict(name='Store-tagmaster-iphone', udid=str(index))
                                        for index in range(2)]}}
        with patch('ios_simulator.subprocess.check_output', return_value=json.dumps(data)), \
             patch('ios_simulator.delete', side_effect=[
                 subprocess.TimeoutExpired('simctl delete', 30), None]) as remove:
            with self.assertRaisesRegex(RuntimeError, 'Could not clean'):
                clean_abandoned_captures()
        self.assertEqual(remove.call_count, 2)
