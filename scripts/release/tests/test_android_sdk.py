from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'ci'))
from android_sdk import BUILD_PACKAGES, find_sdk, missing_files, install


class InstalledSDKTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name).resolve()
        for name in missing_files(self.root, BUILD_PACKAGES):
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'fixture')
            path.chmod(0o755)

    def test_reuses_complete_sdk_after_missing_candidate(self):
        self.assertEqual(find_sdk(BUILD_PACKAGES, [None, self.root / 'absent', self.root]), self.root)

    def test_missing_or_empty_packages_force_installation(self):
        for name in ['licenses/android-sdk-license', 'platform-tools/adb',
                     'platforms/android-37.0/android.jar', 'build-tools/36.0.0/lib/d8.jar']:
            with self.subTest(name=name):
                path = self.root / name
                data = path.read_bytes()
                path.write_bytes(b'')
                self.assertIsNone(find_sdk(BUILD_PACKAGES, [self.root]))
                path.unlink()
                self.assertIsNone(find_sdk(BUILD_PACKAGES, [self.root]))
                path.write_bytes(data)
                path.chmod(0o755)

    def test_nonexecutable_tools_force_installation(self):
        (self.root / 'platform-tools/adb').chmod(0o644)
        self.assertIsNone(find_sdk(BUILD_PACKAGES, [self.root]))

    def test_instrumented_job_also_requires_its_emulator_images(self):
        packages = BUILD_PACKAGES + ['emulator', 'system-images;android-30;google_apis;arm64-v8a']
        self.assertEqual(missing_files(self.root, packages),
                         ['emulator/emulator', 'system-images/android-30/google_apis/arm64-v8a/system.img'])

    def test_unknown_package_fails_instead_of_silently_skipping_it(self):
        with self.assertRaises(ValueError):
            missing_files(self.root, ['unknown'])

    def test_complete_sdk_does_not_run_installer(self):
        with patch('android_sdk.subprocess.run') as run:
            install(self.root, BUILD_PACKAGES)
        run.assert_not_called()

    def test_installs_with_existing_tools_and_checks_the_result(self):
        manager = self.root / 'cmdline-tools/latest/bin/sdkmanager'
        manager.parent.mkdir(parents=True)
        manager.write_bytes(b'fixture')
        manager.chmod(0o755)
        platform = self.root / 'platforms/android-37.0/android.jar'
        platform.unlink()
        with patch('android_sdk.subprocess.run') as run:
            with self.assertRaisesRegex(RuntimeError, 'incomplete'):
                install(self.root, BUILD_PACKAGES)
        self.assertEqual(run.call_args.args[0],
                         [str(manager), f'--sdk_root={self.root}', '--install', *BUILD_PACKAGES])
        self.assertEqual(run.call_args.kwargs['timeout'], 240)
        self.assertTrue(run.call_args.kwargs['check'])
        with patch('android_sdk.subprocess.run', side_effect=lambda *a, **kw: platform.write_bytes(b'download')):
            install(self.root, BUILD_PACKAGES)
        self.assertFalse(missing_files(self.root, BUILD_PACKAGES))
