from pathlib import Path
import signal
import subprocess
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from emulator import stop_process_group


class EmulatorShutdownTests(unittest.TestCase):
    def start(self, code):
        process = subprocess.Popen([sys.executable, '-u', '-c', code],
                                   stdout=subprocess.PIPE, text=True, start_new_session=True)
        self.addCleanup(process.stdout.close)
        self.addCleanup(stop_process_group, process, 0.1)
        self.assertEqual(process.stdout.readline().strip(), 'ready')
        return process

    def test_shutdown_kills_a_stuck_process_without_touching_another_session(self):
        code = 'import signal,time; signal.signal(signal.SIGTERM, signal.SIG_IGN); print("ready"); time.sleep(60)'
        emulator = self.start(code)
        unrelated = self.start(code)
        stop_process_group(emulator, grace=0.1)
        self.assertEqual(emulator.returncode, -signal.SIGKILL)
        self.assertIsNone(unrelated.poll())

    def test_shutdown_closes_handles_inherited_by_children(self):
        emulator = self.start('''
import subprocess, sys, time
child = subprocess.Popen([sys.executable, '-u', '-c',
    'import signal,time; signal.signal(signal.SIGTERM,signal.SIG_IGN); print("ready"); time.sleep(60)'])
time.sleep(60)
''')
        stop_process_group(emulator, grace=0.1)
        # A leaked child would keep this pipe open even after its parent exited.
        import select
        readable, _, _ = select.select([emulator.stdout], [], [], 2)
        self.assertTrue(readable, 'An emulator child still holds the output pipe open')
        self.assertEqual(emulator.stdout.read(), '')
