"""Focused shared-quartet drift checks, no native SDK or third-party dependencies."""
import json
import shutil
import tempfile
import unittest
from pathlib import Path
import generate_quartet_artwork as g


class QuartetArtworkTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.definition = json.loads((g.ROOT / g.DEFINITION).read_text())
        self.outputs = g.outputs(self.definition)
        for path in [*self.outputs, g.ANDROID/'TagLoadingView.kt', g.IOS/'DPTagViewController.m']:
            target = self.root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(g.ROOT/path, target)

    def test_checked_in_outputs(self):
        for path, content in self.outputs.items():
            self.assertEqual((g.ROOT/path).read_text(), content, str(path))
        g.check_consumers(g.ROOT)

    def test_complete_curve_and_still_pose(self):
        rows = g.samples(self.definition)
        self.assertEqual(len(rows), 4)
        for row in rows:
            self.assertEqual(len(row), 121)
            self.assertEqual(row[0], row[-1])
            self.assertEqual(row[0], 0)
            self.assertGreaterEqual(min(row), -4)
            self.assertEqual(max(row), 0)
        self.assertEqual(self.definition['motion']['stillTranslations'], [0]*4)
        self.assertEqual(self.definition['motion']['periodSeconds'], 2.8)

    def test_each_shared_input_reaches_both_platforms(self):
        import copy
        for category, key, value in [('notes','headRx',8),('notes','stemLength',24),('notes','dy',-8),('staff','spacing',8),('colors','lightNote','#007BA3'),('motion','periodSeconds',2.4),('motion','stagger',0.16),('motion','lift',5),('artwork','width',204)]:
            with self.subTest(category=category, key=key):
                changed = copy.deepcopy(self.definition)
                changed[category][key] = value
                outputs = g.outputs(changed)
                for path in [g.ANDROID/'QuartetArtwork.kt', g.IOS/'TMQuartetArtwork.m']:
                    self.assertNotEqual(outputs[path], self.outputs[path])

    def test_consumers_reject_independent_geometry_palette_and_motion(self):
        replacements = {
            g.ANDROID/'TagLoadingView.kt': [('QuartetArtwork.translation','sin'),('QuartetArtwork.WIDTH','204f'),('QuartetArtwork.note','android.graphics.Path()'),('QuartetArtwork.PERIOD','2.4f'),('QuartetArtwork.lightNote','context.getColor(R.color.md_primary)')],
            g.IOS/'DPTagViewController.m': [('TMQuartetSamples(i)','@[@0, @(-4), @0]'),('TMQuartetWidth','204'),('TMQuartetNotePath()','CGPathCreateMutable()'),('TMQuartetPeriod','2.4'),('kCAAnimationLinear','kCAAnimationCubic'),('TMQuartetColor(','IndependentColor(')]
        }
        for path, mutations in replacements.items():
            original = (self.root/path).read_text()
            for old, new in mutations:
                with self.subTest(path=path, mutation=old):
                    (self.root/path).write_text(original.replace(old,new))
                    with self.assertRaises(ValueError): g.check_consumers(self.root)
                    (self.root/path).write_text(original)


if __name__ == '__main__':
    unittest.main()
