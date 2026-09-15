#!/usr/bin/env python3
"""Capture real native app screens on disposable simulators/emulators."""
import argparse
import fcntl
import signal
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

from PIL import Image, ImageStat
from release import APPS, ROOT, git, write_json, fingerprint

IOS_DEVICES = {'iphone': ('iPhone 17 Pro Max', {(1320, 2868)}),
               'ipad': ('iPad Pro 13-inch (M5)', {(2064, 2752)})}
ANDROID_DEVICES = {'phoneScreenshots': ('1080x1920', '420'),
                   'tenInchScreenshots': ('1600x2560', '240')}


def run(*args, **kwargs):
    print('+ ' + ' '.join(map(str, args)), flush=True)
    return subprocess.run(list(map(str, args)), check=True, **kwargs)


def output(*args):
    return subprocess.check_output(list(map(str, args)), text=True, timeout=300).strip()


def ios(app, dest):
    config = APPS[app]
    devices = json.loads(output('xcrun', 'simctl', 'list', 'runtimes', '-j'))['runtimes']
    runtimes = [r for r in devices if r.get('isAvailable') and 'iOS' in r['name']]
    runtime = max(runtimes, key=lambda r: tuple(map(int, r['version'].split('.'))))['identifier']
    with tempfile.TemporaryDirectory(prefix='store-ios-') as temp:
        work = Path(temp)
        for family, (model, _) in IOS_DEVICES.items():
            udid = output('xcrun', 'simctl', 'create', f'Store-{app}-{family}', model, runtime)
            try:
                run('xcrun', 'simctl', 'boot', udid)
                run('xcrun', 'simctl', 'bootstatus', udid, '-b')
                run('xcrun', 'simctl', 'status_bar', udid, 'override', '--time', '9:41',
                    '--dataNetwork', 'wifi', '--wifiMode', 'active', '--wifiBars', '3',
                    '--batteryState', 'charged', '--batteryLevel', '100')
                run('xcrun', 'simctl', 'ui', udid, 'appearance', 'light')
                result = work / f'{family}.xcresult'
                env = dict(os.environ, TEST_RUNNER_STORE_SCREENSHOTS='1')
                run('xcodebuild', 'test', '-workspace', ROOT / 'iOS/iOS.xcworkspace',
                    '-scheme', config['ios_scheme'], '-destination', f'platform=iOS Simulator,id={udid}',
                    '-derivedDataPath', ROOT / 'build/release/DerivedData' / app,
                    '-clonedSourcePackagesDirPath', ROOT / 'build/release/SourcePackages',
                    '-resultBundlePath', result, '-parallel-testing-enabled', 'NO',
                    '-only-testing:' + app + 'UITests/StoreScreenshotTests',
                    'CODE_SIGNING_ALLOWED=NO', env=env)
                attachments = work / family
                run('xcrun', 'xcresulttool', 'export', 'attachments', '--path', result,
                    '--output-path', attachments)
                manifest = json.loads((attachments / 'manifest.json').read_text())
                for test in manifest:
                    for attachment in test['attachments']:
                        name = attachment['suggestedHumanReadableName']
                        for scene in config['scenes']:
                            if name.startswith(f'store-{scene}'):
                                target = dest / 'screenshots/en-US' / f'{family}-{scene}.png'
                                target.parent.mkdir(parents=True, exist_ok=True)
                                with Image.open(attachments / attachment['exportedFileName']) as image:
                                    image.convert('RGB').save(target)
            finally:
                subprocess.run(['xcrun', 'simctl', 'shutdown', udid], check=False)
                run('xcrun', 'simctl', 'delete', udid)


