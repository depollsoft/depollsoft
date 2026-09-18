import base64
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / 'signing.sh'


class SigningTests(unittest.TestCase):
    def run_signing(self, *, write=False, ssh_read=True, ssh_write=False,
                    https_read=True, https_write=True, authorization='user:fake-token'):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            binaries = root / 'bin'
            binaries.mkdir()
            git = binaries / 'git'
            git.write_text('''#!/usr/bin/env python3
import json, os, sys
kind = 'https' if any(arg.startswith('https:') for arg in sys.argv) else 'ssh'
operation = sys.argv[1]
permission = 'WRITE' if operation == 'push' else 'READ'
with open(os.environ['CALLS'], 'a') as calls:
    calls.write(json.dumps({'kind': kind, 'args': sys.argv[1:],
        'authorization': os.environ.get('GIT_CONFIG_VALUE_0')}) + '\\n')
if operation == 'push' and '--dry-run' not in sys.argv:
    raise SystemExit('A real push is forbidden in this test')
raise SystemExit(0 if os.environ[kind.upper() + '_' + permission] == '1' else 1)
''')
            git.chmod(0o755)
            scan = binaries / 'ssh-keyscan'
            scan.write_text('#!/bin/sh\necho fake-host-key\n')
            scan.chmod(0o755)
            env_file = root / 'environment'
            env_file.touch()
            calls_file = root / 'calls'
            calls_file.touch()
            environment = {
                **os.environ, 'PATH': str(binaries) + os.pathsep + os.environ['PATH'],
                'RUNNER_TEMP': str(root), 'GITHUB_ENV': str(env_file),
                'GITHUB_RUN_ID': '123', 'CALLS': str(calls_file),
                'RELEASE_SIGNING_WRITE': str(write).lower(),
                'MATCH_GIT_SSH_KEY': 'fake-private-key',
                'MATCH_GIT_BASIC_AUTHORIZATION': authorization,
                'SSH_READ': str(int(ssh_read)), 'SSH_WRITE': str(int(ssh_write)),
                'HTTPS_READ': str(int(https_read)), 'HTTPS_WRITE': str(int(https_write)),
            }
            result = subprocess.run(['bash', str(SCRIPT)], env=environment,
                                    text=True, capture_output=True)
            selected = dict(line.split('=', 1) for line in env_file.read_text().splitlines())
            calls = [json.loads(line) for line in calls_file.read_text().splitlines()]
            self.assertNotIn('fake-private-key', result.stdout + result.stderr)
            self.assertNotIn('fake-token', result.stdout + result.stderr)
            self.assertNotIn('fake-token', json.dumps([call['args'] for call in calls]))
            return result, selected, calls

    def test_deployment_uses_read_only_ssh_and_clears_unused_basic_auth(self):
        result, selected, calls = self.run_signing()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(selected['MATCH_GIT_URL'], 'git@github.com:depollsoft/certificates.git')
        self.assertEqual(selected['MATCH_GIT_BASIC_AUTHORIZATION'], '')
        self.assertEqual([(c['kind'], c['args'][0]) for c in calls], [('ssh', 'ls-remote')])

    def test_failed_ssh_falls_back_to_https(self):
        result, selected, calls = self.run_signing(ssh_read=False)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue(selected['MATCH_GIT_URL'].startswith('https://'))
        self.assertEqual([c['kind'] for c in calls], ['ssh', 'https'])

    def test_provisioning_prefers_https_and_probes_write_without_mutating(self):
        result, selected, calls = self.run_signing(write=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([c['kind'] for c in calls], ['https', 'https'])
        self.assertEqual(calls[1]['args'][0:2], ['push', '--dry-run'])
        self.assertEqual(selected['GIT_SSH_COMMAND'], '')

    def test_provisioning_falls_back_to_writable_ssh(self):
        result, selected, calls = self.run_signing(write=True, https_write=False, ssh_write=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([c['kind'] for c in calls], ['https', 'https', 'ssh', 'ssh'])
        self.assertTrue(selected['MATCH_GIT_URL'].startswith('git@'))

    def test_read_only_credentials_cannot_provision(self):
        result, selected, _ = self.run_signing(write=True, https_write=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(selected, {})
        self.assertIn('read/write access', result.stderr)

    def test_failed_credentials_do_not_export_a_selection(self):
        result, selected, _ = self.run_signing(ssh_read=False, https_read=False)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(selected, {})

    def test_https_normalizes_supported_secret_formats_for_fastlane(self):
        normalized = base64.b64encode(b'user:fake-token').decode()
        for value in ('user:fake-token', normalized, 'Basic ' + normalized,
                      'Authorization: Basic ' + normalized):
            with self.subTest(value=value):
                result, selected, calls = self.run_signing(ssh_read=False, authorization=value)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(selected['MATCH_GIT_BASIC_AUTHORIZATION'], normalized)
                self.assertEqual(calls[-1]['authorization'], 'Authorization: Basic ' + normalized)
                self.assertIn('::add-mask::' + normalized, result.stdout)

    def test_raw_token_uses_token_username(self):
        result, selected, _ = self.run_signing(ssh_read=False, authorization='fake-token')
        self.assertEqual(result.returncode, 0, result.stderr)
        decoded = base64.b64decode(selected['MATCH_GIT_BASIC_AUTHORIZATION']).decode()
        self.assertEqual(decoded, 'x-access-token:fake-token')
