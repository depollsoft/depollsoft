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

# Unit suites run on every PR. The `-ui` suites are the XCUITest residue that
# still needs a real app launch (launch metrics, software keyboard, rotation
# hit targets, system sheets, store screenshot capture); they run only with
# --extended (weekly schedule and workflow_dispatch). Everything else the UI
# bundles used to check now lives in the in-process unit targets.
SUITES = {
    'pitchperfect': ('pitchperfect', ['pitchperfectTests'], []),
    'tagmaster': ('tagmaster', ['tagmasterTests'], []),
    'pitchperfectlib': ('pitchperfectlib', [], []),
    'tagmaster-ui': ('tagmaster', ['tagmasterUITests'], []),
    'pitchperfect-ui': ('pitchperfectUITests', ['pitchperfectUITests'], []),
}


# Suites that share one runner job. Every job pays for checkout, artifact
# download and a simulator boot before its first test, so a suite whose tests
# finish in under a second (pitchperfectlib) rides along with its app's suite
# instead of occupying a macOS runner of its own.
JOBS = {'pitchperfectlib': 'pitchperfect'}


def is_ui(suite):
    return suite.endswith('-ui')


def case_timeout(suite):
    return 90 if is_ui(suite) else 30


def selected(extended=False, app=None):
    """Suites for one app (str), several apps (list) or every app (None)."""
    apps = None if app is None else tuple([app] if isinstance(app, str) else app)
    return [suite for suite in SUITES
            if (extended or not is_ui(suite)) and (apps is None or suite.startswith(apps))]


def matrix(extended=False, app=None):
    jobs = {}
    for suite in selected(extended, app):
        jobs.setdefault(JOBS.get(suite, suite), []).append(suite)
    return {'include': [{'job': job, 'suites': ' '.join(suites),
                         'timeout': max(case_timeout(suite) for suite in suites)}
                        for job, suites in jobs.items()]}


def suites_in(selection):
    return [suite for entry in selection['include'] for suite in entry['suites'].split()]


def manifest(scheme, products):
    runs = list(products.glob(f'{scheme}_*.xctestrun'))
    if len(runs) != 1:
        raise ValueError(f'Expected one {scheme} test configuration, found {len(runs)}')
    return runs[0]


def validate_products(selection, products=Path('DerivedData/Build/Products')):
    for scheme in {SUITES[suite][0] for suite in suites_in(selection)}:
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
    for suite in suites_in(expected):
        try:
            summary = json.loads((directory / f'{suite}-summary.json').read_text())
            validate(summary)
            report = ET.parse(directory / f'{suite}-junit.xml')
            check_durations(report, case_timeout(suite))
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
    selection.add_argument('--app', choices=['pitchperfect', 'tagmaster'], action='append',
                           help='repeat to select several apps; omit for every app')
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
