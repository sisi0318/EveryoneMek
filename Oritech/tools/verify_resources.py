"""Check released dependency asset contracts and the distributable without starting a client."""
from pathlib import Path
from zipfile import ZipFile
import argparse
import json

parser = argparse.ArgumentParser()
parser.add_argument('--oritech', type=Path, required=True)
parser.add_argument('--mekanism', type=Path, required=True)
parser.add_argument('--jar', type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
profiles = ['pulverizer', 'fragment_forge', 'assembler', 'foundry', 'centrifuge', 'powered_furnace', 'refinery', 'cooler', 'atomic_forge', 'refinery_module']
with ZipFile(args.oritech) as upstream:
    names = set(upstream.namelist())
    for profile in profiles:
        stem = profile + '_block'
        geo = f'assets/oritech/geo/block/models/{stem}.geo.json'
        texture = f'assets/oritech/textures/block/models/{stem}.png'
        animation = f'assets/oritech/animations/block/models/{stem}.animation.json'
        for path in [geo, texture, animation]:
            assert path in names, path
        model = json.loads(upstream.read(geo))
        assert model['minecraft:geometry'][0]['bones'], geo
        animations = json.loads(upstream.read(animation))['animations']
        assert 'idle' in animations
        if profile != 'refinery_module':
            assert 'working' in animations
        if profile != 'pulverizer':
            assert {'deploy', 'packaged'} <= animations.keys()
        else:
            assert 'deploy' not in animations
    for path in ['gui_base.png', 'arrow_empty.png', 'arrow_full.png']:
        assert f'assets/oritech/textures/gui/modular/{path}' in names
with ZipFile(args.mekanism) as upstream:
    assert 'assets/mekanism/textures/block/steel_casing.png' in upstream.namelist()
zh = json.loads((resources / 'assets/oritechmekanism/lang/zh_cn.json').read_text(encoding='utf-8'))
en = json.loads((resources / 'assets/oritechmekanism/lang/en_us.json').read_text(encoding='utf-8'))
assert zh.keys() == en.keys()
for path in resources.rglob('*.json'):
    json.loads(path.read_text(encoding='utf-8'))
if args.jar:
    with ZipFile(args.jar) as jar:
        files = jar.namelist()
        assert not any('GameTest' in f or f.startswith(('art/', 'tools/', 'assets/oritech/', 'assets/mekanism/')) for f in files)
        assert 'META-INF/neoforge.mods.toml' in files
        assert 'dev/everyonemek/oritech/Processor.class' in files
        assert 'dev/everyonemek/oritech/client/ProcessorRenderer.class' in files
print(f'PASS: {len(profiles)} original model/texture/animation sets, native GUI assets, {len(zh)} paired language keys' + (' and release JAR' if args.jar else ''))
