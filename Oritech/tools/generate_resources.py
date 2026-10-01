"""Generate owned data and models; original Oritech textures remain in its JAR."""
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
shell = 'oritech:block/iron_plating_block/particle'
write(f'assets/{ID}/models/block/empty.json', {'textures': {'particle': shell}})
write(f'assets/{ID}/models/block/universal_processor.json', {
    'parent': 'minecraft:block/cube',
    'textures': {'particle': shell, 'north': 'oritech:block/machine_core_3',
                 'south': shell, 'east': shell, 'west': shell, 'down': shell,
                 'up': 'oritech:block/machine_plating_block/particle'}})
write(f'assets/{ID}/models/item/universal_processor.json', {'parent': f'{ID}:block/universal_processor'})
write(f'data/{ID}/loot_table/blocks/universal_processor.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{ID}:universal_processor', 'functions': [{'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'{ID}:processor']}]}]}]})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'{ID}:universal_processor', f'{ID}:processor_part']})
write(f'data/{ID}/recipe/universal_processor.json', {'type': 'minecraft:crafting_shaped', 'category': 'redstone',
    'pattern': ['PCP', 'MKM', 'PCP'],
    'key': {'P': {'item': 'oritech:machine_plating_block'}, 'C': {'item': 'oritech:processing_unit'},
            'M': {'item': 'oritech:motor'}, 'K': {'item': 'oritech:machine_core_3'}},
    'result': {'id': f'{ID}:universal_processor', 'count': 1}})
zh = {'block.oritechmekanism.universal_processor': '通用奥瑞处理器', 'block.oritechmekanism.processor_part': '奥瑞处理器部件'}
en = {'block.oritechmekanism.universal_processor': 'Universal Oritech Processor', 'block.oritechmekanism.processor_part': 'Oritech Processor Part'}
grades = [('原始', 'Primitive'), ('基础', 'Basic'), ('中级', 'Improved'), ('高级', 'Advanced'), ('精良', 'Elite'), ('极致', 'Ultra'), ('终极', 'Ultimate')]
# Test-stage redesign: retire the lower upgrades entirely, including previously generated files.
for tier in (2, 3):
    for path in (f'assets/{ID}/models/item/capacity_upgrade_{tier}.json', f'data/{ID}/recipe/capacity_upgrade_{tier}.json'):
        (RES / path).unlink(missing_ok=True)
for tier in range(4, 8):
    item = f'capacity_upgrade_{tier}'
    zh[f'item.{ID}.{item}'] = grades[tier - 1][0] + '处理器扩容升级'
    en[f'item.{ID}.{item}'] = grades[tier - 1][1] + ' Processor Capacity Upgrade'
    write(f'assets/{ID}/models/item/{item}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'oritech:block/machine_core_{tier}'}})
    write(f'data/{ID}/recipe/{item}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['PEP', 'CKC', 'PEP'],
        'key': {'P': {'item': 'oritech:machine_plating_block'}, 'E': {'item': 'oritech:machine_extender'},
                'C': {'item': 'oritech:processing_unit'}, 'K': {'item': f'oritech:machine_core_{tier}'}},
        'result': {'id': f'{ID}:{item}', 'count': 1}})
for key, pair in {
    'tooltip.processor_tier': ('机器品质：%s · 插件容量：%s', 'Machine quality: %s · Addon capacity: %s'),
    'tooltip.capacity_upgrade': ('右键处理器，将插件容量提升至 %s。', 'Use on a processor to increase its addon capacity to %s.'),
    'tooltip.upgrade_menu': ('也可在机器界面中 Shift 点击使用。', 'You can also shift-click this item in a processor menu.'),
    'message.upgraded': ('机器品质提升至 %s，插件容量 %s。', 'Machine quality upgraded to %s. Addon capacity: %s.'),
    'message.upgrade_lower': ('需要更高等级的扩容升级。', 'Use a capacity upgrade above the current machine quality.'),
}.items():
    kind, name_key = key.split('.', 1)
    zh[f'{kind}.{ID}.{name_key}'], en[f'{kind}.{ID}.{name_key}'] = pair
pairs = {
    'host': ('主机', 'Machine'), 'addons': ('插件', 'Addons'),
    'host_hint': ('放入原机器主方块；更换前清空物料和储罐。原子锻造不能搭配处理室。', 'Insert an original machine block. Empty items and tanks before changing it. Atomic forging does not support processing chambers.'),
    'manage_addons': ('装载、查看和卸载插件。', 'Load, inspect and uninstall addons.'),
    'quality': ('机器品质：%s\n插件容量：%s\n使用更高等级扩容升级可继续提升。', 'Machine quality: %s\nAddon capacity: %s\nUse a higher-tier capacity upgrade to expand it.'),
    'capacity': ('插件 %s/%s', '%s/%s slots'), 'capacity_tier': ('品质 %s · 插件 %s/%s', 'Quality %s · Addons %s/%s'),
    'upgrades': ('插件管理', 'Addon management'), 'back': ('返回机器', 'Back'),
    'not_selected': ('未选择', 'No selection'), 'unload': ('卸载', 'Uninstall'),
    'unload_hint': ('卸载一个；按住 Shift 卸载全部同类插件。', 'Uninstall one. Hold Shift to uninstall all matching addons.'),
    'load': ('装载', 'Load'), 'retrieve': ('取出', 'Take'),
    'load_hint': ('自动安装奥瑞或 OritechThings 加工插件。容量不足或不兼容的物品会留在槽内。', 'Automatically install Oritech or OritechThings processing addons. Excess or incompatible addons stay in this slot.'),
    'retrieve_hint': ('从这里取回已卸载的插件。', 'Retrieve uninstalled addons here.'),
    'no_addons': ('尚未安装插件', 'No addons installed'), 'installed_count': ('已安装：%s', 'Installed: %s'),
    'capacity_full': ('插件容量已满', 'Capacity full'), 'load_pending': ('等待装载', 'Load pending'),
    'load_unsupported': ('不支持此插件', 'Unsupported addon'), 'load_atomic': ('原子锻造不支持处理室', 'No chambers here'),
    'load_modules': ('罐室已达上限', 'Module limit'), 'load_combined': ('组合插件需独立安装', 'Combined conflict'),
    'retrieve_first': ('先取出已卸载插件', 'Retrieve first'), 'empty_fluid': ('先排空相关储罐', 'Drain tanks'),
    'speed': ('速度 ×%s', 'Spd ×%s'), 'energy': ('耗能 ×%s', 'FE ×%s'),
    'progress': ('加工：%s / %s tick', 'Processing: %s / %s ticks'), 'charge': ('已充能：%s / %s FE', 'Charged: %s / %s FE'),
    'eject_on': ('自动输出：开', 'Eject: on'), 'eject_off': ('自动输出：关', 'Eject: off'),
}
for i, pair in enumerate([('缺少主机','Insert a machine'),('展开空间不足','Clear deployment space'),('缺少材料','Supply ingredients'),('输出空间不足','Empty outputs'),('电力不足','Supply power'),('红石暂停','Redstone pause'),('插件参数无效','Invalid addon settings'),('运行中','Working')]):
    pairs[f'status.{i}'] = pair
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
