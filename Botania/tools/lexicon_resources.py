"""Add a category to the existing resource-pack-backed Lexica Botania.

No book.json override, extra book item, or client-only Java page is required.
Mechanical recipes use small Patchouli templates generated from our actual recipe data.
"""
from machine_resources import MACHINES

BASE = 'assets/botania/patchouli_books/lexica_botania/en_us'
MOD = 'botanicalmekanism'
HELP = {
    'mana_lotus': [('$(item)仿生导能莲$(0)能够将电能转化为魔力。接上电缆，再用森林法杖将它绑定到附近的魔力发射器，即可开始供魔。$(p)每秒最多产生 80 魔力，消耗 4,000 FE。', 'The $(item)Conduction Lotus$(0) turns electricity into mana. Connect a power cable and bind it to a nearby mana spreader with the Wand of the Forest.$(p)It produces up to 80 mana per second, using 4,000 FE.')],
    'bionic_amaranthus': [('$(item)仿生翡翠苋$(0)会在周围长出神秘花。给它供电，并留出一片可以种花的地面即可。', 'The $(item)Bionic Jaded Amaranthus$(0) grows mystical flowers nearby. Supply electricity and leave some suitable ground for the flowers.')],
    'bionic_clayconia': [('$(item)仿生粘土花$(0)会把附近的沙变成粘土球，需要持续供电。$(p)搭配手掌花补沙、漏斗花收集，就能自动生产粘土。', 'The $(item)Bionic Clayconia$(0) turns nearby sand into clay balls while powered.$(p)A Rannuncarpus can replace the sand, and a Hopperhock can collect the clay.')],
    'bionic_agricarnation': [('$(item)仿生田园康乃馨$(0)用电能促进附近植物生长。放在农田旁并接通电源，就能帮助作物更快成熟。', 'The $(item)Bionic Agricarnation$(0) uses electricity to encourage plant growth. Place it beside a farm and supply power to help crops mature faster.')],
    'bionic_hopperhock': [('$(item)仿生漏斗花$(0)会拾取附近的掉落物，放进相邻的箱子。它需要电能才能工作。$(p)在箱子上挂物品展示框，可以指定收集的物品。潜行使用森林法杖右击花，可切换筛选方式。', 'The $(item)Bionic Hopperhock$(0) uses electricity to collect nearby drops into adjacent chests.$(p)Item frames on the chests filter their contents. Sneak-use the Wand on the flower to change the filtering mode.')],
    'bionic_rannuncarpus': [('$(item)仿生手掌花$(0)会拾起掉落的方块并放到地面上，需要电能。$(p)花下两格的方块决定它会在哪种地面上放置。潜行使用森林法杖右击花，可切换是否区分方块状态。', 'The $(item)Bionic Rannuncarpus$(0) uses electricity to pick up and place dropped blocks.$(p)The block two below the flower determines the ground it builds on. Sneak-use the Wand to toggle block-state matching.')],
    'bionic_exoflame': [('$(item)仿生冶炼火$(0)能够用电能加热附近的熔炉，省去燃料并加快烧炼。熔炉中需要放好可烧炼的材料。', 'The $(item)Bionic Exoflame$(0) heats nearby furnaces with electricity, replacing fuel and speeding up smelting. Put smeltable ingredients in the furnaces first.')],
    'mechanical_apothecary': [('$(item)机械花药台$(0)会自动调合花瓣、种子等材料，制作魔法花和仿生花。材料放左侧，种子或其他辅料放下方的小槽。$(p)每次制作需要一桶水和电能。', 'The $(item)Mechanical Apothecary$(0) combines petals, seeds and other ingredients into magical and bionic flowers. Put ingredients on the left and the reagent in the small lower slot.$(p)Each craft needs one bucket of water and electricity.'), ('可以放入多桶水，也可以用流体管道补水。空桶会送到旁边的槽位。$(p)管道从背面送入辅料，右侧取出成品。仿生花需要在这里制作。', 'Fill it with water buckets or fluid pipes. Empty buckets move to the adjacent slot.$(p)Feed reagents through the back and take products from the right. Bionic flowers must be made here.'), ('在机器界面打开 JEI，点击配方旁的加号即可放入材料。按住 Shift 点击可放入更多。', 'While a machine is open, use the JEI recipe plus button to fill its ingredients. Hold Shift to fill more.')],
    'mana_bridge': [('$(item)魔力互通器$(0)能在魔力池和加压管道之间输送魔力。将它贴着魔力池放下，在左侧“连接设置”中选择池子所在的一面，再选择抽取或供给。$(p)需要电能，每秒最多输送 20,000 魔力。连接池的一面不能再接管道。', 'The $(item)Mana Bridge$(0) moves mana between a pool and pressurized tubes. Place it beside a pool, open Connection Settings on the left to select its side, then choose draw or supply.$(p)It needs electricity and transfers up to 20,000 mana per second. The pool face cannot also connect to a tube.')],
    'mana_charger': [('放入一件魔力石板、戒指等储魔物品，选择充入或抽出，再设定目标百分比。达到目标后，物品会送到产物槽。', 'Insert one mana tablet, ring or other mana item. Choose charge or drain and set a target percentage. The item moves to the output when it reaches the target.'), ('拿着火花右键充能座即可安装，再给附近魔力池装上同色火花。也可以从管道或 ME 输出总线供魔。打开侧面配置中的“魔力”，可设置进出的方向。', 'Right-click the stand with a spark, then attach a matching spark to a nearby mana pool. Pipes and ME export buses can also supply mana. Use the Mana tab in side configuration to set input and output faces.')],
    'mana_infuser': [('$(item)魔力灌注室$(0)能够给材料注入魔力，用来制作魔力钢、魔力珍珠等物品。它需要电能和魔力。$(p)在辅料槽放入炼金催化器或炼造催化器，还能进行相应的炼金与复制。', 'The $(item)Mana Infusion Chamber$(0) uses electricity and mana to make manasteel, mana pearls and other infused materials.$(p)An Alchemy Catalyst or Conjuration Catalyst in the extra slot enables its corresponding recipes.')],
    'runic_forge': [('$(item)符文锻造室$(0)能自动制作符文，需要电能和魔力。材料放在左侧，活石等辅料另放。$(p)用来合成高级符文的基础符文会留在材料槽中，供下次使用。', 'The $(item)Runic Forge$(0) automates rune crafting using electricity and mana. Put ingredients on the left and livingrock or another reagent in its own slot.$(p)Runes used as catalysts stay in the ingredient slots for the next craft.')],
    'pure_converter': [('投入原木、石头等材料，通电后制作活木、活石等产物。水和需要特殊环境的转化请使用白雏菊。', 'Supply power and insert logs, stone or other supported materials to make livingwood, livingrock and similar products. Use a Pure Daisy for water and conversions that need special surroundings.')],
    'terra_condenser': [('搭一个 3×3 平台：中心和四角放活石，其余位置放青金石块。机器放在中心上方，加入材料并供魔，即可凝聚泰拉钢。', 'Build a 3x3 platform with livingrock at the center and corners and lapis blocks at the remaining positions. Place the machine above the center, add ingredients and supply mana to make terrasteel.')],
    'botanical_brewery': [('$(item)植物酿造室$(0)能够自动酿制精酿，需要电能和魔力。将材料放在左侧，魔法玻璃小瓶或精灵玻璃烧瓶放在辅料槽。$(p)小瓶的种类决定可盛放的药量。', 'The $(item)Botanical Brewery$(0) brews with electricity and mana. Put ingredients on the left and a Managlass Vial or Alfglass Flask in the extra slot.$(p)The vessel determines how many doses it holds.')],
    'ore_processor': [('$(item)凝矿处理室$(0)会用魔力将石头变成矿石，需要电能。辅料槽放入凝矿兰后即可工作。$(p)使用炎矿兰时，请在下界等有基岩顶层的维度中加工地狱岩。生成的矿物不能指定。', 'The $(item)Ore Processing Chamber$(0) uses mana and electricity to turn stone into ore. Place an Orechid in its extra slot.$(p)With an Orechid Ignem, process netherrack in a ceiling dimension such as the Nether. The resulting ore is random.')],
    'metamorphic_stone': [('$(item)异构石转化室$(0)能够将石头变成异构石，需要电能和魔力。$(p)不同生物群系会更容易生成不同种类的异构石，无法直接指定成品。', 'The $(item)Metamorphic Chamber$(0) turns stone into metamorphic stone using electricity and mana.$(p)The biome affects which varieties are more common; the result cannot be selected directly.')],
    'elven_trade_controller': [('$(item)精灵贸易控制器$(0)会将材料送进精灵传送门，并收取精灵送回的物品。把控制器贴在传送门核心旁，选择核心所在方向，再接上电源。', 'The $(item)Elven Trade Controller$(0) sends ingredients through an Alfheim Portal and collects what the elves return. Place it beside the portal core, select that side and supply electricity.'), ('传送门仍需要完整的门框、自然水晶和魔力池。用森林法杖开启传送门后，控制器才能进行贸易。$(p)词典升级请直接把词典投入传送门。', 'The portal still needs its frame, Natura Pylons and mana pools. Open it with the Wand before trading.$(p)To upgrade a Lexica, throw it directly into the portal.')],
    'mana_enchanter_controller': [('$(item)魔力附魔控制器$(0)能给装备附魔。把它贴在搭好的魔力附魔台旁，选好方向，再放入装备和附魔书。$(p)需要电能和魔力，附魔书不会消耗。', 'The $(item)Mana Enchanter Controller$(0) enchants equipment. Place it beside a completed Mana Enchanter, select that side and supply equipment and enchanted books.$(p)It needs electricity and mana. The books are reusable.'), ('可以给附魔台装火花，也可以从控制器供魔。若在附魔途中拆掉控制器，装备会留在附魔台上，可从那里取回。', 'Supply the enchanter with a spark or through the controller. If the controller is removed during enchanting, the equipment remains on the enchanter and can be retrieved there.')],
    'resonance_spark_augment': [('火花可以装在充能座、灌注室等储魔机器上。手持火花右击机器，再为附近的魔力池装上同色火花即可供魔。若不进魔力，打开侧面配置，将“魔力”的顶部设为输入。', 'Right-click a mana-storing machine, such as a Charger or Infuser, with a spark. A matching spark on a nearby pool supplies it. If mana is not arriving, open side configuration and set the top Mana face to input.'), ('给供魔的火花和接收的火花各装一个共鸣增幅器，传送距离可提高到 32 格。用染料分组；潜行使用森林法杖可拆下升级。', 'Fit a Resonance Augment to the supplying spark and the receiving spark to extend their range to 32 blocks. Use dyes to group sparks. Sneak-use the Wand to remove an augment.')],
    'corporea_orchid': [('把花接到有电的 ME 电缆上，需要 1 个通道，每 tick 消耗 4 AE。花上装普通多媒体火花，箱子上也装火花，再另放一个主火花。', 'Connect the flower to a powered ME cable. It uses one channel and 4 AE per tick. Attach ordinary Corporea sparks to the flower and chests, then add a separate master spark.'), ('ME 终端可以存取这些箱子里的物品，多媒体漏斗也能从 ME 取货。空手右键花，可查看连接状态并选择允许的方向。', 'The ME terminal can access these chests, and Corporea funnels can request items from ME. Right-click the flower with an empty hand to check its connection and choose which direction to allow.'), ('一组多媒体火花只放一朵织网花。双箱只装一枚火花；已经这样接入的箱子，不要再接同一网络的 ME 存储总线。', 'Use one Orchid per Corporea network and one spark per double chest. Do not also connect those chests to the same ME network through a storage bus.'), ('织网花每 tick 最多转移 2,048 件物品。断电、缺少通道或没有主火花时会停止，在界面中可查看原因。魔力池接入 ME 请使用输入、输出总线和魔力存储盘。', 'The Orchid moves up to 2,048 items per tick. It stops without power, a channel or a master spark; the screen shows the reason. For mana pools, use ME import/export buses and a Mana Storage Cell.')],
    'resonance_flower': [('$(item)共鸣花$(0)能够连接附近的共鸣芽，组成魔力网络。空手右击可以设置网络名称和成员。$(p)也可以在合成台中将它回收为共鸣增幅器。', 'The $(item)Resonance Flower$(0) links nearby Resonance Buds into a mana network. Right-click with an empty hand to set its name and members.$(p)It can also be recycled into a Resonance Augment in a crafting grid.')],
    'resonance_bud': [('$(item)共鸣芽$(0)会通过共鸣花输送魔力。将它贴着魔力池放下，右击选择网络和传输方向。$(p)设为中继时，可以延伸网络的距离。', 'The $(item)Resonance Bud$(0) transfers mana through a Resonance Flower. Place it beside a pool, then right-click to select a network and direction.$(p)Relay mode extends the network range.')],
    'mana_storage_cell': [('魔力盘有 1k、4k、16k、64k 和 256k 五档。放入 ME 驱动器或 ME 箱子后，即可在终端中存取魔力。1k 可存 8,192,000 魔力，每升一档容量变为四倍。', 'Mana cells come in 1k, 4k, 16k, 64k and 256k sizes. Put one in an ME Drive or Chest to store mana. A 1k cell holds 8,192,000 mana; each larger tier holds four times as much.'), ('将 ME 输入总线贴在魔力池上，可以把魔力存进盘里。机器上接 ME 输出总线，拿着魔力盘右击筛选格，就能选择魔力。机器相应一面要设为魔力输入。', 'Place an ME Import Bus against a mana pool to fill the cell. Attach an Export Bus to a machine and right-click its filter with a Mana Cell to select mana. Set that machine face to Mana input.'), ('存储总线可让终端直接使用池中或机器中的魔力。拿着魔力盘，在终端魔力图标上点击可装入或归还魔力；Shift 点击可多取一些。', 'A Storage Bus lets the terminal use mana directly from a pool or machine. Use a held Mana Cell on the terminal mana entry to transfer mana; Shift-click transfers more.')],
    'mana_packet': [('拆除存有魔力的 ME 接口等装置时，散出的魔力会聚成魔力团。对魔力池或机器使用，可以把魔力放回去；也可以交回 ME 终端。', 'Breaking an ME Interface or similar block that holds mana releases a Mana Wisp. Use it on a mana pool or machine to return the mana, or deposit it through an ME terminal.')],
}

