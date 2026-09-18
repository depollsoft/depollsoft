import json
import os
from pathlib import Path
import runpy
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from test_release import GitFixture, plan


class PublishTests(GitFixture, unittest.TestCase):
    def invoke(self, command, response=None):
        calls = []
        original = subprocess.run
        def run(args, **kwargs):
            if args[0] != 'gh':
                return original(args, **kwargs)
            calls.append(args)
            if args[1:3] == ['release', 'view']:
                return subprocess.CompletedProcess(args, 0 if response is not None else 1,
                                                   json.dumps(response), '')
            return subprocess.CompletedProcess(args, 0, '', '')
        with tempfile.TemporaryDirectory() as folder:
            directory = Path(folder)
            assets = directory / 'assets'
            assets.mkdir()
            (assets / 'capture.json').write_text('{}')
            output = directory / 'output'
            environment = {'RELEASE_APP': 'tagmaster', 'RELEASE_PLATFORM': 'ios',
                           'GITHUB_OUTPUT': str(output), 'RELEASE_ASSETS': str(assets)}
            with patch.dict(os.environ, environment), patch('sys.argv', ['publish.py', command]), \
                 patch('subprocess.run', side_effect=run):
                runpy.run_path(str(Path(__file__).resolve().parents[1] / 'publish.py'), run_name='__main__')
            return calls, output.read_text() if output.exists() else ''

    def test_complete_release_is_skipped_but_draft_is_recoverable(self):
        self.add_plan(plan())
        self.git('tag', 'tagmaster/ios/v2.0.3')
        for draft in [False, True]:
            _, result = self.invoke('identity', {'isDraft': draft, 'assets': [{'name': 'store-assets.tar.gz'}]})
            self.assertEqual(result, f'done={str(not draft).lower()}\n')

    def test_first_record_is_created_with_assets_before_publication(self):
        self.add_plan(plan())
        calls, _ = self.invoke('publish')
        self.assertEqual([c[2] for c in calls], ['view', 'create', 'upload', 'edit'])
        self.assertIn('--draft', calls[1])
        self.assertIn(self.git('rev-parse', 'HEAD'), calls[1])
        self.assertIn('--draft=false', calls[-1])

    def test_refuses_to_move_tag_or_retry_behind_newer_release(self):
        self.add_plan(plan())
        self.git('tag', 'tagmaster/ios/v2.0.3')
        (self.root / 'README').write_text('New source')
        self.commit()
        with self.assertRaisesRegex(ValueError, 'different source commit'):
            self.invoke('publish')
        self.git('tag', 'tagmaster/ios/v2.0.4')
        with self.assertRaisesRegex(ValueError, 'newer release'):
            self.invoke('identity')
