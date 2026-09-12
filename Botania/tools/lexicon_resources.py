"""Add a category to the existing resource-pack-backed Lexica Botania.

No book.json override, extra book item, or client-only Java page is required.
Mechanical recipes use small Patchouli templates generated from our actual recipe data.
"""
from machine_resources import MACHINES

BASE = 'assets/botania/patchouli_books/lexica_botania/en_us'
MOD = 'botanicalmekanism'
HELP = {
    'mana_lotus': [('接 FE 后，用森林法杖绑定 6 格内的魔力发射器。默认每 tick 200 FE 生成 4 魔力。未绑定、满储备或暂停时停止。', 'Supply FE and bind a spreader within six blocks with the Wand. Default output is 4 mana/t for 200 FE/t. An unbound, full or paused Lotus stops producing.')],
    'bionic_amaranthus': [('在合适地面生成普通神秘花。每次使用相当于 5,000 FE 的储备，颜色和种植条件沿用原花。', 'Grows ordinary mystical flowers on suitable ground. Each operation uses a reserve worth 5,000 FE; colors and planting rules remain native.')],
    'bionic_clayconia': [('消耗附近世界中的沙，每次产出一个粘土球，默认花费相当于 4,000 FE。可与手掌花和漏斗花组成补沙、转化、收集线。', 'Consumes sand in the world to produce one clay ball, using a reserve worth 4,000 FE. Pair with Rannuncarpus and Hopperhock for placement and collection.')],
    'bionic_agricarnation': [('加快合适植物的生长尝试，每次有效尝试默认相当于 250 FE。一次尝试不保证植物长大。', 'Accelerates eligible plant growth attempts. Each valid attempt costs a reserve worth 250 FE, even when growth does not occur.')],
    'bionic_hopperhock': [('收集掉落物到相邻容器，保留物品框筛选。潜行使用森林法杖切换模式。每次成功收集批次消耗 1 魔力储备；断供后停止。', 'Collects drops into adjacent inventories with item-frame filters. Sneak-use the Wand to change mode. Each successful collection batch uses 1 reserved mana; no free fallback.')],
    'bionic_rannuncarpus': [('以花下两格的方块作地面样板，消耗掉落方块并放置。潜行使用森林法杖切换状态匹配；每次放置 1 魔力，另需保留 1 魔力。', 'Places dropped blocks on ground matching the block two below the flower. Sneak-use the Wand to change state matching. Uses 1 mana per placement and keeps a 1-mana reserve.')],
    'bionic_exoflame': [('给可加工的熔炉补充燃烧时间并推动烹煮。补燃烧消耗 300 魔力储备，默认相当于 15,000 FE；加速沿用原储备条件。', 'Heats working furnaces and boosts cooking. Adding burn time costs 300 reserved mana, worth 15,000 FE by default. Cooking boosts retain the native reserve requirements.')],
    'mechanical_apothecary': [('16 格材料，独立终结槽和六格产物。每批需要 1,000 mB 水；原版花需 100 tick、基础 5,000 FE。仿生花使用自己的材料和工时。', 'Provides 16 material slots, a reagent slot and six outputs. Each batch uses 1,000 mB water. Native flowers take 100 ticks and 5,000 FE; bionic recipes set their own costs.'),
        ('水桶可连续补水，空桶单独输出，也能接流体导管。材料默认前、上、左输入，终结材料从背面输入，右侧出料。普通花药台不能合成仿生花。', 'Fill with repeated water buckets or fluid pipes; empty buckets have their own output. Materials enter front/top/left, reagent at back, outputs at right. Native basins cannot make bionic flowers.')],
    'mana_bridge': [('紧邻普通原魔力池，选池所在方向和抽取／供给模式。1 单位管道魔力等于 1 原魔力，最多 1,000/t。池连接面不接化学管道。', 'Place beside a native pool and select its side and transfer mode. One chemical unit equals one mana, up to 1,000/t. The pool face is reserved from chemical tubes.')],
    'mana_charger': [('紧邻原池，放入单件可储魔物品，选择充入／抽出和目标百分比。达到目标后输出。遵守物品充放许可，不支持创造池。', 'Place beside a pool. Insert one mana item and choose charge/drain plus a target percentage. It moves to output at the target. Respects item permissions; no creative pools.')],
    'mana_infuser': [('处理灌注、炼金与复制，基础 100 tick。背面额外槽可装原炼金或复制催化方块，匹配催化配方优先。配方魔力不受能量升级折扣影响。', 'Handles infusion, alchemy and conjuration in 100 base ticks. Place a native catalyst in the extra slot at the back. Catalyst recipes take priority; mana costs are not discounted.')],
    'runic_forge': [('材料和配方催化物共用 16 格，终结材料另放。基础 200 tick，支付原配方魔力；按原规则返还催化物到原槽，其余容器进入输出。', 'Uses 16 slots for ingredients and recipe catalysts, plus a reagent slot. Takes 200 base ticks and native mana costs. Retained catalysts return to their slots; containers go to output.')],
    'pure_converter': [('只处理可安全变成物品、无需世界函数的固体转化。流体和带回调的深板岩配方仍用原白雏菊。基础吞吐等效原花八个位置同时工作。', 'Handles solid conversions without world functions. Fluids and callback-based deepslate recipes remain with the native Pure Daisy. Base throughput matches all eight native positions working.')],
    'terra_condenser': [('放在真实 3×3 平台中央：下层中心和四角为原底座标签方块，四边为青金石块。基础 400 tick，支付原魔力成本；平台坏掉暂停。', 'Place at the center of a real 3x3 terra platform: native base blocks at center/corners, lapis at the four edges. Uses native mana and 400 base ticks. A broken platform pauses work.')],
    'botanical_brewery': [('背面放空魔力玻璃瓶、精灵玻璃瓶等容器，材料放左侧。基础 200 tick，成品组件和魔力消耗由实际容器决定。成品药剂瓶不能再次当空瓶。', 'Supply empty managlass vials, alfglass flasks or other eligible vessels at the back. Base time is 200 ticks; the vessel determines the brew and mana cost. Filled brews are not empty vessels.')],
    'ore_processor': [('额外槽装凝矿兰或炎矿兰，投入对应石材。按所在地和原配方权重随机产矿。炎矿要求有顶维度。输出堵塞时不会抽取结果或花费材料。', 'Insert an Orechid or Orechid Ignem and suitable stone. Uses native local weights for random ores. Ignem requires a ceiling dimension. Blocked output prevents rolls and material spending.')],
    'metamorphic_stone': [('按原异构花配方转化石材，保留所在地生物群系权重。结果不能指定，完成时只抽一次；魔力扣实际选中成本。', 'Transforms stone with native Marimorphosis recipes and local biome weights. Results cannot be selected. Rolls once on completion and pays the chosen mana cost.')],
    'elven_trade_controller': [('紧邻真实精灵门核心并选方向，先按原方式开门。门支付 200,000 开门魔力，每批贸易再支付原 500 魔力；本机只支付搬运能量。', 'Place beside a real portal core and select its side. Open the portal normally: it pays 200,000 mana to open and the native 500 per trade. The controller uses energy for handling.'),
        ('保留门框、自然水晶和原池。所有产物进入输出；门未就绪或产物满时保留材料。词典升级、原样退回和特殊贸易仍直接使用原门。', 'Keep the frame, natura pylons and pools. All products enter output. An inactive portal or full output keeps materials intact. Upgrade the Lexica and process special returns at the native portal.')],
    'mana_enchanter_controller': [('紧邻已经形成的原魔力附魔装置，选择方向并放入装备和附魔书。真实装置处理有效附魔、冲突和魔力成本，书籍保留。', 'Place beside a formed mana enchanter, select its side, and insert equipment and enchanted books. The real device decides valid enchantments, conflicts and mana cost. Books remain.'),
        ('装置可由原火花或本机供魔。结构损坏后暂停，修好后续作；拆下控制器时装备留在真实装置，不会复制进控制器物品。', 'Supply mana through native sparks or the controller. Structure damage pauses the operation; repairs resume it. Removing the controller leaves equipment in the real device, without copying it.')],
    'resonance_spark_augment': [('在原池和魔力机器上装同色原火花，即可供魔。机器顶部需要允许 Chemical 输入。魔力火花传魔力，多媒体火花传物品，两种网络不同。', 'Native sparks of matching color supply pools and mana machines. Machine top faces must accept chemicals. Mana sparks transfer mana; Corporea sparks transfer items. These are different networks.'),
        ('两端火花都装共鸣增幅器后，各轴距离由 12 格扩大到 32 格。保留染料、法杖、原传输和池升级；潜行拆下时保留整个升级效果。', 'Install this augment at both ends to extend each-axis range from 12 to 32 blocks. Keeps native dyes, Wand controls, transfer and pool augments. Sneak-removal preserves the combined augment.')],
    'corporea_orchid': [('仿生织网花连接 ME 电缆与多媒体火花网络。由 ME 供能，占用一个通道，待机 4 AE/t，无需额外 FE 线。未安装 AE2 时保留方块和设置。', 'Links ME cables with Corporea sparks. Uses one ME channel and 4 AE/t idle power, with no extra FE cable. If AE2 is absent, the block and settings remain saved.'),
        ('花和存放物品的箱子上安装普通多媒体火花，主火花另放在旁边的支撑上。主火花下方不作为库存节点。ME 终端和多媒体装置双向访问真实物品。', 'Put ordinary Corporea sparks on the flower and item stores. Place the master separately: its own block is not a stock node. ME terminals and Corporea devices can both access real items.'),
        ('默认双向，也能只开放一侧。ME 返回视图排除本花挂载的多媒体库存，防止循环计数。一组多媒体网络只允许一朵活动接入花。', 'Defaults to both directions; either direction can be disabled. The ME view excludes Corporea storage mounted by bridge flowers to avoid counting loops. One active bridge per Corporea network.'),
        ('同一箱子不要再用存储总线接回同一 ME 网络，双箱只装一枚多媒体火花。冲突会显示原因并停用。断电、无通道、缺主火花或卸载时停止访问。', 'Do not also connect the same chest through an ME storage bus; use one Corporea spark per double chest. Conflicts disable access with a reason. Offline grids, missing masters and unloaded chunks stop access.'),
        ('仅互通物品，不传流体或魔力，也不自动下单合成。默认两向合计最多 2,048 件/t。多媒体向 ME 取物按 ME 原规则付电；ME 端直接访问物理库存。', 'Items only: no fluids, mana or automatic crafting requests. Both directions share a default 2,048 items/t limit. Corporea extraction pays normal ME energy; the ME side accesses physical stores.')],
    'resonance_flower': [('旧共鸣花继续兼容已有网络，但不再作为新传输方案。可回收为共鸣增幅器。此花的模型、贴图和原稿完整保留，后续另有用途。', 'Legacy Resonance Flowers remain compatible with existing networks. Recycle them into a spark augment if desired. Their model, textures and source art stay preserved for future use.')],
    'resonance_bud': [('旧共鸣芽继续兼容已有网络，可以回收为共鸣增幅器。新生产线直接使用原火花，无需选择网络或管理成员。', 'Legacy Resonance Buds keep working on existing networks and can be recycled into a spark augment. New setups use native sparks without network selection or member management.')],
}


