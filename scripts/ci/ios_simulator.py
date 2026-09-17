#!/usr/bin/env python3
"""Create an owned test device without reusing a runner's saved app data."""
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid


def delete(udid):
    xcrun = os.environ.get('XCRUN', 'xcrun')
    # A stuck shutdown must not prevent trying to delete our own device.
    try:
        subprocess.run([xcrun, 'simctl', 'shutdown', udid], check=True, timeout=30)
    except (subprocess.SubprocessError, OSError) as error:
        print(f'Simulator shutdown did not complete: {error}', flush=True)
    subprocess.run([xcrun, 'simctl', 'delete', udid], check=True, timeout=30)


def main():
    xcrun = os.environ.get('XCRUN', 'xcrun')

    def output(*args):
        return subprocess.check_output([xcrun, *args], text=True, timeout=60).strip()

    sdk = tuple(map(int, output('--sdk', 'iphonesimulator', '--show-sdk-version').split('.')))[:2]
    devices = json.loads(output('simctl', 'list', 'devices', 'available', '-j'))['devices']
    candidates = []
    for runtime, available in devices.items():
        if '.iOS-' not in runtime:
            continue
        version = tuple(map(int, runtime.rsplit('iOS-', 1)[1].split('-')))[:2]
        if version > sdk:
            continue
        for device in available:
            model = device.get('deviceTypeIdentifier', '')
            if device.get('isAvailable') and '.iPhone-' in model:
                candidates.append((version, 'iPhone-16' in model, model, runtime))
    if not candidates:
        raise RuntimeError('No available iPhone runtime supported by the selected Xcode')
    _, _, model, runtime = max(candidates)
    name = f'Depollsoft-CI-{uuid.uuid4()}'
    udid = output('simctl', 'create', name, model, runtime)
    try:
        with Path(os.environ['GITHUB_OUTPUT']).open('a') as stream:
            stream.write(f'udid={udid}\ndestination=platform=iOS Simulator,id={udid}\n')
    except BaseException:
        subprocess.run([xcrun, 'simctl', 'delete', udid], check=False, timeout=60)
        raise
    print(f'Created {name}: {udid} ({model}, {runtime})')


if __name__ == '__main__':
    if len(sys.argv) == 3 and sys.argv[1] == '--delete':
        delete(sys.argv[2])
    elif len(sys.argv) == 1:
        main()
    else:
        raise SystemExit('Usage: ios_simulator.py [--delete UDID]')
