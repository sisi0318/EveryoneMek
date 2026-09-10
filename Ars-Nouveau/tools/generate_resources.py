"""Generate block resources, translations, crafting recipes and the server test structure."""
import gzip
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ID = "arsmekanism"
MACHINES = {
    "source_generator": ("通用魔源发生器", "Universal Source Generator", "arsmekanism:fe_sourcelink",
        "消耗 FE 生成魔源。",
        "Generates Source using FE."),
    "source_converter": ("通用魔源转换器", "Universal Source Converter", "ars_nouveau:source_jar",
        "消耗 FE，在加压管道与相邻魔源设备之间转移魔源。",
        "Uses FE to transfer Source between pressurized tubes and an adjacent Source device."),
    "imbuement_chamber": ("通用魔源灌注室", "Universal Source Imbuement Chamber", "ars_nouveau:imbuement_chamber",
        "消耗原料、FE 和魔源进行灌注。",
        "Imbues materials using FE and Source."),
    "enchanting_apparatus": ("通用魔源附魔装置", "Universal Source Enchanting Apparatus", "ars_nouveau:enchanting_apparatus",
        "消耗材料、FE 和所需魔源，合成物品或附魔。",
        "Crafts or enchants items using materials, FE and the required Source."),
    "source_extractor": ("通用魔源萃取机", "Universal Source Extractor", "ars_nouveau:mycelial_sourcelink",
        "消耗食物或燃料与 FE 提取魔源，可利用燃料余热加工材料。",
        "Uses food or fuel and FE to extract Source. Fuel heat can process materials."),
    "magic_crusher": ("通用魔法粉碎机", "Universal Magic Crusher", "ars_nouveau:glyph_crush",
        "消耗原料与 FE，进行魔法粉碎。", "Crushes materials using FE."),
    "glyph_scribe": ("通用魔符抄写机", "Universal Glyph Scribe", "ars_nouveau:scribes_table",
        "消耗材料、经验点与 FE，使用相应等级的法术书制作魔符。",
        "Uses materials, experience points, FE and a suitable spellbook to make glyphs."),
    "potion_mixer": ("通用药水混合器", "Universal Potion Mixer", "ars_nouveau:potion_melder",
        "消耗 FE 与魔源，混合左右药水罐中的药液并输出至上方药水罐。",
        "Uses FE and Source to mix potions from jars on either side into the jar above."),
    "potion_bottler": ("通用药水灌装机", "Universal Potion Bottler", "ars_nouveau:potion_jar",
        "消耗 FE，从前方药水罐灌装药瓶、烧瓶或药水箭，也可回收药液。",
        "Uses FE and the jar in front to fill bottles, flasks or arrows, or recover potions."),
    "drygmy_station": ("通用德格米收获站", "Universal Drygmy Station", "ars_nouveau:drygmy_charm",
        "放入装有生物的收容罐，消耗 FE 与魔源生产掉落物。速度升级可加快生产。",
        "Uses FE and Source to harvest drops from creatures in captive jars. Speed upgrades increase production speed."),
    "whirlisprig_station": ("通用风转草培育站", "Universal Whirlisprig Station", "ars_nouveau:whirlisprig_charm",
        "消耗 FE，收集前方风转草之花的产物。可通过上方魔源罐供魔。",
        "Uses FE to collect items from a Whirlisprig flower in front. Can supply Source through a jar above."),
    "ritual_controller": ("通用仪式控制器", "Universal Ritual Controller", "ars_nouveau:ritual_brazier",
        "消耗 FE，为前方仪式火盆装填石板、添加增幅并启动仪式。可通过上方魔源罐供魔。",
        "Uses FE to load tablets and augments into the brazier in front and start rituals. Can supply Source through a jar above."),
}


def write(path, data):
    destination = RES / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


