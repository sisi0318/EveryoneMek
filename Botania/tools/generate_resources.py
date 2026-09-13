"""Generate prototype runtime resources and a separate headless-test template."""
import gzip
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
MOD = 'botanicalmekanism'
PLANTS = {
    'mana_lotus': ('仿生导能莲', 'Bionic Conduction Lotus', '将电能转变为魔力，需要连接魔力发射器。', 'Turns electricity into mana. Requires a connected mana spreader.'),
    'bionic_amaranthus': ('仿生翡翠苋', 'Bionic Jaded Amaranthus', '用电能在周围培育神秘花。', 'Grows nearby mystical flowers using electricity.'),
    'bionic_clayconia': ('仿生粘土花', 'Bionic Clayconia', '用电能将附近的沙变成粘土球。', 'Uses electricity to turn nearby sand into clay balls.'),
    'bionic_agricarnation': ('仿生田园康乃馨', 'Bionic Agricarnation', '用电能促进周围植物生长。', 'Uses electricity to encourage nearby plant growth.'),
    'bionic_hopperhock': ('仿生漏斗花', 'Bionic Hopperhock', '收集附近的掉落物，放入相邻容器。需要电能。', 'Collects nearby drops into adjacent inventories. Requires electricity.'),
    'bionic_rannuncarpus': ('仿生手掌花', 'Bionic Rannuncarpus', '拾起并放置附近掉落的方块。需要电能。', 'Picks up and places nearby dropped blocks. Requires electricity.'),
    'bionic_exoflame': ('仿生冶炼火', 'Bionic Exoflame', '用电能加热附近的熔炉，并加快烧炼。', 'Heats nearby furnaces and speeds up smelting using electricity.'),
    'corporea_orchid': ('仿生织网花', 'Bionic Corporea Orchid', '让多媒体网络存取 ME 物品，占用一个通道。', 'Connects Corporea inventories to ME. Uses one channel.'),
    'resonance_flower': ('共鸣花', 'Resonance Flower', '连接共鸣芽，组成魔力网络。', 'Links Resonance Buds into a mana network.'),
    'resonance_bud': ('共鸣芽', 'Resonance Bud', '通过共鸣花输送魔力。', 'Transfers mana through a Resonance Flower.'),
}


def write(path, value):
    destination = RES / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')


