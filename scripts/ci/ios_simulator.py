#!/usr/bin/env python3
"""Create an owned test device without reusing a runner's saved app data."""
from contextlib import contextmanager
import fcntl
import json
import plistlib
import os
import re
from pathlib import Path
import subprocess
import sys
import uuid


REGISTRY = Path('/tmp/depollsoft-ios-devices')


@contextmanager
def allocation():
    # Protect only bookkeeping and allocation, never a build or test execution.
    with Path('/tmp/depollsoft-ios-simulator.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        yield


def process_identity(pid):
    result = subprocess.run(['ps', '-p', str(pid), '-o', 'ppid=', '-o', 'lstart=', '-o', 'comm='],
                            text=True, capture_output=True, timeout=5,
                            env=dict(os.environ, LC_ALL='C'))
    fields = result.stdout.strip().split(None, 6)
    if result.returncode or len(fields) != 7:
        return None
    return {'pid': pid, 'parent': int(fields[0]), 'started': ' '.join(fields[1:6]),
            'command': fields[6]}


def owner_identity(*, parent=False):
    # Runner.Worker lives for the entire Actions job, including the gaps between
    # shell steps. Process start time guards against reuse of its PID.
    pid = os.getpid()
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        while pid > 1:
            identity = process_identity(pid)
            if not identity:
                break
            if Path(identity['command']).name == 'Runner.Worker':
                return identity
            pid = identity['parent']
        raise RuntimeError('Cannot identify the Actions job process owning this simulator')
    identity = process_identity(os.getppid() if parent else pid)
    if not identity:
        raise RuntimeError('Cannot identify the simulator owner process')
    return identity


def has_live_owner(udid):
    path = REGISTRY / (udid + '.json')
    if not path.exists():
        return False
    owner = json.loads(path.read_text())
    current = process_identity(owner['pid'])
    return current is not None and current['started'] == owner['started']


def create(name, model, runtime, owner=None):
    # Caller holds allocation() until creation and registration both finish.
    owner = owner or owner_identity()
    xcrun = os.environ.get('XCRUN', 'xcrun')
    udid = subprocess.check_output([xcrun, 'simctl', 'create', name, model, runtime],
                                   text=True, timeout=60).strip()
    try:
        REGISTRY.mkdir(mode=0o700, exist_ok=True)
        (REGISTRY / (udid + '.json')).write_text(json.dumps(owner))
    except BaseException:
        delete(udid)
        raise
    return udid


def delete(udid):
    xcrun = os.environ.get('XCRUN', 'xcrun')
    # A stuck shutdown must not prevent trying to delete our own device.
    try:
        shutdown(udid)
    except (subprocess.SubprocessError, OSError) as error:
        print(f'Simulator shutdown did not complete: {error}', flush=True)
    subprocess.run([xcrun, 'simctl', 'delete', udid], check=True, timeout=30)
    (REGISTRY / (udid + '.json')).unlink(missing_ok=True)


def clean_abandoned():
    """Under allocation(), reclaim only owned devices whose job has exited."""
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
            # A maintainer can explicitly retire a legacy shared CI device
            # which predates ownership records. Preserve its data and all
            # other unregistered devices; never stop a registered live job.
            if (device['udid'] == os.environ.get('IOS_LEGACY_SIMULATOR_UDID')
                    and device.get('state') == 'Booted'
                    and not has_live_owner(device['udid'])):
                print(f"Retiring legacy CI simulator: {device['name']} ({device['udid']})", flush=True)
                shutdown(device['udid'])
            reserved = re.fullmatch(
                r'(Store-(pitchperfect|tagmaster)-(iphone|ipad)(-[a-f0-9-]{36})?|Depollsoft-Test-[a-f0-9-]{36})',
                device['name'])
            if reserved and not has_live_owner(device['udid']):
                print(f"Removing abandoned simulator: {device['name']} ({device['udid']})", flush=True)
                try:
                    delete(device['udid'])
                except (subprocess.SubprocessError, OSError) as error:
                    failures.append(str(error))
    if failures:
        raise RuntimeError('Could not clean abandoned simulators: ' + '; '.join(failures))


def boot(udid):
    xcrun = os.environ.get('XCRUN', 'xcrun')
    print(f'Booting test simulator {udid}', flush=True)
    # -b boots a stopped device and also succeeds when it is already running.
    # A fresh iOS 26 device can spend several minutes in LaunchServices
    # data migration on hosted runners, before any app or test can start.
    result = subprocess.run([xcrun, 'simctl', 'bootstatus', udid, '-b'], check=True,
                            timeout=600, capture_output=True, text=True)
    print(result.stdout, end='', flush=True)
    # CoreSimulator can report migration failure with exit status zero even
    # when native apps launch successfully. Let actual test/capture readiness
    # checks decide success; preserve this diagnostic instead of rejecting it.
    if 'Data Migration Failed' in result.stdout:
        print('CoreSimulator reported migration errors; native tests will verify readiness', flush=True)
    if result.stderr:
        print(result.stderr, file=sys.stderr, flush=True)


def shutdown(udid):
    xcrun = os.environ.get('XCRUN', 'xcrun')
    print(f'Shutting down test simulator {udid}', flush=True)
    result = subprocess.run([xcrun, 'simctl', 'shutdown', udid],
                            capture_output=True, text=True, timeout=30)
    if result.returncode and 'current state: Shutdown' not in result.stderr:
        print(result.stderr, flush=True)
        result.check_returncode()


def main():
    xcrun = os.environ.get('XCRUN', 'xcrun')

    def output(*args):
        return subprocess.check_output([xcrun, *args], text=True, timeout=60).strip()

    sdk = tuple(map(int, output('--sdk', 'iphonesimulator', '--show-sdk-version').split('.')))[:2]
    requested = os.environ.get('IOS_SIMULATOR_VERSION')
    devices = json.loads(output('simctl', 'list', 'devices', 'available', '-j'))['devices']
    candidates = []
    for runtime, available in devices.items():
        if '.iOS-' not in runtime:
            continue
        version = tuple(map(int, runtime.rsplit('iOS-', 1)[1].split('-')))[:2]
        if requested and version != tuple(map(int, requested.split('.')))[:2]:
            continue
        if version > sdk:
            continue
        for device in available:
            model = device.get('deviceTypeIdentifier', '')
            if device.get('isAvailable') and '.iPhone-' in model:
                candidates.append((version, 'iPhone-16' in model, model, runtime))
    if not candidates:
        raise RuntimeError('No available iPhone runtime supported by the selected Xcode')
    _, _, model, runtime = max(candidates)
    name = f'Depollsoft-Test-{uuid.uuid4()}'
    udid = create(name, model, runtime, owner_identity(parent=True))
    try:
        with Path(os.environ['GITHUB_OUTPUT']).open('a') as stream:
            stream.write(f'udid={udid}\ndestination=platform=iOS Simulator,id={udid}\n')
    except BaseException:
        delete(udid)
        raise
    print(f'Created {name}: {udid} ({model}, {runtime})')


if __name__ == '__main__':
    if len(sys.argv) == 3 and sys.argv[1] == '--delete':
        delete(sys.argv[2])
    elif len(sys.argv) == 1:
        with allocation():
            clean_abandoned()
            main()
    else:
        raise SystemExit('Usage: ios_simulator.py [--delete UDID]')
