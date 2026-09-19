#!/usr/bin/env python3
"""Partition native tests across runners and verify all expected results."""
import argparse
import json
import os
import plistlib
from pathlib import Path
import xml.etree.ElementTree as ET

import ios_run
from ios_test_results import check_durations, validate

POLISH = 'tagmasterUITests/TagMasterPolishUITests'
LISTS = ['tagmasterUITests/FavoritesUITests', 'tagmasterUITests/SearchUITests']
SUITES = {
    'pitchperfect': ('pitchperfect', ['pitchperfectTests'], []),
    'tagmaster': ('tagmaster', ['tagmasterTests'], []),
    'pitchperfectlib': ('pitchperfectlib', [], []),
    'tagmaster-ui-layout': ('tagmaster', [POLISH], []),
    'tagmaster-ui-lists': ('tagmaster', LISTS, []),
    # The complement includes new classes automatically, exactly once.
    'tagmaster-ui-other': ('tagmaster', ['tagmasterUITests'], [POLISH, *LISTS]),
    'pitchperfect-ui': ('pitchperfectUITests', ['pitchperfectUITests'], []),
}


def case_timeout(suite):
    return 90 if "-ui" in suite else 30


def matrix(extended=False, app=None):
    return {'include': [{'suite': suite, 'timeout': case_timeout(suite)} for suite in SUITES
                        if (extended or suite != 'pitchperfect-ui')
                        and (app is None or suite.startswith(app))]}


def manifest(scheme, products):
    runs = list(products.glob(f'{scheme}_*.xctestrun'))
    if len(runs) != 1:
        raise ValueError(f'Expected one {scheme} test configuration, found {len(runs)}')
    return runs[0]


def validate_products(selection, products=Path('DerivedData/Build/Products')):
    for scheme in {SUITES[entry['suite']][0] for entry in selection['include']}:
        path = manifest(scheme, products)
        data = plistlib.loads(path.read_bytes())
        coverage = (data.get('CodeCoverageBuildableInfos') or
                    data.get('__xctestrun_metadata__', {}).get('CodeCoverageBuildableInfos'))
        if not coverage:
            raise ValueError(f'{path}: missing coverage metadata; build for a concrete destination with coverage enabled')


def command(suite, destination, products=Path('DerivedData/Build/Products')):
    scheme, only, skip = SUITES[suite]
    path = manifest(scheme, products)
    return ['xcodebuild', 'test-without-building', '-xctestrun', str(path),
            '-destination', destination, '-enableCodeCoverage', 'YES',
            '-resultBundlePath', f'{suite}-results.xcresult',
            '-parallel-testing-enabled', 'NO', '-test-timeouts-enabled', 'YES',
            '-default-test-execution-time-allowance', str(case_timeout(suite)),
            '-maximum-test-execution-time-allowance', str(case_timeout(suite)),
            *[f'-only-testing:{item}' for item in only],
            *[f'-skip-testing:{item}' for item in skip],
            'CODE_SIGNING_ALLOWED=NO', 'SDKROOT=iphonesimulator']


def summarize(directory, expected):
    rows = ['### iOS CI', '', '| Suite | Passed | Skipped | Line coverage |',
            '| --- | ---: | ---: | ---: |']
    errors = []
    seen_ui = set()
    for entry in expected['include']:
        suite = entry['suite']
        try:
            summary = json.loads((directory / f'{suite}-summary.json').read_text())
            validate(summary)
            report = ET.parse(directory / f'{suite}-junit.xml')
            check_durations(report, case_timeout(suite))
            if '-ui-' in suite or suite.endswith('-ui'):
                for case in report.findall('.//testcase'):
                    identity = (suite.split('-ui')[0], case.get('classname'), case.get('name'))
                    if identity in seen_ui:
                        raise ValueError(f'Duplicate UI test across groups: {identity}')
                    seen_ui.add(identity)
            coverage = json.loads((directory / f'{suite}-coverage.json').read_text())
            rows.append(f'| {suite} | {summary["passedTests"]} | '
                        f'{summary.get("skippedTests", 0)} | {coverage["lineCoverage"]:.2%} |')
        except (OSError, ValueError, KeyError, ET.ParseError) as error:
            errors.append(f'{suite}: {error}')
            rows.append(f'| {suite} | Missing or failed | | |')
    if errors:
        rows.extend(['', *[f'- {error}' for error in errors]])
    return '\n'.join(rows) + '\n', errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='action', required=True)
    selection = sub.add_parser('matrix')
    selection.add_argument('--extended', action='store_true')
    selection.add_argument('--app', choices=['pitchperfect', 'tagmaster'])
    products = sub.add_parser('validate-products')
    products.add_argument('--app', choices=['pitchperfect', 'tagmaster'], required=True)
    products.add_argument('--extended', action='store_true')
    runner = sub.add_parser('run')
    runner.add_argument('suite', choices=SUITES)
    runner.add_argument('destination')
    summary = sub.add_parser('summarize')
    summary.add_argument('directory', type=Path)
    args = parser.parse_args()
    if args.action == 'matrix':
        print(json.dumps(matrix(args.extended, args.app)))
        return 0
    if args.action == 'validate-products':
        validate_products(matrix(args.extended, args.app))
        return 0
    if args.action == 'run':
        return ios_run.run(command(args.suite, args.destination), fail_fast=True,
                           test_timeout=case_timeout(args.suite),
                           keep_simulator_booted=True)
    text, errors = summarize(args.directory, json.loads(os.environ['IOS_TEST_MATRIX']))
    Path('ios-ci-comment.md').write_text(text)
    print(text)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as output:
            output.write(text)
    return int(bool(errors))


if __name__ == '__main__':
    raise SystemExit(main())
