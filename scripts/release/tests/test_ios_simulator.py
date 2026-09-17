from pathlib import Path
import json
import plistlib
import tempfile
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from ios_simulator import boot, shutdown, delete, clean_abandoned, owner_identity, has_live_owner, process_identity


class SimulatorCleanupTests(unittest.TestCase):
    def test_boot_waits_and_also_handles_an_already_booted_device(self):
        with patch('ios_simulator.subprocess.run') as run:
            boot('owned-device')
        self.assertEqual(run.call_args.args[0][-3:], ['bootstatus', 'owned-device', '-b'])
        self.assertEqual(run.call_args.kwargs['timeout'], 180)

    def test_shutdown_accepts_stopped_devices_but_preserves_other_failures(self):
        for error in ['Unable to shutdown device in current state: Shutdown', 'Device unavailable']:
            result = subprocess.CompletedProcess(['simctl'], 149, '', error)
            with self.subTest(error=error), patch('ios_simulator.subprocess.run', return_value=result):
                if 'current state: Shutdown' in error:
                    shutdown('owned-device')
                else:
                    with self.assertRaises(subprocess.CalledProcessError):
                        shutdown('owned-device')

    def test_explicit_legacy_retirement_preserves_data_and_live_owners(self):
        data = {'devices': {'runtime': [dict(name='iPhone 16e', udid=udid, state='Booted')
                                        for udid in ['legacy', 'personal']]}}
        for live in [False, True]:
            with self.subTest(live=live), \
                 patch.dict('os.environ', {'IOS_LEGACY_SIMULATOR_UDID': 'legacy'}), \
                 patch('ios_simulator.subprocess.check_output', return_value=json.dumps(data)), \
                 patch('ios_simulator.has_live_owner', return_value=live), \
                 patch('ios_simulator.shutdown') as shutdown, patch('ios_simulator.delete') as remove:
                clean_abandoned()
                remove.assert_not_called()
                if live:
                    shutdown.assert_not_called()
                else:
                    shutdown.assert_called_once_with('legacy')

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
            subprocess.CompletedProcess(['simctl'], 0, '', ''), subprocess.TimeoutExpired('simctl delete', 30),
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
            clean_abandoned()
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
                clean_abandoned()
            remove.assert_called_once_with('owned')

    def test_cleanup_attempts_remaining_devices_after_one_failure(self):
        data = {'devices': {'runtime': [dict(name='Store-tagmaster-iphone', udid=str(index))
                                        for index in range(2)]}}
        with patch('ios_simulator.subprocess.check_output', return_value=json.dumps(data)), \
             patch('ios_simulator.delete', side_effect=[
                 subprocess.TimeoutExpired('simctl delete', 30), None]) as remove:
            with self.assertRaisesRegex(RuntimeError, 'Could not clean'):
                clean_abandoned()
        self.assertEqual(remove.call_count, 2)

    def test_actions_owner_is_worker_across_shell_steps(self):
        identities = {
            30: dict(pid=30, parent=20, started='child', command='python3'),
            20: dict(pid=20, parent=10, started='shell', command='bash'),
            10: dict(pid=10, parent=1, started='job', command='/runner/bin/Runner.Worker'),
        }
        with patch.dict('os.environ', {'GITHUB_ACTIONS': 'true'}), \
             patch('ios_simulator.os.getpid', return_value=30), \
             patch('ios_simulator.process_identity', side_effect=identities.get):
            self.assertEqual(owner_identity(), identities[10])

    def test_live_owners_survive_cleanup_but_reused_pids_do_not(self):
        with tempfile.TemporaryDirectory() as directory:
            registry = Path(directory)
            (registry / 'active.json').write_text(json.dumps({'pid': 10, 'started': 'today'}))
            (registry / 'reused.json').write_text(json.dumps({'pid': 10, 'started': 'yesterday'}))
            data = {'devices': {'runtime': [dict(name='Store-tagmaster-iphone', udid=udid)
                                            for udid in ['active', 'reused', 'unregistered']]}}
            with patch('ios_simulator.REGISTRY', registry), \
                 patch('ios_simulator.process_identity', return_value={'started': 'today'}), \
                 patch('ios_simulator.subprocess.check_output', return_value=json.dumps(data)), \
                 patch('ios_simulator.delete') as remove:
                clean_abandoned()
            self.assertEqual([call.args[0] for call in remove.call_args_list], ['reused', 'unregistered'])

    def test_exited_owner_is_not_live(self):
        with tempfile.TemporaryDirectory() as directory:
            registry = Path(directory)
            (registry / 'exited.json').write_text(json.dumps({'pid': 10, 'started': 'today'}))
            with patch('ios_simulator.REGISTRY', registry), \
                 patch('ios_simulator.process_identity', return_value=None):
                self.assertFalse(has_live_owner('exited'))