def android(app, dest, serial):
    if not serial.startswith('emulator-'):
        raise ValueError('Use a disposable emulator, never a personal device')
    config = APPS[app]
    env = dict(os.environ, ANDROID_SERIAL=serial)
    adb = ['adb', '-s', serial]
    # Only this emulator is targeted, even on hosts with other running devices.
    run('./gradlew', f':{config["module"]}:assembleDebug', f':{config["module"]}:assembleDebugAndroidTest',
        cwd=ROOT / 'Android', env=env)
    base = ROOT / 'Android' / config['module'] / 'build/outputs/apk'
    package = config['bundle_id']
    run(*adb, 'install', '-r', base / f'debug/{config["module"]}-debug.apk')
    run(*adb, 'install', '-r', base / f'androidTest/debug/{config["module"]}-debug-androidTest.apk')
    try:
        for setting in ('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale'):
            run(*adb, 'shell', 'settings', 'put', 'global', setting, '0')
        run(*adb, 'shell', 'cmd', 'uimode', 'night', 'no')
        run(*adb, 'shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        run(*adb, 'shell', 'settings', 'put', 'system', 'user_rotation', '0')
        for family, (size, density) in ANDROID_DEVICES.items():
            run(*adb, 'shell', 'wm', 'size', size)
            run(*adb, 'shell', 'wm', 'density', density)
            run(*adb, 'shell', 'pm', 'clear', package)
            result = output(*adb, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                            f'{package}.StoreScreenshotTest', '-e', 'storeScreenshots', 'true',
                            f'{package}.test/androidx.test.runner.AndroidJUnitRunner')
            print(result)
            # am instrument can return exit 0 even when the test failed.
            if 'OK (1 test)' not in result or 'FAILURES' in result:
                raise ValueError('Screenshot instrumentation failed')
            target = dest / 'metadata/en-US/images' / family
            target.mkdir(parents=True, exist_ok=True)
            for scene in config['scenes']:
                with (target / f'{scene}.png').open('wb') as handle:
                    run(*adb, 'exec-out', 'run-as', package, 'cat', f'files/store-screenshots/{scene}.png', stdout=handle)
    finally:
        run(*adb, 'shell', 'wm', 'size', 'reset')
        run(*adb, 'shell', 'wm', 'density', 'reset')


def validate(app, platform, dest):
    expected = {}
    for family, info in (IOS_DEVICES if platform == 'ios' else ANDROID_DEVICES).items():
        hashes = set()
        for scene in APPS[app]['scenes']:
            relative = (f'screenshots/en-US/{family}-{scene}.png' if platform == 'ios'
                        else f'metadata/en-US/images/{family}/{scene}.png')
            path = dest / relative
            with Image.open(path) as image:
                image.load()
                sizes = info[1] if platform == 'ios' else {tuple(map(int, info[0].split('x')))}
                if image.format != 'PNG' or image.size not in sizes or image.mode != 'RGB':
                    raise ValueError(f'Invalid store screenshot format/dimensions: {path}: {image.size}, {image.mode}')
                if max(ImageStat.Stat(image).stddev) < 5:
                    raise ValueError(f'Blank screenshot: {path}')
            sha = hashlib.sha256(path.read_bytes()).hexdigest()
            if sha in hashes:
                raise ValueError(f'Duplicate scene: {path}')
            hashes.add(sha)
            expected[relative] = sha
    actual = {str(p.relative_to(dest)) for p in dest.rglob('*.png')}
    if actual != set(expected):
        raise ValueError('Unexpected screenshot files in bundle')
    return expected


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--app', required=True, choices=APPS)
    parser.add_argument('--platform', required=True, choices=['ios', 'android'])
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--serial', default=os.environ.get('ANDROID_SERIAL', ''))
    parser.add_argument('--validate-only', action='store_true')
    args = parser.parse_args()
    dest = args.output.resolve()
    if not args.validate_only:
        if dest.exists():
            raise ValueError(f'Output already exists; choose a fresh directory: {dest}')
        dest.mkdir(parents=True)
        (ios(args.app, dest) if args.platform == 'ios' else android(args.app, dest, args.serial))
        # Android UiAutomation may encode RGBA; stores receive opaque RGB PNGs.
        for path in dest.rglob('*.png'):
            with Image.open(path) as image:
                image.convert('RGB').save(path)
    files = validate(args.app, args.platform, dest)
    if not args.validate_only:
        write_json(dest / 'capture.json', {'app': args.app, 'platform': args.platform,
                   'source_sha': git('rev-parse', 'HEAD'), 'source_fingerprint': fingerprint(), 'files': files})
    else:
        evidence = json.loads((dest / 'capture.json').read_text())
        if evidence != {'app': args.app, 'platform': args.platform,
                        'source_sha': git('rev-parse', 'HEAD'), 'source_fingerprint': fingerprint(), 'files': files}:
            raise ValueError('Capture identity, source commit, or file hashes do not match')


if __name__ == '__main__':
    def interrupted(_signum, _frame):
        raise KeyboardInterrupt('Capture interrupted')

    signal.signal(signal.SIGTERM, interrupted)
    # Local capture commands can otherwise resize the same emulator or compete
    # for simulator resources. Serialize them just as the Actions jobs do.
    with (Path(tempfile.gettempdir()) / 'depollsoft-store-capture.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        main()
