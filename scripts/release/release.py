#!/usr/bin/env python3
"""Prepare and validate app-scoped release plans and Fastlane metadata."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]
APPS = json.loads((Path(__file__).with_name('apps.json')).read_text())
SHARED = ['iOS/depolllib', 'iOS/pitchperfect/pitchperfectlib', 'Android/PitchPerfectLib',
          'Android/DepollSoftCommon', 'Android/DepollSoftCommon.Compat',
          'Android/depollsoft.lib.kotlin', 'Android/build.gradle', 'Android/gradle',
          'Android/Bindroid', 'CloudCode', 'AppEngine']
LIMITS = {'name': 30, 'subtitle': 30, 'description': 4000, 'keywords': 100,
          'promotional_text': 170, 'title': 30, 'short_description': 80, 'full_description': 4000}


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def version(value):
    if not isinstance(value, str) or not re.fullmatch(r'(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)', value):
        raise ValueError(f'Expected numeric X.Y.Z version, got {value!r}')
    return tuple(map(int, value.split('.')))


def plan_path(app):
    return ROOT / 'releases' / app / 'release.json'


def validate_plan(app, plan, previous=None):
    if plan.get('app') != app or plan.get('schema') != 1:
        raise ValueError('Release app/schema mismatch')
    if set(plan) != {'schema', 'app', 'platforms', 'notes', 'since'}:
        raise ValueError('Unexpected release plan fields')
    platforms = plan['platforms']
    if not platforms or set(platforms) - {'ios', 'android'}:
        raise ValueError('Select ios and/or android')
    if not isinstance(plan['notes'], str) or not plan['notes'].strip() or len(plan['notes']) > 500:
        raise ValueError('Release notes must contain 1–500 characters, the Play limit')
    if re.search(r'\b(TODO|TBD|PLACEHOLDER)\b', plan['notes'], re.I):
        raise ValueError('Replace draft release notes before preparing a release')
    if set(plan['since']) != set(platforms):
        raise ValueError('Each platform needs a changelog baseline')
    for platform, values in platforms.items():
        if set(values) != {'version', 'build'}:
            raise ValueError('Each platform needs exactly version and build')
        version(values['version'])
        build = values['build']
        if type(build) is not int or not 1 <= build <= 2100000000:
            raise ValueError('Build must be a positive store-compatible integer')
        if not re.fullmatch(r'[0-9a-f]{40}', plan['since'][platform]):
            raise ValueError('Changelog baseline must be a full commit SHA')
        if previous is not None:
            old = previous.get('platforms', {}).get(platform)
            baseline = old['version'] if old else APPS[app]['baseline'][platform]
            if version(values['version']) <= version(baseline):
                raise ValueError(f'{app}/{platform} version must exceed {baseline}')
            if old and build <= old['build']:
                raise ValueError(f'{app}/{platform} build must increase')


def listing(app):
    data = json.loads((ROOT / 'store' / app / 'listing.json').read_text())
    required = {'ios': {'name', 'subtitle', 'description', 'keywords', 'promotional_text', 'support_url', 'privacy_url'},
                'android': {'title', 'short_description', 'full_description'}}
    if set(data) != set(required):
        raise ValueError(f'{app}: expected ios and android copy')
    for platform, fields in required.items():
        if set(data[platform]) != fields:
            raise ValueError(f'{app}/{platform}: incorrect listing fields')
        for key, value in data[platform].items():
            if not isinstance(value, str) or not value.strip():
                raise ValueError(f'{app}/{key}: empty copy')
            if key in LIMITS and len(value) > LIMITS[key]:
                raise ValueError(f'{app}/{key}: exceeds {LIMITS[key]} characters')
            if key.endswith('_url') and not value.startswith('https://'):
                raise ValueError(f'{app}/{key}: expected HTTPS URL')
    return data


def previous_plan(app, ref):
    path = f'releases/{app}/release.json'
    found = git('ls-tree', ref, '--', path)
    return json.loads(git('show', f'{ref}:{path}')) if found else {}


def changed_plans(base, head='HEAD'):
    changed = set(git('diff', '--name-only', base, head).splitlines())
    matrix = []
    for app in APPS:
        if f'releases/{app}/release.json' not in changed:
            continue
        current = json.loads(git('show', f'{head}:releases/{app}/release.json'))
        validate_plan(app, current, previous_plan(app, base))
        # A platform can be omitted from the next plan; compare with its last
        # published tag as well so alternating platform releases never regress.
        for platform, values in current['platforms'].items():
            tags = git('tag', '--list', f'{app}/{platform}/v*').splitlines()
            for tag in tags:
                tag_version = version(tag.rsplit('/v', 1)[1])
                same_submission = (version(values['version']) == tag_version and
                                   git('rev-list', '-n', '1', tag) == git('rev-parse', head))
                if version(values['version']) <= tag_version and not same_submission:
                    raise ValueError(f'{app}/{platform}: version already released: {tag}')
                released = previous_plan(app, tag).get('platforms', {}).get(platform)
                if released and values['build'] <= released['build'] and not same_submission:
                    raise ValueError(f'{app}/{platform}: build must exceed {released["build"]}')
            matrix.append({'app': app, 'platform': platform})
    return matrix


def prepare(args):
    app = args.app
    platforms = {p: {'version': v, 'build': args.build or int(time.time())}
                 for p in ('ios', 'android') if (v := getattr(args, f'{p}_version'))}
    if not platforms:
        raise ValueError('Provide --ios-version and/or --android-version')
    notes = Path(args.notes).read_text().strip()
    since = {}
    evidence = []
    for platform in platforms:
        tags = git('tag', '--list', f'{app}/{platform}/v*').splitlines()
        baseline = getattr(args, f'{platform}_since') or args.since or (max(tags, key=lambda t: version(t.rsplit('/v', 1)[1])) if tags else None)
        if not baseline:
            raise ValueError(f'No {app}/{platform} release tag yet; supply --since with the last shipped commit')
        for tag in tags:
            if version(platforms[platform]['version']) <= version(tag.rsplit('/v', 1)[1]):
                raise ValueError(f'{app}/{platform}: version must exceed {tag}')
            published = previous_plan(app, tag).get('platforms', {}).get(platform)
            if published and platforms[platform]['build'] <= published['build']:
                raise ValueError(f'{app}/{platform}: build must exceed {published["build"]}')
        since[platform] = git('rev-parse', '--verify', f'{baseline}^{{commit}}')
        subprocess.run(['git', 'merge-base', '--is-ancestor', since[platform], 'HEAD'], cwd=ROOT, check=True)
        evidence.append(f'## {platform}: {since[platform]}..HEAD\n\n' +
                        git('log', '--no-merges', '--format=%h %s', f'{since[platform]}..HEAD', '--',
                            *APPS[app]['paths'], *SHARED))
    plan = {'schema': 1, 'app': app, 'platforms': platforms, 'notes': notes, 'since': since}
    validate_plan(app, plan, json.loads(plan_path(app).read_text()) if plan_path(app).exists() else {})
    listing(app)
    write_json(plan_path(app), plan)
    (plan_path(app).parent / 'changes.md').write_text('\n\n'.join(evidence) + '\n')
    print(plan_path(app))


def export(app, platform, output, allow_unplanned=False):
    copy = listing(app)[platform]
    plan = json.loads(plan_path(app).read_text()) if plan_path(app).exists() else None
    if plan:
        validate_plan(app, plan)
    planned = plan and platform in plan['platforms']
    if not planned and not allow_unplanned:
        raise ValueError(f'{platform} is not in this release')
    folder = output / 'metadata' / 'en-US'
    folder.mkdir(parents=True, exist_ok=True)
    for key, text in copy.items():
        (folder / f'{key}.txt').write_text(text.strip() + '\n')
    if not planned:
        return
    notes_file = folder / ('release_notes.txt' if platform == 'ios' else 'changelogs/default.txt')
    notes_file.parent.mkdir(parents=True, exist_ok=True)
    notes_file.write_text(plan['notes'] + '\n')


def fingerprint():
    """Include local tracked edits, while ignoring build output and caches."""
    return hashlib.sha256((git('rev-parse', 'HEAD') + '\n' +
                           git('diff', 'HEAD', '--binary', '--', '.',
                               ':(exclude)**/__pycache__/**', ':(exclude)**/*.pyc')).encode()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    p = commands.add_parser('prepare')
    p.add_argument('--app', choices=APPS, required=True)
    p.add_argument('--ios-version')
    p.add_argument('--android-version')
    p.add_argument('--build', type=int)
    p.add_argument('--since', help='Shared baseline for first-time releases')
    p.add_argument('--ios-since', help='Override iOS changelog baseline')
    p.add_argument('--android-since', help='Override Android changelog baseline')
    p.add_argument('--notes', required=True, help='Reviewed plain-text release notes, maximum 500 characters')
    p = commands.add_parser('plan')
    p.add_argument('--base', required=True)
    p.add_argument('--head', default='HEAD')
    commands.add_parser('validate')
    p = commands.add_parser('export')
    p.add_argument('--app', choices=APPS, required=True)
    p.add_argument('--platform', choices=['ios', 'android'], required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--allow-unplanned', action='store_true', help='Export listing copy even without a release plan')
    args = parser.parse_args()
    if args.command == 'prepare':
        prepare(args)
    elif args.command == 'plan':
        print(json.dumps({'include': changed_plans(args.base, args.head)}))
    elif args.command == 'validate':
        for app in APPS:
            listing(app)
            if plan_path(app).exists():
                validate_plan(app, json.loads(plan_path(app).read_text()))
        print('Release plans and listing copy are valid')
    elif args.command == 'export':
        export(args.app, args.platform, args.output, args.allow_unplanned)


if __name__ == '__main__':
    main()