HELP['corporea_orchid'].extend([
    ('在设置中点击快捷栏物品，再点击样品格，就能指定允许通过的物品。右击样品格可清除。$(p)同一种物品若有不同名字或附魔，可以选择是否区分。', 'Click a hotbar item and then a sample slot to choose which items may pass. Right-click a sample to clear it.$(p)You can choose whether names and enchantments should count as different items.'),
    ('开启自动合成后，漏斗取货时若库存不足，织网花会请 ME 制作缺少的物品。网络中需要相应的样板、材料和合成 CPU。$(p)最多同时保留 4 项任务，每项最多制作 4,096 件。', 'With autocrafting enabled, the Orchid asks ME to make items missing from a funnel request. The network needs a matching pattern, ingredients and a crafting CPU.$(p)It keeps up to four jobs, with at most 4,096 items per job.'),
    ('做好以后，再让漏斗取一次货即可。也可以用多媒体拦截器和多媒体固定器记录缺少的数量，收到红石信号后再次取货。', 'Once crafting finishes, request the items again. A Corporea Interceptor and Retainer can remember the missing amount and repeat the request when given a redstone signal.'),
])

for name in ['mana_charger', 'mana_infuser', 'runic_forge', 'terra_condenser', 'botanical_brewery', 'ore_processor', 'metamorphic_stone', 'mana_enchanter_controller']:
    HELP[name].append(('魔力池紧贴机器时，把相应一面设为魔力输入，即可直接取魔力。多个面合计每秒最多 20,000 魔力，满了会自动停下。', 'A pool touching a Mana input face can supply this machine directly. All faces share a limit of 20,000 mana per second. Transfer stops when full.'))
