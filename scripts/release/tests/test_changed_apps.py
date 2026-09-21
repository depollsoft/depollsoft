import json
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
import changed_apps
from changed_apps import (APPS, COMPARE_FILE_LIMIT, OWNED, SHARED_INSIDE_OWNED, changed_files,
                          everything, outputs, select)


def apps(android=(), ios=(), shared=()):
    return {'android': list(android), 'ios': list(ios), 'shared': list(shared)}


class SelectionTests(unittest.TestCase):
    def test_app_owned_paths_select_only_that_app_on_that_platform(self):
        self.assertEqual(select(['Android/TagMaster/src/main/java/A.kt']), apps(android=['tagmaster']))
        self.assertEqual(select(['iOS/pitchperfect/pitchperfect/AppDelegate.swift',
                                 'Android/PitchPerfectWear/build.gradle']),
                         apps(android=['pitchperfect'], ios=['pitchperfect']))
        self.assertEqual(select(['Android/PitchPerfectLicense/src/main/AndroidManifest.xml']),
                         apps(android=['pitchperfect']))

    def test_shared_code_selects_every_app_on_its_platform_and_marks_it_shared(self):
        for path in ['Android/PitchPerfectLib/src/main/java/A.java', 'Android/Bindroid',
                     'Android/DepollSoftCommon.Compat/build.gradle', 'Android/build.gradle',
                     'Android/gradle/wrapper/gradle-wrapper.properties', 'Android/buildSrc/build.gradle.kts',
                     'Android/depollsoft.lib.kotlin/src/main/A.kt', 'Android/fastlane/Fastfile',
                     '.github/workflows/android.yml', 'scripts/ci/android_sdk.py']:
            self.assertEqual(select([path]), apps(android=APPS, shared=['android']), path)
        for path in ['iOS/depolllib/depolllib/A.swift', 'iOS/shared/TelemetryConsent.swift',
                     'iOS/iOS.xcworkspace/xcshareddata/swiftpm/Package.resolved', 'iOS/Gemfile.lock',
                     'iOS/fastlane/Fastfile', 'iOS/pitchperfect/pitchperfectlib/Note.swift',
                     '.github/workflows/ios.yml', '.github/workflows/ios-app-tests.yml',
                     'scripts/ci/ios_suites.py']:
            self.assertEqual(select([path]), apps(ios=APPS, shared=['ios']), path)

    def test_one_owned_file_per_app_selects_both_apps_but_no_shared_code(self):
        self.assertEqual(select(['Android/TagMaster/a', 'Android/PitchPerfect/b']), apps(android=APPS))
        self.assertEqual(select(['iOS/tagmaster/a', 'iOS/pitchperfect/pitchperfect/b']), apps(ios=APPS))

    def test_prefixes_do_not_bleed_into_similarly_named_modules(self):
        self.assertEqual(select(['Android/PitchPerfectLib/build.gradle'])['android'], list(APPS))
        self.assertEqual(select(['Android/PitchPerfect/build.gradle'])['android'], ['pitchperfect'])
        for platform, owners in OWNED.items():
            for prefixes in owners.values():
                for prefix in prefixes:
                    self.assertTrue(prefix.endswith('/'), prefix)
        for prefix in SHARED_INSIDE_OWNED:
            self.assertTrue(prefix.endswith('/'), prefix)

    def test_unrelated_paths_select_nothing(self):
        self.assertEqual(select(['api/src/index.ts', 'README.md', 'Firebase/tagmaster/x']), apps())

    def test_workflow_and_selector_changes_select_everything(self):
        for path in ['.github/workflows/pr-preview.yml', 'scripts/ci/changed_apps.py',
                     '.github/workflows/deploy-pr-preview.yml']:
            self.assertEqual(select(['api/x', path]), everything(), path)
        self.assertEqual(everything()['shared'], ['android', 'ios'])


class OutputTests(unittest.TestCase):
    def test_outputs_cover_lists_flags_and_gradle_modules(self):
        result = outputs(select(['Android/TagMaster/a', 'iOS/depolllib/b']))
        self.assertEqual(json.loads(result['android']), ['tagmaster'])
        self.assertEqual(json.loads(result['ios']), ['pitchperfect', 'tagmaster'])
        self.assertEqual(result['android-any'], 'true')
        self.assertEqual(result['android-shared'], 'false')
        self.assertEqual(result['ios-shared'], 'true')
        self.assertEqual(result['android-pitchperfect'], 'false')
        self.assertEqual(result['android-tagmaster'], 'true')
        self.assertEqual(result['android-ci-modules'], 'TagMaster')
        self.assertEqual(result['android-preview-modules'], 'TagMaster')
        empty = outputs(select([]))
        self.assertEqual((empty['ios'], empty['ios-any'], empty['android-ci-modules']), ('[]', 'false', ''))

    def test_both_apps_selected_by_owned_files_is_not_shared(self):
        result = outputs(select(['Android/TagMaster/a', 'Android/PitchPerfect/b']))
        self.assertEqual(result['android-shared'], 'false')
        self.assertEqual(result['android-ci-modules'],
                         'PitchPerfect PitchPerfectWear PitchPerfectLicense TagMaster')
        self.assertEqual(result['android-preview-modules'], 'PitchPerfect PitchPerfectWear TagMaster')
        self.assertEqual(outputs(everything())['android-shared'], 'true')

    def test_license_only_changes_compile_the_license_app_in_ci_but_not_previews(self):
        result = outputs(select(['Android/PitchPerfectLicense/src/main/AndroidManifest.xml']))
        self.assertEqual(result['android-ci-modules'], 'PitchPerfect PitchPerfectWear PitchPerfectLicense')
        self.assertEqual(result['android-preview-modules'], 'PitchPerfect PitchPerfectWear')


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

    def test_a_possibly_truncated_compare_selects_everything(self):
        event = {'before': 'a' * 40, 'after': 'b' * 40}
        listing = '\n'.join(f'api/file{index}' for index in range(COMPARE_FILE_LIMIT)) + '\n'
        with patch.object(changed_apps, 'gh', return_value=listing):
            self.assertIsNone(changed_files('push', event, 'o/r'))
        listing = '\n'.join(f'api/file{index}' for index in range(COMPARE_FILE_LIMIT - 1)) + '\n'
        with patch.object(changed_apps, 'gh', return_value=listing):
            self.assertEqual(len(changed_files('push', event, 'o/r')), COMPARE_FILE_LIMIT - 1)

    def test_manual_and_scheduled_runs_select_everything(self):
        for name in ('workflow_dispatch', 'schedule'):
            self.assertIsNone(changed_files(name, {}, 'o/r'))


if __name__ == '__main__':
    unittest.main()
