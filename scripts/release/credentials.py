#!/usr/bin/env python3
"""Materialize Android CI secrets into runner-temp files without printing them."""

import base64
import json
import os
from pathlib import Path


def main():
    root = Path(os.environ['RUNNER_TEMP'])
    keystore_path = root / 'release-upload.jks'
    play_path = root / 'release-play.json'
    try:
        keystore = base64.b64decode(
            ''.join(os.environ['ANDROID_UPLOAD_KEYSTORE'].split()), validate=True)
        if not keystore:
            raise ValueError('Empty keystore')
        raw = os.environ['PLAY_SERVICE_ACCOUNT_JSON'].strip()
        key = json.loads(raw if raw.startswith('{') else base64.b64decode(
            ''.join(raw.split()), validate=True))
        if not isinstance(key, dict) or key.get('type') != 'service_account':
            raise ValueError('Not a service account')

        # Validate both secrets before writing either file. Never leave a partial
        # preparation behind, including when exporting GITHUB_ENV fails.
        for path, data in [(keystore_path, keystore), (play_path, json.dumps(key).encode())]:
            path.touch(mode=0o600, exist_ok=True)
            path.chmod(0o600)
            path.write_bytes(data)
        with open(os.environ['GITHUB_ENV'], 'a') as env:
            env.write(f'ANDROID_UPLOAD_KEYSTORE_PATH={keystore_path}\n'
                      f'PLAY_SERVICE_ACCOUNT_JSON_PATH={play_path}\n')
    except (KeyError, ValueError, OSError):
        keystore_path.unlink(missing_ok=True)
        play_path.unlink(missing_ok=True)
        raise SystemExit('Could not prepare Android deploy credentials. Check the CI secrets and runner paths.') from None


if __name__ == '__main__':
    main()
