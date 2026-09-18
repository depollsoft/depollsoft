from contextlib import ExitStack, nullcontext
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import capture
from PIL import Image


class IOSCaptureTests(unittest.TestCase):
    def exercise(self, app, *, fail_build=False, retry=False):
        calls = []
        tours = 0

        def run(*args, **kwargs):
            nonlocal tours
            args = list(map(str, args))
            calls.append(args)
            if 'build-for-testing' in args and fail_build:
                raise subprocess.CalledProcessError(65, args)
            if 'test-without-building' in args:
                tours += 1
                if retry and tours == 1:
                    raise subprocess.CalledProcessError(65, args)
            if args[:4] == ['xcrun', 'xcresulttool', 'export', 'attachments']:
                dest = Path(args[args.index('--output-path') + 1])
                dest.mkdir()
                attachments = []
                color = 'white' if dest.name.endswith('light') else 'black'
                for scene in capture.APPS[app]['scenes']:
                    name = f'store-{scene}'
                    Image.new('RGB', (2, 2), color).save(dest / (name + '.png'))
                    attachments.append({'suggestedHumanReadableName': name + '_1.png',
                                        'exportedFileName': name + '.png'})
                (dest / 'manifest.json').write_text(json.dumps([{'attachments': attachments}]))

        runtime = json.dumps({'runtimes': [{'name': 'iOS 26.2', 'version': '26.2',
                                           'identifier': 'runtime', 'isAvailable': True}]})
        with tempfile.TemporaryDirectory() as folder, ExitStack() as stack:
            dest = Path(folder)
            stack.enter_context(patch.dict(capture.os.environ, {'DEVELOPER_DIR': '/test/Xcode'}))
            stack.enter_context(patch.object(capture.subprocess, 'check_output', side_effect=['26.2', runtime]))
            uninstall = stack.enter_context(patch.object(capture.subprocess, 'run'))
            stack.enter_context(patch.object(capture, 'run', side_effect=run))
            stack.enter_context(patch.object(capture.time, 'sleep'))
            stack.enter_context(patch.object(capture.ios_simulator, 'allocation', side_effect=nullcontext))
            stack.enter_context(patch.object(capture.ios_simulator, 'clean_abandoned'))
            create = stack.enter_context(patch.object(capture.ios_simulator, 'create', side_effect=['phone', 'tablet']))
            stack.enter_context(patch.object(capture.ios_simulator, 'boot'))
            stack.enter_context(patch.object(capture.ios_simulator, 'shutdown'))
            delete = stack.enter_context(patch.object(capture.ios_simulator, 'delete'))
            if fail_build:
                with self.assertRaises(subprocess.CalledProcessError):
                    capture.ios(app, dest)
                create.assert_not_called()
            else:
                capture.ios(app, dest)
                self.assertEqual([call.args[0] for call in delete.call_args_list], ['phone', 'tablet'])
                self.assertTrue(all(call.args[0][:3] == ['xcrun', 'simctl', 'uninstall']
                                    for call in uninstall.call_args_list))
                self.assertEqual(uninstall.call_count, tours)
                expected = set()
                for family in ('iphone', 'ipad'):
                    for scene in capture.APPS[app]['scenes']:
                        for theme, color in [('light', (255, 255, 255)), ('dark', (0, 0, 0))]:
                            relative = capture.screenshot_path(app, 'ios', family, f'{scene}-{theme}')
                            expected.add(relative)
                            with Image.open(dest / relative) as image:
                                self.assertEqual(image.getpixel((0, 0)), color)
                self.assertEqual({str(p.relative_to(dest)) for p in dest.rglob('*.png')}, expected)
            return calls, json.loads((dest / 'capture-timings.json').read_text())

    def test_all_appearances_reuse_one_build_with_fresh_app_data(self):
        for app in capture.APPS:
            with self.subTest(app=app):
                calls, timings = self.exercise(app)
                self.assertEqual(sum('build-for-testing' in c for c in calls), 1)
                self.assertEqual(sum('test-without-building' in c for c in calls), 4)
                self.assertFalse(any('erase' in c for c in calls))
                self.assertEqual([t['stage'] for t in timings],
                                 ['build', 'iphone-light-0', 'iphone-dark-0', 'ipad-light-0', 'ipad-dark-0'])
                self.assertTrue(all(t['success'] for t in timings))

    def test_retry_resets_own_device_without_rebuilding(self):
        calls, timings = self.exercise('pitchperfect', retry=True)
        self.assertEqual(sum('build-for-testing' in c for c in calls), 1)
        self.assertEqual(sum('test-without-building' in c for c in calls), 5)
        self.assertIn(['xcrun', 'simctl', 'erase', 'phone'], calls)
        self.assertEqual([t['success'] for t in timings], [True, False, True, True, True, True])

    def test_build_failure_records_timing_without_starting_simulator(self):
        calls, timings = self.exercise('tagmaster', fail_build=True)
        self.assertEqual(len(calls), 1)
        self.assertEqual(timings[0]['stage'], 'build')
        self.assertFalse(timings[0]['success'])
