#!/usr/bin/env python3
"""Verify actual XCTest results; xcpretty can emit empty reports for parallel tests."""
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


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
    suite = ET.SubElement(root, 'testsuite', name=scheme, tests=str(len(cases)),
                         **{kind + 's': str(sum(case.find(kind) is not None for case in cases))
                            for kind in ('failure', 'error')},
                         skipped=str(sum(case.find('skipped') is not None for case in cases)))
    suite.extend(cases)
    return ET.ElementTree(root)


def main(schemes):
    failed = False
    for scheme in schemes:
        try:
            raw = subprocess.check_output([
                os.environ.get('XCRUN', 'xcrun'), 'xcresulttool', 'get', 'test-results', 'summary',
                '--path', f'{scheme}-results.xcresult', '--compact',
            ], text=True, timeout=60)
            summary = json.loads(raw)
            Path(f'{scheme}-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
            tree = json.loads(subprocess.check_output([
                os.environ.get('XCRUN', 'xcrun'), 'xcresulttool', 'get', 'test-results', 'tests',
                '--path', f'{scheme}-results.xcresult', '--compact',
            ], text=True, timeout=60))
            junit(scheme, tree).write(f'{scheme}-junit.xml', encoding='utf-8', xml_declaration=True)
            validate(summary)
            print(f'{scheme}: {summary["passedTests"]} passed, {summary.get("skippedTests", 0)} skipped')
        except (OSError, subprocess.SubprocessError, ValueError) as error:
            print(f'::error::{scheme}: {error}')
            failed = True
    return int(failed)


if __name__ == '__main__':
    if len(sys.argv) < 2:
        raise SystemExit('Provide at least one test scheme')
    raise SystemExit(main(sys.argv[1:]))
