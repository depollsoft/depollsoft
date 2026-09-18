import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class InstrumentationIsolationTests(unittest.TestCase):
    def run_suite(self, fail_fixture=False):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            script = root / 'scripts/ci/android_instrumented.sh'
            script.parent.mkdir(parents=True)
            shutil.copyfile(Path(__file__).resolve().parents[2] / 'ci/android_instrumented.sh', script)
            android = root / 'Android'
            android.mkdir()
            gradle = android / 'gradlew'
            gradle.write_text('''#!/usr/bin/env python3
import json, os, pathlib, shutil, sys
args = sys.argv[1:]
with open('invocations.jsonl', 'a') as f:
    f.write(json.dumps(args) + '\\n')
if args[0] == 'createDebugAndroidTestCoverageReport':
    assert '-x' in args
    assert len(list(pathlib.Path('TagMaster/build/outputs/androidTest-results/connected').rglob('*.xml'))) == 3
    assert len(list(pathlib.Path('TagMaster/build/outputs/code_coverage/debugAndroidTest/connected').rglob('*.ec'))) == 3
    sys.exit(0)
selected = next((a.split('=', 1)[1] for a in args if a.startswith('-Pandroid.testInstrumentationRunnerArguments.class=')), 'shared')
for kind, relative in [('androidTest-results', 'connected/test.xml'), ('code_coverage', 'debugAndroidTest/connected/coverage.ec')]:
    base = pathlib.Path('TagMaster/build/outputs') / kind
    shutil.rmtree(base, ignore_errors=True)
    target = base / relative
    target.parent.mkdir(parents=True)
    target.write_text(selected)
sys.exit(1 if os.environ.get('FAIL_FIXTURE') and selected.endswith('BarberPoleQueryRegressionTest') else 0)
''')
            gradle.chmod(0o755)
            result = subprocess.run(['bash', str(script)], env=dict(os.environ, FAIL_FIXTURE='yes' if fail_fixture else ''),
                                    capture_output=True, text=True)
            calls = [json.loads(line) for line in (android / 'invocations.jsonl').read_text().splitlines()]
            return result, calls

    def test_isolates_global_handlers_and_preserves_all_results(self):
        result, calls = self.run_suite()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(calls), 4)
        for index, name in enumerate(['BarberPoleQueryRegressionTest', 'CompactLoadingRegressionTest']):
            self.assertIn('-Pandroid.testInstrumentationRunnerArguments.class=depollsoft.tagmaster.' + name, calls[index])
        self.assertIn('-Pandroid.testInstrumentationRunnerArguments.notClass=depollsoft.tagmaster.BarberPoleQueryRegressionTest,depollsoft.tagmaster.CompactLoadingRegressionTest', calls[2])
        self.assertTrue(all('--no-parallel' in call for call in calls))

    def test_failure_is_reported_after_collecting_remaining_results(self):
        result, calls = self.run_suite(fail_fixture=True)
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertEqual(len(calls), 4)
