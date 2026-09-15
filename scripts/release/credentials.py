#!/usr/bin/env python3
"""Materialize Android CI secrets into runner-temp files without printing them."""
import base64
import json
import os
from pathlib import Path

root = Path(os.environ['RUNNER_TEMP'])
files = {'ANDROID_UPLOAD_KEYSTORE_PATH': ('release-upload.jks', base64.b64decode(
    ''.join(os.environ['ANDROID_UPLOAD_KEYSTORE'].split()), validate=True))}
raw = os.environ['PLAY_SERVICE_ACCOUNT_JSON'].strip()
key = json.loads(raw if raw.startswith('{') else base64.b64decode(raw, validate=True))
if key.get('type') != 'service_account':
    raise ValueError('PLAY_SERVICE_ACCOUNT_JSON must be a service account')
files['PLAY_SERVICE_ACCOUNT_JSON_PATH'] = ('release-play.json', json.dumps(key).encode())
for name, (filename, data) in files.items():
    path = root / filename
    path.touch(mode=0o600, exist_ok=True)
    path.chmod(0o600)
    path.write_bytes(data)
    with open(os.environ['GITHUB_ENV'], 'a') as env:
        env.write(f'{name}={path}\n')
