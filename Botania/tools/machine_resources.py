"""Machine recipes, names, and runtime resources."""

MACHINES = {
    'mana_bridge': ('魔力互通器', 'Mana Bridge', 'mana_pool', 'manasteel_ingot', '在魔力池和加压管道之间输送魔力。', 'Transfers mana between pools and pressurized tubes.'),
    'mana_charger': ('魔力充能座', 'Mana Charging Stand', 'mana_tablet', 'manasteel_ingot', '给魔力石板等物品充魔，也能抽出其中的魔力。', 'Charges mana items or draws out their stored mana.'),
    'mana_infuser': ('魔力灌注室', 'Mana Infusion Chamber', 'mana_pool', 'manasteel_ingot', '用魔力灌注材料，也能进行炼金和复制。', 'Infuses items with mana, including alchemy and conjuration.'),
    'runic_forge': ('符文锻造室', 'Runic Forge', 'runic_altar', 'mana_diamond', '自动制作符文，需要电能和魔力。', 'Crafts runes using electricity and mana.'),
    'pure_converter': ('纯净转化室', 'Pure Conversion Chamber', 'pure_daisy', 'manasteel_ingot', '将原木、石头等材料转变为活木、活石。', 'Turns logs, stone and other materials into livingwood, livingrock and similar blocks.'),
    'terra_condenser': ('泰拉凝聚室', 'Terra Condensation Chamber', 'terrestrial_agglomeration_plate', 'terrasteel_ingot', '在泰拉凝聚平台上自动制作泰拉钢。', 'Automates terrasteel crafting on a terrestrial agglomeration platform.'),
    'botanical_brewery': ('植物酿造室', 'Botanical Brewing Chamber', 'botanical_brewery', 'mana_diamond', '消耗电能和魔力，自动酿制精酿。', 'Brews automatically using electricity and mana.'),
    'ore_processor': ('凝矿处理室', 'Ore Processing Chamber', 'orechid', 'terrasteel_ingot', '用凝矿兰将石头变成矿石。需要电能和魔力。', 'Uses an Orechid to turn stone into ore. Requires electricity and mana.'),
    'metamorphic_stone': ('异构石转化室', 'Metamorphic Stone Chamber', 'marimorphosis', 'terrasteel_ingot', '用魔力转化异构石，产物随生物群系而异。', 'Makes metamorphic stone with mana. Results vary by biome.'),
    'elven_trade_controller': ('精灵贸易控制器', 'Elven Trade Controller', 'elven_gateway_core', 'elementium_ingot', '向精灵传送门运送材料，并收取贸易所得。', 'Sends ingredients through an Alfheim Portal and collects the traded goods.'),
    'mana_enchanter_controller': ('魔力附魔控制器', 'Mana Enchanter Controller', 'mana_pylon', 'elementium_ingot', '为魔力附魔台提供装备、附魔书和魔力。', 'Supplies equipment, enchanted books and mana to a Mana Enchanter.'),
}


def generate(root, write, zh, en):
    mod = 'botanicalmekanism'
    pickaxes = [f'{mod}:mechanical_apothecary']
    for name, (cn, english, center, material, hint_cn, hint_en) in MACHINES.items():
        zh[f'block.{mod}.{name}'], en[f'block.{mod}.{name}'] = cn, english
        zh[f'description.{mod}.{name}'], en[f'description.{mod}.{name}'] = hint_cn, hint_en
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
        pickaxes.append(f'{mod}:{name}')
    write('data/minecraft/tags/block/mineable/pickaxe.json', {'replace': False, 'values': pickaxes})
    zh[f'chemical.{mod}.mana'], en[f'chemical.{mod}.mana'] = '魔力', 'Mana'
    pairs = {
        'materials': ('材料', 'Materials'), 'products': ('产物', 'Output'), 'books': ('附魔书', 'Books'),
        'extra.mana_infuser': ('催化剂', 'Catalyst'), 'extra.runic_forge': ('辅料', 'Reagent'),
        'extra.botanical_brewery': ('容器', 'Vessel'), 'extra.ore_processor': ('花', 'Flower'),
        'bridge_mode.0': ('从池抽取', 'From pool'), 'bridge_mode.1': ('向池供给', 'To pool'),
        'charge_mode.0': ('充入物品', 'Charge item'), 'charge_mode.1': ('抽出物品', 'Drain item'),
        'connected_side': ('连接方向：%s', 'Connection: %s'), 'connection_settings': ('连接设置', 'Connection Settings'),
        'target_apply': ('设定 %', 'Set %'), 'mana': ('魔力：%s / %s', 'Mana: %s / %s'),
        'choose_recipe': ('选择配方', 'Choose recipe'), 'automatic': ('自动匹配', 'Automatic'), 'close': ('关闭', 'Close'),
        'recipe_selected': ('配方：%s', 'Recipe: %s'), 'recipe_missing': ('配方已不适用，请重新选择', 'Recipe unavailable; choose again'),
        'search_name': ('搜索产物名称', 'Search output names'), 'no_results': ('没有适用配方', 'No supported recipes'),
        'applying': ('正在应用…', 'Applying…'), 'rejected': ('设置未生效', 'Setting rejected'),
    }
    for i, pair in enumerate([
        ('正在工作', 'Working'), ('放入配方材料', 'Insert recipe materials'), ('缺少辅料或容器', 'Needs reagent, container or books'),
        ('魔力不足', 'Not enough mana'), ('需要能量', 'Needs energy'), ('产物空间不足', 'Output full'),
        ('红石已禁止', 'Redstone disabled'), ('请检查装置或平台', 'Check the structure or platform'), ('所选方向没有魔力池', 'No mana pool on the selected side'),
        ('物品或魔力池不接受此操作', 'The item or pool cannot perform this operation'), ('等待工作', 'Waiting'), ('周围区块未加载', 'Nearby chunks unloaded'),
        ('装置正在使用或控制器重复', 'Device occupied or duplicate controllers'),
        ('炎矿需要有顶维度', 'Orechid Ignem needs a ceiling dimension'),
        ('魔力已满', 'Mana storage is full'),
    ]): pairs[f'status.{i}'] = pair
    for key, (cn, english) in pairs.items(): zh[f'gui.{mod}.machine.{key}'], en[f'gui.{mod}.machine.{key}'] = cn, english
    from botanical_models import generate as generate_models
    generate_models(root, write)
    for name in ['mechanical_apothecary', *MACHINES]:
        zh[f'container.{mod}.{name}'] = zh[f'block.{mod}.{name}']
        en[f'container.{mod}.{name}'] = en[f'block.{mod}.{name}']
