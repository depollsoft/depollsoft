from io import BytesIO
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import review


class BaselineTests(unittest.TestCase):
    def test_download_requires_fresh_directory(self):
        image = BytesIO()
        Image.new('RGB', (20, 40), 'blue').save(image, format='PNG')
        listings = {
            'ios': json.dumps({'results': [{
                'screenshotUrls': ['https://example.com/phone'],
                'ipadScreenshotUrls': ['https://example.com/tablet'],
                'trackViewUrl': 'https://example.com/listing',
            }]}).encode(),
            'android': b'<img data-screenshot-index="0" src="https://example.com/phone">',
        }
        for platform, listing in listings.items():
            with self.subTest(platform=platform), tempfile.TemporaryDirectory() as folder:
                dest = Path(folder) / 'baseline'
                downloads = [listing, image.getvalue()]
                if platform == 'ios':
                    downloads.append(image.getvalue())
                with patch.object(review, 'fetch', side_effect=downloads):
                    review.current('tagmaster', platform, dest)
                review.gallery(dest)
                family = 'iphone' if platform == 'ios' else 'android'
                stale = dest / 'current-store' / family / '09.jpg'
                stale.write_bytes(image.getvalue())
                before = {p.relative_to(dest): p.read_bytes() for p in dest.rglob('*') if p.is_file()}
                with patch.object(review, 'fetch') as fetch:
                    with self.assertRaises(FileExistsError):
                        review.current('tagmaster', platform, dest)
                    fetch.assert_not_called()
                after = {p.relative_to(dest): p.read_bytes() for p in dest.rglob('*') if p.is_file()}
                self.assertEqual(before, after)
                self.assertTrue((dest / 'index.html').exists())
                self.assertTrue((dest / 'current-store/sources.json').exists())


if __name__ == '__main__':
    unittest.main()