HELP['mana_storage_cell'].append(('五档待机消耗依次为 0.5、1、1.5、2 和 2.5 AE/t。制作时使用对应档位的 ME 存储组件。$(p)旧魔力盘会作为 1k 盘继续使用，盘内已有魔力不变。', 'Idle power is 0.5, 1, 1.5, 2 and 2.5 AE/t, respectively. Craft each tier with its matching ME storage component.$(p)Existing mana cells become 1k cells and keep their contents.'))
HELP['mana_bridge'].append(('永恒魔力池也能供魔，池内魔力不会减少。机器仍按每秒最多 20,000 魔力输送。', 'An Everlasting Mana Pool supplies mana without running out. The machine still transfers up to 20,000 mana per second.'))
for name in ['mana_charger', 'elven_trade_controller', 'mana_enchanter_controller']:
    HELP[name].append(('需要选择相邻目标时，打开左侧的“连接设置”，点击目标所在的一面。关闭窗口后仍按所选方向工作。', 'To choose an adjacent target, open Connection Settings on the left and click its side. The selection remains active after closing the window.'))

HELP['mana_storage_cell'].extend([
    ('样板供应器能把网络中的魔力和材料一起送出。接收面的魔力设为输入；需要送入辅料时，物品设为输入/输出，也能从这里送回成品。', 'A Pattern Provider can send mana and ingredients together. Set the receiving Mana face to Input. Use Item Input/Output to receive reagents and return products through that face.'),
    ('安装 AE2 JEI Integration 后，在样板终端使用 JEI 加号，就会带入灌注、符文、泰拉和酿造所需的魔力。也可以搜索“魔力”，拖入样板再修改数量。', 'With AE2 JEI Integration installed, the JEI plus button includes mana for infusion, rune, terra and brewing patterns. You can also search for Mana and drag it into a pattern before adjusting the amount.'),
    ('酿造会按页面显示的容器填写魔力和成品。用来催化的符文、炼金催化器等先放进机器，不必写进样板。', 'Brewing uses the displayed vessel for its mana cost and output. Preload reusable runes and catalysts in the machine; they do not need to be included in the pattern.'),
])