zh = {f"itemGroup.{ID}": "魔源机械", f"chemical.{ID}.source": "魔源"}
en = {f"itemGroup.{ID}": "Ars Mekanism", f"chemical.{ID}.source": "Source"}
for name, (cn, english, core, description_cn, description_en) in MACHINES.items():
    for prefix in ("block", "container"):
        zh[f"{prefix}.{ID}.{name}"] = cn
        en[f"{prefix}.{ID}.{name}"] = english
    zh[f"description.{ID}.{name}"] = description_cn
    en[f"description.{ID}.{name}"] = description_en
    texture = f"{ID}:block/{name}"
    write(f"assets/{ID}/models/block/{name}.json", {
        "parent": "minecraft:block/cube",
        "textures": {"particle": f"{texture}/side", "north": f"{texture}/front", "up": f"{texture}/top",
                     **{face: f"{texture}/side" for face in ("south", "east", "west", "down")}},
    })
    write(f"assets/{ID}/models/block/{name}_active.json", {
        "parent": f"{ID}:block/{name}", "textures": {"north": f"{texture}/front_active"},
    })
    write(f"assets/{ID}/models/item/{name}.json", {"parent": f"{ID}:block/{name}"})
    write(f"assets/{ID}/blockstates/{name}.json", {"variants": {
        f"facing={face},active={str(active).lower()}": {
            "model": f"{ID}:block/{name}{'_active' if active else ''}", "y": rotation,
        }
        for face, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270))
        for active in (False, True)
    }})
    components = [f"mekanism:{key}" for key in (
        "ejector", "owner", "redstone_control", "security", "side_config", "upgrades", "energy", "items", "chemicals",
    )] + [f"{ID}:settings"]
    write(f"data/{ID}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": f"{ID}:{name}", "functions": [
                {"function": "minecraft:copy_name", "source": "block_entity"},
                {"function": "minecraft:copy_components", "source": "block_entity", "include": components},
            ],
        }]}],
    })
    alloy, circuit = ("mekanism:alloy_atomic", "mekanism:ultimate_control_circuit") if name in (
        "drygmy_station", "whirlisprig_station",
    ) else ("mekanism:alloy_infused", "mekanism:advanced_control_circuit")
    write(f"data/{ID}/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ASA", "CKC", "AGA"],
        "key": {"A": {"item": alloy}, "S": {"item": "mekanism:steel_casing"},
                "C": {"item": circuit}, "K": {"item": core},
                "G": {"item": "ars_nouveau:source_gem_block"}},
        "result": {"id": f"{ID}:{name}", "count": 1},
    })

# Reference the installed Ars model and texture; do not redistribute its art assets.
write(f"assets/{ID}/models/block/fe_sourcelink.json", {
    "parent": "ars_nouveau:block/agronomic_sourcelink", "render_type": "minecraft:cutout",
})
write(f"assets/{ID}/models/item/fe_sourcelink.json", {"parent": f"{ID}:block/fe_sourcelink"})
write(f"assets/{ID}/blockstates/fe_sourcelink.json", {"variants": {"": {"model": f"{ID}:block/fe_sourcelink"}}})
write(f"data/{ID}/loot_table/blocks/fe_sourcelink.json", {
    "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": f"{ID}:fe_sourcelink", "functions": [{
            "function": "minecraft:copy_components", "source": "block_entity", "include": [f"{ID}:fe_energy"],
        }],
    }]}],
})
write(f"data/{ID}/recipe/fe_sourcelink.json", {
    "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ACA", " S ", "AGA"],
    "key": {"A": {"item": "mekanism:alloy_infused"}, "C": {"item": "mekanism:basic_control_circuit"},
            "S": {"item": "ars_nouveau:agronomic_sourcelink"}, "G": {"item": "ars_nouveau:source_gem"}},
    "result": {"id": f"{ID}:fe_sourcelink", "count": 1},
})
for key, cn, english in [
    (f"block.{ID}.fe_sourcelink", "FE 魔源通道", "FE Sourcelink"),
    (f"description.{ID}.fe_sourcelink", "消耗 FE，为正上方或正下方的魔源罐生成魔源。",
        "Uses FE to generate Source in a jar directly above or below."),
    (f"gui.{ID}.fe_energy", "%s / %s FE", "%s / %s FE"),
]:
    zh[key] = cn
    en[key] = english
