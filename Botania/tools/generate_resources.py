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
    'bionic_clayconia': ('仿生粘土花', 'Bionic Clayconia', '消耗 FE，将周围的沙转为粘土球。', 'Uses FE to turn nearby sand into clay balls.'),
    'bionic_agricarnation': ('仿生田园康乃馨', 'Bionic Agricarnation', '消耗 FE，加快周围合适植物的生长。', 'Uses FE to accelerate suitable nearby plants.'),
    'bionic_hopperhock': ('仿生漏斗花', 'Bionic Hopperhock', '消耗 FE，将掉落物送入相邻容器。物品框筛选，潜行使用森林法杖切换模式。', 'Uses FE to collect drops into adjacent inventories. Item frames filter; sneak-use a Wand to change mode.'),
    'bionic_rannuncarpus': ('仿生手掌花', 'Bionic Rannuncarpus', '消耗 FE，放置掉落方块。以花下两格的方块为地面样板，潜行使用森林法杖切换模式。', 'Uses FE to place dropped blocks. Matches the block two below the flower; sneak-use a Wand to change mode.'),
    'bionic_exoflame': ('仿生冶炼火', 'Bionic Exoflame', '消耗 FE，给周围可加工的熔炉供热并加速。', 'Uses FE to heat and accelerate nearby working furnaces.'),
    'resonance_flower': ('共鸣花', 'Resonance Flower', '管理同维度魔力无线网络。右键设置网络与成员。', 'Manages a wireless mana network in this dimension. Right-click to configure its name and members.'),
    'resonance_bud': ('共鸣芽', 'Resonance Bud', '从相邻魔力池供给或接收魔力，也可作为无线中继。', 'Supplies or receives mana from an adjacent pool, or relays a wireless connection.'),
}


def write(path, value):
    destination = RES / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')


zh, en = {'itemGroup.' + MOD: '植物机械'}, {'itemGroup.' + MOD: 'Botanical Mekanism'}
for name, (cn, english, description_cn, description_en) in PLANTS.items():
    key = f'block.{MOD}.{name}'
    zh[key], en[key] = cn, english
    zh[key + '.description'] = description_cn + ' 可安装在普通承托方块或电缆上，无需泥土。'
    en[key + '.description'] = description_en + ' Mount on solid supports or cables; no soil is required.'
    native = ('jaded_amaranthus' if name == 'bionic_amaranthus' else name.removeprefix('bionic_')) if name.startswith('bionic_') else None
    model = f'botania:block/{native}' if native else f'{MOD}:block/{name}'
    write(f'assets/{MOD}/blockstates/{name}.json', {'variants': {'': {'model': model}}})
    if native is None:
        write(f'assets/{MOD}/models/block/{name}.json', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': f'{MOD}:block/{name}'}})
    write(f'assets/{MOD}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'botania:block/{native}' if native else f'{MOD}:block/{name}'}})
    write(f'data/{MOD}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MOD}:{name}'}]}]})

