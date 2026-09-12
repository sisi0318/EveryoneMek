"""Generate prototype runtime resources and a separate headless-test template."""
import gzip
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
MOD = 'botanicalmekanism'
PLANTS = {
    'mana_lotus': ('仿生导能莲', 'Bionic Conduction Lotus', '消耗 FE 产生魔力。使用森林法杖连接魔力发射器。', 'Uses FE to produce mana. Bind to a mana spreader with a Wand of the Forest.'),
    'bionic_amaranthus': ('仿生翡翠苋', 'Bionic Jaded Amaranthus', '消耗 FE，在合适地面生成神秘花。', 'Uses FE to grow mystical flowers on suitable ground.'),
    'resonance_flower': ('共鸣花', 'Resonance Flower', '管理同维度魔力无线网络。右键设置网络与成员。', 'Manages a wireless mana network in this dimension. Right-click to configure its name and members.'),
    'resonance_bud': ('共鸣芽', 'Resonance Bud', '从相邻魔力池供给或接收魔力，也可作为无线中继。', 'Supplies or receives mana from an adjacent pool, or relays a wireless connection.'),
}


def write(path, value):
    destination = RES / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


zh, en = {'itemGroup.' + MOD: '植物机械'}, {'itemGroup.' + MOD: 'Botanical Mekanism'}
for name, (cn, english, description_cn, description_en) in PLANTS.items():
    key = f'block.{MOD}.{name}'
    zh[key], en[key] = cn, english
    zh[key + '.description'] = description_cn + ' 可安装在普通承托方块或电缆上，无需泥土。'
    en[key + '.description'] = description_en + ' Mount on solid supports or cables; no soil is required.'
    model = 'botania:block/jaded_amaranthus' if name == 'bionic_amaranthus' else f'{MOD}:block/{name}'
    write(f'assets/{MOD}/blockstates/{name}.json', {'variants': {'': {'model': model}}})
    if name != 'bionic_amaranthus':
        write(f'assets/{MOD}/models/block/{name}.json', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': f'{MOD}:block/{name}'}})
    write(f'assets/{MOD}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'botania:block/jaded_amaranthus' if name == 'bionic_amaranthus' else f'{MOD}:block/{name}'}})
    write(f'data/{MOD}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{name}'}]}]})

messages = {
    'denied': ('无配置权限', 'Access denied'), 'pause': ('暂停', 'Pause'), 'resume': ('启用', 'Enable'),
    'apply': ('应用', 'Apply'), 'member_toggle': ('增减', 'Toggle'), 'disconnect': ('断开', 'Unlink'),
    'energy': ('储能：%s / %s FE', 'Energy: %s / %s FE'), 'mana': ('魔力：%s / %s', 'Mana: %s / %s'),
    'network': ('网络：%s', 'Network: %s'), 'network_name': ('网络名称', 'Network name'), 'unlinked': ('未连接', 'Unlinked'),
    'member_name': ('成员名称（新增需在线）', 'Member name (online to add)'),
    'members': ('成员：%s', 'Members: %s'), 'nodes': ('节点：%s / %s', 'Nodes: %s / %s'),
    'member_count': ('授权成员：%s（悬停查看）', 'Authorized members: %s (hover)'),
    'flow_fee': ('本批到货 %s，费用 %s', 'Last batch: %s delivered, fee %s'),
    'relay_flow': ('最近一批转发：%s', 'Last batch relayed: %s'),
    'reserve': ('保留量', 'Reserve'), 'target': ('目标量', 'Target'), 'confirmed_limit': ('已确认数量：%s', 'Confirmed amount: %s'),
    'unmeasured': ('尚未连接魔力池', 'No pool connected'),
    'lotus_hint': ('满速 %s FE/t → %s 魔力/t', 'Full rate: %s FE/t → %s mana/t'),
    'amaranthus_hint': ('周围需有可种植神秘花的地面', 'Needs suitable ground for mystical flowers'),
    'mode.0': ('供给', 'Supply'), 'mode.1': ('接收', 'Receive'), 'mode.2': ('中继', 'Relay'),
    'priority.0': ('优先级：低', 'Priority: Low'), 'priority.1': ('优先级：普通', 'Priority: Normal'), 'priority.2': ('优先级：高', 'Priority: High'),
}
for i, (cn, english) in enumerate(zip(['下方', '上方', '北侧', '南侧', '西侧', '东侧'], ['Down', 'Up', 'North', 'South', 'West', 'East'])):
    messages[f'direction.{i}'] = (f'目标：{cn}', f'Target: {english}')
