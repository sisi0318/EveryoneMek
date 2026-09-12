"""Machine resources. Reuse this repository's original 16-pixel industrial textures."""
import shutil
from pathlib import Path

MACHINES = {
    'mana_bridge': ('魔力互通器', 'Mana Bridge', 'mana_pool', 'manasteel_ingot', 'source_converter', '连接相邻原魔力池与魔力管道。选择方向和传输模式。', 'Connects an adjacent native mana pool to chemical tubes. Select the pool face and transfer direction.'),
    'mana_charger': ('魔力充能座', 'Mana Charging Stand', 'mana_tablet', 'manasteel_ingot', 'potion_bottler', '从相邻原池充放魔力物品，达到目标百分比后输出。', 'Charges or drains a mana item using an adjacent native pool; outputs it at the selected percentage.'),
    'mana_infuser': ('魔力灌注室', 'Mana Infusion Chamber', 'mana_pool', 'manasteel_ingot', 'imbuement_chamber', '消耗能量与魔力灌注材料。背面补充炼金或复制催化剂。', 'Uses energy and mana to infuse materials. Insert alchemy or conjuration catalysts at the back.'),
    'runic_forge': ('符文锻造室', 'Runic Forge', 'runic_altar', 'mana_diamond', 'enchanting_apparatus', '消耗能量、魔力和终结材料制作符文，保留配方催化物。', 'Uses energy, mana and a reagent to craft runes, retaining recipe catalysts.'),
    'pure_converter': ('纯净转化室', 'Pure Conversion Chamber', 'pure_daisy', 'manasteel_ingot', 'whirlisprig_station', '消耗能量转化活木、活石等材料。仅支持无需世界条件的固体转化。', 'Uses energy to make livingwood, livingrock and other supported solid conversions without world effects.'),
    'terra_condenser': ('泰拉凝聚室', 'Terra Condensation Chamber', 'terrestrial_agglomeration_plate', 'terrasteel_ingot', 'ritual_controller', '置于完整泰拉平台中央，消耗能量和原配方魔力凝聚材料。', 'Place at the center of a complete terra platform. Uses energy and the recipe mana cost.'),
    'botanical_brewery': ('植物酿造室', 'Botanical Brewing Chamber', 'botanical_brewery', 'mana_diamond', 'potion_mixer', '消耗能量和魔力酿造植物药剂。背面补充药剂容器。', 'Uses energy and mana to brew botanical potions. Supply brew containers at the back.'),
    'ore_processor': ('凝矿处理室', 'Ore Processing Chamber', 'orechid', 'terrasteel_ingot', 'magic_crusher', '消耗能量和魔力随机凝矿。背面装入凝矿兰或炎矿兰；炎矿要求有顶维度。', 'Uses energy and mana for random ores. Insert an Orechid or Orechid Ignem; Ignem needs a ceiling dimension.'),
    'metamorphic_stone': ('异构石转化室', 'Metamorphic Stone Chamber', 'marimorphosis', 'terrasteel_ingot', 'source_extractor', '消耗能量和魔力转化异构石，结果受所在生物群系影响。', 'Uses energy and mana to make metamorphic stone, weighted by the local biome.'),
    'elven_trade_controller': ('精灵贸易控制器', 'Elven Trade Controller', 'elven_gateway_core', 'elementium_ingot', 'drygmy_station', '紧邻已开启的真实精灵门，以能量执行贸易。魔力由门旁原池承担。', 'Place beside an open native elven portal. Uses energy for trades; the portal draws mana from its own pools.'),
    'mana_enchanter_controller': ('魔力附魔控制器', 'Mana Enchanter Controller', 'mana_pylon', 'elementium_ingot', 'glyph_scribe', '紧邻已形成的魔力附魔装置。提供装备、附魔书、能量和魔力，书籍保留。', 'Place beside a formed mana enchanter. Supply equipment, books, energy and mana; books remain.'),
}


