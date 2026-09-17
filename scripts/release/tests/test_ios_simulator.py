from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from ios_simulator import delete


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
