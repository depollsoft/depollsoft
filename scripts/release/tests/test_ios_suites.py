from pathlib import Path
import json
import plistlib
import re
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from ios_suites import SUITES, command, matrix, summarize, validate_products, case_timeout


class ParallelSuiteTests(unittest.TestCase):
    def test_apps_partition_all_suites_and_only_optional_ui_is_added(self):
        standard = {item['suite'] for item in matrix()['include']}
        pitch = {item['suite'] for item in matrix(app='pitchperfect')['include']}
        tag = {item['suite'] for item in matrix(app='tagmaster')['include']}
        self.assertFalse(pitch & tag)
        self.assertEqual(pitch | tag, standard)
        self.assertEqual({item['suite'] for item in matrix(True)['include']} - standard,
                         {'pitchperfect-ui'})

    def test_every_ui_class_including_new_classes_runs_exactly_once(self):
        root = Path(__file__).resolve().parents[3] / 'iOS/tagmaster/tagmasterUITests'
        classes = ['FutureRegressionTests']
        for source in root.glob('*.swift'):
            classes.extend(re.findall(r'class\s+(\w+)\s*:\s*(?:XCTestCase|TagMasterUITestCase)', source.read_text()))
        for name in classes:
            identifier = f'tagmasterUITests/{name}'
            owners = []
            for suite, (_, only, skip) in SUITES.items():
                if not suite.startswith('tagmaster-ui-'):
                    continue
                matches = lambda choices: any(identifier == item or identifier.startswith(item + '/')
                                               for item in choices)
                if matches(only) and not matches(skip):
                    owners.append(suite)
            self.assertEqual(len(owners), 1, (identifier, owners))

    def test_prebuilt_commands_never_resolve_packages_or_build_again(self):
        with tempfile.TemporaryDirectory() as directory:
            products = Path(directory)
            for suite, (scheme, only, skip) in SUITES.items():
                (products / f'{scheme}_iphonesimulator26.5-arm64.xctestrun').touch()
                args = command(suite, 'platform=iOS Simulator,id=owned', products)
                self.assertEqual(args[:2], ['xcodebuild', 'test-without-building'])
                self.assertNotIn('-workspace', args)
                self.assertIn(f'{suite}-results.xcresult', args)
                self.assertIn('-parallel-testing-enabled', args)
                self.assertEqual(args[args.index('-maximum-test-execution-time-allowance') + 1],
                                 str(case_timeout(suite)))
            (products / 'tagmaster_other.xctestrun').touch()
            with self.assertRaises(ValueError):
                command('tagmaster', 'owned', products)

    def test_generic_builds_without_coverage_metadata_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            path = folder / 'tagmaster_iphonesimulator26.5-arm64.xctestrun'
            selection = matrix(app='tagmaster')
            path.write_bytes(plistlib.dumps({'__xctestrun_metadata__': {'FormatVersion': 1}}))
            with self.assertRaisesRegex(ValueError, 'missing coverage metadata'):
                validate_products(selection, folder)
            for data in [
                {'__xctestrun_metadata__': {'CodeCoverageBuildableInfos': [{'Name': 'App'}]}},
                {'CodeCoverageBuildableInfos': [{'Name': 'App'}]},
            ]:
                path.write_bytes(plistlib.dumps(data))
                validate_products(selection, folder)

    def write_result(self, folder, suite, classname=None, duration=1):
        (folder / f'{suite}-summary.json').write_text(json.dumps({
            'result': 'Passed', 'totalTestCount': 1, 'passedTests': 1,
            'failedTests': 0, 'skippedTests': 0,
        }))
        (folder / f'{suite}-junit.xml').write_text(
            f'<testsuites><testsuite><testcase classname="{classname or suite}" '
            f'name="testExample" time="{duration}"/></testsuite></testsuites>')
        (folder / f'{suite}-coverage.json').write_text('{"lineCoverage": 0.5}')

    def test_missing_or_slow_results_cannot_pass_aggregate(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            expected = matrix(app='pitchperfect')
            self.write_result(folder, 'pitchperfect')
            _, errors = summarize(folder, expected)
            self.assertTrue(errors)
            self.write_result(folder, 'pitchperfectlib')
            _, errors = summarize(folder, expected)
            self.assertFalse(errors)
            self.write_result(folder, 'pitchperfectlib', duration=31)
            _, errors = summarize(folder, expected)
            self.assertTrue(errors)

    def test_ui_uses_a_bounded_budget_for_native_automation(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            expected = {'include': [{'suite': 'tagmaster-ui-layout'}]}
            self.write_result(folder, 'tagmaster-ui-layout', duration=60)
            self.assertFalse(summarize(folder, expected)[1])
            self.write_result(folder, 'tagmaster-ui-layout', duration=91)
            self.assertTrue(summarize(folder, expected)[1])
        for entry in matrix(True)['include']:
            self.assertEqual(entry['timeout'], 90 if '-ui' in entry['suite'] else 30)

    def test_duplicate_ui_cases_cannot_pass_aggregate(self):
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            expected = {'include': [{'suite': 'tagmaster-ui-layout'}, {'suite': 'tagmaster-ui-other'}]}
            for entry in expected['include']:
                self.write_result(folder, entry['suite'], classname='SameClass')
            _, errors = summarize(folder, expected)
            self.assertTrue(any('Duplicate UI test' in error for error in errors))
