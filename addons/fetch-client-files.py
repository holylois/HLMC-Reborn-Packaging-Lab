"""Give the local test client what players get besides mods: the pack's shaderpacks and resource packs (from the signed
assets/pack.json, sha256-checked) and the shipped settings (assets/defaults.zip, applied by YOSBR on the first start).

Usage: python fetch-client-files.py [run/client|run/gametest]   (default run/client; existing files with the right hash are kept, settings never overwrite
what the test client already changed).
Unpublished candidates use matching files from work/release-VERSION (profile, verified-cache or release)
before falling back to the manifest URL. Local files must match both size and SHA-256.
"""
import hashlib, json, shutil, sys, urllib.error, urllib.request, zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parent / 'assets'


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1 << 20), b''): digest.update(block)
    return digest.hexdigest()


def matches(path, entry):
    return path.is_file() and path.stat().st_size == entry['size'] and sha256(path) == entry['sha256']

def prepare(client, assets=ASSETS, workspace=HERE.parent.parent):
    manifest = json.loads((assets / 'pack.json').read_text(encoding='utf-8'))
    version = manifest['version']
    if not all(part.isdigit() for part in version.split('.')):
        raise ValueError('Invalid candidate version')
    candidate = workspace / 'work' / ('release-' + version.replace('.', ''))
    fetched = copied = kept = 0
    for entry in manifest['files']:
        relative = Path(entry['path'])
        if relative.is_absolute() or '..' in relative.parts:
            raise ValueError(f'Unsafe pack path: {entry["path"]}')
        if not entry['path'].startswith(('shaderpacks/', 'resourcepacks/')): continue
        target = client / relative
        if matches(target, entry):
            kept += 1
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        # Private candidates have future release URLs. Use only bytes matching the signed manifest.
        sources = (candidate / 'profile' / relative,
                   candidate / 'verified-cache' / (entry['sha256'] + '.verified'),
                   candidate / 'release' / relative.name)
        source = next((path for path in sources if matches(path, entry)), None)
        if source is not None:
            shutil.copyfile(source, target)
            copied += 1
            continue
        request = urllib.request.Request(entry['url'], headers={'User-Agent': 'holylois-test-client'})
        try:
            with urllib.request.urlopen(request, timeout=300) as response:
                data = response.read()
        except urllib.error.HTTPError as error:
            raise SystemExit(f'Cannot fetch {entry["path"]} for pack {version}: HTTP {error.code}. '
                             f'For an unpublished candidate, prepare its local files in {candidate}.') from None
        if len(data) != entry['size'] or hashlib.sha256(data).hexdigest() != entry['sha256']:
            raise SystemExit(f'Size/hash mismatch for {entry["path"]}')
        target.write_bytes(data)
        fetched += 1

    settings = 0
    with zipfile.ZipFile(assets / 'defaults.zip') as defaults:
        for name in defaults.namelist():
            relative = Path(name)
            target = client / relative
            if name.endswith('/') or relative.is_absolute() or '..' in relative.parts or target.exists(): continue
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(defaults.read(name))
            settings += 1
    print(f'Test client: {copied} local packs copied, {fetched} downloaded, {kept} already there, '
          f'{settings} shipped settings files added in {client}')
    return copied, fetched, kept, settings


if __name__ == '__main__':
    prepare(HERE / (sys.argv[1] if len(sys.argv) > 1 else 'run/client'))
