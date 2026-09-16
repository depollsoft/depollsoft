import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import release
import capture
from PIL import Image, ImageDraw


def plan(app='tagmaster', platforms=None):
    platforms = platforms or {'ios': {'version': '2.0.3', 'build': 1800000000}}
    return {'schema': 1, 'app': app, 'platforms': platforms,
            'notes': 'Improved tablet navigation.', 'since': {p: 'a' * 40 for p in platforms}}


class PlanTests(unittest.TestCase):
    def test_platform_versions_are_independent(self):
        value = plan(platforms={'ios': {'version': '2.0.3', 'build': 1800000000},
                                'android': {'version': '5.2.2', 'build': 1800000001}})
        release.validate_plan('tagmaster', value, {})

    def test_rejects_wrong_app_version_regression_and_invalid_notes(self):
        cases = [dict(plan(), app='pitchperfect'), dict(plan(), notes='TODO'), dict(plan(), notes='a' * 501)]
        bad = plan()
        bad['platforms']['ios']['version'] = '2.0.2'
        cases.append(bad)
        for value in cases:
            with self.subTest(value=value), self.assertRaises(ValueError):
                release.validate_plan('tagmaster', value, {})

    def test_build_must_increase(self):
        old = plan()
        new = plan()
        new['platforms']['ios']['version'] = '2.0.4'
        with self.assertRaises(ValueError):
            release.validate_plan('tagmaster', new, old)

    def test_single_app_export_does_not_include_other_app(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'store/tagmaster').mkdir(parents=True)
            (root / 'store/tagmaster/listing.json').write_text((release.ROOT / 'store/tagmaster/listing.json').read_text())
            (root / 'releases/tagmaster').mkdir(parents=True)
            (root / 'releases/tagmaster/release.json').write_text(json.dumps(plan()))
            with patch.object(release, 'ROOT', root):
                release.export('tagmaster', 'ios', root / 'output')
            self.assertEqual((root / 'output/metadata/en-US/name.txt').read_text().strip(), 'Tag Master')
            self.assertFalse((root / 'output/metadata/en-US/title.txt').exists())
            self.assertIn('tablet', (root / 'output/metadata/en-US/release_notes.txt').read_text())


class GitFixture:
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.patch = patch.object(release, 'ROOT', self.root)
        self.patch.start()
        self.git('init', '-q')
        self.git('config', 'user.name', 'Release test')
        self.git('config', 'user.email', 'release@example.invalid')
        (self.root / 'README').write_text('Repository')
        self.commit()
        self.base = self.git('rev-parse', 'HEAD')

    def tearDown(self):
        self.patch.stop()
        self.temp.cleanup()

    def git(self, *args):
        return subprocess.check_output(['git', *args], cwd=self.root, text=True).strip()

    def commit(self):
        self.git('add', '.')
        self.git('commit', '-qm', 'test')

    def add_plan(self, value):
        path = release.plan_path(value['app'])
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value))
        self.commit()


class GitPlanTests(GitFixture, unittest.TestCase):
    def test_infrastructure_does_not_deploy(self):
        (self.root / 'script.py').write_text('print(1)')
        self.commit()
        self.assertEqual(release.changed_plans(self.base), [])

    def test_tagmaster_release_does_not_deploy_pitchperfect(self):
        self.add_plan(plan())
        self.assertEqual(release.changed_plans(self.base), [{'app': 'tagmaster', 'platform': 'ios'}])

    def test_both_apps_and_platforms(self):
        self.add_plan(plan())
        self.add_plan(plan('pitchperfect', {'android': {'version': '4.0.1', 'build': 1800000000}}))
        self.assertEqual(len(release.changed_plans(self.base)), 2)

    def test_retry_same_submission_but_reject_reusing_version_on_new_commit(self):
        self.add_plan(plan())
        self.git('tag', 'tagmaster/ios/v2.0.3')
        self.assertEqual(len(release.changed_plans(self.base)), 1)
        (self.root / 'README').write_text('Different source')
        self.commit()
        with self.assertRaises(ValueError):
            release.changed_plans(self.base)

    def test_prepare_preserves_platform_baselines_and_only_writes_selected_app(self):
        (self.root / 'README').write_text('Second release baseline')
        self.commit()
        android_base = self.git('rev-parse', 'HEAD')
        notes = self.root / 'notes.txt'
        notes.write_text('Improved tablet navigation.')
        args = SimpleNamespace(app='tagmaster', ios_version='2.0.3', android_version='5.2.2',
                               build=1800000000, notes=str(notes), since=None,
                               ios_since=self.base, android_since=android_base)
        with patch.object(release, 'listing', return_value={}):
            release.prepare(args)
        result = json.loads(release.plan_path('tagmaster').read_text())
        self.assertEqual(result['since'], {'ios': self.base, 'android': android_base})
        self.assertFalse(release.plan_path('pitchperfect').exists())

    def test_alternating_platforms_cannot_regress(self):
        self.add_plan(plan())
        self.git('tag', 'tagmaster/ios/v2.0.3')
        self.add_plan(plan(platforms={'android': {'version': '5.2.2', 'build': 1800000000}}))
        base = self.git('rev-parse', 'HEAD')
        self.add_plan(plan())
        with self.assertRaises(ValueError):
            release.changed_plans(base)


