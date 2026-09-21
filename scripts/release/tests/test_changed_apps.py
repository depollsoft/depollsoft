import json
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
import changed_apps
from changed_apps import APPS, OWNED, SHARED_INSIDE_OWNED, changed_files, everything, outputs, select


class SelectionTests(unittest.TestCase):
    def test_app_owned_paths_select_only_that_app_on_that_platform(self):
        self.assertEqual(select(['Android/TagMaster/src/main/java/A.kt']),
                         {'android': ['tagmaster'], 'ios': []})
        self.assertEqual(select(['iOS/pitchperfect/pitchperfect/AppDelegate.swift',
                                 'Android/PitchPerfectWear/build.gradle']),
                         {'android': ['pitchperfect'], 'ios': ['pitchperfect']})
        self.assertEqual(select(['Android/PitchPerfectLicense/src/main/AndroidManifest.xml']),
                         {'android': ['pitchperfect'], 'ios': []})

    def test_shared_code_selects_every_app_on_its_platform(self):
        for path in ['Android/PitchPerfectLib/src/main/java/A.java', 'Android/Bindroid',
                     'Android/DepollSoftCommon.Compat/build.gradle', 'Android/build.gradle',
                     'Android/gradle/wrapper/gradle-wrapper.properties', 'Android/buildSrc/build.gradle.kts',
                     'Android/depollsoft.lib.kotlin/src/main/A.kt', 'Android/fastlane/Fastfile',
                     '.github/workflows/android.yml', 'scripts/ci/android_sdk.py']:
            self.assertEqual(select([path]), {'android': list(APPS), 'ios': []}, path)
        for path in ['iOS/depolllib/depolllib/A.swift', 'iOS/shared/TelemetryConsent.swift',
                     'iOS/iOS.xcworkspace/xcshareddata/swiftpm/Package.resolved', 'iOS/Gemfile.lock',
                     'iOS/fastlane/Fastfile', 'iOS/pitchperfect/pitchperfectlib/Note.swift',
                     '.github/workflows/ios.yml', '.github/workflows/ios-app-tests.yml',
                     'scripts/ci/ios_suites.py']:
            self.assertEqual(select([path]), {'android': [], 'ios': list(APPS)}, path)

    def test_prefixes_do_not_bleed_into_similarly_named_modules(self):
        self.assertEqual(select(['Android/PitchPerfectLib/build.gradle'])['android'], list(APPS))
        self.assertEqual(select(['Android/PitchPerfect/build.gradle'])['android'], ['pitchperfect'])
        for platform, apps in OWNED.items():
            for prefixes in apps.values():
                for prefix in prefixes:
                    self.assertTrue(prefix.endswith('/'), prefix)
        for prefix in SHARED_INSIDE_OWNED:
            self.assertTrue(prefix.endswith('/'), prefix)

    def test_unrelated_paths_select_nothing_and_selection_stays_ordered(self):
        self.assertEqual(select(['api/src/index.ts', 'README.md', 'Firebase/tagmaster/x']),
                         {'android': [], 'ios': []})
        self.assertEqual(select(['Android/TagMaster/a', 'Android/PitchPerfect/b']),
                         {'android': list(APPS), 'ios': []})

    def test_workflow_and_selector_changes_select_everything(self):
        for path in ['.github/workflows/pr-preview.yml', 'scripts/ci/changed_apps.py',
                     '.github/workflows/deploy-pr-preview.yml']:
            self.assertEqual(select(['api/x', path]), everything(), path)


class OutputTests(unittest.TestCase):
    def test_outputs_cover_lists_flags_and_gradle_modules(self):
        result = outputs(select(['Android/TagMaster/a', 'iOS/depolllib/b']))
        self.assertEqual(json.loads(result['android']), ['tagmaster'])
        self.assertEqual(json.loads(result['ios']), ['pitchperfect', 'tagmaster'])
        self.assertEqual(result['android-any'], 'true')
        self.assertEqual(result['android-all'], 'false')
        self.assertEqual(result['ios-all'], 'true')
        self.assertEqual(result['android-pitchperfect'], 'false')
        self.assertEqual(result['android-tagmaster'], 'true')
        self.assertEqual(result['android-modules'], 'TagMaster')
        self.assertEqual(outputs(everything())['android-modules'],
                         'PitchPerfect PitchPerfectWear TagMaster')
        empty = outputs(select([]))
        self.assertEqual((empty['ios'], empty['ios-any'], empty['android-modules']), ('[]', 'false', ''))


class EventTests(unittest.TestCase):
    def test_pull_requests_list_files_including_rename_sources(self):
        with patch.object(changed_apps, 'gh', return_value='iOS/a\nold/b\n') as api:
            files = changed_files('pull_request', {'pull_request': {'number': 7}}, 'o/r')
        self.assertEqual(files, ['iOS/a', 'old/b'])
        self.assertIn('repos/o/r/pulls/7/files?per_page=100', api.call_args.args)
        self.assertIn('--paginate', api.call_args.args)

    def test_pushes_compare_before_and_after_or_fall_back_to_everything(self):
        event = {'before': 'a' * 40, 'after': 'b' * 40}
        with patch.object(changed_apps, 'gh', return_value='Android/TagMaster/x\n') as api:
            self.assertEqual(changed_files('push', event, 'o/r'), ['Android/TagMaster/x'])
        self.assertIn(f'repos/o/r/compare/{"a" * 40}...{"b" * 40}?per_page=100', api.call_args.args)
        with patch.object(changed_apps, 'gh', side_effect=subprocess.CalledProcessError(1, 'gh')):
            self.assertIsNone(changed_files('push', event, 'o/r'))
        with patch.object(changed_apps, 'gh') as api:
            self.assertIsNone(changed_files('push', {'before': '0' * 40, 'after': 'b'}, 'o/r'))
            api.assert_not_called()

    def test_manual_and_scheduled_runs_select_everything(self):
        for name in ('workflow_dispatch', 'schedule'):
            self.assertIsNone(changed_files(name, {}, 'o/r'))


if __name__ == '__main__':
    unittest.main()
