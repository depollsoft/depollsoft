#!/usr/bin/env python3
"""Capture real native app screens on disposable simulators/emulators."""
import argparse
import fcntl
import signal
import sys
import shutil
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
import uuid

from PIL import Image, ImageStat
from release import APPS, ROOT, git, write_json, fingerprint
import icons

sys.path.insert(0, str(ROOT / 'scripts/ci'))
import ios_simulator
import ios_run

IOS_DEVICES = {'iphone': ('iPhone 17 Pro Max', {(1320, 2868)}),
               'ipad': ('iPad Pro 13-inch (M5)', {(2064, 2752)})}
ANDROID_DEVICES = {'phoneScreenshots': ('1080x1920', '420'),
                   'tenInchScreenshots': ('1600x2560', '240')}


def run(*args, **kwargs):
    print('+ ' + ' '.join(map(str, args)), flush=True)
    return subprocess.run(list(map(str, args)), check=True, **kwargs)


def output(*args, timeout=300):
    return subprocess.check_output(list(map(str, args)), text=True, timeout=timeout).strip()


def run_owned(*args, timeout, env, shutdown_timeout=15):
    """Stop this command's descendants before returning or retrying."""
    command = list(map(str, args))
    print('+ ' + ' '.join(command), flush=True)
    child = subprocess.Popen(command, start_new_session=True,
                             env=dict(env, NSUnbufferedIO='YES'))
    try:
        status = child.wait(timeout=timeout)
        if status:
            raise subprocess.CalledProcessError(status, command)
    finally:
        # Even a completed xcodebuild can leave descendants behind. Use the
        # same permission-safe signaling as CI, restricted to our process group.
        ios_run.signal_command(child, signal.SIGTERM)
        try:
            child.wait(timeout=shutdown_timeout)
        except subprocess.TimeoutExpired:
            pass
        finally:
            ios_run.signal_command(child, signal.SIGKILL)
            child.wait(timeout=5)


class NativeCaptureError(RuntimeError):
    pass


def native_capture(app, label, operation, *, simulator=False):
    # Tag Master captures live catalog/media requests throughout each scene tour.
    # Retry the whole tour from a fresh app; export only a completed attempt.
    # Either iOS app can encounter an unavailable XCTest/accessibility service.
    attempts = 2 if simulator or app == 'tagmaster' else 1
    for attempt in range(attempts):
        try:
            return operation(attempt)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, NativeCaptureError):
            if attempt + 1 == attempts:
                raise
            print(f'{label} failed; retrying the native capture once in 15s', flush=True)
            time.sleep(15)


def screenshot_path(app, platform, family, scene):
    if scene not in APPS[app]['store_scenes'][platform]:
        return f'review/{family}/{scene}.png'
    position = APPS[app]['store_scenes'][platform].index(scene) + 1
    ordered = f'{position:02}-{scene[3:]}'
    return (f'screenshots/en-US/{family}-{ordered}.png' if platform == 'ios'
            else f'metadata/en-US/images/{family}/{ordered}.png')


def ios_runtime(sdk, runtimes):
    sdk_version = tuple(map(int, sdk.split('.')))[:2]
    candidates = []
    for runtime in runtimes:
        version = tuple(map(int, runtime['version'].split('.')))[:2]
        if runtime.get('isAvailable') and 'iOS' in runtime['name'] and (26, 0) <= version <= sdk_version:
            candidates.append((version, runtime['identifier']))
    if not candidates:
        raise ValueError('Install an iOS 26 simulator runtime supported by the selected Xcode; '
                         'set DEVELOPER_DIR explicitly to use a different toolchain')
    return max(candidates)[1]