SHORT_TITLES = {
    'mana_lotus': 'Conduction Lotus', 'bionic_amaranthus': 'Jaded Amaranthus', 'bionic_clayconia': 'Clayconia',
    'bionic_agricarnation': 'Agricarnation', 'bionic_hopperhock': 'Hopperhock', 'bionic_rannuncarpus': 'Rannuncarpus',
    'bionic_exoflame': 'Exoflame', 'mechanical_apothecary': 'Apothecary', 'mana_bridge': 'Mana Bridge', 'mana_charger': 'Mana Charger',
    'mana_infuser': 'Mana Infusion', 'runic_forge': 'Runic Forge', 'pure_converter': 'Pure Conversion', 'terra_condenser': 'Terra Condenser',
    'botanical_brewery': 'Botanical Brewery', 'ore_processor': 'Ore Processing', 'metamorphic_stone': 'Metamorphic Stone',
    'elven_trade_controller': 'Elven Trade', 'mana_enchanter_controller': 'Mana Enchanter', 'resonance_spark_augment': 'Spark Resonance',
    'mana_storage_cell': 'Mana Storage Cell', 'mana_packet': 'Mana Wisp', 'corporea_orchid': 'Corporea Orchid', 'resonance_flower': 'Resonance Flower', 'resonance_bud': 'Resonance Bud',
}


