"""Fish colors and Fish of Thieves eating frames, generated from the real textures (owner 2026-10-11).

  python make-fish-art.py

Reads Fish of Thieves (MIT) and Farmer's Delight from ../.deps and vanilla from the Loom cache, then writes:
  assets/holylois/fish_palettes.json   three main colors per fish (per variant for Fish of Thieves) plus a readable name
                                        color, used by FishTooltip for the swatches and the colored variant name
  ../fish-eating/                       a resource pack: three bite frames for every Fish of Thieves fish, raw and cooked,
                                        wired like the Eating Animation pack (using_item -> use_duration frames)
Rerun after a Fish of Thieves update: the pack replaces its item model files.
"""
import colorsys, io, json, math, re, shutil, zipfile
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
DEPS = HERE.parent / '.deps'
FOT = next(DEPS.glob('fish_of_thieves-*.jar'))
FD = next(DEPS.glob('FarmersDelight-*.jar'))
VANILLA = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'
PALETTES = HERE / 'assets/holylois/fish_palettes.json'
PACK = HERE.parent / 'fish-eating'
SPECIES = ['ancientscale', 'battlegill', 'devilfish', 'islehopper', 'plentifin', 'pondie', 'splashtail', 'stormfish',
           'wildsplash', 'wrecker']
VANILLA_FISH = ['cod', 'salmon', 'cooked_cod', 'cooked_salmon', 'pufferfish', 'tropical_fish']
FD_FISH = ['cod_slice', 'salmon_slice', 'cooked_cod_slice', 'cooked_salmon_slice']
CUTS = (0.78, 0.56, 0.34)          # share of the fish left in frames 0..2 (the Eating Animation cod eats about this much)
THRESHOLDS = (0.3, 0.55, 0.8)


def image(jar, path):
    return Image.open(io.BytesIO(jar.read(path))).convert('RGBA')


def hexcolor(rgb): return '#%02X%02X%02X' % tuple(int(round(c)) for c in rgb)