class CaptureTests(unittest.TestCase):
    def test_missing_wrong_size_blank_and_duplicate_screenshots_fail(self):
        with tempfile.TemporaryDirectory() as folder:
            dest = Path(folder)
            path = dest / 'screenshots/en-US/iphone-01-pitch-pipe-light.png'
            path.parent.mkdir(parents=True)
            with self.assertRaises(FileNotFoundError):
                capture.validate('pitchperfect', 'ios', dest)
            Image.new('RGB', (100, 100), 'red').save(path)
            with self.assertRaises(ValueError):
                capture.validate('pitchperfect', 'ios', dest)
            Image.new('RGB', (1320, 2868), 'white').save(path)
            with self.assertRaises(ValueError):
                capture.validate('pitchperfect', 'ios', dest)
            image = Image.new('RGB', (1320, 2868), 'white')
            ImageDraw.Draw(image).rectangle((0, 0, 800, 1000), fill='black')
            image.save(path)
            image.save(path.with_name('iphone-02-notes-light.png'))
            with self.assertRaisesRegex(ValueError, 'Duplicate'):
                capture.validate('pitchperfect', 'ios', dest)


class ExpandedCaptureTests(unittest.TestCase):
    def test_store_limits_do_not_drop_review_scenes(self):
        for app, config in release.APPS.items():
            for platform, maximum in [('ios', 10), ('android', 8)]:
                paths = [capture.screenshot_path(app, platform, 'device', f'{scene}-{theme}')
                         for theme in ('light', 'dark') for scene in config['scenes']]
                uploads = [p for p in paths if not p.startswith('review/')]
                self.assertEqual(len(uploads), maximum)
                self.assertEqual(len(set(paths)), 2 * len(config['scenes']))
                self.assertTrue(any('dark' in p for p in uploads))
                for position, scene in enumerate(config['store_scenes'][platform], 1):
                    path = capture.screenshot_path(app, platform, 'device', scene)
                    self.assertIn(f'{position:02}-{scene[3:]}', path)

    def test_missing_dark_review_scene_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder, patch.object(capture, 'IOS_DEVICES', {'iphone': ('test', {(32, 48)})}):
            dest = Path(folder)
            i = 0
            for theme in ('light', 'dark'):
                for scene in release.APPS['pitchperfect']['scenes']:
                    path = dest / capture.screenshot_path('pitchperfect', 'ios', 'iphone', f'{scene}-{theme}')
                    path.parent.mkdir(parents=True, exist_ok=True)
                    image = Image.new('RGB', (32, 48), 'white')
                    ImageDraw.Draw(image).rectangle((0, 0, 12, 12+i), fill=(10*i, 0, 0))
                    image.save(path)
                    i += 1
            self.assertEqual(len(capture.validate('pitchperfect', 'ios', dest)), 12)
            (dest / capture.screenshot_path('pitchperfect', 'ios', 'iphone', '02-notes-dark')).unlink()
            with self.assertRaises(FileNotFoundError):
                capture.validate('pitchperfect', 'ios', dest)

    def test_pitch_perfect_android_requires_the_derived_store_icon(self):
        import icons
        with tempfile.TemporaryDirectory() as folder, patch.object(capture, 'ANDROID_DEVICES', {'phoneScreenshots': ('32x48', '1')}):
            dest = Path(folder)
            i = 0
            for theme in ('light', 'dark'):
                for scene in release.APPS['pitchperfect']['scenes']:
                    path = dest / capture.screenshot_path('pitchperfect', 'android', 'phoneScreenshots', f'{scene}-{theme}')
                    path.parent.mkdir(parents=True, exist_ok=True)
                    image = Image.new('RGB', (32, 48), 'white')
                    ImageDraw.Draw(image).rectangle((0, 0, 12, 12 + i), fill=(10 * i, 0, 0))
                    image.save(path)
                    i += 1
            for shape, shade in (('round', 40), ('square', 200)):
                path = dest / f'metadata/en-US/images/wearScreenshots/{shape}.png'
                path.parent.mkdir(parents=True, exist_ok=True)
                image = Image.new('RGB', (384, 384), 'black')
                ImageDraw.Draw(image).ellipse((40, 40, 300, 300), fill=(shade, shade, shade))
                image.save(path)
            icon = dest / 'metadata/en-US/images/icon.png'
            with self.assertRaises(FileNotFoundError):
                capture.validate('pitchperfect', 'android', dest)
            rendered = icons.store('production')
            self.assertEqual((rendered.size, rendered.mode, rendered.getpixel((0, 0))), ((512, 512), 'RGB', icons.PLATE))
            rendered.save(icon)
            self.assertIn('metadata/en-US/images/icon.png', capture.validate('pitchperfect', 'android', dest))
            Image.new('RGB', (256, 256), 'white').save(icon)
            with self.assertRaisesRegex(ValueError, 'store icon'):
                capture.validate('pitchperfect', 'android', dest)

    def test_play_baseline_only_collects_screenshots(self):
        from review import PlayScreenshots
        parser = PlayScreenshots()
        parser.feed('<img src="https://example.com/icon"><img data-screenshot-index="0" src="https://example.com/a=w526-h296"><img data-screenshot-index="0" src="https://example.com/a=w526-h296">')
        self.assertEqual(parser.urls, ['https://example.com/a=s1600'])


if __name__ == '__main__':
    unittest.main()