SHORT_TITLES = {
    'mana_lotus': 'Conduction Lotus', 'bionic_amaranthus': 'Jaded Amaranthus', 'bionic_clayconia': 'Clayconia',
    'bionic_agricarnation': 'Agricarnation', 'bionic_hopperhock': 'Hopperhock', 'bionic_rannuncarpus': 'Rannuncarpus',
    'bionic_exoflame': 'Exoflame', 'mechanical_apothecary': 'Apothecary', 'mana_bridge': 'Mana Bridge', 'mana_charger': 'Mana Charger',
    'mana_infuser': 'Mana Infusion', 'runic_forge': 'Runic Forge', 'pure_converter': 'Pure Conversion', 'terra_condenser': 'Terra Condenser',
    'botanical_brewery': 'Botanical Brewery', 'ore_processor': 'Ore Processing', 'metamorphic_stone': 'Metamorphic Stone',
    'elven_trade_controller': 'Elven Trade', 'mana_enchanter_controller': 'Mana Enchanter', 'resonance_spark_augment': 'Spark Resonance',
    'corporea_orchid': 'Corporea Orchid', 'resonance_flower': 'Resonance Flower', 'resonance_bud': 'Resonance Bud',
}


def generate(root, write, zh, en, plants, recipes):
    zh['book.botanicalmekanism.category'], en['book.botanicalmekanism.category'] = '植物机械', 'Botanical Mechanisms'
    zh['book.botanicalmekanism.intro'], en['book.botanicalmekanism.intro'] = '仿生花、自动加工、火花传输和物品网络。手持本词典右键设备，可直接打开对应条目。', 'Bionic flowers, automated processing, sparks and item networks. Use this Lexica on a device to open its entry.'
    write(f'{BASE}/categories/botanicalmekanism.json', {'name': 'book.botanicalmekanism.category', 'description': 'book.botanicalmekanism.intro',
          'icon': f'{MOD}:mana_lotus', 'sortnum': 11})
    mechanical = {row[0]: row for row in recipes if row[0] not in ('resonance_flower', 'resonance_bud')}
    for index, (name, descriptions) in enumerate(HELP.items()):
        item = f'{MOD}:{name}'; key = f'book.{MOD}.{name}'
        title = f'item.{MOD}.{name}' if name == 'resonance_spark_augment' else f'block.{MOD}.{name}'
        zh[f'{key}.title'], en[f'{key}.title'] = zh[title], SHORT_TITLES[name]
        pages = []
        for page, (cn, english) in enumerate(descriptions):
            zh[f'{key}.{page}'], en[f'{key}.{page}'] = cn, english
            pages.append({'type': 'patchouli:spotlight' if page == 0 else 'patchouli:text', 'text': f'{key}.{page}',
                          **({'item': item, 'title': f'{key}.title'} if page == 0 else {})})
        if name in mechanical:
            _, materials, reagent, count, ticks, power = mechanical[name]
            components = [{'type': 'patchouli:header', 'x': -1, 'y': 0, 'text': 'book.botanicalmekanism.mechanical_apothecary.title'}]
            for slot, material in enumerate(materials):
                components.append({'type': 'patchouli:item', 'x': 8+slot%4*26, 'y': 18+slot//4*26, 'item': material, 'framed': True})
            for x, ingredient in [(8, reagent), (34, 'minecraft:water_bucket'), (86, item)]:
                components.append({'type': 'patchouli:item', 'x': x, 'y': 132, 'item': ingredient, 'framed': True, 'link_recipe': ingredient == item})
            components.append({'type': 'patchouli:text', 'x': 62, 'y': 136, 'text': '→'})
            template = f'botanicalmekanism/recipe/{name}'
            write(f'{BASE}/templates/{template}.json', {'components': components})
            pages.append({'type': f'botania:{template}', **({'flag': 'mod:ae2'} if name == 'corporea_orchid' else {})})
            zh[f'{key}.cost'] = f'每批需要 1,000 mB 水、{ticks*power:,} FE，基础工时 {ticks} tick。图中底排为终结材料、水和产物。速度／能量升级按机械花药台规则生效。'
            en[f'{key}.cost'] = f'Each batch needs 1,000 mB water and {ticks*power:,} FE over {ticks} base ticks. The bottom row shows reagent, water and output. Apothecary speed/energy upgrades apply.'
            pages.append({'type': 'patchouli:text', 'text': f'{key}.cost'})
        elif name in MACHINES or name in ('mechanical_apothecary', 'resonance_spark_augment'):
            pages.append({'type': 'patchouli:crafting', 'recipe': item})
        write(f'{BASE}/entries/botanicalmekanism/{name}.json', {'name': title, 'icon': item, 'category': 'botania:botanicalmekanism',
              'sortnum': index, 'pages': pages, 'extra_recipe_mappings': {item: 0}})