messages = {
    'tab.settings': ('设置', 'Settings'), 'tab.networks': ('网络', 'Networks'),
    'tab.members': ('成员', 'Members'), 'tab.connections': ('连接', 'Connections'),
    'search': ('搜索名称', 'Search names'), 'empty_list': ('没有匹配的条目', 'No matching entries'),
    'back': ('返回', 'Back'), 'online': ('在线', 'Online'), 'offline': ('离线', 'Offline'),
    'add_member': ('添加成员', 'Add member'), 'add_name': ('添加：%s', 'Add: %s'), 'remove_name': ('移除：%s', 'Remove: %s'),
    'connected_name': ('已连接：%s', 'Connected: %s'),
    'network_details': ('%s\n节点：%s / %s\n%s\n核心：%s', '%s\nNodes: %s / %s\n%s\nCore: %s'),
    'connection_row': ('%s · %s', '%s · %s'),
    'connection_details': ('位置：%s\n%s\n本批传输：%s', 'Position: %s\n%s\nLast batch: %s'),
    'detect_pool': ('自动检测', 'Detect pool'), 'target_side': ('目标方向', 'Target direction'),
    'priority_label': ('优先级', 'Priority'), 'fill_target': ('目标容量', 'Max'),
    'join_as.0': ('供给接入', 'Join: supply'), 'join_as.1': ('接收接入', 'Join: receive'), 'join_as.2': ('中继接入', 'Join: relay'),
    'mode_help.0': ('从相邻池或机器输出面取魔力，供给网络；保留量以下不取。', 'Takes mana from an adjacent pool or machine output, keeping the reserve.'),
    'mode_help.1': ('从网络接收魔力，补充相邻池或机器输入面至目标量。', 'Receives mana into an adjacent pool or machine input up to the target.'),
    'mode_help.2': ('延伸网络连接，无需相邻魔力池。', 'Extends network connections; no adjacent pool is required.'),
    'join_help': ('接入“%s”\n%s', 'Join "%s"\n%s'),
    'select_network_first': ('先在列表中选择网络', 'Select a network from the list first'),
    'no_networks': ('先放置共鸣花，或请网络所有者添加你', 'Place a Resonance Flower or ask its owner to add you'),
    'network_full': ('网络节点已满', 'The network has no free node slots'),
    'connecting': ('正在接入…', 'Connecting…'), 'applying': ('正在应用…', 'Applying…'),
    'priority_short.0': ('低', 'Low'), 'priority_short.1': ('普通', 'Normal'), 'priority_short.2': ('高', 'High'),
    'detect_failed': ('附近需要唯一的有效魔力池或魔力机器', 'Needs exactly one adjacent supported pool or mana machine'),
    'setting_failed': ('设置未生效，请检查条件或权限', 'Setting rejected; check requirements and access'),
    'apothecary.materials': ('材料', 'Materials'), 'apothecary.products': ('产物', 'Output'),
    'apothecary.reagent': ('终结材料', 'Reagent'), 'apothecary.water': ('水：%s / %s mB', 'Water: %s / %s mB'),
    'apothecary.buckets': ('水桶', 'Buckets'),
    'apothecary.energy_cost': ('每批耗电：%s FE', 'Energy per batch: %s FE'),
    'apothecary.time_water': ('%s tick · 1000 mB 水', '%s ticks · 1000 mB water'),
    'apothecary.jei': ('机械花药台', 'Mechanical Apothecary'),
    'energy_label': ('储能 · FE', 'Energy · FE'), 'mana_label': ('魔力储备', 'Mana reserve'),
    'quantity': ('%s / %s', '%s / %s'),
    'loading': ('正在读取花的状态…', 'Reading flower state…'),
    'member_hint': ('输入在线玩家名添加成员；再次输入已授权名字可移除。', 'Add an online player by name; enter an authorized name again to remove them.'),
    'bind_hint': ('法杖绑定模式：\n潜行右键花 → 发射器', 'Bind Mode + sneak:\nFlower → spreader'),
    'bind_help': ('潜行右键空气将森林法杖切到绑定模式，再潜行右键花和 6 格内的发射器。', 'Sneak-use the Wand in the air to select Bind Mode, then sneak-use the flower and a spreader within 6 blocks.'),
    'bound_to': ('已绑定发射器：%s, %s, %s', 'Bound spreader: %s, %s, %s'),
    'amaranthus_ground': ('花本身可立于石材或电缆上', 'The bionic flower can stand on stone or cables'),
    'relay_hint': ('中继只延伸连接，无需相邻魔力池', 'Relays extend links; no adjacent pool needed'),
    'denied': ('无配置权限', 'Access denied'), 'pause': ('暂停', 'Pause'), 'resume': ('启用', 'Enable'),
    'apply': ('应用', 'Apply'), 'member_toggle': ('增减', 'Toggle'), 'disconnect': ('断开', 'Unlink'),
    'energy': ('储能：%s / %s FE', 'Energy: %s / %s FE'), 'mana': ('魔力：%s / %s', 'Mana: %s / %s'),
    'network': ('网络：%s', 'Network: %s'), 'network_name': ('网络名称', 'Network name'), 'unlinked': ('未连接', 'Unlinked'),
    'member_name': ('成员名称', 'Member name'),
    'members': ('成员：%s', 'Members: %s'), 'nodes': ('节点：%s / %s', 'Nodes: %s / %s'),
    'member_count': ('授权成员：%s', 'Members: %s'),
    'flow_fee': ('本批到货 %s，费用 %s', 'Delivered %s · Fee %s'),
    'relay_flow': ('最近一批转发：%s', 'Last batch relayed: %s'),
    'reserve': ('保留量', 'Reserve'), 'target': ('目标量', 'Target'), 'confirmed_limit': ('已确认数量：%s', 'Confirmed: %s'),
    'unmeasured': ('尚未连接魔力池', 'No pool connected'),
    'lotus_hint': ('满速 %s FE/t → %s 魔力/t', 'Full rate: %s FE/t → %s mana/t'),
    'amaranthus_hint': ('周围需有可种植神秘花的地面', 'Needs suitable ground for mystical flowers'),
    'mode.0': ('供给', 'Supply'), 'mode.1': ('接收', 'Receive'), 'mode.2': ('中继', 'Relay'),
    'priority.0': ('优先级：低', 'Priority: Low'), 'priority.1': ('优先级：普通', 'Priority: Normal'), 'priority.2': ('优先级：高', 'Priority: High'),
}
for i, (cn, english) in enumerate(zip(['下方', '上方', '北侧', '南侧', '西侧', '东侧'], ['Down', 'Up', 'North', 'South', 'West', 'East'])):
    messages[f'direction.{i}'] = (f'目标：{cn}', f'Target: {english}')
    messages[f'side.{i}'] = (cn, english)
