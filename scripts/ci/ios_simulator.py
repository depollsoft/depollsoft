#!/usr/bin/env python3
"""Create an owned test device without reusing a runner's saved app data."""
import fcntl
import json
import plistlib
import os
import re
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


def clean_abandoned_captures():
    """Call only while holding depollsoft-ios-simulator.lock.

    Capture creates and deletes these disposable devices inside that lock, so
    any left at acquisition belong to a terminated capture. Never touch named
    developer devices or CI devices allocated by a job waiting for the lock.
    """
    xcrun = os.environ.get('XCRUN', 'xcrun')
    try:
        devices = json.loads(subprocess.check_output(
            [xcrun, 'simctl', 'list', 'devices', '-j'], text=True, timeout=60))['devices']
    except subprocess.TimeoutExpired:
        # Discovery can hang when abandoned simulators exhaust host resources.
        # Read only Apple's device metadata to recover our exact reserved names;
        # still ask simctl to shut down/delete them, never remove files ourselves.
        print('Simulator discovery stalled; reading device metadata for capture cleanup', flush=True)
        devices = {'local': []}
        for path in (Path.home() / 'Library/Developer/CoreSimulator/Devices').glob('*/device.plist'):
            device = plistlib.loads(path.read_bytes())
            if device.get('UDID') == path.parent.name:
                devices['local'].append({'name': device.get('name', ''), 'udid': device['UDID']})
    failures = []
    for available in devices.values():
        for device in available:
            if re.fullmatch(r'Store-(pitchperfect|tagmaster)-(iphone|ipad)', device['name']):
                print(f"Removing abandoned capture simulator: {device['name']} ({device['udid']})", flush=True)
                try:
                    delete(device['udid'])
                except (subprocess.SubprocessError, OSError) as error:
                    failures.append(str(error))
    if failures:
        raise RuntimeError('Could not clean abandoned capture devices: ' + '; '.join(failures))


def boot(udid):
    xcrun = os.environ.get('XCRUN', 'xcrun')
    print(f'Booting test simulator {udid}', flush=True)
    subprocess.run([xcrun, 'simctl', 'boot', udid], check=True, timeout=60)
    subprocess.run([xcrun, 'simctl', 'bootstatus', udid, '-b'], check=True, timeout=180)


def shutdown(udid):
    xcrun = os.environ.get('XCRUN', 'xcrun')
    print(f'Shutting down test simulator {udid}', flush=True)
    subprocess.run([xcrun, 'simctl', 'shutdown', udid], check=True, timeout=30)


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
        # Discovery itself can compete with a capture booting or shutting down.
        with Path('/tmp/depollsoft-ios-simulator.lock').open('a') as lock:
            print('Waiting for the host simulator slot before device creation', flush=True)
            fcntl.flock(lock, fcntl.LOCK_EX)
            clean_abandoned_captures()
            main()
    else:
        raise SystemExit('Usage: ios_simulator.py [--delete UDID]')