for index, (cn, english) in enumerate([
    ("上下未连接魔源罐", "No jar above or below"), ("魔源罐已满", "Source jar full"),
    ("电力不足", "Not enough energy"), ("运行中", "Running"),
]):
    zh[f"gui.{ID}.fe_sourcelink_status.{index}"] = cn
    en[f"gui.{ID}.fe_sourcelink_status.{index}"] = english

statuses = [
    ("运行中", "Running"), ("红石控制：暂停", "Paused by redstone"),
    ("输出空间不足", "Output full"), ("缺少原料", "Add an ingredient"),
    ("缺少材料", "Missing materials"),
    ("无匹配配方", "No matching recipe"),
    ("魔源不足", "Not enough Source"), ("电力不足", "Not enough energy"),
    ("未连接魔源容器", "No Source container"),
    ("容器无法转移魔源", "Source transfer blocked"),
    ("传输已停止", "Transfer stopped"), ("配方已失效，请重新选择", "Choose an available recipe"),
    ("经验点不足", "Not enough experience"), ("放入法术书", "Insert a spellbook"),
    ("未连接药水罐", "Potion jar missing"), ("药液无法混合或装填", "Incompatible potions"),
    ("前方未连接对应装置", "Required device missing in front"), ("缺少已绑定的生物", "Bound creature missing"),
    ("周围植物不足", "More plants needed nearby"), ("等待产物", "Waiting for output"),
    ("余热已满", "Heat storage full"), ("此仪式不适用当前模式", "Select a suitable ritual mode"),
    ("目标装置已停用", "Target device disabled"), ("余热不足", "Not enough heat"),
    ("药液不足", "Not enough potion"),
    ("请选择要制作的魔符", "Choose a glyph to craft"), ("法术书等级不足", "Spellbook tier too low"),
    ("增幅就绪后点击启动", "Press Start when augments are ready"), ("仪式启动条件未满足", "Ritual requirements not met"),
    ("罐中没有可收获的生物", "No harvestable creatures in jars"),
]
for index, (cn, english) in enumerate(statuses):
    zh[f"gui.{ID}.status.{index}"] = cn
    en[f"gui.{ID}.status.{index}"] = english
