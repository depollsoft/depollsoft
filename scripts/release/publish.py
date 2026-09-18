#!/usr/bin/env python3
"""Record completed store submissions under immutable app/platform tags."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import tarfile
import tempfile
from release import APPS, ROOT, git, plan_path, validate_plan, version as parse_version

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('command', choices=['identity', 'publish'])
args = parser.parse_args()

app = os.environ['RELEASE_APP']
platform = os.environ['RELEASE_PLATFORM']
if app not in APPS or platform not in ('ios', 'android'):
    raise ValueError('Unknown release target')
plan = json.loads(plan_path(app).read_text())
validate_plan(app, plan)
version = plan['platforms'][platform]['version']
tag = f'{app}/{platform}/v{version}'
sha = git('rev-parse', 'HEAD')
for published in git('tag', '--list', f'{app}/{platform}/v*').splitlines():
    if parse_version(published.rsplit('/v', 1)[1]) > parse_version(version):
        raise ValueError(f'A newer release already shipped: {published}; do not retry an old deployment')
exists = bool(git('tag', '--list', tag))
if exists and git('rev-list', '-n', '1', tag) != sha:
    raise ValueError(f'{tag} already refers to a different source commit')
if args.command == 'identity':
    # A tag alone is not a success marker. Only a published GitHub release with
    # its asset bundle counts, so interrupted release publication can recover.
    result = subprocess.run(['gh', 'release', 'view', tag, '--json', 'isDraft,assets'], capture_output=True, text=True)
    done = False
    if result.returncode == 0:
        release = json.loads(result.stdout)
        done = not release['isDraft'] and any(a['name'] == 'store-assets.tar.gz' for a in release['assets'])
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write(f'done={str(done).lower()}\n')
else:
    assets = Path(os.environ['RELEASE_ASSETS'])
    with tempfile.TemporaryDirectory() as folder:
        archive = Path(folder) / 'store-assets.tar.gz'
        with tarfile.open(archive, 'w:gz') as tar:
            tar.add(assets, arcname='assets')
            tar.add(plan_path(app), arcname='release.json')
        notes = Path(folder) / 'notes.md'
        notes.write_text(plan['notes'] + f'\n\nSource: {sha}\n\nSubmitted to the store. Publication follows store review.\n')
        result = subprocess.run(['gh', 'release', 'view', tag], capture_output=True)
        if result.returncode != 0:
            subprocess.run(['gh', 'release', 'create', tag, '--target', sha, '--draft', '--title',
                            f'{APPS[app]["name"]} {platform} {version}', '--notes-file', str(notes)], check=True)
        subprocess.run(['gh', 'release', 'upload', tag, str(archive), '--clobber'], check=True)
        subprocess.run(['gh', 'release', 'edit', tag, '--draft=false'], check=True)
