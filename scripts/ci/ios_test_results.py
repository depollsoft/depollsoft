#!/usr/bin/env python3
"""Verify actual XCTest results; xcpretty can emit empty reports for parallel tests."""
import json
import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def validate(summary):
    if (summary.get('result') != 'Passed' or summary.get('totalTestCount', 0) <= 0
            or summary.get('passedTests', 0) <= 0 or summary.get('failedTests', 0) != 0
            or summary.get('testFailures')):
        raise ValueError('Tests failed, were skipped, or did not run successfully')


def junit(scheme, tree):
    cases = []
    def visit(node, parent=''):
        if node['nodeType'] == 'Test Case':
            case = ET.Element('testcase', name=node['name'], classname=parent,
                              time=str(node.get('durationInSeconds', 0)))
            result = node.get('result')
            if result in ('Skipped', 'Expected Failure'):
                ET.SubElement(case, 'skipped', message=result)
            elif result != 'Passed':
                ET.SubElement(case, 'failure' if result == 'Failed' else 'error',
                              message=node.get('details') or str(result))
            cases.append(case)
        else:
            for child in node.get('children', []):
                visit(child, node['name'])
    for node in tree.get('testNodes', []):
        visit(node)
    if not cases:
        raise ValueError('Xcode returned no test cases')
    root = ET.Element('testsuites')
    attributes = {'name': scheme, 'tests': str(len(cases)),
                  'skipped': str(sum(case.find('skipped') is not None for case in cases))}
    for kind in ('failure', 'error'):
        attributes[kind + 's'] = str(sum(case.find(kind) is not None for case in cases))
    suite = ET.SubElement(root, 'testsuite', attributes)
    suite.extend(cases)
    return ET.ElementTree(root)


def check_durations(report, limit):
    cases = sorted(report.findall('.//testcase'),
                   key=lambda case: float(case.get('time', 0)), reverse=True)
    for case in cases[:5]:
        print(f'{case.get("classname")}/{case.get("name")}: {float(case.get("time", 0)):.2f}s')
    slow = [case for case in cases if float(case.get('time', 0)) > limit]
    if slow:
        names = ', '.join(f'{case.get("classname")}/{case.get("name")} ({case.get("time")}s)'
                          for case in slow)
        raise ValueError(f'Tests exceeded the {limit:g}s budget: {names}')


def read_result(scheme, kind):
    command = [
        os.environ.get('XCRUN', 'xcrun'), 'xcresulttool', 'get', 'test-results', kind,
        '--path', f'{scheme}-results.xcresult', '--compact',
    ]
    try:
        raw = subprocess.check_output(command, text=True, timeout=120)
    except subprocess.TimeoutExpired:
        print(f'::warning::{scheme}: xcresulttool {kind} timed out; retrying once', flush=True)
        raw = subprocess.check_output(command, text=True, timeout=120)
    return json.loads(raw)


def main(schemes):
    failed = False
    for scheme in schemes:
        try:
            # A failed re-export must not leave an older passing report behind.
            for suffix in ('summary.json', 'junit.xml'):
                Path(f'{scheme}-{suffix}').unlink(missing_ok=True)
            summary = read_result(scheme, 'summary')
            Path(f'{scheme}-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
            tree = read_result(scheme, 'tests')
            report = junit(scheme, tree)
            report.write(f'{scheme}-junit.xml', encoding='utf-8', xml_declaration=True)
            validate(summary)
            if os.environ.get('IOS_MAX_TEST_SECONDS'):
                check_durations(report, float(os.environ['IOS_MAX_TEST_SECONDS']))
            print(f'{scheme}: {summary["passedTests"]} passed, {summary.get("skippedTests", 0)} skipped')
        except (OSError, subprocess.SubprocessError, ValueError) as error:
            print(f'::error::{scheme}: {error}')
            failed = True
    return int(failed)


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise SystemExit('Provide at least one test scheme')
    raise SystemExit(main(sys.argv[1:]))