for key, cn, english in [
    ("generation_rate", "产率：%s 魔源/t", "Rate: %s Source/t"),
    ("transfer_rate", "转移上限：%s 魔源/t", "Transfer limit: %s Source/t"),
    ("converter_mode.0", "向魔源设备输送", "Send to Source device"),
    ("converter_mode.1", "从魔源设备抽取", "Extract from Source device"),
    ("converter_mode.2", "停止传输", "Stop transfer"),
    ("ars_side", "连接方向：%s", "Connection: %s"),
    ("apparatus_mode.0", "物品合成", "Crafting"),
    ("apparatus_mode.1", "装备附魔", "Enchanting"),
    ("invalid_recipe", "该配方已不可用，请重新选择", "This recipe is unavailable. Please choose another."),
    ("choose_recipe", "选择配方", "Choose recipe"),
    ("choose_glyph", "选择魔符", "Choose glyph"),
    ("selected_recipe", "已选择配方", "Selected recipe"),
    ("no_selection", "尚未选择魔符", "No glyph selected"),
    ("automatic", "自动匹配", "Automatic"),
    ("clear_selection", "取消选择", "Clear selection"),
    ("close", "关闭", "Close"),
    ("search_name", "搜索名称", "Search by name"),
    ("no_results", "没有找到匹配的配方", "No matching recipes"),
    ("choose_hint", "点击上方按钮选择", "Use the button above to choose"),
    ("choose_again", "请重新选择", "Please choose again"),
    ("tier_all", "全部", "All"),
    ("tier.1", "一级", "Tier I"), ("tier.2", "二级", "Tier II"), ("tier.3", "三级", "Tier III"),
    ("glyph_cost", "%s · %s 点经验", "%s · %s XP"),
    ("source_cost", "%s 点魔源", "%s Source"),
    ("power_only", "需要电力", "Requires power"),
    ("recipe_input", "原料：%s", "Input: %s"),
    ("required_materials", "所需材料：", "Required materials:"),
    ("material_count", "%s × %s", "%s × %s"),
    ("ingredient_options", "%s 等 %s 种任选", "%s (%s accepted options)"),
    ("missing_ingredient", "材料不可用", "Ingredient unavailable"),
    ("catalysts", "催化物", "Catalysts"),
    ("materials", "材料", "Materials"),
    ("output", "产物", "Output"),
    ("collected", "收集", "Collected"),
    ("filter", "筛选", "Filter"),
    ("spell_book", "法术书", "Book"),
    ("tablet", "石板", "Tablet"),
    ("not_connected", "未连接", "Not connected"),
    ("experience", "经验：%s 点", "Experience: %s points"),
    ("heat", "余热：%s / %s 点", "Heat: %s / %s points"),
    ("deposit_experience", "存入经验", "Deposit XP"),
    ("source_extractor_mode.0", "食物萃取", "Food extraction"),
    ("source_extractor_mode.1", "燃料萃取", "Fuel extraction"),
    ("source_extractor_mode.2", "余热加工", "Heat processing"),
    ("potion_bottler_mode.0", "灌装容器", "Fill containers"),
    ("potion_bottler_mode.1", "回收药液", "Recover potions"),
    ("filter_mode.0", "筛选：允许", "Filter: allow"),
    ("filter_mode.1", "筛选：排除", "Filter: exclude"),
    ("ritual_controller_mode.0", "自动启动", "Automatic start"),
    ("ritual_controller_mode.1", "骨块增幅", "Bone block augment"),
    ("ritual_controller_mode.2", "手动启动", "Manual start"),
    ("start_ritual", "启动", "Start"),
    ("augment", "增幅", "Augment"),
    ("potion_left", "左侧：%s", "Left: %s"),
    ("potion_right", "右侧：%s", "Right: %s"),
    ("potion_top", "上方：%s", "Above: %s"),
    ("potion_front", "药液：%s", "Potion: %s"),
    ("potion_bottles", "%s 瓶", "%s bottles"),
    ("drygmy_bonus", "产量奖励：%s", "Yield bonus: %s"),
    ("native_progress", "生物进度：%s / %s", "Creature progress: %s / %s"),
    ("grove_score", "环境评分：%s", "Grove score: %s"),
    ("grove_diversity", "植物多样性：%s", "Plant diversity: %s"),
    ("ritual_state.0", "等待石板", "Waiting for tablet"),
    ("ritual_state.1", "准备仪式", "Preparing ritual"),
    ("ritual_state.2", "仪式运行中", "Ritual running"),
    ("drygmy_station.status.16", "放入收容罐或在前方放石阵", "Insert captive jars or connect a henge"),
    ("whirlisprig_station.status.16", "前方放置风转草之花", "Place a Whirlisprig flower in front"),
    ("ritual_controller.status.16", "前方放置仪式火盆", "Place a ritual brazier in front"),
    ("drygmy_station.status.17", "将德格米绑定到石阵", "Bind a Drygmy to the henge"),
    ("whirlisprig_station.status.17", "将风转草绑定到花", "Bind a Whirlisprig to the flower"),
    ("drygmy_station.status.19", "等待德格米收获", "Waiting for Drygmy harvest"),
    ("whirlisprig_station.status.19", "等待风转草收获", "Waiting for Whirlisprig harvest"),
    ("ritual_controller.status.19", "仪式运行中", "Ritual running"),
    ("controller_missing_jar", "上方放置魔源罐以供魔", "Place a Source jar above to supply Source"),
    ("insert_tablet", "放入仪式石板", "Insert a ritual tablet"),
    ("mob_jars", "收容罐", "Captive jars"),
    ("jar_population", "生物 %s · 种类 %s", "%s creatures · %s types"),
    ("harvest_summary", "奖励 %s · %s 秒/轮", "Bonus %s · %s s/cycle"),
    ("pending_items", "待输出：%s", "Queued: %s"),
    ("external_henge", "外接石阵", "External henge"),
]:
    zh[f"gui.{ID}.{key}"] = cn
    en[f"gui.{ID}.{key}"] = english