for i, pair in enumerate([
    ('正在调合', 'Mixing'), ('放入配方材料', 'Insert recipe ingredients'), ('缺少终结材料', 'Needs reagent'),
    ('需要水', 'Needs water'), ('需要能量', 'Needs energy'), ('产物空间不足', 'Output full'), ('红石已禁止', 'Redstone disabled'),
]): messages[f'apothecary.status.{i}'] = pair
statuses = {
    'unlinked': ('请选择网络', 'Select a network'), 'unowned': ('请右键初始化', 'Right-click to initialize'),
    'working': ('正在工作', 'Working'), 'ready': ('等待工作条件', 'Waiting for work'), 'waiting': ('等待供给或带宽', 'Waiting for supply or bandwidth'),
    'paused': ('已暂停', 'Paused'), 'no_energy': ('需要 FE', 'Needs FE'), 'unbound': ('需要连接魔力发射器', 'Bind a mana spreader'),
    'full': ('已满或达到目标', 'Full or target reached'), 'reserve': ('已达到保留量', 'Reserve reached'),
    'no_supply': ('没有可用供给端', 'No supply endpoint'), 'core_offline': ('核心离线或暂停', 'Core offline or paused'),
    'relay': ('中继已连接', 'Relay connected'), 'out_of_range': ('超出连接范围', 'Out of range'),
    'missing_pool': ('目标或魔力接口不可用', 'No supported target or mana port'), 'duplicate_target': ('目标已有无线端点', 'Target already has an endpoint'),
    'duplicate_core': ('网络已有核心', 'Network already has a core'), 'denied': ('无网络访问权限', 'Network access denied'),
    'unloaded': ('区块未加载', 'Chunk unloaded'),
}
for key, pair in statuses.items(): messages['status.' + key] = pair
for key, (cn, english) in messages.items(): zh[f'gui.{MOD}.{key}'], en[f'gui.{MOD}.{key}'] = cn, english
zh[f'block.{MOD}.mechanical_apothecary'], en[f'block.{MOD}.mechanical_apothecary'] = '机械花药台', 'Mechanical Apothecary'
zh[f'description.{MOD}.mechanical_apothecary'] = '消耗水与能量调合原版花和仿生花。背面输入终结材料，右侧输出产物。'
en[f'description.{MOD}.mechanical_apothecary'] = 'Uses water and energy to craft native and bionic flowers. Reagent enters at the back; products leave on the right.'
write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Botanical Mekanism'}})
write('botanicalmekanism.mixins.json', {'required': True, 'package': 'dev.everyonemek.botania.mixin', 'compatibilityLevel': 'JAVA_21',
      'mixins': ['FunctionalFlowerPowerMixin', 'AmaranthusWorkMixin', 'BionicWandMixin', 'ManaPoolAccess', 'EnchanterAccess', 'EnchanterControlMixin'], 'client': [], 'injectors': {'defaultRequire': 1}})


def shaped(name, pattern, keys):
    write(f'data/{MOD}/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
          'key': {key: {'item': item} for key, item in keys.items()}, 'result': {'id': f'{MOD}:{name}', 'count': 1}})