zh, en = {'itemGroup.' + MOD: '植物机械'}, {'itemGroup.' + MOD: 'Botanical Mekanism'}
for name, (cn, english, description_cn, description_en) in PLANTS.items():
    key = f'block.{MOD}.{name}'
    zh[key], en[key] = cn, english
    zh[key + '.description'] = description_cn
    en[key + '.description'] = description_en
    native = ('jaded_amaranthus' if name == 'bionic_amaranthus' else name.removeprefix('bionic_')) if name.startswith('bionic_') else None
    model = f'botania:block/{native}' if native else f'{MOD}:block/{name}'
    write(f'assets/{MOD}/blockstates/{name}.json', {'variants': {'': {'model': model}}})
    if name == 'corporea_orchid':
        zh[key + '.flavor'] = '（仿生花会梦见电子蜜蜂吗）'
        en[key + '.flavor'] = '(Do bionic flowers dream of electric bees?)'
        write(f'assets/{MOD}/models/block/{name}.json', {'parent': f'{MOD}:block/resonance_flower'})
    elif native is None:
        write(f'assets/{MOD}/models/block/{name}.json', {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout', 'textures': {'cross': f'{MOD}:block/{name}'}})
    write(f'assets/{MOD}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'botania:block/{native}' if native else f'{MOD}:block/{name}'}})
    if name == 'corporea_orchid': write(f'assets/{MOD}/models/item/{name}.json', {'parent': f'{MOD}:item/resonance_flower'})
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
    'apothecary.reagent': ('辅料', 'Reagent'), 'apothecary.water': ('水：%s / %s mB', 'Water: %s / %s mB'),
    'apothecary.buckets': ('水桶', 'Buckets'),
    'apothecary.energy_cost': ('每批耗电：%s FE', 'Energy per batch: %s FE'),
    'apothecary.time_water': ('%s tick · 1000 mB 水', '%s ticks · 1000 mB water'),
    'apothecary.jei': ('机械花药台', 'Mechanical Apothecary'),
    'energy_label': ('储能 · FE', 'Energy · FE'), 'mana_label': ('魔力', 'Mana'),
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
    ('正在调合', 'Mixing'), ('放入配方材料', 'Insert recipe ingredients'), ('缺少辅料', 'Needs reagent'),
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
zh[f'description.{MOD}.mechanical_apothecary'] = '自动调合花瓣和其他材料，制作魔法花。'
en[f'description.{MOD}.mechanical_apothecary'] = 'Automatically combines petals and other ingredients into magical flowers.'
write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Botanical Mekanism'}})
write('botanicalmekanism.mixins.json', {'required': True, 'package': 'dev.everyonemek.botania.mixin', 'compatibilityLevel': 'JAVA_21',
      'mixins': ['FunctionalFlowerPowerMixin', 'AmaranthusWorkMixin', 'BionicWandMixin', 'ManaPoolAccess', 'EnchanterAccess', 'EnchanterControlMixin', 'SparkTransfersAccess', 'SparkRangeMixin', 'SparkRequestMixin', 'AppliedBotanicsStorageMixin', 'AppliedBotanicsManaKeyMixin', 'AppliedBotanicsManaDensityMixin', 'AppliedBotanicsCellCapacityMixin', 'MechanicalSparkPlacementMixin', 'MechanicalSparkMixin', 'ManaTerminalDefaultsMixin', 'ManaWirelessDefaultsMixin'], 'plugin': 'dev.everyonemek.botania.mixin.OptionalJeiMixinPlugin', 'client': ['ManaSideConfigMixin', 'ManaConfigTabMixin', 'ManaInfusionJeiMixin', 'RunicJeiMixin', 'TerraJeiMixin', 'BrewJeiMixin'], 'injectors': {'defaultRequire': 1}})


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
recipes.append(('corporea_orchid', ['botania:corporea_spark', 'botania:hopperhock', 'botania:purple_mystical_petal',
                'botania:light_blue_mystical_petal', 'botania:elementium_ingot', 'botania:elementium_ingot',
                'ae2:fluix_pearl', 'ae2:engineering_processor', 'ae2:annihilation_core', 'ae2:formation_core'],
                'mekanism:alloy_reinforced', 1, 240, 150))
for name, ingredients, reagent, count, ticks, power in recipes:
    if name in ('resonance_flower', 'resonance_bud'): continue
    write(f'data/{MOD}/recipe/{name}.json', {'type': f'{MOD}:mechanical_apothecary', 'ingredients': [{'item': item} for item in ingredients],
          'reagent': {'item': reagent}, 'result': {'id': f'{MOD}:{name}', 'count': count}, 'ticks': ticks, 'fe_per_tick': power,
          **({'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'ae2'}]} if name == 'corporea_orchid' else {})})
shaped('mechanical_apothecary', ['MAM', 'CSC', 'MIM'], {'M': 'botania:manasteel_ingot', 'A': 'botania:petal_apothecary',
       'C': 'mekanism:basic_control_circuit', 'S': 'mekanism:steel_casing', 'I': 'mekanism:alloy_infused'})
name = 'mechanical_apothecary'
write(f'assets/{MOD}/blockstates/{name}.json', {'variants': {f'facing={face},active={str(active).lower()}': {
      'model': f'{MOD}:block/{name}' + ('_active' if active else ''), 'y': rotation}
      for face, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)] for active in [False, True]}})
write(f'data/{MOD}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
      'type': 'minecraft:item', 'name': f'{MOD}:{name}', 'functions': [{'function': 'minecraft:copy_name', 'source': 'block_entity'},
      {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': [f'mekanism:{key}' for key in
      ['ejector', 'owner', 'redstone_control', 'security', 'side_config', 'upgrades', 'energy', 'items', 'fluids']]}]}]}]})
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'{MOD}:{name}']})