write(f"assets/{ID}/lang/zh_cn.json", zh)
write(f"assets/{ID}/lang/en_us.json", en)
for tag in ("mineable/pickaxe", "needs_iron_tool"):
    write(f"data/minecraft/tags/block/{tag}.json", {"replace": False, "values": [f"{ID}:{name}" for name in (*MACHINES, "fe_sourcelink")]})


def string(value):
    encoded = value.encode()
    return struct.pack(">H", len(encoded)) + encoded


def named(tag_type, name, data):
    return bytes([tag_type]) + string(name) + data


def list_tag(name, element_type, entries):
    return named(9, name, bytes([element_type]) + struct.pack(">i", len(entries)) + b"".join(entries))


nbt = b"\x0a\x00\x00" + named(3, "DataVersion", struct.pack(">i", 3955))
nbt += list_tag("size", 3, [struct.pack(">i", value) for value in (12, 5, 12)])
nbt += list_tag("palette", 10, [named(8, "Name", string("minecraft:air")) + b"\x00"])
nbt += list_tag("blocks", 10, []) + list_tag("entities", 10, []) + b"\x00"
structure = ROOT / f"src/gameTest/resources/data/{ID}/structure/empty.nbt"
structure.parent.mkdir(parents=True, exist_ok=True)
structure.write_bytes(gzip.compress(nbt, mtime=0))

# Development recipe exercises component-preserving output, repeated ingredients, a retained
# Ars parchment and a returned bucket in one transaction. It is excluded from the release JAR.
recipe = ROOT / f"src/gameTest/resources/data/{ID}/recipe/component_contract.json"
recipe.parent.mkdir(parents=True, exist_ok=True)
recipe.write_text(json.dumps({
    "type": "ars_nouveau:enchanting_apparatus", "reagent": {"item": "minecraft:diamond_sword"},
    "result": {"id": "minecraft:diamond_sword", "count": 1}, "sourceCost": 250, "keepNbtOfReagent": True,
    "pedestalItems": [{"item": item} for item in (
        "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:water_bucket", "ars_nouveau:spell_parchment",
    )],
}, indent=2) + "\n", encoding="utf-8", newline="\n")
print(f"Generated resources for {len(MACHINES)} machines and the FE Sourcelink.")

test_recipes = {
    "expansion_crush": {"type": "ars_nouveau:crush", "input": {"item": "minecraft:raw_iron"}, "output": [
        {"stack": {"id": "minecraft:gold_nugget", "count": 1}, "chance": 1.0, "maxRange": 3},
        {"stack": {"id": "minecraft:iron_nugget", "count": 1}, "chance": 1.0, "maxRange": 1},
    ]},
    "expansion_glyph": {"type": "ars_nouveau:glyph", "output": {"id": "ars_nouveau:glyph_light", "count": 1},
        "inputs": [{"item": "minecraft:gold_ingot"}, {"item": "minecraft:gold_ingot"}, {"item": "minecraft:water_bucket"}], "exp": 21},
}
for name, value in test_recipes.items():
    path = ROOT / f"src/gameTest/resources/data/{ID}/recipe/{name}.json"
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")

test_loot = ROOT / f"src/gameTest/resources/data/{ID}/loot_table/drygmy_contract.json"
test_loot.parent.mkdir(parents=True, exist_ok=True)
test_loot.write_text(json.dumps({"type": "minecraft:entity", "pools": [{"rolls": 1,
    "entries": [{"type": "minecraft:item", "name": "minecraft:gold_nugget"}]}]}, indent=2) + "\n", encoding="utf-8", newline="\n")
