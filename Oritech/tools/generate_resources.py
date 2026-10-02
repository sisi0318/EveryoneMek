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
write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': [f'{ID}:universal_processor', f'{ID}:processor_part', f'{ID}:mini_particle_collider']})
write(f'data/{ID}/recipe/universal_processor.json', {'type': 'minecraft:crafting_shaped', 'category': 'redstone',
    'pattern': ['PCP', 'MKM', 'PCP'],
    'key': {'P': {'item': 'oritech:machine_plating_block'}, 'C': {'item': 'oritech:processing_unit'},
            'M': {'item': 'oritech:motor'}, 'K': {'item': 'oritech:machine_core_3'}},
    'result': {'id': f'{ID}:universal_processor', 'count': 1}})
zh = {'block.oritechmekanism.universal_processor': '通用奥瑞处理器', 'block.oritechmekanism.processor_part': '奥瑞处理器部件'}
en = {'block.oritechmekanism.universal_processor': 'Universal Oritech Processor', 'block.oritechmekanism.processor_part': 'Oritech Processor Part'}
zh[f'block.{ID}.mini_particle_collider'] = '微型粒子碰撞机'
en[f'block.{ID}.mini_particle_collider'] = 'Miniature Particle Collider'
write(f'assets/{ID}/blockstates/mini_particle_collider.json', {'variants': {
    f'facing={f}': {'model': f'{ID}:block/mini_particle_collider', 'y': y}
    for f, y in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]}})
def collider_box(low, high, texture):
    face_data = {'texture': texture}
    if texture == '#core':
        face_data['uv'] = [0, 0, 16, 16]
    return {'from': low, 'to': high, 'faces': {face: dict(face_data) for face in ['north','south','east','west','up','down']}}
write(f'assets/{ID}/models/block/mini_particle_collider.json', {
    'parent': 'minecraft:block/block',
    'textures': {'particle': shell, 'shell': shell, 'top': 'oritech:block/machine_plating_block/particle', 'core': 'oritech:block/machine_core_3'},
    'elements': [collider_box([0,0,0],[16,2,16],'#shell'),collider_box([0,2,3],[16,15,16],'#shell'),
                 collider_box([0,15,0],[16,16,16],'#top'),collider_box([0,2,0],[2,15,3],'#shell'),
                 collider_box([14,2,0],[16,15,3],'#shell'),collider_box([7,2,0],[9,15,3],'#shell'),
                 collider_box([2,5,1],[7,12,3],'#core'),collider_box([9,5,1],[14,12,3],'#core')]})
write(f'assets/{ID}/models/item/mini_particle_collider.json', {'parent': f'{ID}:block/mini_particle_collider'})
write(f'data/{ID}/recipe/mini_particle_collider.json', {'type':'minecraft:crafting_shaped','category':'redstone','pattern':['ACA'],
    'key':{'A':{'item':'oritech:accelerator_controller'},'C':{'item':'oritech:machine_core_3'}},
    'result':{'id':f'{ID}:mini_particle_collider','count':1}})
write(f'data/{ID}/loot_table/blocks/mini_particle_collider.json', {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'{ID}:mini_particle_collider',
    'functions':[{'function':'minecraft:copy_components','source':'block_entity','include':[f'{ID}:collider']}]}]}]})
