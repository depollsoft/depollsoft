import io
import json
import subprocess
import sys
import tempfile
import unittest
from contextlib import chdir, redirect_stdout
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from ios_test_results import check_durations, junit, main, read_result, validate


class XcodeResultTests(unittest.TestCase):
    def test_real_passing_tests_can_include_skips(self):
        validate({'result': 'Passed', 'totalTestCount': 12, 'passedTests': 10,
                  'failedTests': 0, 'skippedTests': 2, 'testFailures': []})

    def test_empty_or_all_skipped_tests_never_pass(self):
        for total in [0, 12]:
            with self.subTest(total=total), self.assertRaises(ValueError):
                validate({'result': 'Passed', 'totalTestCount': total, 'passedTests': 0,
                          'failedTests': 0, 'skippedTests': total})

    def test_simulator_failure_cannot_be_hidden_by_zero_test_failures(self):
        with self.assertRaises(ValueError):
            validate({'result': 'Failed', 'totalTestCount': 10, 'passedTests': 10, 'failedTests': 0})

    def test_failure_details_are_not_ignored(self):
        with self.assertRaises(ValueError):
            validate({'result': 'Passed', 'totalTestCount': 10, 'passedTests': 10,
                      'failedTests': 0, 'testFailures': [{'failureText': 'Runner could not initialize'}]})

    def test_missing_summary_fields_fail_closed(self):
        with self.assertRaises(ValueError):
            validate({})

    def test_actual_cases_replace_empty_xcpretty_reports(self):
        tree = {'testNodes': [{'nodeType': 'Test Suite', 'name': 'ModelTests', 'children': [
            {'nodeType': 'Test Case', 'name': 'testGood', 'result': 'Passed'},
            {'nodeType': 'Test Case', 'name': 'testBad', 'result': 'Failed'},
            {'nodeType': 'Test Case', 'name': 'testOptIn', 'result': 'Skipped'},
        ]}]}
        suite = junit('app', tree).getroot().find('testsuite')
        self.assertEqual(suite.attrib, {'name': 'app', 'tests': '3', 'failures': '1', 'errors': '0', 'skipped': '1'})
        self.assertEqual(len(suite.findall('testcase')), 3)
        self.assertEqual(suite.find('testcase').get('classname'), 'ModelTests')

    def test_empty_xcode_tree_cannot_generate_passing_report(self):
        with self.assertRaises(ValueError):
            junit('app', {'testNodes': []})

    def test_slow_passing_test_still_exceeds_budget(self):
        tree = {'testNodes': [{'nodeType': 'Test Case', 'name': 'testSlow',
                              'result': 'Passed', 'durationInSeconds': 30.1}]}
        with self.assertRaisesRegex(ValueError, 'testSlow'):
            check_durations(junit('app', tree), 30)
        tree['testNodes'][0]['durationInSeconds'] = 2.5
        check_durations(junit('app', tree), 30)


class XcodeResultExportTests(unittest.TestCase):
    def setUp(self):
        self.summary = {'result': 'Passed', 'totalTestCount': 1, 'passedTests': 1,
                        'failedTests': 0, 'skippedTests': 0}
        self.tree = {'testNodes': [{'nodeType': 'Test Case', 'name': 'testGood',
                                   'result': 'Passed', 'durationInSeconds': 1}]}

    def test_timed_out_reads_retry_once_with_a_longer_bounded_budget(self):
        for kind in ['summary', 'tests']:
            with self.subTest(kind=kind), redirect_stdout(io.StringIO()), \
                 patch('ios_test_results.subprocess.check_output', side_effect=[
                     subprocess.TimeoutExpired('xcresulttool', 120), '{"value": 1}',
                 ]) as read:
                self.assertEqual(read_result('app', kind), {'value': 1})
                self.assertEqual(read.call_count, 2)
                self.assertTrue(all(call.kwargs['timeout'] == 120 for call in read.call_args_list))
                self.assertEqual(read.call_args_list[0].args, read.call_args_list[1].args)
                self.assertIn(kind, read.call_args.args[0])

    def test_non_timeout_tool_errors_and_malformed_json_are_not_retried(self):
        for response in [subprocess.CalledProcessError(1, 'xcresulttool'), 'invalid json']:
            with self.subTest(response=response), \
                 patch('ios_test_results.subprocess.check_output', side_effect=[response]) as read, \
                 self.assertRaises((subprocess.CalledProcessError, ValueError)):
                read_result('app', 'summary')
            self.assertEqual(read.call_count, 1)

    def test_both_exports_can_recover_without_bypassing_verification(self):
        with tempfile.TemporaryDirectory() as directory, chdir(directory), \
             redirect_stdout(io.StringIO()), \
             patch('ios_test_results.subprocess.check_output', side_effect=[
                 subprocess.TimeoutExpired('summary', 120), json.dumps(self.summary),
                 subprocess.TimeoutExpired('tests', 120), json.dumps(self.tree),
             ]):
            self.assertEqual(main(['app']), 0)
            self.assertEqual(json.loads(Path('app-summary.json').read_text()), self.summary)
            self.assertIn('name="testGood"', Path('app-junit.xml').read_text())

    def test_exhausted_export_retries_fail_and_remove_stale_passing_reports(self):
        for kind in ['summary', 'tests']:
            responses = [] if kind == 'summary' else [json.dumps(self.summary)]
            responses += [subprocess.TimeoutExpired('xcresulttool', 120)] * 2
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as directory, chdir(directory), \
                 redirect_stdout(io.StringIO()) as output, \
                 patch('ios_test_results.subprocess.check_output', side_effect=responses) as read:
                Path('app-summary.json').write_text('{"stale": true}')
                Path('app-junit.xml').write_text('<stale/>')
                self.assertEqual(main(['app']), 1)
                self.assertIn('::error::app:', output.getvalue())
                self.assertFalse(Path('app-junit.xml').exists())
                if kind == 'summary':
                    self.assertFalse(Path('app-summary.json').exists())
                self.assertEqual(read.call_count, 2 if kind == 'summary' else 3)

    def test_failed_empty_all_skipped_and_slow_tests_still_fail(self):
        summaries = [
            {**self.summary, 'result': 'Failed', 'failedTests': 1},
            {**self.summary, 'totalTestCount': 0, 'passedTests': 0},
            {**self.summary, 'passedTests': 0, 'skippedTests': 1},
        ]
        fixtures = [(summary, self.tree) for summary in summaries]
        fixtures += [(self.summary, {'testNodes': []}),
                     (self.summary, {'testNodes': [{**self.tree['testNodes'][0], 'durationInSeconds': 31}]})]
        for summary, tree in fixtures:
            with self.subTest(summary=summary, tree=tree), tempfile.TemporaryDirectory() as directory, \
                 chdir(directory), redirect_stdout(io.StringIO()), \
                 patch.dict('os.environ', {'IOS_MAX_TEST_SECONDS': '30'}), \
                 patch('ios_test_results.subprocess.check_output',
                       side_effect=[json.dumps(summary), json.dumps(tree)]):
                self.assertEqual(main(['app']), 1)
