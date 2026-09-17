#!/usr/bin/env python3
"""Reuse installed Android SDK packages without querying Google's repository."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess


BUILD_PACKAGES = ['platform-tools', 'platforms;android-35', 'platforms;android-37.0',
                  'build-tools;36.0.0']


def missing_files(root, packages):
    required = [('licenses/android-sdk-license', False)]
    for package in packages:
        directory = package.replace(';', '/')
        if package == 'platform-tools':
            files = [('adb', True)]
        elif package.startswith('platforms;'):
            files = [('android.jar', False)]
        elif package.startswith('build-tools;'):
            files = [('aapt2', True), ('d8', True), ('lib/d8.jar', False)]
        elif package == 'emulator':
            files = [('emulator', True)]
        elif package.startswith('system-images;'):
            files = [('system.img', False)]
        else:
            raise ValueError(f'Unsupported SDK package: {package}')
        required.extend((f'{directory}/{name}', executable) for name, executable in files)
    return [name for name, executable in required
            if not (root / name).is_file() or (root / name).stat().st_size == 0
            or (executable and not os.access(root / name, os.X_OK))]


def find_sdk(packages, candidates):
    for candidate in candidates:
        if candidate and not missing_files(Path(candidate), packages):
            return Path(candidate).resolve()
    return None


def sdk_manager(root):
    manager = root / 'cmdline-tools/latest/bin/sdkmanager'
    return manager if manager.is_file() and os.access(manager, os.X_OK) else None


def install(root, packages):
    if not missing_files(root, packages):
        return
    manager = sdk_manager(root)
    if manager is None:
        raise RuntimeError('No installed sdkmanager; run the setup action first')
    # --install prompts for the requested packages' licenses. A separate
    # --licenses invocation scans every remote package and can stall CI.
    print(f'Installing required packages with {manager}', flush=True)
    subprocess.run([str(manager), f'--sdk_root={root}', '--install', *packages],
                   input='y\n' * 100, text=True, check=True, timeout=240)
    missing = missing_files(root, packages)
    if missing:
        raise RuntimeError('SDK installation is incomplete: ' + ', '.join(missing))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--require', action='store_true')
    parser.add_argument('--install', action='store_true')
    parser.add_argument('packages', nargs='*', default=BUILD_PACKAGES)
    args = parser.parse_args()
    adb = shutil.which('adb')
    candidates = [os.environ.get('ANDROID_SDK_ROOT'), os.environ.get('ANDROID_HOME'),
                  Path(adb).resolve().parent.parent if adb else None, '/opt/android-sdk',
                  Path.home() / 'Android/Sdk', Path.home() / 'Library/Android/sdk']
    root = find_sdk(args.packages, candidates)
    available = root is not None
    if root is None:
        root = next((Path(candidate).resolve() for candidate in candidates
                     if candidate and sdk_manager(Path(candidate))), None)
    if args.install:
        if root is None:
            raise SystemExit('No installed sdkmanager; run the setup action first')
        install(root, args.packages)
        available = True
    if os.environ.get('GITHUB_OUTPUT'):
        with Path(os.environ['GITHUB_OUTPUT']).open('a') as output:
            output.write(f'available={str(available).lower()}\n')
            output.write(f'manager={str(root is not None and sdk_manager(root) is not None).lower()}\n')
    if not available:
        if args.require:
            raise SystemExit('Required Android SDK packages or accepted licenses are missing')
        print('Required SDK packages are missing; setup must install them', flush=True)
        if root is not None:
            print(f'{root}: missing ' + ', '.join(missing_files(root, args.packages)), flush=True)
    if root is None:
        return
    print(f'Using installed Android SDK: {root}', flush=True)
    if os.environ.get('GITHUB_ENV'):
        with Path(os.environ['GITHUB_ENV']).open('a') as output:
            output.write(f'ANDROID_HOME={root}\nANDROID_SDK_ROOT={root}\n')
        with Path(os.environ['GITHUB_PATH']).open('a') as output:
            for directory in ['platform-tools', 'cmdline-tools/latest/bin', 'emulator']:
                if (root / directory).is_dir():
                    output.write(f'{root / directory}\n')


if __name__ == '__main__':
    main()