statuses = {
    'unlinked': ('请选择网络', 'Select a network'), 'unowned': ('请右键初始化', 'Right-click to initialize'),
    'working': ('正在工作', 'Working'), 'ready': ('等待工作条件', 'Waiting for work'), 'waiting': ('等待供给或带宽', 'Waiting for supply or bandwidth'),
    'paused': ('已暂停', 'Paused'), 'no_energy': ('需要 FE', 'Needs FE'), 'unbound': ('需要连接魔力发射器', 'Bind a mana spreader'),
    'full': ('已满或达到目标', 'Full or target reached'), 'reserve': ('已达到保留量', 'Reserve reached'),
    'no_supply': ('没有可用供给端', 'No supply endpoint'), 'core_offline': ('核心离线或暂停', 'Core offline or paused'),
    'relay': ('中继已连接', 'Relay connected'), 'out_of_range': ('超出连接范围', 'Out of range'),
    'missing_pool': ('目标不是可用魔力池', 'No supported pool at target'), 'duplicate_target': ('魔力池已有无线端点', 'Pool already has an endpoint'),
    'duplicate_core': ('网络已有核心', 'Network already has a core'), 'denied': ('无网络访问权限', 'Network access denied'),
}
for key, pair in statuses.items(): messages['status.' + key] = pair
for key, (cn, english) in messages.items(): zh[f'gui.{MOD}.{key}'], en[f'gui.{MOD}.{key}'] = cn, english
write(f'assets/{MOD}/lang/zh_cn.json', zh)
write(f'assets/{MOD}/lang/en_us.json', en)
write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Botanical Mekanism'}})
write('botanicalmekanism.mixins.json', {'required': True, 'package': 'dev.everyonemek.botania.mixin', 'compatibilityLevel': 'JAVA_21',
      'mixins': ['FunctionalFlowerPowerMixin', 'AmaranthusWorkMixin'], 'client': [], 'injectors': {'defaultRequire': 1}})


def shaped(name, pattern, keys):
    write(f'data/{MOD}/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
          'key': {key: {'item': item} for key, item in keys.items()}, 'result': {'id': f'{MOD}:{name}', 'count': 1}})


shaped('mana_lotus', ['DCD', 'RLW', 'ISI'], {'D': 'botania:mana_diamond', 'C': 'mekanism:advanced_control_circuit',
       'R': 'botania:rune_of_fire', 'L': 'botania:endoflame', 'W': 'botania:rune_of_air', 'I': 'botania:manasteel_ingot', 'S': 'botania:livingwood_twig'})
shaped('resonance_flower', ['SES', 'DCD', 'RTR'], {'S': 'botania:mana_spark', 'E': 'botania:elementium_ingot', 'D': 'botania:dragonstone',
       'C': 'mekanism:advanced_control_circuit', 'R': 'botania:livingrock', 'T': 'botania:livingwood_twig'})
for name, ingredients, count in [
    ('bionic_amaranthus', ['botania:jaded_amaranthus', 'mekanism:basic_control_circuit', 'botania:manasteel_ingot'], 1),
    ('resonance_bud', ['botania:mana_spark', 'botania:elementium_ingot', 'mekanism:basic_control_circuit'], 2),
]:
    write(f'data/{MOD}/recipe/{name}.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
          'ingredients': [{'item': item} for item in ingredients], 'result': {'id': f'{MOD}:{name}', 'count': count}})

def text(value):
    encoded = value.encode('utf-8')
    return struct.pack('>H', len(encoded)) + encoded

def named(kind, name, payload): return bytes([kind]) + text(name) + payload

template = b'\x0a\x00\x00' + named(3, 'DataVersion', struct.pack('>i', 3955))
template += named(9, 'size', b'\x03' + struct.pack('>iiii', 3, 96, 12, 96))
template += named(9, 'palette', b'\x0a' + struct.pack('>i', 1) + named(8, 'Name', text('minecraft:air')) + b'\x00')
template += named(9, 'blocks', b'\x0a' + struct.pack('>i', 0)) + named(9, 'entities', b'\x0a' + struct.pack('>i', 0)) + b'\x00'
path = ROOT / f'src/gameTest/resources/data/{MOD}/structure/empty.nbt'
path.parent.mkdir(parents=True, exist_ok=True)
path.write_bytes(gzip.compress(template, mtime=0))
print(f'Generated resources for {len(PLANTS)} flowers')
