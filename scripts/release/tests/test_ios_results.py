from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from ios_test_results import check_durations, junit, validate


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
