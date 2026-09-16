"""Checks for the store icon renderer and shared private launcher artwork."""

from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from PIL import Image

from scripts.release import icons


ROOT = Path(__file__).resolve().parents[3]
PLATE = (0x47, 0x47, 0x47)


class StoreIconTests(unittest.TestCase):
    def test_store_icons_have_opaque_square_plate(self):
        for variant in ("production", "private"):
            with self.subTest(variant=variant), icons.store(variant) as icon:
                self.assertEqual(icon.size, (512, 512))
                self.assertEqual(icon.mode, "RGB")
                self.assertEqual(icon.getpixel((0, 0)), PLATE)

    def test_private_store_icon_has_blue_beta_band(self):
        with icons.store("production") as production, icons.store("private") as private:
            self.assertNotEqual(private.tobytes(), production.tobytes())
            # Sample the band near the bottom centre, below the beta glyph.
            band_pixel = (256, 460)
            red, _, blue = private.getpixel(band_pixel)
            self.assertGreater(blue, red + 80)
            self.assertEqual(production.getpixel(band_pixel), PLATE)

    def test_private_cli_creates_listing_metadata_icon(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "metadata/en-US/images/icon.png"
            subprocess.run(
                [
                    sys.executable,
                    str(ROOT / "scripts/release/icons.py"),
                    "--variant", "private",
                    "--output", str(output),
                ],
                check=True,
                capture_output=True,
                text=True,
            )
            with Image.open(output) as rendered, icons.store("private") as expected:
                self.assertEqual(rendered.format, "PNG")
                self.assertEqual(rendered.size, (512, 512))
                self.assertEqual(rendered.mode, "RGB")
                self.assertEqual(rendered.tobytes(), expected.tobytes())

    def test_watch_private_foregrounds_match_phone_at_every_density(self):
        phone_res = ROOT / "Android/PitchPerfect/src/private/res"
        watch_res = ROOT / "Android/PitchPerfectWear/src/private/res"
        pattern = "mipmap-*/ic_launcher_foreground.png"
        phone_files = {path.relative_to(phone_res) for path in phone_res.glob(pattern)}
        watch_files = {path.relative_to(watch_res) for path in watch_res.glob(pattern)}
        self.assertTrue(phone_files, "No private phone foregrounds found")
        self.assertEqual(watch_files, phone_files)
        for relative_path in sorted(watch_files):
            with self.subTest(density=relative_path.parent.name):
                self.assertEqual(
                    (watch_res / relative_path).read_bytes(),
                    (phone_res / relative_path).read_bytes(),
                )


if __name__ == "__main__":
    unittest.main()
