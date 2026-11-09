import io
import json
import plistlib
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from ios_simulator import (
    boot,
    clean_abandoned,
    cleanup,
    delete,
    has_live_owner,
    main,
    owner_identity,
    shutdown,
)


class SimulatorCleanupTests(unittest.TestCase):
    def test_requested_model_creates_a_new_device_on_the_pinned_runtime(self):
        runtime = 'com.apple.CoreSimulator.SimRuntime.iOS-26-5'
        model = 'com.apple.CoreSimulator.SimDeviceType.iPhone-SE-3rd-generation'
        existing = {'devices': {runtime: [{
            'isAvailable': True, 'deviceTypeIdentifier': 'com.apple.CoreSimulator.SimDeviceType.iPhone-16e',
        }]}}
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'output'
            with patch.dict('os.environ', {'IOS_SIMULATOR_VERSION': '26.5',
                    'IOS_SIMULATOR_DEVICE': model, 'GITHUB_OUTPUT': str(output)}), \
                    patch('ios_simulator.subprocess.check_output', side_effect=['26.5', json.dumps(existing)]), \
                    patch('ios_simulator.owner_identity', return_value={'pid': 123}), \
                    patch('ios_simulator.create', return_value='new-owned-device') as create:
                main()
            self.assertEqual(create.call_args.args[1:3], (model, runtime))
            self.assertIn('id=new-owned-device', output.read_text())

    def test_device_specific_runtimes_are_skipped(self):
        # The xcode-27 image lists a runtime for one device type beside the plain ones.
        runtime = 'com.apple.CoreSimulator.SimRuntime.iOS-27-0'
        phone = {'isAvailable': True, 'deviceTypeIdentifier': 'com.apple.CoreSimulator.SimDeviceType.iPhone-17'}
        existing = {'devices': {
            'com.apple.CoreSimulator.SimRuntime.iOS-27-1-iPhone19-2': [dict(phone)],
            runtime: [dict(phone)],
            'com.apple.CoreSimulator.SimRuntime.watchOS-12-0': [],
        }}
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'output'
            with patch.dict('os.environ', {'IOS_SIMULATOR_VERSION': '', 'GITHUB_OUTPUT': str(output)}), \
                    patch('ios_simulator.subprocess.check_output', side_effect=['27.1', json.dumps(existing)]), \
                    patch('ios_simulator.owner_identity', return_value={'pid': 123}), \
                    patch('ios_simulator.create', return_value='new-owned-device') as create:
                main()
            self.assertEqual(create.call_args.args[2], runtime)

    def test_boot_waits_and_also_handles_an_already_booted_device(self):
        with patch('ios_simulator.subprocess.run', return_value=subprocess.CompletedProcess(
                ['simctl'], 0, 'Finished', '')) as run:
            boot('owned-device')
        self.assertEqual(run.call_args.args[0][-3:], ['bootstatus', 'owned-device', '-b'])
        self.assertEqual(run.call_args.kwargs['timeout'], 600)

    def test_migration_diagnostic_does_not_override_successful_exit(self):
        result = subprocess.CompletedProcess(['simctl'], 0,
            'Status=3, isTerminal=YES\nData Migration Failed\n', '')
        with patch('ios_simulator.subprocess.run', return_value=result):
            boot('owned-device')

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
        data = {'devices': {'runtime': [{'name': 'iPhone 16e', 'udid': udid, 'state': 'Booted'}
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
        self.assertEqual([call.kwargs['timeout'] for call in run.call_args_list], [30, 60])

    def test_delete_retries_once_and_only_removes_registration_after_success(self):
        with tempfile.TemporaryDirectory() as directory:
            registry = Path(directory)
            record = registry / 'owned-device.json'
            record.write_text('{"pid": 123}')
            with patch('ios_simulator.REGISTRY', registry), \
                 patch('ios_simulator.subprocess.run', side_effect=[
                     subprocess.CompletedProcess(['simctl'], 0, '', ''),
                     subprocess.TimeoutExpired('simctl delete', 60), None,
                 ]) as run:
                delete('owned-device')
            self.assertFalse(record.exists())
            self.assertEqual([call.args[0][-2:] for call in run.call_args_list],
                             [['shutdown', 'owned-device'], ['delete', 'owned-device'], ['delete', 'owned-device']])
            self.assertEqual([call.kwargs['timeout'] for call in run.call_args_list], [30, 60, 60])

    def test_delete_failure_is_not_hidden_and_keeps_ownership(self):
        with tempfile.TemporaryDirectory() as directory:
            registry = Path(directory)
            record = registry / 'owned-device.json'
            record.write_text('{"pid": 123}')
            with patch('ios_simulator.REGISTRY', registry), \
                 patch('ios_simulator.subprocess.run', side_effect=[
                     subprocess.CompletedProcess(['simctl'], 0, '', ''),
                     subprocess.TimeoutExpired('simctl delete', 60),
                     subprocess.TimeoutExpired('simctl delete', 60),
                 ]) as run, self.assertRaises(subprocess.TimeoutExpired):
                delete('owned-device')
            self.assertTrue(record.exists())
            self.assertEqual(run.call_count, 3, 'deletion attempts must remain bounded')

    def test_cleanup_warns_only_on_disposable_github_hosted_vms(self):
        for actions, environment in [('true', 'github-hosted'), ('true', 'self-hosted'),
                                     ('false', 'github-hosted'), ('true', '')]:
            with self.subTest(actions=actions, environment=environment), \
                 patch.dict('os.environ', {'GITHUB_ACTIONS': actions, 'RUNNER_ENVIRONMENT': environment}), \
                 patch('ios_simulator.delete', side_effect=subprocess.TimeoutExpired('simctl delete', 60)), \
                 redirect_stdout(io.StringIO()) as output:
                if actions == 'true' and environment == 'github-hosted':
                    cleanup('owned-device')
                    self.assertIn('::warning::Simulator owned-device cleanup failed', output.getvalue())
                else:
                    with self.assertRaises(subprocess.TimeoutExpired):
                        cleanup('owned-device')
                    self.assertNotIn('::warning::', output.getvalue())

    def test_cleanup_success_uses_normal_deletion_on_all_runners(self):
        with patch('ios_simulator.delete') as remove:
            cleanup('owned-device')
        remove.assert_called_once_with('owned-device')

    def test_ci_cleanup_has_a_sufficient_budget_without_ignoring_test_failures(self):
        workflow = (Path(__file__).resolve().parents[3] / '.github/workflows/ios-app-tests.yml').read_text()
        self.assertEqual(workflow.count('--cleanup "$SIMULATOR_UDID"'), 2)
        for name in ['Delete build destination', 'Delete owned iOS Simulator']:
            step = workflow.split(f'- name: {name}')[1].split('\n      - ')[0]
            self.assertIn('timeout-minutes: 4', step)
            self.assertNotIn('continue-on-error', step)
        self.assertNotIn('continue-on-error', workflow)

    def test_cheap_job_checkout_includes_the_helpers_source_fixtures(self):
        workflow = (Path(__file__).resolve().parents[3] / '.github/workflows/ios.yml').read_text()
        self.assertIn('python3 -m unittest -b', workflow)
        checkout = workflow.split('sparse-checkout: |', 1)[1].split('- name:', 1)[0]
        for path in ['.github/workflows', 'scripts/ci', 'scripts/release/tests',
                     'iOS/tagmaster/tagmasterUITests']:
            self.assertIn(f'            {path}\n', checkout)

    def test_abandoned_capture_cleanup_preserves_other_devices(self):
        names = ['Store-pitchperfect-iphone', 'Store-tagmaster-ipad',
                 'iPhone 16e', 'Depollsoft-CI-waiting-job', 'Store-personal-iphone',
                 'Store-tagmaster-iphone-personal']
        data = {'devices': {'runtime': [{'name': name, 'udid': str(index), 'state': 'Booted'}
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
        data = {'devices': {'runtime': [{'name': 'Store-tagmaster-iphone', 'udid': str(index)}
                                        for index in range(2)]}}
        with patch('ios_simulator.subprocess.check_output', return_value=json.dumps(data)), \
             patch('ios_simulator.delete', side_effect=[
                 subprocess.TimeoutExpired('simctl delete', 30), None]) as remove, \
             self.assertRaisesRegex(RuntimeError, 'Could not clean'):
            clean_abandoned()
        self.assertEqual(remove.call_count, 2)

    def test_actions_owner_is_worker_across_shell_steps(self):
        identities = {
            30: {'pid': 30, 'parent': 20, 'started': 'child', 'command': 'python3'},
            20: {'pid': 20, 'parent': 10, 'started': 'shell', 'command': 'bash'},
            10: {'pid': 10, 'parent': 1, 'started': 'job', 'command': '/runner/bin/Runner.Worker'},
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
            data = {'devices': {'runtime': [{'name': 'Store-tagmaster-iphone', 'udid': udid}
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
