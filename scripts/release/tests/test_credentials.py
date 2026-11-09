import base64
import json
import os
import stat
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / 'credentials.py'
KEY = {'type': 'service_account', 'private_key': 'fake-private-key'}
KEY_JSON = json.dumps(KEY)


class CredentialTests(unittest.TestCase):
    def prepare(self, root, *, play=KEY_JSON, keystore='a2V5c3RvcmU=', export_fails=False):
        env_file = root / 'environment'
        result = subprocess.run([sys.executable, str(SCRIPT)], check=False, capture_output=True, text=True,
                                env={**os.environ, 'RUNNER_TEMP': str(root),
                                     'GITHUB_ENV': str(root if export_fails else env_file),
                                     'ANDROID_UPLOAD_KEYSTORE': keystore,
                                     'PLAY_SERVICE_ACCOUNT_JSON': play})
        self.assertNotIn('fake-private-key', result.stdout + result.stderr)
        return result, env_file

    def test_raw_and_base64_json_and_wrapped_inputs(self):
        encoded = base64.b64encode(json.dumps(KEY).encode()).decode()
        for play in [json.dumps(KEY), encoded, '\n'.join(encoded[i:i+12] for i in range(0, len(encoded), 12))]:
            with self.subTest(play=play), tempfile.TemporaryDirectory() as folder:
                root = Path(folder)
                result, env_file = self.prepare(root, play=play, keystore=' a2V5\nc3RvcmU= ')
                self.assertEqual(result.returncode, 0, result.stderr)
                paths = dict(line.split('=', 1) for line in env_file.read_text().splitlines())
                keystore = Path(paths['ANDROID_UPLOAD_KEYSTORE_PATH'])
                key = Path(paths['PLAY_SERVICE_ACCOUNT_JSON_PATH'])
                self.assertEqual(keystore.read_bytes(), b'keystore')
                self.assertEqual(json.loads(key.read_text()), KEY)
                for path in (keystore, key):
                    self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)

    def test_invalid_inputs_do_not_create_or_export_credentials(self):
        for changes in [
            {'keystore': ''}, {'keystore': 'not-base64'},
            {'play': ''}, {'play': 'not-base64'}, {'play': '{'},
            {'play': '[]'}, {'play': '{"type":"authorized_user"}'},
        ]:
            with self.subTest(changes=changes), tempfile.TemporaryDirectory() as folder:
                root = Path(folder)
                result, env_file = self.prepare(root, play=changes.get('play', json.dumps(KEY)),
                                                keystore=changes.get('keystore', 'a2V5c3RvcmU='))
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse((root / 'release-upload.jks').exists())
                self.assertFalse((root / 'release-play.json').exists())
                self.assertFalse(env_file.exists())

    def test_existing_files_get_private_permissions(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for name in ['release-upload.jks', 'release-play.json']:
                path = root / name
                path.write_text('old')
                path.chmod(0o644)
            result, _ = self.prepare(root)
            self.assertEqual(result.returncode, 0, result.stderr)
            for name in ['release-upload.jks', 'release-play.json']:
                self.assertEqual(stat.S_IMODE((root / name).stat().st_mode), 0o600)

    def test_export_failure_removes_prepared_files(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            result, _ = self.prepare(root, export_fails=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(list(root.iterdir()), [])
