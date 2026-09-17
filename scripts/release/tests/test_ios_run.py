from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest
from unittest.mock import patch

CI = Path(__file__).resolve().parents[2] / 'ci'


class NativeRunnerTests(unittest.TestCase):
    def test_stack_capture_only_samples_apps_on_its_own_device(self):
        sys.path.insert(0, str(CI))
        import ios_run
        processes = ('12 /CoreSimulator/Devices/owned/data/Containers/Bundle/Application/A/App.app/App\n'
                     '13 /CoreSimulator/Devices/other/data/Containers/Bundle/Application/B/App.app/App\n'
                     '14 /usr/bin/something\n')
        with patch('ios_run.subprocess.check_output', return_value=processes), \
             patch('ios_run.subprocess.run') as sample:
            ios_run.sample_simulator_apps('owned')
        self.assertEqual(sample.call_count, 1)
        self.assertEqual(sample.call_args.args[0][:4], ['sample', '12', '1', '10'])
        self.assertEqual(sample.call_args.kwargs['timeout'], 4)

    def launch(self, marker, *, wait=False, status=0):
        child = ('from pathlib import Path; import time; '
                 f'Path({str(marker)!r}).touch(); '
                 f'time.sleep({60 if wait else 0}); raise SystemExit({status})')
        code = (f'import sys; sys.path.insert(0, {str(CI)!r}); '
                'from ios_run import run; from pathlib import Path; '
                f'raise SystemExit(run([sys.executable, "-c", {child!r}]))')
        process = subprocess.Popen([sys.executable, '-c', code], stdout=subprocess.DEVNULL,
                                   stderr=subprocess.DEVNULL)
        self.addCleanup(self.stop, process)
        return process

    @staticmethod
    def stop(process):
        if process.poll() is None:
            process.terminate()
        process.wait(timeout=15)

    def wait_for(self, marker):
        deadline = time.monotonic() + 5
        while not marker.exists() and time.monotonic() < deadline:
            time.sleep(.01)
        self.assertTrue(marker.exists())

    def test_independent_commands_run_concurrently_and_cancel_independently(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            first = self.launch(root / 'first', wait=True)
            self.wait_for(root / 'first')
            second = self.launch(root / 'second', wait=True)
            self.wait_for(root / 'second')
            self.assertIsNone(first.poll())
            self.assertIsNone(second.poll())
            self.stop(first)
            self.assertIsNone(second.poll())
            self.stop(second)

    def test_first_failed_case_stops_command_before_more_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            marker = Path(directory) / 'should-not-run'
            child = ("import time; from pathlib import Path; "
                     "print(\"Test Case '-[Example testFailure]' failed (0.01 seconds).\", flush=True); "
                     f'time.sleep(60); Path({str(marker)!r}).touch()')
            code = (f'import sys; sys.path.insert(0, {str(CI)!r}); '
                    'from ios_run import run; from pathlib import Path; '
                    f'raise SystemExit(run([sys.executable, "-c", {child!r}], fail_fast=True))')
            result = subprocess.run([sys.executable, '-c', code], capture_output=True,
                                    text=True, timeout=5)
            self.assertEqual(result.returncode, 1)
            self.assertIn('Stopping after the first failed test', result.stdout)
            self.assertFalse(marker.exists())

    def test_startup_and_individual_test_timeouts(self):
        for output, expected in [('', 'did not start'),
                                 ("Test Case '-[Example testHang]' started.", 'test budget')]:
            with self.subTest(output=output), tempfile.TemporaryDirectory() as directory:
                child = f'import time; print({output!r}, flush=True); time.sleep(60)'
                code = (f'import sys; sys.path.insert(0, {str(CI)!r}); '
                        'from ios_run import run; from pathlib import Path; '
                        f'raise SystemExit(run([sys.executable, "-c", {child!r}], '
                        'fail_fast=True, '
                        'startup_timeout=.2, test_timeout=.2, shutdown_timeout=.2))')
                result = subprocess.run([sys.executable, '-c', code], capture_output=True,
                                        text=True, timeout=5)
                self.assertEqual(result.returncode, 1)
                self.assertIn(expected, result.stdout)

    def test_unresponsive_command_is_killed_after_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            child = ('import signal, time; signal.signal(signal.SIGINT, signal.SIG_IGN); '
                     'print("Test Case \'-[Example testFailure]\' failed (0.01 seconds).", flush=True); '
                     'time.sleep(60)')
            code = (f'import sys; sys.path.insert(0, {str(CI)!r}); '
                    'from ios_run import run; from pathlib import Path; '
                    f'raise SystemExit(run([sys.executable, "-c", {child!r}], '
                    'fail_fast=True, shutdown_timeout=.2))')
            result = subprocess.run([sys.executable, '-c', code], capture_output=True,
                                    text=True, timeout=5)
            self.assertEqual(result.returncode, 1)
            self.assertIn('Stopping after the first failed test', result.stdout)

    def test_inherited_output_pipe_cannot_keep_finished_command_running(self):
        with tempfile.TemporaryDirectory() as directory:
            child = ('import subprocess, sys; '
                     'subprocess.Popen([sys.executable, "-c", "import time; time.sleep(60)"]); '
                     'print("Parent finished", flush=True)')
            code = (f'import sys; sys.path.insert(0, {str(CI)!r}); '
                    'from ios_run import run; from pathlib import Path; '
                    f'raise SystemExit(run([sys.executable, "-c", {child!r}]))')
            result = subprocess.run([sys.executable, '-c', code], capture_output=True,
                                    text=True, timeout=5)
            self.assertEqual(result.returncode, 0)
            self.assertIn('Parent finished', result.stdout)

    def test_owned_simulator_stops_on_command_failure(self):
        sys.path.insert(0, str(CI))
        import ios_run
        udid = '12345678-1234-1234-1234-123456789ABC'
        with patch('ios_run.ios_simulator.boot') as boot, \
             patch('ios_run.ios_simulator.shutdown') as shutdown:
            status = ios_run.run([sys.executable, '-c', 'raise SystemExit(7)',
                                  f'platform=iOS Simulator,id={udid}'])
        self.assertEqual(status, 7)
        boot.assert_called_once_with(udid)
        shutdown.assert_called_once_with(udid)