# Bionic recipes deliberately use only the mechanical recipe type, never the native basin or crafting table.
recipes = [
    ('mana_lotus', ['botania:endoflame', 'botania:cyan_mystical_petal', 'botania:cyan_mystical_petal', 'botania:white_mystical_petal', 'botania:white_mystical_petal',
                    'botania:manasteel_ingot', 'botania:manasteel_ingot', 'botania:mana_diamond', 'botania:rune_of_fire', 'botania:rune_of_air',
                    'mekanism:advanced_control_circuit'], 'mekanism:alloy_infused', 1, 200, 150),
    ('bionic_amaranthus', ['botania:jaded_amaranthus', 'botania:green_mystical_petal', 'botania:lime_mystical_petal', 'botania:manasteel_ingot',
                           'botania:manasteel_ingot', 'botania:mana_pearl', 'botania:rune_of_earth', 'mekanism:basic_control_circuit'],
                          'mekanism:alloy_infused', 1, 160, 100),
    ('resonance_flower', ['botania:manastar', 'botania:purple_mystical_petal', 'botania:purple_mystical_petal', 'botania:light_blue_mystical_petal', 'botania:light_blue_mystical_petal',
                         'botania:mana_spark', 'botania:mana_spark', 'botania:elementium_ingot', 'botania:elementium_ingot', 'botania:dragonstone',
                         'botania:rune_of_air', 'mekanism:advanced_control_circuit'], 'mekanism:alloy_reinforced', 1, 300, 200),
    ('resonance_bud', ['botania:mana_spark', 'botania:cyan_mystical_petal', 'botania:purple_mystical_petal', 'botania:elementium_ingot', 'botania:elementium_ingot',
                      'botania:rune_of_air', 'mekanism:basic_control_circuit'], 'mekanism:alloy_infused', 2, 100, 100),
]
for flower, petals, rune in [
    ('clayconia', ['light_gray', 'cyan'], 'earth'),
    ('agricarnation', ['lime', 'green'], 'spring'),
    ('hopperhock', ['gray', 'light_gray'], 'air'),
    ('rannuncarpus', ['orange', 'yellow'], 'earth'),
    ('exoflame', ['red', 'orange'], 'fire'),
]:
    recipes.append(('bionic_' + flower, [f'botania:{flower}', *[f'botania:{color}_mystical_petal' for color in petals],
                    'botania:manasteel_ingot', 'botania:manasteel_ingot', 'botania:mana_pearl', f'botania:rune_of_{rune}',
                    'mekanism:basic_control_circuit'], 'mekanism:alloy_infused', 1, 160, 100))
for name, ingredients, reagent, count, ticks, power in recipes:
    write(f'data/{MOD}/recipe/{name}.json', {'type': f'{MOD}:mechanical_apothecary', 'ingredients': [{'item': item} for item in ingredients],
          'reagent': {'item': reagent}, 'result': {'id': f'{MOD}:{name}', 'count': count}, 'ticks': ticks, 'fe_per_tick': power})
shaped('mechanical_apothecary', ['MAM', 'CSC', 'MIM'], {'M': 'botania:manasteel_ingot', 'A': 'botania:petal_apothecary',
       'C': 'mekanism:basic_control_circuit', 'S': 'mekanism:steel_casing', 'I': 'mekanism:alloy_infused'})
name = 'mechanical_apothecary'
texture = f'{MOD}:block/{name}'
write(f'assets/{MOD}/models/block/{name}.json', {'parent': 'minecraft:block/cube', 'textures': {
      'particle': f'{texture}/side', 'north': f'{texture}/front', 'up': f'{texture}/top',
      **{face: f'{texture}/side' for face in ['south', 'east', 'west', 'down']}}})
write(f'assets/{MOD}/models/block/{name}_active.json', {'parent': f'{MOD}:block/{name}', 'textures': {'north': f'{texture}/front_active'}})
write(f'assets/{MOD}/models/item/{name}.json', {'parent': f'{MOD}:block/{name}'})
write(f'assets/{MOD}/blockstates/{name}.json', {'variants': {f'facing={face},active={str(active).lower()}': {
      'model': f'{MOD}:block/{name}' + ('_active' if active else ''), 'y': rotation}
      for face, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)] for active in [False, True]}})
write(f'data/{MOD}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
      'type': 'minecraft:item', 'name': f'{MOD}:{name}', 'functions': [{'function': 'minecraft:copy_name', 'source': 'block_entity'},
      {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'mekanism:{key}' for key in
      ['ejector', 'owner', 'redstone_control', 'security', 'side_config', 'upgrades', 'energy', 'items', 'fluids']]}]}]}]})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'{MOD}:{name}']})

from machine_resources import generate as generate_machines
generate_machines(ROOT, write, zh, en)
write(f'assets/{MOD}/lang/zh_cn.json', zh)
write(f'assets/{MOD}/lang/en_us.json', en)

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
