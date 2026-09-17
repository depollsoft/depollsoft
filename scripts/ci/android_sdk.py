#!/usr/bin/env python3
"""Reuse installed Android SDK packages without querying Google's repository."""
import argparse
import os
from pathlib import Path
import shutil


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


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--require', action='store_true')
    parser.add_argument('packages', nargs='*', default=BUILD_PACKAGES)
    args = parser.parse_args()
    adb = shutil.which('adb')
    root = find_sdk(args.packages, [os.environ.get('ANDROID_SDK_ROOT'),
                    os.environ.get('ANDROID_HOME'), Path(adb).resolve().parent.parent if adb else None,
                    '/opt/android-sdk', Path.home() / 'Android/Sdk',
                    Path.home() / 'Library/Android/sdk'])
    if os.environ.get('GITHUB_OUTPUT'):
        with Path(os.environ['GITHUB_OUTPUT']).open('a') as output:
            output.write(f'available={str(root is not None).lower()}\n')
    if root is None:
        if args.require:
            raise SystemExit('Required Android SDK packages or accepted licenses are missing')
        print('Required SDK packages are missing; setup must install them', flush=True)
        return
    print(f'Reusing installed Android SDK: {root}', flush=True)
    if os.environ.get('GITHUB_ENV'):
        with Path(os.environ['GITHUB_ENV']).open('a') as output:
            output.write(f'ANDROID_HOME={root}\nANDROID_SDK_ROOT={root}\n')
        with Path(os.environ['GITHUB_PATH']).open('a') as output:
            for directory in ['platform-tools', 'cmdline-tools/latest/bin', 'emulator']:
                if (root / directory).is_dir():
                    output.write(f'{root / directory}\n')


if __name__ == '__main__':
    main()
