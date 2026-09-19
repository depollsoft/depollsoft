from pathlib import Path
import os
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[2] / 'ci/upload_crashlytics_symbols.sh'


class CrashlyticsSymbolsTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix='crashlytics test ')
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.source = self.root / 'repo/iOS/pitchperfect'
        self.build = self.root / 'DerivedData/Build/Products'
        self.env = {
            **os.environ,
            'SRCROOT': str(self.source),
            'BUILD_DIR': str(self.build),
            'PLATFORM_NAME': 'iphoneos',
        }
        self.source.mkdir(parents=True)
        self.build.mkdir(parents=True)

    def run_script(self):
        return subprocess.run(['/bin/sh', str(SCRIPT)], env=self.env,
                              capture_output=True, text=True)

    def test_resolves_each_build_cache_with_spaces_in_path(self):
        caches = [self.root / 'DerivedData/SourcePackages',
                  self.source.parent / 'SourcePackages',
                  self.source.parents[1] / 'build/release/SourcePackages']
        for cache in caches:
            with self.subTest(cache=cache):
                runner = cache / 'checkouts/firebase-ios-sdk/Crashlytics/run'
                runner.parent.mkdir(parents=True)
                runner.write_text('echo "Crashlytics invoked for $PLATFORM_NAME"\nexit 17\n')
                result = self.run_script()
                self.assertEqual(result.returncode, 17, result.stderr)
                self.assertIn('Crashlytics invoked for iphoneos', result.stdout)
                runner.unlink()

    def test_simulator_skips_upload_without_package_checkout(self):
        self.env['PLATFORM_NAME'] = 'iphonesimulator'
        result = self.run_script()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('Skipping Crashlytics', result.stdout)

    def test_missing_device_upload_script_fails_build(self):
        result = self.run_script()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('error: Firebase Crashlytics upload script was not found', result.stderr)


if __name__ == '__main__':
    unittest.main()
