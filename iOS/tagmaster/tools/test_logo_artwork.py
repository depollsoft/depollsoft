#!/usr/bin/env python3
"""Offline shared-loader drift regression. Run with python3 -m unittest discover
-s iOS/tagmaster/tools -p 'test_logo_artwork.py'. No simulator or dependencies.
"""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
GENERATOR = Path('iOS/tagmaster/tools/generate_logo_artwork.py')
ANDROID = Path('Android/TagMaster/src/main/java/depollsoft/tagmaster/BarberPoleLoadingView.kt')
IOS = Path('iOS/tagmaster/tagmaster/TMBarberPoleLoadingView.m')
KOTLIN = ANDROID.with_name('BarberPoleLogo.kt')
OBJC = IOS.with_name('TMLogoArtwork.m')
FILES = [GENERATOR, ANDROID, IOS, KOTLIN, OBJC, IOS.with_name('TMLogoArtwork.h'),
         Path('shared/tagmaster/barberpole-loader.json'),
         Path('Android/TagMaster/src/main/res/drawable/ic_barberpole.xml'),
         Path('Android/TagMaster/src/main/res/layout/tagqueryview.xml'),
         Path('Android/TagMaster/src/main/res/values/barberpole_dimensions.xml'),
         Path('Android/TagMaster/src/main/res/values/barberpole_loader.xml'),
         IOS.with_name('DPTagQueryViewController.m')]
ASSET = Path('iOS/tagmaster/tagmaster/Images.xcassets/LaunchWatermark.imageset')
FILES += [ASSET / 'LaunchWatermark.pdf', ASSET / 'Contents.json']


class SharedLoaderDriftTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for relative in FILES:
            dest = self.root / relative
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / relative, dest)

    def check(self):
        return subprocess.run([sys.executable, str(self.root / GENERATOR), '--check'],
                              capture_output=True, text=True, timeout=30)

    def drift(self, file, old, new):
        path = self.root / file
        text = path.read_text()
        self.assertIn(old, text)
        path.write_text(text.replace(old, new))
        result = self.check()
        self.assertNotEqual(result.returncode, 0, result.stdout)
        self.assertRegex(result.stderr, 'Stale generated artwork|Renderer bypasses|Independent renderer')

    def test_current_native_outputs_and_pdf(self):
        result = self.check()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_kotlin_path_drift(self):
        self.drift(KOTLIN, '253.47f', '253.48f')

    def test_objc_palette_drift(self):
        self.drift(OBJC, '0.74509804', '0.9')

    def test_shared_period_requires_regeneration(self):
        self.drift(Path('shared/tagmaster/barberpole-loader.json'), '"stripeStep": 108', '"stripeStep": 96')

    def test_android_independent_palette(self):
        self.drift(ANDROID, 'BarberPoleLogo.RED', 'Color.RED')

    def test_android_independent_period(self):
        self.drift(ANDROID, 'BarberPoleLogo.STRIPE_STEP', '96f')

    def test_ios_independent_palette(self):
        self.drift(IOS, 'TMLoaderColor(@"white")', 'UIColor.systemRedColor.CGColor')

    def test_android_compact_size_drift(self):
        self.drift(ANDROID, 'BarberPoleLogo.COMPACT_HEIGHT', '31f')

    def test_ios_compact_size_drift(self):
        self.drift(IOS, 'TMLoaderCompactHeight', '31')

    def test_compact_definition_requires_regeneration(self):
        self.drift(Path('shared/tagmaster/barberpole-loader.json'), '"compactArtworkHeight": 32', '"compactArtworkHeight": 30')

    def test_compact_xml_size_drift(self):
        self.drift(Path('Android/TagMaster/src/main/res/values/barberpole_loader.xml'),
                   '@dimen/barberpole_compact_height', '31dp')

    def test_ios_independent_period(self):
        self.drift(IOS, 'TMLoaderStripeStep * TMLoaderPhaseMultiplier', '96')


if __name__ == '__main__':
    unittest.main()
