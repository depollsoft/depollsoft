#!/usr/bin/env python3
"""Build a screenshot gallery, optionally downloading a separate store baseline."""
import argparse
from datetime import datetime, timezone
from html import escape
from html.parser import HTMLParser
from io import BytesIO
import json
from pathlib import Path
import ssl
from urllib.request import urlopen

import certifi
from PIL import Image, ImageDraw
from release import APPS, write_json


class PlayScreenshots(HTMLParser):
    def __init__(self):
        super().__init__()
        self.urls = []

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == 'img' and attrs.get('data-screenshot-index') is not None:
            url = attrs['src'].split('=')[0] + '=s1600'
            if url not in self.urls:
                self.urls.append(url)


def fetch(url):
    # Self-hosted Python installations may not have a system CA bundle.
    context = ssl.create_default_context(cafile=certifi.where())
    with urlopen(url, timeout=60, context=context) as response:
        return response.read()


def current(app, platform, dest):
    bundle = APPS[app]['bundle_id']
    if platform == 'ios':
        source = f'https://itunes.apple.com/lookup?bundleId={bundle}&country=us'
        results = json.loads(fetch(source))['results']
        if len(results) != 1:
            raise ValueError(f'Cannot identify current listing for {app}')
        listing = results[0]
        groups = {'iphone': listing['screenshotUrls'], 'ipad': listing['ipadScreenshotUrls']}
        listing_url = listing['trackViewUrl']
    else:
        source = f'https://play.google.com/store/apps/details?id={bundle}&hl=en_US&gl=US'
        parser = PlayScreenshots()
        parser.feed(fetch(source).decode())
        groups = {'android': parser.urls}
        listing_url = source
    if any(not urls for urls in groups.values()):
        raise ValueError('Store returned no screenshots; review cannot silently omit the baseline')
    for family, urls in groups.items():
        directory = dest / 'current-store' / family
        directory.mkdir(parents=True, exist_ok=True)
        for index, url in enumerate(urls, 1):
            with Image.open(BytesIO(fetch(url))) as image:
                image.convert('RGB').save(directory / f'{index:02}.jpg', quality=95)
    write_json(dest / 'current-store/sources.json', {
        'app': app, 'platform': platform, 'listing': listing_url,
        'retrieved_at': datetime.now(timezone.utc).isoformat(), 'screenshots': groups,
    })


def gallery(dest):
    groups = {}
    for path in sorted(dest.rglob('*')):
        if path.suffix not in ('.png', '.jpg') or 'contact-sheets' in path.parts:
            continue
        relative = path.relative_to(dest)
        groups.setdefault(str(relative.parent), []).append(relative)
    contacts = dest / 'contact-sheets'
    contacts.mkdir(exist_ok=True)
    sections = []
    for index, (name, paths) in enumerate(groups.items()):
        width, height, columns = 300, 510, 4
        sheet = Image.new('RGB', (width * columns, height * ((len(paths) + columns - 1) // columns)), '#e7e7e7')
        draw = ImageDraw.Draw(sheet)
        cards = []
        for i, relative in enumerate(paths):
            x, y = (i % columns) * width, (i // columns) * height
            with Image.open(dest / relative) as source:
                thumbnail = source.convert('RGB')
                thumbnail.thumbnail((width - 16, height - 35))
                sheet.paste(thumbnail, (x + (width - thumbnail.width) // 2, y + 30))
            draw.text((x + 8, y + 8), relative.stem, fill='black')
            url = escape(relative.as_posix(), quote=True)
            cards.append(f'<a href="{url}"><img src="{url}" loading="lazy"><span>{escape(relative.stem)}</span></a>')
        sheet.save(contacts / f'{index:02}.jpg', quality=90)
        sections.append(f'<h2>{escape(name)}</h2><p><a href="contact-sheets/{index:02}.jpg">Contact sheet</a></p><section>{"".join(cards)}</section>')
    (dest / 'index.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><title>Release screenshot review</title>
<style>body{font:16px system-ui;margin:32px;background:#eee;color:#222}section{display:flex;flex-wrap:wrap;gap:16px}a{color:inherit}section a{width:240px}img{width:100%;display:block}span{display:block;margin:8px 0}h2{margin-top:48px}</style>
<h1>Release screenshot review</h1><p>screenshots/ and metadata/ contain the selected store uploads. review/ contains additional captured scenes.</p>
''' + ''.join(sections))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--app', required=True, choices=APPS)
    parser.add_argument('--platform', required=True, choices=['ios', 'android'])
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--download-current', action='store_true',
                        help='Download store screenshots into a separate baseline output directory')
    args = parser.parse_args()
    if args.download_current:
        current(args.app, args.platform, args.output)
    gallery(args.output)