collider_pairs = {
    'track':('内部轨道','Beamline'), 'recipes':('配方','Recipes'), 'ports':('接口','Ports'),
    'magnetic':('磁场','Magnet'),
    'magnet_hint':('一个磁场 · 效率／电容插件','One field · efficiency / capacitor addons'),
    'magnet_slot':('安装一个加速器磁场；取出时保留储能。','Install one accelerator magnetic field. Stored energy stays with the item.'),
    'magnet_addons':('安装效率或电容插件，每槽一个。','Install one efficiency or capacitor addon per slot.'),
    'magnet_energy':('磁场储能：%s','Field energy: %s'),
    'magnet_efficiency':('辅助耗能 ×%s','Assistance cost ×%s'),
    'magnet_need':('本次辅助需要：%s FE','This assistance requires %s FE'),
    'magnet_spent':('本轮磁场耗电：%s FE','Field energy used this batch: %s FE'),
    'magnet_missing':('请安装加速器磁场','Install an accelerator magnetic field'),
    'magnet_disabled':('磁场辅助已被配置禁用','Magnetic assistance is disabled in config'),
    'smart':('智能','Smart'), 'manual':('手动','Manual'), 'smart_drag':('拖动自动连线','Drag to connect'),
    'smart_toggle':('再次点击切换智能／手动铺轨。智能模式自动对齐并将直角切成两个 45° 弯。','Click again to toggle smart/manual placement. Smart mode aligns guides and bevels right-angle corners.'),
    'smart_cost':('新部件：%s · 可用：%s','New parts: %s · Available: %s'),
    'smart_error.1':('铺轨路径无效','Invalid stroke'),
    'smart_error.2':('转向过急，请留出斜段','Leave room for a diagonal'),
    'smart_error.3':('转角被占用','Corner occupied'),
    'smart_error.4':('转弯处需要导环','Use guide rings for bends'),
    'smart_error.5':('注入端朝向不匹配','Adjust emitter direction'),
    'smart_error.6':('连接有歧义，请手动调整','Ambiguous; use manual mode'),
    'smart_error.7':('部件不足','Not enough parts'),
    'input_a':('输入 A','Input A'), 'input_b':('输入 B','Input B'), 'output':('产物','Output'),
    'start':('启动','Start'), 'stop':('暂停','Pause'), 'cancel':('终止','Abort'),
    'cancel_hint':('停止任务并退回原料；已消耗电量不退回。','Abort and return reserved ingredients. Spent energy is not refunded.'),
    'auto':('自动识别','Automatic'), 'locked':('已锁定','Locked'), 'lock_current':('锁定识别','Lock recipe'),
    'eject_on':('弹出：开','Eject: on'), 'eject_off':('弹出：关','Eject: off'),
    'part_in':('部件','Parts'), 'part_out':('取回','Return'),
    'part_hint':('放入导环、加速马达或粒子传感器。','Insert guide rings, accelerator motors or particle sensors.'),
    'editor_hint':('拖动布置／右键拆除\n滚轮缩放\nShift 拖动平移','Drag to place / right-click to remove\nScroll to zoom\nShift-drag to pan'),
    'place':('放置','Place'), 'remove':('拆除','Remove'), 'move_a':('移动 A','Move A'), 'move_b':('移动 B','Move B'),
    'bend.0':('直线','Line'), 'bend.1':('左弯','Left'), 'bend.2':('右弯','Right'),
    'search':('搜索产物','Search products'), 'required':('碰撞能量：%s J','Collision energy: %s J'),
    'speed':('速度：%s m/s','Speed: %s m/s'), 'spent':('本轮耗电：%s FE','Energy used: %s FE'),
    'empty':('空格','Empty cell'), 'emitter_a':('A 注入端','Emitter A'), 'emitter_b':('B 注入端','Emitter B'),
    'port_hint':('点击切换物品接口；方向相对机器正面。','Click to cycle. Sides follow the machine front.'),
    'side.front':('前','Front'), 'side.left':('左','Left'), 'side.right':('右','Right'), 'side.back':('后','Back'), 'side.top':('上','Top'), 'side.bottom':('下','Bottom'),
    'mode.0':('关闭','Closed'), 'mode.1':('输入 A','Input A'), 'mode.2':('输入 B','Input B'), 'mode.3':('双输入','Both inputs'), 'mode.4':('输出','Output'),
    'status.0':('待机','Idle'), 'status.1':('缺少原料 A','Supply input A'), 'status.2':('缺少原料 B','Supply input B'),
    'status.3':('原料不匹配','No matching recipe'), 'status.4':('输出空间不足','Empty the output'), 'status.5':('电力不足','Supply power'),
    'status.6':('粒子加速中','Accelerating'), 'status.7':('碰撞完成','Collision complete'), 'status.8':('任务已暂停','Paused'),
    'status.9':('先腾出原料槽','Make room for returns'), 'status.10':('配方已变更，请终止','Recipe changed; abort'),
    'status.11':('等待磁场充能','Charging magnetic field'), 'status.28':('磁场容量不足','Increase field capacity'),
    'status.21':('轨道断开','Connect the beamline'), 'status.22':('部件朝向不匹配','Adjust the orientation'),
    'status.23':('轨道未经过 B','Route through B'), 'status.24':('缺少加速马达','Add accelerator motors'),
    'status.25':('弯道过紧','Widen the bends'), 'status.26':('导环间距过大','Reduce guide spacing'), 'status.27':('轨道数据无效','Invalid beamline'),
}
for key, (a, b) in collider_pairs.items():
    zh[f'collider.{ID}.{key}'], en[f'collider.{ID}.{key}'] = a, b