# The old network blocks remain registered for saves; new worlds use native sparks.
zh[f'item.{MOD}.resonance_spark_augment'], en[f'item.{MOD}.resonance_spark_augment'] = '共鸣增幅器', 'Resonance Spark Augment'
zh[f'tooltip.{MOD}.spark_range'] = '为相连的火花各装一个，传送距离可提高到 %s 格。'
en[f'tooltip.{MOD}.spark_range'] = 'Fit one to each connected spark to extend the range to %s blocks.'
write(f'assets/{MOD}/models/item/resonance_spark_augment.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'botania:item/spark_star'}})
shaped('resonance_spark_augment', ['EAE', 'DSD', 'EAE'], {'E': 'botania:elementium_ingot', 'A': 'mekanism:alloy_reinforced', 'D': 'botania:dragonstone', 'S': 'botania:mana_spark'})
write(f'data/{MOD}/recipe/legacy_resonance_recycling.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
      'ingredients': [[{'item': f'{MOD}:resonance_flower'}, {'item': f'{MOD}:resonance_bud'}]],
      'result': {'id': f'{MOD}:resonance_spark_augment', 'count': 1}})
for tag in ['mana_spark_augments', 'dominant_spark_pull_source', 'recessive_spark_push_target']:
    write(f'data/botania/tags/item/{tag}.json', {'replace': False, 'values': [f'{MOD}:resonance_spark_augment']})

from machine_resources import generate as generate_machines
generate_machines(ROOT, write, zh, en)
corporea_messages = {
    'mode.0': ('双向', 'Both ways'), 'mode.1': ('ME 访问', 'ME access'), 'mode.2': ('火花访问', 'Corporea'),
    'online': ('已连接', 'Online'), 'offline': ('未就绪', 'Offline'), 'ae': ('ME 网络：%s', 'ME network: %s'),
    'nodes': ('多媒体库存：%s 个', 'Corporea inventories: %s'), 'stock': ('火花库存：%s 种 / %s 件', 'Corporea: %s types / %s items'),
    'me_stock': ('ME 库存：%s 种 / %s 件', 'ME: %s types / %s items'),
    'transfer': ('上一 tick 传输：%s / %s 件', 'Last tick: %s / %s items transferred'),
    'status.ready': ('已连接 · 4 AE/t · 1 通道', 'Connected · 4 AE/t · 1 channel'),
    'status.missing_ae2': ('需要安装 AE2', 'AE2 is not installed'), 'status.connecting': ('等待 ME 网络连接', 'Waiting for ME connection'),
    'status.no_power': ('ME 网络需要电力', 'ME network needs power'), 'status.no_channel': ('ME 网络缺少可用通道', 'ME network needs a channel'),
    'status.ordinary_spark': ('花上请安装普通多媒体火花', 'Use an ordinary Corporea spark on the flower'),
    'status.missing_spark': ('请在花上安装多媒体火花', 'Attach a Corporea spark'), 'status.missing_master': ('多媒体网络需要主火花', 'Corporea needs a master spark'),
    'status.duplicate_bridge': ('该多媒体网络已有接入花', 'This Corporea network has another bridge'),
    'status.duplicate_storage': ('移除同一库存重复的 ME 存储总线', 'Remove the duplicate ME storage-bus route'),
    'status.duplicate_chest': ('双箱只需一枚多媒体火花', 'Use one Corporea spark per double chest'),
    'status.too_many_nodes': ('多媒体节点超过 128 个', 'More than 128 Corporea nodes'),
    'status.too_many_slots': ('库存槽超过 8192 个', 'More than 8192 inventory slots'),
    'status.paused': ('已暂停', 'Paused'), 'status.redstone': ('红石信号已暂停访问', 'Paused by redstone'),
    'status.unloaded': ('所需区块未加载', 'Required chunks are unloaded'),
}
corporea_messages.update({
    'filter.0': ('筛选：全部', 'Filter: all'), 'filter.1': ('筛选：仅样品', 'Filter: allow'), 'filter.2': ('筛选：排除样品', 'Filter: deny'),
    'exact': ('精确匹配', 'Exact match'), 'item_only': ('只看种类', 'Item type'),
    'samples': ('样品', 'Filter'), 'hotbar': ('物品', 'Hotbar'), 'clear': ('清空筛选', 'Clear filter'),
    'sample_help': ('拿起物品后点击或拖过样品格，Shift 点击背包物品可快速添加。按住右键拖动可清除，取样不消耗物品。', 'Click or drag over filter slots with a held item. Shift-click inventory items to add samples. Right-drag clears slots without consuming items.'),
    'cycle_help': ('左键下一项，右键上一项。', 'Left-click: next. Right-click: previous.'),
    'samples_inactive': ('当前允许全部物品通过。点击左侧筛选按钮，启用或排除样品。', 'All items may pass. Use the filter button on the left to allow or exclude samples.'),
    'craft.on': ('合成：开', 'Craft: on'), 'craft.off': ('合成：关', 'Craft: off'),
    'crafting': ('合成：%s · %s 项', 'Craft: %s · %s jobs'),
    'craft_help': ('缺货时让 ME 自动制作。需要样板和合成 CPU，做好后再取一次货。', 'Asks ME to craft missing items. Requires a pattern and crafting CPU; request the items again when ready.'),
    'craft.status.off': ('关闭', 'Off'), 'craft.status.idle': ('待命', 'Idle'), 'craft.status.calculating': ('计算中', 'Calculating'),
    'craft.status.running': ('进行中', 'Running'), 'craft.status.no_pattern': ('没有可用样板', 'No pattern'),
    'craft.status.ambiguous': ('多个结果，请精确指定物品', 'Ambiguous item'), 'craft.status.missing_materials': ('合成缺料', 'Missing materials'),
    'craft.status.no_cpu': ('CPU 忙碌或计划已变化', 'CPU busy / plan changed'), 'craft.status.failed': ('计算失败，请重试', 'Retry calculation'),
    'craft.status.canceled': ('任务已取消', 'Canceled'), 'craft.status.': ('待命', 'Idle'),
})
for key, cn, english in [
    ('unsupported', '此机器不支持填充该配方', 'This recipe cannot be filled in this machine'),
    ('unavailable', '无法操作此机器', 'This machine is unavailable'),
    ('missing', '缺少材料、辅料或容器', 'Missing ingredients, reagent or container'),
    ('full', '背包没有空间收回原材料', 'No inventory space to return existing ingredients'),
]:
    zh[f'gui.{MOD}.transfer.{key}'], en[f'gui.{MOD}.transfer.{key}'] = cn, english
