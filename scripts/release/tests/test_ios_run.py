from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest

CI = Path(__file__).resolve().parents[2] / 'ci'


class SimulatorLockTests(unittest.TestCase):
    def launch(self, lock, marker, *, wait=False, status=0):
        child = ('from pathlib import Path; import time; '
                 f'Path({str(marker)!r}).touch(); '
                 f'time.sleep({60 if wait else 0}); raise SystemExit({status})')
        code = (f'import sys; sys.path.insert(0, {str(CI)!r}); '
                'from ios_run import run; from pathlib import Path; '
                f'raise SystemExit(run([sys.executable, "-c", {child!r}], Path({str(lock)!r})))')
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

    def test_exclusive_slot_is_released_on_cancellation_and_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            lock = root / 'lock'
            first = self.launch(lock, root / 'first', wait=True)
            self.wait_for(root / 'first')
            second = self.launch(lock, root / 'second', status=7)
            # The first command owns the slot until it has actually stopped.
            time.sleep(.1)
            self.assertFalse((root / 'second').exists())
            self.stop(first)
            self.assertEqual(second.wait(timeout=5), 7)
            third = self.launch(lock, root / 'third')
            self.assertEqual(third.wait(timeout=5), 0)
            self.assertTrue((root / 'third').exists())

    def test_first_failed_case_stops_command_before_more_tests(self):
        with tempfile.TemporaryDirectory() as directory:
            marker = Path(directory) / 'should-not-run'
            child = ("import time; from pathlib import Path; "
                     "print(\"Test Case '-[Example testFailure]' failed (0.01 seconds).\", flush=True); "
                     f'time.sleep(60); Path({str(marker)!r}).touch()')
            code = (f'import sys; sys.path.insert(0, {str(CI)!r}); '
                    'from ios_run import run; from pathlib import Path; '
                    f'raise SystemExit(run([sys.executable, "-c", {child!r}], '
                    f'Path({str(Path(directory) / "lock")!r}), fail_fast=True))')
            result = subprocess.run([sys.executable, '-c', code], capture_output=True,
                                    text=True, timeout=5)
            self.assertEqual(result.returncode, 1)
            self.assertIn('Stopping after the first failed test', result.stdout)
            self.assertFalse(marker.exists())
