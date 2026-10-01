"""Generate owned data and models; original Oritech and Mekanism textures remain in their JARs."""
from pathlib import Path
import json
import gzip
import struct

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ID = 'oritechmekanism'

def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Oritech Mekanism'}})
write(f'assets/{ID}/blockstates/universal_processor.json', {'variants': {
    f'facing={f},deployed={str(d).lower()}': {'model': f'{ID}:block/' + ('empty' if d else 'universal_processor'), 'y': y}
    for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)] for d in [False, True]}})
write(f'assets/{ID}/blockstates/processor_part.json', {'variants': {'': {'model': f'{ID}:block/empty'}}})
write(f'assets/{ID}/models/block/empty.json', {'textures': {'particle': 'mekanism:block/steel_casing'}})
write(f'assets/{ID}/models/block/universal_processor.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'mekanism:block/steel_casing'}})
write(f'assets/{ID}/models/item/universal_processor.json', {'parent': f'{ID}:block/universal_processor'})
write(f'data/{ID}/loot_table/blocks/universal_processor.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{ID}:universal_processor', 'functions': [{'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'{ID}:processor']}]}]}]})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'{ID}:universal_processor', f'{ID}:processor_part']})
write(f'data/{ID}/recipe/universal_processor.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['ACA', 'ESE', 'ACA'], 'key': {'A': {'tag': 'c:alloys/advanced'}, 'C': {'tag': 'c:circuits/advanced'}, 'E': {'item': 'mekanism:energy_tablet'}, 'S': {'item': 'mekanism:steel_casing'}}, 'result': {'id': f'{ID}:universal_processor', 'count': 1}})
zh = {'block.oritechmekanism.universal_processor': '通用奥瑞处理器', 'block.oritechmekanism.processor_part': '奥瑞处理器部件'}
en = {'block.oritechmekanism.universal_processor': 'Universal Oritech Processor', 'block.oritechmekanism.processor_part': 'Oritech Processor Part'}
pairs = {
    'host': ('主机', 'Machine'), 'addons': ('插件', 'Addons'), 'settings': ('设置', 'Settings'),
    'host_hint': ('放入原机器主方块；更换前清空物料和储罐。原子锻造不能搭配处理室。', 'Insert an original machine block. Empty items and tanks before changing it. Atomic forging does not support processing chambers.'),
    'addon_hint': ('每格安装一个加工插件。组合插件单独安装；精炼罐室最多两个。', 'Install one processing addon per slot. Use a combined addon alone; up to two refinery modules may be added.'),
    'speed': ('速度 ×%s', 'Spd ×%s'), 'energy': ('耗能 ×%s', 'FE ×%s'),
    'progress': ('加工：%s / %s tick', 'Processing: %s / %s ticks'), 'charge': ('已充能：%s / %s FE', 'Charged: %s / %s FE'),
    'eject_on': ('自动输出：开', 'Eject: on'), 'eject_off': ('自动输出：关', 'Eject: off'),
}
for i, pair in enumerate([('缺少主机','Insert a machine'),('展开空间不足','Clear deployment space'),('缺少材料','Supply ingredients'),('输出空间不足','Empty outputs'),('电力不足','Supply power'),('红石暂停','Redstone pause'),('插件参数无效','Invalid addon settings'),('运行中','Working')]):
    pairs[f'status.{i}'] = pair
for i, pair in enumerate([('前','Front'),('后','Back'),('左','Left'),('右','Right'),('上','Up'),('下','Down')]):
    pairs[f'side.{i}'] = pair
for i, pair in enumerate([('双向','I/O'),('输入','In'),('输出','Out'),('关闭','Off')]):
    pairs[f'mode.{i}'] = pair
for key, (a, b) in pairs.items():
    zh[f'gui.{ID}.{key}'] = a
    en[f'gui.{ID}.{key}'] = b
for locale, data in [('zh_cn',zh),('en_us',en)]:
    write(f'assets/{ID}/lang/{locale}.json', data)

# Empty 16×12×16 GameTest structure, outside the runtime resource set.
def name(value):
    b = value.encode()
    return struct.pack('>H', len(b)) + b
def tag(kind, key, payload):
    return bytes([kind]) + name(key) + payload
nbt = b'\x0a\x00\x00' + tag(3, 'DataVersion', struct.pack('>i', 3955))
nbt += tag(9, 'size', b'\x03' + struct.pack('>iiii', 3, 16, 12, 16))
nbt += tag(9, 'palette', b'\x0a' + struct.pack('>i', 1) + tag(8, 'Name', name('minecraft:air')) + b'\x00')
nbt += tag(9, 'blocks', b'\x0a' + struct.pack('>i', 0)) + tag(9, 'entities', b'\x0a' + struct.pack('>i', 0)) + b'\x00'
test = ROOT / f'src/gameTest/resources/data/{ID}/structure/empty.nbt'
test.parent.mkdir(parents=True, exist_ok=True)
test.write_bytes(gzip.compress(nbt, mtime=0))
print(f'Generated {len(zh)} paired language keys and resources.')