def ios(app, dest):
    config = APPS[app]
    # CoreSimulatorService is shared by this user's runner jobs. Even probing
    # another Xcode with simctl can replace the service beneath running tests.
    # Honor the same selected toolchain as ordinary CI; never scan other Xcodes.
    developer = os.environ.get('DEVELOPER_DIR') or output('xcode-select', '-p')
    env = dict(os.environ, DEVELOPER_DIR=developer)
    sdk = subprocess.check_output(['xcrun', '--sdk', 'iphonesimulator', '--show-sdk-version'],
                                  env=env, text=True).strip()
    data = json.loads(subprocess.check_output(['xcrun', 'simctl', 'list', 'runtimes', '-j'], env=env, text=True))
    runtime = ios_runtime(sdk, data['runtimes'])
    os.environ['DEVELOPER_DIR'] = developer
    print(f'Capture toolchain: {developer}; runtime: {runtime}', flush=True)
    build = ['-workspace', ROOT / 'iOS/iOS.xcworkspace', '-scheme', config['ios_scheme'],
             '-derivedDataPath', ROOT / 'build/release/DerivedData' / app,
             '-clonedSourcePackagesDirPath', ROOT / 'build/release/SourcePackages',
             '-only-testing:' + app + 'UITests/StoreScreenshotTests',
             'CODE_SIGNING_ALLOWED=NO', 'SDKROOT=iphonesimulator', f'ARCHS={os.uname().machine}']
    timings = []

    def xcode(label, *args, **kwargs):
        started = time.monotonic()
        succeeded = False
        try:
            # Keep compiler command lines out of the live log so the actual
            # capture progress remains visible through GitHub's log API.
            run_owned('xcodebuild', *(['-quiet'] if label == 'build' else []), *args, **kwargs)
            succeeded = True
        finally:
            seconds = round(time.monotonic() - started, 1)
            timings.append({'stage': label, 'seconds': seconds, 'success': succeeded})
            write_json(dest / 'capture-timings.json', timings)
            print(f'{label}: {seconds}s, success={succeeded}', flush=True)

    # Build before booting a simulator, and reuse these products for both sizes.
    xcode('build', 'build-for-testing', '-jobs', '2', '-destination',
          'generic/platform=iOS Simulator', *build, env=env, timeout=600)
    with tempfile.TemporaryDirectory(prefix='store-ios-') as temp:
        work = Path(temp)
        for family, (model, _) in IOS_DEVICES.items():
            with ios_simulator.allocation():
                ios_simulator.clean_abandoned()
                udid = ios_simulator.create(f'Store-{app}-{family}-{uuid.uuid4()}', model, runtime)
            try:
                for attempt in range(2):
                    try:
                        ios_simulator.boot(udid)
                        break
                    except subprocess.TimeoutExpired:
                        ios_simulator.shutdown(udid)
                        if attempt == 1:
                            raise
                        print('Simulator boot stalled; retrying this disposable device once', flush=True)
                for theme in ('light', 'dark'):
                    def take(attempt):
                        if attempt:
                            # Reset only our device if XCTest becomes unavailable.
                            ios_simulator.shutdown(udid)
                            run('xcrun', 'simctl', 'erase', udid, timeout=60)
                        ios_simulator.boot(udid)
                        run('xcrun', 'simctl', 'status_bar', udid, 'override', '--time', '9:41',
                            '--dataNetwork', 'wifi', '--wifiMode', 'active', '--wifiBars', '3',
                            '--batteryState', 'charged', '--batteryLevel', '100')
                        subprocess.run(['xcrun', 'simctl', 'uninstall', udid, config['bundle_id']],
                                       check=False, timeout=30)
                        run('xcrun', 'simctl', 'ui', udid, 'appearance', theme)
                        result = work / f'{family}-{theme}-{attempt}.xcresult'
                        # Reuse compiled products while each tour starts with
                        # fresh app data, matching the original capture behavior.
                        # XCTest startup also counts against this process limit;
                        # the live-catalog tour can take four minutes by itself.
                        xcode(f'{family}-{theme}-{attempt}', 'test-without-building', *build,
                              '-destination', f'platform=iOS Simulator,id={udid}',
                              '-resultBundlePath', result, '-parallel-testing-enabled', 'NO',
                              env=dict(env, TEST_RUNNER_STORE_SCREENSHOTS='1'), timeout=600)
                        return result
                    result = native_capture(app, f'{family}-{theme}', take, simulator=True)
                    attachments = work / f'{family}-{theme}'
                    run('xcrun', 'xcresulttool', 'export', 'attachments', '--path', result,
                        '--output-path', attachments)
                    manifest = json.loads((attachments / 'manifest.json').read_text())
                    for test in manifest:
                        for attachment in test['attachments']:
                            name = attachment['suggestedHumanReadableName']
                            for scene in config['scenes']:
                                if name.startswith(f'store-{scene}'):
                                    target = dest / screenshot_path(app, 'ios', family, f'{scene}-{theme}')
                                    target.parent.mkdir(parents=True, exist_ok=True)
                                    with Image.open(attachments / attachment['exportedFileName']) as image:
                                        image.convert('RGB').save(target)
            finally:
                ios_simulator.delete(udid)


