from pathlib import Path
import signal
import socket
import subprocess
import sys
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from emulator import check_port_available, private_adb, stop_process_group


class EmulatorPortTests(unittest.TestCase):
    def listener(self):
        server = socket.socket()
        self.addCleanup(server.close)
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind(('127.0.0.1', 0))
        server.listen()
        return server, server.getsockname()[1]

    def test_rejects_a_running_emulator_port(self):
        _, port = self.listener()
        with self.assertRaises(OSError):
            check_port_available(port)

    def test_accepts_a_closed_connection_in_time_wait(self):
        server, port = self.listener()
        with socket.create_connection(('127.0.0.1', port), timeout=2) as client:
            connection, _ = server.accept()
            connection.close()  # Server initiates close, leaving its port in TIME_WAIT.
            self.assertEqual(client.recv(1), b'')
        server.close()
        check_port_available(port)


class EmulatorShutdownTests(unittest.TestCase):
    def test_denied_signal_does_not_hide_a_live_process(self):
        process = Mock()
        process.poll.return_value = None
        with patch('emulator.os.killpg', side_effect=PermissionError), self.assertRaises(PermissionError):
            stop_process_group(process)

    def test_sandbox_denial_after_process_exit_does_not_mask_capture_failure(self):
        process = Mock()
        process.poll.return_value = 0
        with patch('emulator.os.killpg', side_effect=PermissionError):
            stop_process_group(process)

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


class PrivateAdbTests(unittest.TestCase):
    def test_uses_one_private_server_for_emulator_clients_and_cleanup_after_failure(self):
        original = {'PATH': '/bin', 'ADB_SERVER_SOCKET': 'tcp:5037'}
        with patch('emulator.check_port_available'), patch('emulator.subprocess.run') as run:
            with self.assertRaisesRegex(RuntimeError, 'capture failed'):
                with private_adb(Path('/sdk'), 15560, original) as env:
                    self.assertEqual(env['ADB_SERVER_SOCKET'], 'tcp:15560')
                    self.assertEqual(env['ANDROID_ADB_SERVER_PORT'], '15560')
                    self.assertEqual(env['ADB_LOCAL_TRANSPORT_MAX_PORT'], '0')
                    self.assertEqual(env['PATH'].split(':')[0], '/sdk/platform-tools')
                    raise RuntimeError('capture failed')
        self.assertEqual(original['ADB_SERVER_SOCKET'], 'tcp:5037')
        self.assertEqual([call.args[0] for call in run.call_args_list],
                         [['/sdk/platform-tools/adb', 'start-server'], ['/sdk/platform-tools/adb', 'kill-server']])
        self.assertTrue(all(call.kwargs['env'] == env for call in run.call_args_list))

    def test_never_stops_an_existing_server_on_the_private_port(self):
        with patch('emulator.check_port_available', side_effect=OSError('occupied')), patch('emulator.subprocess.run') as run:
            with self.assertRaises(OSError):
                with private_adb(Path('/sdk'), 15560, {}):
                    self.fail('Must not acquire an occupied port')
        run.assert_not_called()