for key, (cn, english) in corporea_messages.items(): zh[f'gui.{MOD}.corporea.{key}'], en[f'gui.{MOD}.corporea.{key}'] = cn, english
# Portable mana uses addon-owned data, including when AE2 is absent.
for key, cn, english in [
    ('gui.botanicalmekanism.mana_type', '魔力', 'Mana'),
    ('jei.botanicalmekanism.mana_amount', '魔力：%s', 'Mana: %s'),
    ('jei.botanicalmekanism.choose_vessel', '请选择药剂容器', 'Choose a brew vessel'),
    ('jei.botanicalmekanism.pattern_full', '样板放不下这些材料', 'Too many ingredients for this pattern'),
    ('item.botanicalmekanism.mana_storage_cell', '1k ME 魔力存储盘', '1k ME Mana Storage Cell'),
    ('item.botanicalmekanism.mana_packet', '魔力团', 'Mana Wisp'),
    ('tooltip.botanicalmekanism.mana_cell', '魔力：%s / %s', 'Mana: %s / %s'),
    ('tooltip.botanicalmekanism.mana_packet', '魔力：%s。对魔力池或机器使用以归还。', 'Mana: %s. Use on a pool or machine to return it.'),
]: zh[key], en[key] = cn, english
from mana_cell_models import TIERS, cell_id
for tier in TIERS:
    name = cell_id(tier)
    zh[f'item.{MOD}.{name}'], en[f'item.{MOD}.{name}'] = f'{tier}k ME 魔力存储盘', f'{tier}k ME Mana Storage Cell'
    write(f'data/{MOD}/recipe/{name}.json', {
        'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'ae2'}],
        'type': 'minecraft:crafting_shaped', 'pattern': ['SMS', 'CEC', 'SSS'],
        'key': {'S': {'item': 'botania:manasteel_ingot'}, 'M': {'item': 'botania:mana_diamond'},
                'C': {'item': 'ae2:fluix_pearl'}, 'E': {'item': f'ae2:cell_component_{tier}k'}},
        'result': {'id': f'{MOD}:{name}', 'count': 1}})
