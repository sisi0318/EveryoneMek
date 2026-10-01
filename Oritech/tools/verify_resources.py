"""Check released dependency asset contracts and the distributable without starting a client."""
from pathlib import Path
from zipfile import ZipFile
import argparse
import json
import struct

parser = argparse.ArgumentParser()
parser.add_argument('--oritech', type=Path, required=True)
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
    shell = json.loads((resources / 'assets/oritechmekanism/models/block/universal_processor.json').read_text(encoding='utf-8'))
    assert shell['parent'] == 'minecraft:block/cube'
    assert {'north', 'south', 'east', 'west', 'up', 'down', 'particle'} <= shell['textures'].keys()
    for reference in set(shell['textures'].values()):
        namespace, path = reference.split(':', 1)
        assert namespace == 'oritech', reference
        png = upstream.read(f'assets/{namespace}/textures/{path}.png')
        assert struct.unpack('>II', png[16:24]) == (16, 16), reference
    recipe = json.loads((resources / 'data/oritechmekanism/recipe/universal_processor.json').read_text(encoding='utf-8'))
    assert recipe['pattern'] == ['PCP', 'MKM', 'PCP']
    for ingredient in recipe['key'].values():
        namespace, item = ingredient['item'].split(':', 1)
        assert namespace == 'oritech' and f'assets/oritech/models/item/{item}.json' in names, ingredient
    assert recipe['result'] == {'id': 'oritechmekanism:universal_processor', 'count': 1}
    fluid_model = json.loads((resources / 'assets/oritechmekanism/models/item/fluid_capacity_addon.json').read_text(encoding='utf-8'))
    assert fluid_model['parent'] == 'oritech:item/machine_fluid_addon'
    assert 'assets/oritech/models/item/machine_fluid_addon.json' in names
    assert fluid_model['textures']['0'] == 'oritech:block/machine_fluid_addon_on'
    assert 'assets/oritech/textures/block/machine_fluid_addon_on.png' in names
    fluid_recipe = json.loads((resources / 'data/oritechmekanism/recipe/fluid_capacity_addon.json').read_text(encoding='utf-8'))
    assert fluid_recipe['result'] == {'id': 'oritechmekanism:fluid_capacity_addon', 'count': 1}
    for value in fluid_recipe['key'].values():
        assert value['item'].startswith('oritech:')
        assert f"assets/oritech/models/item/{value['item'].split(':')[1]}.json" in names
    for tier in (2, 3):
        assert not (resources / f'assets/oritechmekanism/models/item/capacity_upgrade_{tier}.json').exists()
        assert not (resources / f'data/oritechmekanism/recipe/capacity_upgrade_{tier}.json').exists()
    for tier in range(4, 8):
        model = json.loads((resources / f'assets/oritechmekanism/models/item/capacity_upgrade_{tier}.json').read_text(encoding='utf-8'))
        assert model['textures']['layer0'] == f'oritech:block/machine_core_{tier}'
        png = upstream.read(f'assets/oritech/textures/block/machine_core_{tier}.png')
        assert struct.unpack('>II', png[16:24]) == (16, 16)
        upgrade = json.loads((resources / f'data/oritechmekanism/recipe/capacity_upgrade_{tier}.json').read_text(encoding='utf-8'))
        assert upgrade['key']['K']['item'] == f'oritech:machine_core_{tier}'
        for value in upgrade['key'].values():
            assert value['item'].startswith('oritech:')
            assert f"assets/oritech/models/item/{value['item'].split(':')[1]}.json" in names
    for part in ['center'] + [f'ring_{i}' for i in range(1, 7)]:
        assert f'assets/oritech/textures/gui/modular/machine_core/{part}.png' in names
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
        for tier in (2, 3):
            assert f'assets/oritechmekanism/models/item/capacity_upgrade_{tier}.json' not in files
            assert f'data/oritechmekanism/recipe/capacity_upgrade_{tier}.json' not in files
        for local in resources.rglob('*.json'):
            path=local.relative_to(resources).as_posix()
            assert json.loads(jar.read(path)) == json.loads(local.read_text(encoding='utf-8')), path
print(f'PASS: {len(profiles)} original model/texture/animation sets, Oritech shell and crafting ingredients, native GUI assets, {len(zh)} paired language keys' + (' and release JAR' if args.jar else ''))