def android_status_bar(adb):
    # Let SystemUI render a clean status bar; never retouch captured app pixels.
    # Reapply after density/theme changes, which can recreate SystemUI.
    run(*adb, 'shell', 'settings', 'put', 'global', 'sysui_demo_allowed', '1')
    for command, values in (
        ('enter', {}),
        ('clock', {'hhmm': '0941'}),
        ('battery', {'level': '100', 'plugged': 'false', 'powersave': 'false'}),
        ('network', {'wifi': 'show', 'mobile': 'hide', 'level': '4', 'fully': 'true'}),
        ('notifications', {'visible': 'false'}),
    ):
        extras = [arg for key, value in values.items() for arg in ('--es', key, value)]
        run(*adb, 'shell', 'am', 'broadcast', '-a', 'com.android.systemui.demo',
            '--es', 'command', command, *extras)


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
    previous_demo = output(*adb, 'shell', 'settings', 'get', 'global', 'sysui_demo_allowed')
    try:
        for setting in ('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale'):
            run(*adb, 'shell', 'settings', 'put', 'global', setting, '0')
        run(*adb, 'shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
        run(*adb, 'shell', 'settings', 'put', 'system', 'user_rotation', '0')
        for family, (size, density) in ANDROID_DEVICES.items():
            run(*adb, 'shell', 'wm', 'size', size)
            run(*adb, 'shell', 'wm', 'density', density)
            time.sleep(3)
            for theme in ('light', 'dark'):
                run(*adb, 'shell', 'cmd', 'uimode', 'night', 'yes' if theme == 'dark' else 'no')
                def take(attempt):
                    run(*adb, 'shell', 'pm', 'clear', package)
                    android_status_bar(adb)
                    result = output(*adb, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                                    f'{package}.StoreScreenshotTest', '-e', 'storeScreenshots', 'true',
                                    f'{package}.test/androidx.test.runner.AndroidJUnitRunner', timeout=900)
                    print(result)
                    # am instrument can return exit 0 even when the test failed.
                    if 'OK (1 test)' not in result or 'FAILURES' in result:
                        diagnostics = ROOT / f'build/release/emulator-logs/phone-tablet/{family}-{theme}-{attempt}.log'
                        diagnostics.parent.mkdir(parents=True, exist_ok=True)
                        diagnostics.write_text(output(*adb, 'logcat', '-d', '-t', '1500'))
                        raise NativeCaptureError(f'Screenshot instrumentation failed; see {diagnostics}')
                native_capture(app, f'{family}-{theme}', take)
                for scene in config['scenes']:
                    target = dest / screenshot_path(app, 'android', family, f'{scene}-{theme}')
                    target.parent.mkdir(parents=True, exist_ok=True)
                    with target.open('wb') as handle:
                        run(*adb, 'exec-out', 'run-as', package, 'cat', f'files/store-screenshots/{scene}.png', stdout=handle)
    finally:
        run(*adb, 'shell', 'am', 'broadcast', '-a', 'com.android.systemui.demo', '--es', 'command', 'exit')
        if previous_demo == 'null':
            run(*adb, 'shell', 'settings', 'delete', 'global', 'sysui_demo_allowed')
        else:
            run(*adb, 'shell', 'settings', 'put', 'global', 'sysui_demo_allowed', previous_demo)
        run(*adb, 'shell', 'wm', 'size', 'reset')
        run(*adb, 'shell', 'wm', 'density', 'reset')


def validate(app, platform, dest):
    expected = {}
    for family, info in (IOS_DEVICES if platform == 'ios' else ANDROID_DEVICES).items():
        selected = APPS[app]['store_scenes'][platform]
        available = {f'{scene}-{theme}' for theme in ('light', 'dark') for scene in APPS[app]['scenes']}
        if not selected or len(selected) != len(set(selected)) or len(selected) > (10 if platform == 'ios' else 8) or not set(selected) <= available:
            raise ValueError('Invalid store screenshot selection')
        hashes = set()
        for scene in [f'{scene}-{theme}' for theme in ('light', 'dark') for scene in APPS[app]['scenes']]:
            relative = screenshot_path(app, platform, family, scene)
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
    if app == 'pitchperfect' and platform == 'android':
        for shape in ('round', 'square'):
            relative = f'metadata/en-US/images/wearScreenshots/{shape}.png'
            path = dest / relative
            with Image.open(path) as image:
                if image.format != 'PNG' or image.size != (384, 384) or image.mode != 'RGB' or max(ImageStat.Stat(image).stddev) < 5:
                    raise ValueError(f'Invalid Wear screenshot: {path}')
            expected[relative] = hashlib.sha256(path.read_bytes()).hexdigest()
        if len({expected[k] for k in expected if 'wearScreenshots' in k}) != 2:
            raise ValueError('Round and square Wear screenshots are identical')
        # The listing icon is derived from the launcher icon so the two never drift.
        relative = 'metadata/en-US/images/icon.png'
        path = dest / relative
        with Image.open(path) as image:
            if image.format != 'PNG' or image.size != (512, 512) or image.mode != 'RGB':
                raise ValueError(f'Invalid store icon: {path}')
        expected[relative] = hashlib.sha256(path.read_bytes()).hexdigest()
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
    parser.add_argument('--wear-source', type=Path, help='Round and square outputs from wear.py, required for Pitch Perfect Android')
    args = parser.parse_args()
    dest = args.output.resolve()
    source_sha, source_fingerprint = git('rev-parse', 'HEAD'), fingerprint()
    if not args.validate_only:
        if dest.exists():
            raise ValueError(f'Output already exists; choose a fresh directory: {dest}')
        if args.app == 'pitchperfect' and args.platform == 'android':
            if not args.wear_source:
                raise ValueError('Pitch Perfect Android requires --wear-source with both watch shapes')
            for shape in ('round', 'square'):
                source = args.wear_source / f'{shape}.png'
                evidence = json.loads((args.wear_source / f'{shape}.json').read_text())
                if evidence != {'source_sha': git('rev-parse', 'HEAD'), 'source_fingerprint': fingerprint(),
                                'sha256': hashlib.sha256(source.read_bytes()).hexdigest()}:
                    raise ValueError('Wear screenshots do not match this source')
                target = dest / 'metadata/en-US/images/wearScreenshots' / source.name
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, target)
            icon = dest / 'metadata/en-US/images/icon.png'
            icon.parent.mkdir(parents=True, exist_ok=True)
            icons.store('production').save(icon, optimize=True)
        dest.mkdir(parents=True, exist_ok=True)
        (ios(args.app, dest) if args.platform == 'ios' else android(args.app, dest, args.serial))
        # Android UiAutomation may encode RGBA; stores receive opaque RGB PNGs.
        for path in dest.rglob('*.png'):
            with Image.open(path) as image:
                image.convert('RGB').save(path)
    if (git('rev-parse', 'HEAD'), fingerprint()) != (source_sha, source_fingerprint):
        raise ValueError('Source changed during capture; rerun from a stable checkout')
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
    # Android capture commands can resize the same emulator; serialize them.
    # iOS jobs use separate, registered simulators and may run concurrently.
    platform = sys.argv[sys.argv.index('--platform') + 1] if '--platform' in sys.argv else 'help'
    if platform == 'ios':
        with ios_simulator.allocation():
            ios_simulator.clean_abandoned()
        main()
    else:
        lock_path = Path(tempfile.gettempdir()) / f'depollsoft-store-capture-{platform}.lock'
        with lock_path.open('a') as lock:
            fcntl.flock(lock, fcntl.LOCK_EX)
            main()
