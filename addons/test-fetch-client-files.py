"""Private test preparation must work before the candidate is published."""
import hashlib
import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch, MagicMock

spec = importlib.util.spec_from_file_location('fetch_client_files', Path(__file__).with_name('fetch-client-files.py'))
fetch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fetch)


class ClientFilesTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.assets = self.root / 'assets'
        self.assets.mkdir()
        self.client = self.root / 'client'
        self.data = b'candidate pack bytes'
        self.entry = dict(path='resourcepacks/FishEating.zip', size=len(self.data),
                          sha256=hashlib.sha256(self.data).hexdigest(), url='https://example.invalid/unpublished.zip')
        (self.assets / 'pack.json').write_text(json.dumps(dict(version='1.10.0', files=[self.entry])), encoding='utf-8')
        with zipfile.ZipFile(self.assets / 'defaults.zip', 'w') as archive:
            archive.writestr('options.txt', 'shipped settings')
            archive.writestr('../escape.txt', 'unsafe')
        self.local = self.root / 'work/release-1100/profile' / self.entry['path']
        self.local.parent.mkdir(parents=True)

    def run_prepare(self):
        return fetch.prepare(self.client, self.assets, self.root)

    def test_local_candidate_without_network_and_existing_settings_preserved(self):
        self.local.write_bytes(self.data)
        self.client.mkdir()
        (self.client / 'options.txt').write_text('player settings')
        with patch.object(fetch.urllib.request, 'urlopen', side_effect=AssertionError('Network used')):
            self.assertEqual(self.run_prepare(), (1, 0, 0, 0))
            self.assertEqual(self.run_prepare(), (0, 0, 1, 0))
        self.assertEqual((self.client / 'options.txt').read_text(), 'player settings')
        self.assertFalse((self.root / 'escape.txt').exists())

    def test_stale_local_candidate_falls_back_to_verified_cache(self):
        self.local.write_bytes(b'stale')
        cache = self.root / 'work/release-1100/verified-cache' / (self.entry['sha256'] + '.verified')
        cache.parent.mkdir()
        cache.write_bytes(self.data)
        with patch.object(fetch.urllib.request, 'urlopen', side_effect=AssertionError('Network used')):
            self.assertEqual(self.run_prepare(), (1, 0, 0, 1))

    def test_bad_download_does_not_replace_existing_file(self):
        target = self.client / self.entry['path']
        target.parent.mkdir(parents=True)
        target.write_bytes(b'old')
        response = MagicMock()
        response.__enter__.return_value.read.return_value = b'wrong'
        with patch.object(fetch.urllib.request, 'urlopen', return_value=response):
            with self.assertRaisesRegex(SystemExit, 'Size/hash mismatch'):
                self.run_prepare()
        self.assertEqual(target.read_bytes(), b'old')

    def test_404_identifies_missing_candidate_file(self):
        error = fetch.urllib.error.HTTPError(self.entry['url'], 404, 'Not Found', None, None)
        with patch.object(fetch.urllib.request, 'urlopen', side_effect=error):
            with self.assertRaisesRegex(SystemExit, 'FishEating.zip.*1.10.0.*HTTP 404'):
                self.run_prepare()


if __name__ == '__main__':
    unittest.main()