def palette(im):
    """Three main colors (biggest first) and a name color that reads on a dark tooltip."""
    w, h = im.size
    px = {(x, y): im.getpixel((x, y)) for x in range(w) for y in range(h) if im.getpixel((x, y))[3] > 0}
    def edge(p): return any((p[0] + dx, p[1] + dy) not in px for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    lum = {p: 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2] for p, c in px.items()}
    dark = sorted(lum.values())[max(0, len(lum) // 8 - 1)]
    body = [px[p][:3] for p in px if not (edge(p) and lum[p] <= dark)] or [c[:3] for c in px.values()]
    # k-means with three centers seeded at the luminance quartiles (deterministic).
    ordered = sorted(body, key=lambda c: 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2])
    centers = [ordered[int(len(ordered) * q)] for q in (0.2, 0.5, 0.8)]
    for _ in range(12):
        groups = [[] for _ in centers]
        for c in body: groups[min(range(len(centers)), key=lambda i: sum((c[k] - centers[i][k]) ** 2 for k in range(3)))].append(c)
        centers = [tuple(sum(c[k] for c in g) / len(g) for k in range(3)) if g else centers[i] for i, g in enumerate(groups)]
    ranked = sorted(zip(centers, groups), key=lambda cg: -len(cg[1]))
    def score(cg):
        r, g, b = (v / 255 for v in cg[0]); _, l, s = colorsys.rgb_to_hls(r, g, b)
        return len(cg[1]) * (0.35 + s)
    # The most telling color names the fish (size weighted by saturation); a pale fish (Bone, Sand) keeps its pale body.
    first = ranked[0][0]
    main = first if colorsys.rgb_to_hls(*(v / 255 for v in first))[1] >= 0.75 else max(ranked, key=score)[0]
    r, g, b = (v / 255 for v in main); hh, l, s = colorsys.rgb_to_hls(r, g, b)
    name = colorsys.hls_to_rgb(hh, min(0.78, max(0.58, l)), min(s, 0.85))
    return {'palette': [hexcolor(c) for c, _ in ranked], 'color': hexcolor(tuple(v * 255 for v in name))}


def frames(im, cooked):
    """Three bitten copies: the head end (top right) goes first, along the diagonal, with a soft bite edge."""
    w, h = im.size
    px = [(x, y) for x in range(w) for y in range(h) if im.getpixel((x, y))[3] > 0]
    axis = {p: p[0] - p[1] for p in px}
    lo, hi = min(axis.values()), max(axis.values())
    flesh = (205, 150, 95) if cooked else (232, 158, 138)
    out = []
    for cut in CUTS:
        keep = {p for p in px if (axis[p] - lo) / max(1, hi - lo) <= cut + 0.045 * math.sin((p[0] + p[1]) * 1.7)}
        # Only the piece still in one bite-free chunk stays: no floating specks past the bite edge.
        near = lambda p: [(p[0] + dx, p[1] + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        keep = {p for p in keep if sum(q in keep for q in near(p)) >= 2}
        if keep:
            start = min(keep, key=lambda p: axis[p]); body, todo = {start}, [start]
            while todo:
                for q in near(todo.pop()):
                    if q in keep and q not in body: body.add(q); todo.append(q)
            keep = body
        frame = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        for p in keep:
            c = im.getpixel(p)
            bitten = any((p[0] + dx, p[1] + dy) in axis and (p[0] + dx, p[1] + dy) not in keep for dx, dy in ((1, 0), (0, -1), (1, -1)))
            if bitten: c = tuple(int(c[k] * 0.55 + flesh[k] * 0.45) for k in range(3)) + (c[3],)
            frame.putpixel(p, c)
        out.append(frame)
    return out


def wrap(node, frame_models):
    """Every plain model leaf becomes: not eating -> the leaf; eating -> bite frames by use time."""
    if isinstance(node, dict):
        if node.get('type') == 'minecraft:model' and node.get('model') in frame_models:
            entries = [{'threshold': t, 'model': {'type': 'minecraft:model', 'model': m}} for t, m in zip(THRESHOLDS, frame_models[node['model']])]
            return {'type': 'minecraft:condition', 'property': 'minecraft:using_item', 'on_false': node,
                    'on_true': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:use_duration', 'scale': 0.03,
                                'entries': entries, 'fallback': node}}
        return {k: wrap(v, frame_models) for k, v in node.items()}
    if isinstance(node, list): return [wrap(v, frame_models) for v in node]
    return node


def main():
    fot, fd, vanilla = zipfile.ZipFile(FOT), zipfile.ZipFile(FD), zipfile.ZipFile(VANILLA)
    lang = json.loads(fot.read('assets/fishofthieves/lang/en_us.json'))
    palettes = {}
    if PACK.exists(): shutil.rmtree(PACK)
    (PACK / 'assets').mkdir(parents=True)
    for name in SPECIES + ['cooked_' + s for s in SPECIES]:
        items_path = f'assets/fishofthieves/items/{name}.json'
        definition = json.loads(fot.read(items_path))
        models = sorted(set(re.findall(r'"model":\s*"(fishofthieves:item/[a-z_]+)"', json.dumps(definition))))
        frame_models = {}
        for model in models:
            texture = json.loads(fot.read(f'assets/fishofthieves/models/item/{model.split("/")[-1]}.json'))['textures']['layer0']
            tex_name = texture.split('/')[-1]
            im = image(fot, f'assets/fishofthieves/textures/item/{tex_name}.png')
            names = []
            for i, frame in enumerate(frames(im, name.startswith('cooked_'))):
                frame_name = f'{tex_name}_bite{i}'
                (PACK / 'assets/holylois/textures/item/eating').mkdir(parents=True, exist_ok=True)
                (PACK / 'assets/holylois/models/item/eating').mkdir(parents=True, exist_ok=True)
                frame.save(PACK / f'assets/holylois/textures/item/eating/{frame_name}.png', optimize=True)
                (PACK / f'assets/holylois/models/item/eating/{frame_name}.json').write_text(json.dumps(
                    {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'holylois:item/eating/{frame_name}'}}, indent=2) + '\n', encoding='utf-8')
                names.append(f'holylois:item/eating/{frame_name}')
            frame_models[model] = names
        (PACK / 'assets/fishofthieves/items').mkdir(parents=True, exist_ok=True)
        (PACK / f'assets/fishofthieves/items/{name}.json').write_text(json.dumps(wrap(definition, frame_models), indent=2) + '\n', encoding='utf-8')
        # Palettes: per variant for raw fish (custom_data "<species>_variant"), one for cooked fish.
        entry = {}
        if name in SPECIES:
            entry['variant_key'] = f'{name}_variant'; entry['variants'] = {}
            for case in definition['model'].get('cases', []):
                variant = case['when'][f'{name}_variant']
                model = case['model']['model']
                tex = json.loads(fot.read(f'assets/fishofthieves/models/item/{model.split("/")[-1]}.json'))['textures']['layer0'].split('/')[-1]
                v = palette(image(fot, f'assets/fishofthieves/textures/item/{tex}.png'))
                v['name_key'] = f'entity.fishofthieves.{name}.{variant.split(":")[1]}'
                v['name'] = lang.get(v['name_key'], variant.split(':')[1].title())
                entry['variants'][variant] = v
            fallback = definition['model'].get('fallback', {}).get('model')
            if fallback:
                tex = json.loads(fot.read(f'assets/fishofthieves/models/item/{fallback.split("/")[-1]}.json'))['textures']['layer0'].split('/')[-1]
                entry.update(palette(image(fot, f'assets/fishofthieves/textures/item/{tex}.png')))
        else:
            entry.update(palette(image(fot, f'assets/fishofthieves/textures/item/{name}.png')))
        palettes[f'fishofthieves:{name}'] = entry
    for name in VANILLA_FISH: palettes[f'minecraft:{name}'] = palette(image(vanilla, f'assets/minecraft/textures/item/{name}.png'))
    for name in FD_FISH: palettes[f'farmersdelight:{name}'] = palette(image(fd, f'assets/farmersdelight/textures/item/{name}.png'))
    PALETTES.write_text(json.dumps(palettes, indent=1, ensure_ascii=False) + '\n', encoding='utf-8')
    (PACK / 'pack.mcmeta').write_text(json.dumps({'pack': {
        'description': 'Holy Lois: Fish of Thieves eating frames',
        'min_format': 97, 'max_format': 120}}, indent=2) + '\n', encoding='utf-8')
    (PACK / 'CREDITS.txt').write_text(
        'Bite frames generated from the Fish of Thieves textures by SteveKunG (MIT License,\n'
        'https://github.com/SteveKunG/FishOfThieves). Wired like Eating Animation (CC-BY-4.0).\n', encoding='utf-8')
    shutil.copy2(HERE / 'pack.png', PACK / 'pack.png')
    print(f'{len(palettes)} fish palettes, {sum(1 for _ in (PACK / "assets/holylois/textures/item/eating").glob("*.png"))} bite frames')


if __name__ == '__main__':
    main()