write(f'assets/{MOD}/models/item/mana_packet.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'botania:block/mana_water'}})
from mana_cell_models import generate as generate_mana_cell_models
generate_mana_cell_models(write)

# Mechanical sparks and upgrades reference native Botania sprites; no surrounding frame.
for name, cn, english in [
    ('mechanical_spark', '机械火花', 'Mechanical Spark'), ('master_mechanical_spark', '机械主火花', 'Master Mechanical Spark'),
    ('spark_range_upgrade', '火花范围升级', 'Spark Range Upgrade'), ('spark_efficiency_upgrade', '火花效率升级', 'Spark Throughput Upgrade'),
]:
    zh[f'item.{MOD}.{name}'], en[f'item.{MOD}.{name}'] = cn, english
zh[f'entity.{MOD}.mechanical_spark'], en[f'entity.{MOD}.mechanical_spark'] = '机械火花', 'Mechanical Spark'
for key, cn, english in [
    ('title', '火花网络升级', 'Spark Network Upgrades'), ('range_label', '范围', 'Range'), ('efficiency_label', '效率', 'Speed'),
    ('range', '范围：%s 格', 'Range: %s'), ('rate', '传输：×%s', 'Transfer: x%s'), ('members', '火花：%s', 'Sparks: %s'),
    ('no_master', '需要机械主火花', 'Place a master spark'), ('conflict', '请拆下多余的主火花', 'Remove extra masters'), ('connected', '已连接主火花', 'Master connected'),
]: zh[f'gui.{MOD}.spark.{key}'], en[f'gui.{MOD}.spark.{key}'] = cn, english
for name, native in [('mechanical_spark', 'mana_spark'), ('master_mechanical_spark', 'master_corporea_spark')]:
    write(f'assets/{MOD}/models/item/{name}.json', {'parent': f'botania:item/{native}'})
for name, native in [('spark_range_upgrade', 'rune_of_air'), ('spark_efficiency_upgrade', 'rune_of_mana')]:
    badge = [{'from': [10, 10, z], 'to': [16, 16, z], 'faces': {face: {'texture': '#star', 'uv': [0, 0, 16, 16]}}}
             for face, z in [('south', 8.6), ('north', 7.4)]]
    write(f'assets/{MOD}/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'loader': 'neoforge:composite',
          'textures': {'particle': f'botania:item/{native}'}, 'children': {'rune': {'parent': f'botania:item/{native}'},
          'badge': {'textures': {'star': 'botania:item/spark_star'}, 'elements': badge}}})
write(f'data/{MOD}/recipe/mechanical_spark.json', {'type': 'minecraft:crafting_shaped', 'pattern': [' S ', 'CFC', ' S '],
      'key': {'S': {'item': 'botania:manasteel_ingot'}, 'C': {'item': 'mekanism:basic_control_circuit'}, 'F': {'item': 'botania:mana_spark'}}, 'result': {'id': f'{MOD}:mechanical_spark'}})
shaped('master_mechanical_spark', [' C ', 'GFG', ' T '], {'C': 'mekanism:elite_control_circuit', 'G': 'botania:gaia_spirit', 'F': f'{MOD}:mechanical_spark', 'T': 'botania:terrasteel_ingot'})
shaped('spark_range_upgrade', [' S ', 'PCP', ' S '], {'S': 'botania:manasteel_ingot', 'P': 'botania:mana_pearl', 'C': 'mekanism:upgrade_anchor'})
shaped('spark_efficiency_upgrade', [' S ', 'DCD', ' S '], {'S': 'botania:manasteel_ingot', 'D': 'botania:mana_diamond', 'C': 'mekanism:upgrade_speed'})

from lexicon_resources import generate as generate_lexicon
generate_lexicon(ROOT, write, zh, en, PLANTS, recipes)
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