def generate(root, write, zh, en, plants, recipes):
    zh['book.botanicalmekanism.category'], en['book.botanicalmekanism.category'] = '植物机械', 'Botanical Mechanisms'
    zh['book.botanicalmekanism.intro'], en['book.botanicalmekanism.intro'] = '用电能驱动的仿生花，以及协助调合、灌注和输送魔力的机器。$(p)仿生花可放在方块或电缆上。手持词典右击设备，可以查看它的用法。', 'Electrically powered bionic flowers and machines for crafting, infusion and mana transfer.$(p)Bionic flowers can stand on blocks or cables. Use this Lexica on a device to read about it.'
    write(f'{BASE}/categories/botanicalmekanism.json', {'name': 'book.botanicalmekanism.category', 'description': 'book.botanicalmekanism.intro',
          'icon': f'{MOD}:mana_lotus', 'sortnum': 11})
    mechanical = {row[0]: row for row in recipes if row[0] not in ('resonance_flower', 'resonance_bud')}
    for index, (name, descriptions) in enumerate(HELP.items()):
        item = f'{MOD}:{name}'; key = f'book.{MOD}.{name}'
        title = f'item.{MOD}.{name}' if name in ('resonance_spark_augment', 'mana_storage_cell', 'mana_packet') else f'block.{MOD}.{name}'
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
            zh[f'{key}.cost'] = f'在机械花药台中制作。需要一桶水和 {ticks*power:,} FE，用时 {ticks / 20:g} 秒。'
            en[f'{key}.cost'] = f'Make this in a Mechanical Apothecary with one bucket of water and {ticks*power:,} FE. It takes {ticks / 20:g} seconds.'
            pages.append({'type': 'patchouli:text', 'text': f'{key}.cost'})
        elif name == 'mana_storage_cell':
            from mana_cell_models import TIERS, cell_id
            for tier in TIERS:
                pages.append({'type': 'patchouli:crafting', 'recipe': f'{MOD}:{cell_id(tier)}', 'flag': 'mod:ae2'})
        elif name in MACHINES or name in ('mechanical_apothecary', 'resonance_spark_augment'):
            pages.append({'type': 'patchouli:crafting', 'recipe': item})
        write(f'{BASE}/entries/botanicalmekanism/{name}.json', {'name': title, 'icon': item, 'category': 'botania:botanicalmekanism',
              'sortnum': index, 'pages': pages, 'extra_recipe_mappings': {f'{MOD}:{cell_id(t)}': 0 for t in TIERS} if name == 'mana_storage_cell' else {item: 0}})