grades = [('原始', 'Primitive'), ('基础', 'Basic'), ('中级', 'Improved'), ('高级', 'Advanced'), ('精良', 'Elite'), ('极致', 'Ultra'), ('终极', 'Ultimate')]
zh[f'item.{ID}.fluid_capacity_addon'] = '流体扩容插件'
en[f'item.{ID}.fluid_capacity_addon'] = 'Fluid Capacity Addon'
write(f'assets/{ID}/models/item/fluid_capacity_addon.json', {'parent': 'oritech:item/machine_fluid_addon',
    'textures': {'0': 'oritech:block/machine_fluid_addon_on'}})
write(f'data/{ID}/recipe/fluid_capacity_addon.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc',
    'pattern': ['PTP', 'CAC', 'PTP'],
    'key': {'P': {'item': 'oritech:machine_plating_block'}, 'T': {'item': 'oritech:small_tank_block'},
            'C': {'item': 'oritech:processing_unit'}, 'A': {'item': 'oritech:machine_fluid_addon'}},
    'result': {'id': f'{ID}:fluid_capacity_addon', 'count': 1}})
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
    'tooltip.fluid_capacity': ('装入处理器，使所有流体储罐容量翻倍。', 'Install in a processor to double all fluid tank capacities.'),
    'tooltip.fluid_capacity_limit': ('最多 %s 个，每个占用一个插件位。', 'Up to %s addons; each uses one addon slot.'),
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
    'load_hint': ('自动安装奥瑞、OritechThings 或流体扩容插件。容量不足或不兼容的物品会留在槽内。', 'Automatically install Oritech, OritechThings or fluid capacity addons. Excess or incompatible addons stay in this slot.'),
    'retrieve_hint': ('从这里取回已卸载的插件。', 'Retrieve uninstalled addons here.'),
    'no_addons': ('尚未安装插件', 'No addons installed'), 'installed_count': ('已安装：%s', 'Installed: %s'),
    'capacity_full': ('插件容量已满', 'Capacity full'), 'load_pending': ('等待装载', 'Load pending'),
    'load_unsupported': ('不支持此插件', 'Unsupported addon'), 'load_atomic': ('原子锻造不支持处理室', 'No chambers here'),
    'load_modules': ('罐室已达上限', 'Module limit'), 'load_combined': ('组合插件需独立安装', 'Combined conflict'),
    'retrieve_first': ('先取出已卸载插件', 'Retrieve first'), 'empty_fluid': ('先排空相关储罐', 'Drain tanks'),
    'load_fluid_limit': ('流体扩容已达上限', 'Tank addon limit'),
    'drain_for_capacity': ('流体超过缩容上限', 'Drain excess fluid'),
    'tank_capacity': ('容量：%s 毫桶', 'Capacity: %s mB'),
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