def generate(root, write, zh, en):
    mod = 'botanicalmekanism'
    pickaxes = [f'{mod}:mechanical_apothecary']
    art = {}
    for name, (cn, english, center, material, texture_source, hint_cn, hint_en) in MACHINES.items():
        zh[f'block.{mod}.{name}'], en[f'block.{mod}.{name}'] = cn, english
        zh[f'description.{mod}.{name}'], en[f'description.{mod}.{name}'] = hint_cn, hint_en
        texture = f'{mod}:block/{name}'
        write(f'assets/{mod}/models/block/{name}.json', {'parent': 'minecraft:block/cube', 'textures': {
            'particle': f'{texture}/side', 'north': f'{texture}/front', 'up': f'{texture}/top',
            **{face: f'{texture}/side' for face in ['south', 'east', 'west', 'down']}}})
        write(f'assets/{mod}/models/block/{name}_active.json', {'parent': f'{mod}:block/{name}', 'textures': {'north': f'{texture}/front_active'}})
        write(f'assets/{mod}/models/item/{name}.json', {'parent': f'{mod}:block/{name}'})
        write(f'assets/{mod}/blockstates/{name}.json', {'variants': {f'facing={face},active={str(active).lower()}': {
            'model': f'{mod}:block/{name}' + ('_active' if active else ''), 'y': rotation}
            for face, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)] for active in [False, True]}})
        components = [f'mekanism:{key}' for key in ['ejector', 'owner', 'redstone_control', 'security', 'side_config', 'upgrades', 'energy', 'items', 'chemicals']]
        components.append(f'{mod}:machine_settings')
        write(f'data/{mod}/loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{
            'type': 'minecraft:item', 'name': f'{mod}:{name}', 'functions': [{'function': 'minecraft:copy_name', 'source': 'block_entity'},
            {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': components}]}]}]})
        advanced = material in ('terrasteel_ingot', 'elementium_ingot', 'mana_diamond')
        write(f'data/{mod}/recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['MAM', 'CSC', 'MIM'],
            'key': {key: {'item': item} for key, item in {'M': f'botania:{material}', 'A': f'botania:{center}',
                    'C': 'mekanism:' + ('advanced_control_circuit' if advanced else 'basic_control_circuit'), 'S': 'mekanism:steel_casing',
                    'I': 'mekanism:' + ('alloy_reinforced' if advanced else 'alloy_infused')}.items()}, 'result': {'id': f'{mod}:{name}', 'count': 1}})
        source = root.parent / f'Ars-Nouveau/src/main/resources/assets/arsmekanism/textures/block/{texture_source}'
        destination = root / f'src/main/resources/assets/{mod}/textures/block/{name}'
        destination.mkdir(parents=True, exist_ok=True)
        for face in ('front', 'top', 'side', 'front_active'):
            shutil.copyfile(source / f'{face}.png', destination / f'{face}.png')
        art[name] = str(source.relative_to(root.parent)).replace('\\', '/')
        pickaxes.append(f'{mod}:{name}')
    write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': pickaxes})
    zh[f'chemical.{mod}.mana'], en[f'chemical.{mod}.mana'] = '魔力', 'Mana'
    pairs = {
        'materials': ('材料', 'Materials'), 'products': ('产物', 'Output'), 'books': ('附魔书', 'Books'),
        'extra.mana_infuser': ('催化剂', 'Catalyst'), 'extra.runic_forge': ('终结材料', 'Reagent'),
        'extra.botanical_brewery': ('容器', 'Vessel'), 'extra.ore_processor': ('花', 'Flower'),
        'bridge_mode.0': ('从池抽取', 'From pool'), 'bridge_mode.1': ('向池供给', 'To pool'),
        'charge_mode.0': ('充入物品', 'Charge item'), 'charge_mode.1': ('抽出物品', 'Drain item'),
        'target_apply': ('目标 %', 'Target %'), 'mana': ('魔力：%s / %s', 'Mana: %s / %s'),
        'choose_recipe': ('选择配方', 'Choose recipe'), 'automatic': ('自动匹配', 'Automatic'), 'close': ('关闭', 'Close'),
        'search_name': ('搜索产物名称', 'Search output names'), 'no_results': ('没有适用配方', 'No supported recipes'),
        'applying': ('正在应用…', 'Applying…'), 'rejected': ('设置未生效', 'Setting rejected'),
    }
    for i, pair in enumerate([
        ('正在工作', 'Working'), ('放入配方材料', 'Insert recipe materials'), ('缺少终结材料或容器', 'Needs reagent, container or books'),
        ('需要魔力或目标池空间', 'Needs mana or pool space'), ('需要能量', 'Needs energy'), ('产物空间不足', 'Output full'),
        ('红石已禁止', 'Redstone disabled'), ('原装置或平台未就绪', 'Native device or platform not ready'), ('目标不是有效魔力池', 'No supported pool at target'),
        ('物品或魔力池禁止转移', 'Item or pool denies transfer'), ('等待工作', 'Waiting'), ('周围区块未加载', 'Nearby chunks unloaded'),
        ('装置正在使用或控制器重复', 'Device occupied or duplicate controllers'),
        ('炎矿需要有顶维度', 'Orechid Ignem needs a ceiling dimension'),
    ]): pairs[f'status.{i}'] = pair
    for key, (cn, english) in pairs.items(): zh[f'gui.{mod}.machine.{key}'], en[f'gui.{mod}.machine.{key}'] = cn, english
    import json
    (root / 'art/machine-texture-reuse.json').write_text(json.dumps({'method': 'Direct reuse of original repository 16x16 textures; no repainting or filtering.',
         'sources': art, 'faces': ['front', 'top', 'side', 'front_active']}, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
