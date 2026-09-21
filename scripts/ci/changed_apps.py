#!/usr/bin/env python3
"""Select the apps each mobile workflow must build from the files a change touched.

A file under a directory that only one app owns selects that app alone.
Every other file under a platform root is shared code (Android libraries,
Bindroid, Gradle files, the Xcode workspace, depolllib, Fastlane) and selects
every app on that platform. The platform's workflow files and CI scripts are
shared too. A few files select everything on both platforms.

Android CI, iOS CI and PR Preview all run this before their expensive jobs.
Manual and scheduled runs, and any push whose diff cannot be fetched, select
every app so nothing is silently skipped.
"""
import argparse
import json
import os
import subprocess
import sys

PLATFORMS = ('android', 'ios')
APPS = ('pitchperfect', 'tagmaster')

# Directories owned by exactly one app, per platform. A trailing slash keeps
# Android/PitchPerfect from matching Android/PitchPerfectLib.
OWNED = {
    'android': {
        'pitchperfect': ('Android/PitchPerfect/', 'Android/PitchPerfectWear/',
                         'Android/PitchPerfectLicense/'),
        'tagmaster': ('Android/TagMaster/',),
    },
    'ios': {
        'pitchperfect': ('iOS/pitchperfect/',),
        'tagmaster': ('iOS/tagmaster/',),
    },
}

# Paths inside an owned directory that both apps actually compile.
SHARED_INSIDE_OWNED = ('iOS/pitchperfect/pitchperfectlib/',)

# Prefixes whose remaining files are shared by every app on the platform.
SHARED = {
    'android': ('Android/', '.github/workflows/android.yml', 'scripts/ci/android_sdk.py'),
    'ios': ('iOS/', '.github/workflows/ios.yml', '.github/workflows/ios-app-tests.yml',
            'scripts/ci/ios_'),
}

# Files that change what gets built everywhere.
EVERYTHING = ('scripts/ci/changed_apps.py', '.github/workflows/pr-preview.yml',
              '.github/workflows/preview-deploy-command.yml',
              '.github/workflows/deploy-pr-preview.yml')

# Gradle modules that make up each Android app, in the order they are built.
ANDROID_MODULES = {
    'pitchperfect': ('PitchPerfect', 'PitchPerfectWear'),
    'tagmaster': ('TagMaster',),
}


def everything():
    return {platform: list(APPS) for platform in PLATFORMS}


def select(paths):
    """Map changed paths to {platform: [apps]} with apps in canonical order."""
    chosen = {platform: set() for platform in PLATFORMS}
    for path in paths:
        if path.startswith(EVERYTHING):
            return everything()
        for platform in PLATFORMS:
            owner = None
            if not path.startswith(SHARED_INSIDE_OWNED):
                owner = next((app for app, prefixes in OWNED[platform].items()
                              if path.startswith(prefixes)), None)
            if owner:
                chosen[platform].add(owner)
            elif path.startswith(SHARED[platform]):
                chosen[platform].update(APPS)
    return {platform: [app for app in APPS if app in chosen[platform]]
            for platform in PLATFORMS}


def outputs(selection):
    """Flatten a selection into the key=value pairs the workflows read."""
    result = {}
    for platform in PLATFORMS:
        apps = selection[platform]
        result[platform] = json.dumps(apps)
        result[f'{platform}-any'] = str(bool(apps)).lower()
        result[f'{platform}-all'] = str(set(apps) == set(APPS)).lower()
        for app in APPS:
            result[f'{platform}-{app}'] = str(app in apps).lower()
    result['android-modules'] = ' '.join(
        module for app in selection['android'] for module in ANDROID_MODULES[app])
    return result


def gh(*args):
    return subprocess.check_output(['gh', 'api', *args], text=True)


def pull_request_files(repository, number):
    names = gh(f'repos/{repository}/pulls/{number}/files?per_page=100', '--paginate',
               '--jq', '.[] | .filename, (.previous_filename // empty)')
    return names.split()


def push_files(repository, before, after):
    if not before or set(before) == {'0'}:
        return None
    try:
        names = gh(f'repos/{repository}/compare/{before}...{after}?per_page=100',
                   '--paginate', '--jq', '.files[] | .filename, (.previous_filename // empty)')
    except subprocess.CalledProcessError:
        return None
    return names.split()


def changed_files(event_name, event, repository):
    """The paths a workflow run should judge, or None to select everything."""
    if event_name == 'pull_request':
        return pull_request_files(repository, event['pull_request']['number'])
    if event_name == 'push':
        return push_files(repository, event.get('before', ''), event['after'])
    return None


def summary(selection, paths):
    lines = ['### Apps selected by this change', '']
    for platform in PLATFORMS:
        apps = ', '.join(selection[platform]) or 'none'
        lines.append(f'- {platform}: {apps}')
    if paths is None:
        lines.append('- reason: every app runs for this event')
    else:
        lines.append(f'- changed files: {len(paths)}')
    return '\n'.join(lines) + '\n'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('paths', nargs='*', help='changed paths; omit to read the GitHub event')
    parser.add_argument('--all', action='store_true', help='select every app on both platforms')
    parser.add_argument('--stdin', action='store_true', help='read one changed path per line')
    args = parser.parse_args()
    if args.all:
        paths = None
    elif args.stdin:
        paths = [line.strip() for line in sys.stdin if line.strip()]
    elif args.paths:
        paths = args.paths
    else:
        with open(os.environ['GITHUB_EVENT_PATH']) as handle:
            event = json.load(handle)
        paths = changed_files(os.environ['GITHUB_EVENT_NAME'], event, os.environ['GITHUB_REPOSITORY'])
    selection = everything() if paths is None else select(paths)
    print(json.dumps(selection))
    text = summary(selection, paths)
    print(text, file=sys.stderr)
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as handle:
            for key, value in outputs(selection).items():
                handle.write(f'{key}={value}\n')
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as handle:
            handle.write(text)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
