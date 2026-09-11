"""Generate runtime JSON and a separate empty server-test structure. Does not copy upstream assets."""
import gzip
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ID = "forbiddenmekanism"
MACHINES = {
    "forge_controller": ("赫菲斯托斯锻造室", "Hephaestus Forging Chamber", "forbidden_arcanus:mundabitur_dust",
        "消耗 FE 与辉光、灵魂、血液、经验加工材料。放在完整锻台平台的中心。",
        "Uses FE and Aureal, souls, blood and experience to process materials. Place at the center of a complete forge platform."),
    "clibano_controller": ("炽炉控制器", "Clibano Controller", "forbidden_arcanus:clibano_core",
        "替换炽炉核心，或嵌入侧面、背面中央。用洁净粉末右键激活完整炉壳。消耗 FE 加热和供料收料，特殊火焰需相应灵魂。",
        "Replace the Clibano Core, or a side or rear center. Activate the complete shell with Mundabitur Dust. Uses FE for heating and logistics; special flames require appropriate souls."),
}


def write(path, data):
    destination = RES / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


zh = {f"itemGroup.{ID}": "禁忌机械", f"item.{ID}.glow_module": "辉光柱插件"}
en = {f"itemGroup.{ID}": "Forbidden Mekanism", f"item.{ID}.glow_module": "Aureal Obelisk Module"}
zh[f"description.{ID}.glow_module"] = "装入锻造室升级槽，消耗 FE 产生辉光。基础每个每 5 秒产生 100 点，最多安装 8 个。"
en[f"description.{ID}.glow_module"] = "Install in the forging chamber to produce Aureal using FE. Each produces 100 points per 5 seconds before upgrades; up to 8 modules."
zh[f"description.{ID}.forge_tier_installer"] = "右键将 %s 级锻造室升级为 %s 级。"
en[f"description.{ID}.forge_tier_installer"] = "Use on a tier %s forging chamber to upgrade it to tier %s."
for name, (cn, english, core, description_cn, description_en) in MACHINES.items():
    for prefix in ("block", "container"):
        zh[f"{prefix}.{ID}.{name}"] = cn
        en[f"{prefix}.{ID}.{name}"] = english
    zh[f"description.{ID}.{name}"] = description_cn
    en[f"description.{ID}.{name}"] = description_en
    texture = f"{ID}:block/{name}"
    write(f"assets/{ID}/models/block/{name}.json", {"parent": "minecraft:block/cube", "textures": {
        "particle": f"{texture}/side", "north": f"{texture}/front", "up": f"{texture}/top",
        **{face: f"{texture}/side" for face in ("south", "east", "west", "down")}}})
    write(f"assets/{ID}/models/block/{name}_active.json", {"parent": f"{ID}:block/{name}", "textures": {"north": f"{texture}/front_active"}})
    write(f"assets/{ID}/models/item/{name}.json", {"parent": f"{ID}:block/{name}"})
    variants = {
        f"facing={face},active={str(active).lower()}": {"model": f"{ID}:block/{name}{'_active' if active else ''}", "y": rotation}
        for face, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for active in (False, True)}
    if name == "clibano_controller":
        # Reference the dependency models at runtime, including resource-pack replacements and native flame animations.
        shells = ("side", "front_off", "front_fire", "front_soul_fire", "front_enchanted_fire")
        parts = [{"when": {**dict(field.split("=") for field in key.split(",")), "shell": "standalone"}, "apply": model}
                 for key, model in variants.items()]
        for face, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for shell in shells:
                parts.append({"when": {"facing": face, "shell": shell},
                              "apply": {"model": f"forbidden_arcanus:block/clibano_center_{shell}", "y": rotation}})
            for active in (False, True):
                parts.append({"when": {"facing": face, "shell": "|".join(shells), "active": str(active).lower()},
                              "apply": {"model": f"{ID}:block/clibano_controller_indicator{'_active' if active else ''}", "y": rotation}})
        write(f"assets/{ID}/blockstates/{name}.json", {"multipart": parts})
    else:
        write(f"assets/{ID}/blockstates/{name}.json", {"variants": variants})
    components = [f"mekanism:{key}" for key in ("ejector", "owner", "redstone_control", "security", "side_config", "upgrades", "energy", "items")]
    write(f"data/{ID}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": f"{ID}:{name}", "functions": [
            {"function": "minecraft:copy_name", "source": "block_entity"},
            {"function": "minecraft:copy_components", "source": "block_entity", "include": components + [f"{ID}:settings"]}]}]}]})
    write(f"data/{ID}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ASA", "CKC", "AGA"],
        "key": {"A": {"item": "mekanism:alloy_atomic"}, "S": {"item": "mekanism:steel_casing"},
                "C": {"item": "mekanism:ultimate_control_circuit"}, "K": {"item": core},
                "G": {"item": "forbidden_arcanus:arcane_crystal_block"}}, "result": {"id": f"{ID}:{name}", "count": 1}})

write(f"assets/{ID}/models/item/glow_module.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ID}:item/glow_module"}})
zh[f"block.{ID}.clibano_port"] = "炽炉端口"
en[f"block.{ID}.clibano_port"] = "Clibano Port"
zh[f"description.{ID}.clibano_port"] = "替换炉体上、下、侧面或背面的中央一格，连接物品管道与 FE 电缆。使用控制器对应面的物流设置。"
en[f"description.{ID}.clibano_port"] = "Replace a top, bottom, side or rear center to connect item pipes and FE cables. Uses the controller's corresponding side settings."
port_texture = f"{ID}:block/clibano_port"
write(f"assets/{ID}/models/block/clibano_port.json", {"parent": "minecraft:block/cube", "textures": {
    "particle": f"{port_texture}/side", "north": f"{port_texture}/front", "up": f"{port_texture}/top",
    **{face: f"{port_texture}/side" for face in ("south", "east", "west", "down")}}})
write(f"assets/{ID}/models/block/clibano_port_connected.json", {"parent": f"{ID}:block/clibano_port", "textures": {"north": f"{port_texture}/front_active"}})
write(f"assets/{ID}/models/item/clibano_port.json", {"parent": f"{ID}:block/clibano_port"})
port_parts = []
for face, x, y in (("north", 0, 0), ("east", 0, 90), ("south", 0, 180), ("west", 0, 270), ("up", 270, 0), ("down", 90, 0)):
    port_parts.append({"when": {"facing": face, "formed": "true"}, "apply": {
        "model": f"forbidden_arcanus:block/clibano_center_{'top' if face == 'up' else 'side'}", "x": x, "y": y}})
    for connected in (False, True):
        port_parts.append({"when": {"facing": face, "formed": "false", "connected": str(connected).lower()},
                           "apply": {"model": f"{ID}:block/clibano_port{'_connected' if connected else ''}", "x": x, "y": y}})
        port_parts.append({"when": {"facing": face, "formed": "true", "connected": str(connected).lower()},
                           "apply": {"model": f"{ID}:block/clibano_port_indicator{'_active' if connected else ''}", "x": x, "y": y}})
write(f"assets/{ID}/blockstates/clibano_port.json", {"multipart": port_parts})
for part, start, end in (("controller", [7, 7, -0.002], [9, 8, 0]), ("port", [7, 7, -0.002], [8, 9, 0])):
    for active in (False, True):
        write(f"assets/{ID}/models/block/clibano_{part}_indicator{'_active' if active else ''}.json", {
            "parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": "forbidden_arcanus:block/clibano/clibano_center_side",
                         "indicator": f"{port_texture}/{'front_active' if active else 'front'}"},
            "elements": [{"from": start, "to": end, "shade": False, "faces": {
                "north": {"uv": [8, 11, 9, 12], "texture": "#indicator", "cullface": "north"}}}]})
write(f"data/{ID}/loot_table/blocks/clibano_port.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ID}:clibano_port"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
write(f"data/{ID}/recipe/clibano_port.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" B ", "ICI", " B "],
    "key": {"B": {"item": "forbidden_arcanus:polished_darkstone_bricks"}, "I": {"item": "mekanism:ingot_steel"},
            "C": {"item": "mekanism:basic_control_circuit"}}, "result": {"id": f"{ID}:clibano_port", "count": 2}})
write(f"data/{ID}/recipe/glow_module.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [{"item": f"forbidden_arcanus:{item}"} for item in
        ("arcane_crystal_block", "arcane_crystal_block", "arcane_polished_darkstone", "mundabitur_dust")],
    "result": {"id": f"{ID}:glow_module", "count": 1}})
compressed_blocks = {
    "soul_block": ("灵魂块", "Soul Block", "soul"),
    "xpetrified_block": ("石化经验块", "Petrified Experience Block", "xpetrified_orb"),
}
for block_id, (cn, english, material) in compressed_blocks.items():
    zh[f"block.{ID}.{block_id}"] = cn
    en[f"block.{ID}.{block_id}"] = english
    write(f"assets/{ID}/blockstates/{block_id}.json", {"variants": {"": {"model": f"{ID}:block/{block_id}"}}})
    write(f"assets/{ID}/models/block/{block_id}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{ID}:block/{block_id}"}})
    write(f"assets/{ID}/models/item/{block_id}.json", {"parent": f"{ID}:block/{block_id}"})
    write(f"data/{ID}/loot_table/blocks/{block_id}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ID}:{block_id}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(f"data/{ID}/recipe/{block_id}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["MMM", "MMM", "MMM"],
        "key": {"M": {"item": f"forbidden_arcanus:{material}"}}, "result": {"id": f"{ID}:{block_id}", "count": 1}})
    write(f"data/{ID}/recipe/{material}_from_block.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": f"{ID}:{block_id}"}], "result": {"id": f"forbidden_arcanus:{material}", "count": 9}})
resource_modules = {
    "soul_module": ("灵魂插件", "Soul Module", "灵魂", "souls", 1, {"item": f"{ID}:soul_block"}),
    "blood_module": ("血液插件", "Blood Module", "血液", "blood", 150, {
        "type": "neoforge:components", "items": "forbidden_arcanus:blood_test_tube", "strict": False,
        "components": {"forbidden_arcanus:essence_storage": {"data": {"type": "blood", "amount": 3000}, "limit": 3000}}}),
    "experience_module": ("经验插件", "Experience Module", "经验", "experience", 100, {"item": f"{ID}:xpetrified_block"}),
}
for module_id, (cn, english, resource_cn, resource_en, produced, core) in resource_modules.items():
    zh[f"item.{ID}.{module_id}"] = cn
    en[f"item.{ID}.{module_id}"] = english
    zh[f"description.{ID}.{module_id}"] = f"装入锻造室升级槽，消耗 FE 产生{resource_cn}。基础每个每 5 秒产生 {produced} 点，最多安装 8 个。"
    en[f"description.{ID}.{module_id}"] = f"Install in the forging chamber to produce {resource_en} using FE. Each produces {produced} points per 5 seconds before upgrades; up to 8 modules."
    write(f"assets/{ID}/models/item/{module_id}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ID}:item/{module_id}"}})
    write(f"data/{ID}/recipe/{module_id}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [core, core, {"item": "forbidden_arcanus:arcane_polished_darkstone"}, {"item": "forbidden_arcanus:mundabitur_dust"}],
        "result": {"id": f"{ID}:{module_id}", "count": 1}})
for tier in range(2, 6):
    item_id = f"forge_tier_{tier}_installer"
    zh[f"item.{ID}.{item_id}"] = f"{tier} 级锻造室升级插件"
    en[f"item.{ID}.{item_id}"] = f"Tier {tier} Forging Chamber Installer"
    write(f"assets/{ID}/models/item/{item_id}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ID}:item/{item_id}"}})
    write(f"data/{ID}/recipe/{item_id}.json", {"type": f"{ID}:forge_upgrade_crafting", "ritual": f"forbidden_arcanus:upgrade_tier_{tier}"})
for tag in ("mineable/pickaxe", "needs_stone_tool"):
    write(f"data/minecraft/tags/block/{tag}.json", {"replace": False, "values": [f"{ID}:{name}" for name in [*MACHINES, *compressed_blocks, "clibano_port"]]})
write("pack.mcmeta", {"pack": {"pack_format": 34, "description": "Forbidden Mekanism resources"}})

labels = {
    "enhancer": ("增强器", "Enhancer"), "soul_burning": ("灵魂燃烧", "Soul duration"),
    "processing": ("加工进度", "Processing"), "processing_item": ("%s · %s%%", "%s · %s%%"),
    "soul_time": ("灵魂剩余 %s / %s 秒", "Soul time: %s / %s s"),
    "clibano_summary": ("%s · %s · 残渣 %s/64", "%s · %s · Residues %s/64"),
    "install_wall": ("控制器可替换核心或水平侧面中央；端口可放在上、下、侧面与背面中央。检查权限和已有控制器。", "Controllers replace the core or horizontal wall centers; ports fit top, bottom, side and rear centers. Check access and existing controllers."),
    "stock": ("原料", "Materials"), "output": ("输出", "Output"), "supplies": ("补给", "Supplies"),
    "enhancers": ("增强器", "Enhancers"), "resources": ("资源", "Resources"),
    "pause": ("暂停", "Pause"), "resume": ("继续", "Resume"),
    "recipes": ("选择配方", "Recipes"), "xp": ("经验", "XP"),
    "production_rate": ("+%s/秒", "+%s/s"),
    "resource_storage": ("%s %s / %s", "%s %s / %s"),
    "installer_requires": ("需要 %s 级锻造室", "Requires a tier %s forging chamber"),
    "installer_done": ("锻造室已升至 %s 级", "Forging chamber upgraded to tier %s"),
    "platform_missing": ("锻台平台不完整", "Forge platform is incomplete"),
    "unmeasured": ("炉体尚未成型", "Furnace structure is incomplete"),
    "tier_progress": ("等级 %s  ·  进度 %s / %s", "Tier %s  ·  Progress %s / %s"),
    "resource.0": ("辉光", "Aureal"), "resource.1": ("灵魂", "Souls"), "resource.2": ("血液", "Blood"), "resource.3": ("经验", "XP"),
    "heating": ("电热运行", "Heating"), "heat_idle": ("电热待机", "Heat idle"),
    "fire.0": ("普通火", "Fire"), "fire.1": ("灵魂火", "Soul Fire"), "fire.2": ("附魔火", "Enchanted Fire"),
    "automatic": ("自动匹配", "Automatic"), "all_recipes": ("全部配方", "All recipes"), "applicable": ("当前适用", "Applicable now"),
    "search": ("搜索产物名称…", "Search product name…"), "close": ("关闭", "Close"), "no_results": ("没有匹配配方", "No matching recipes"),
    "batch_production": ("连续生产", "Continuous production"), "material": ("%s × %s", "%s × %s"), "missing_ingredient": ("材料不可用", "Unavailable ingredient"),
}
statuses = [
    ("等待下一批", "Waiting for next batch"), ("加工中", "Processing"), ("请嵌入并组建炽炉", "Embed in and form a Clibano"),
    ("检查炽炉结构", "Check Clibano structure"), ("炉体已有控制器", "This furnace already has a controller"),
    ("缺少电力", "Not enough energy"), ("已暂停", "Paused"), ("缺少配方材料", "Missing recipe materials"),
    ("锻造室等级不满足配方", "Forge tier does not match the recipe"), ("加工条件不满足", "Processing conditions unmet"),
    ("输出空间不足", "Output space is full"), ("缺少所需增强器", "Missing required enhancers"),
    ("辉光不足", "Not enough Aureal"), ("灵魂不足", "Not enough souls"), ("血液不足", "Not enough blood"),
    ("缺少炽炉燃料", "Missing Clibano fuel"), ("需要对应的灵魂火焰", "Requires the appropriate soul flame"),
    ("经验不足", "Not enough experience"), ("炽炉匹配了其他配方", "Clibano selected another recipe"),
]
labels.update({f"status.{i}": pair for i, pair in enumerate(statuses)})
for key, (cn, english) in labels.items():
    zh[f"gui.{ID}.{key}"] = cn
    en[f"gui.{ID}.{key}"] = english
write(f"assets/{ID}/lang/zh_cn.json", zh)
write(f"assets/{ID}/lang/en_us.json", en)

# Minimal NBT structure is test-only, with space for real native multiblocks.
def name(value):
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def int_tag(key, value):
    return b"\x03" + name(key) + struct.pack(">i", value)


def list_tag(key, tag_type, values):
    return b"\x09" + name(key) + bytes([tag_type]) + struct.pack(">i", len(values)) + b"".join(values)


template = b"\x0a\x00\x00" + int_tag("DataVersion", 3955)
template += list_tag("size", 3, [struct.pack(">i", n) for n in (32, 8, 32)])
template += list_tag("entities", 10, [])
template += list_tag("palette", 10, [b"\x08" + name("Name") + name("minecraft:air") + b"\x00"])
template += list_tag("blocks", 10, []) + b"\x00"
destination = ROOT / f"src/gameTest/resources/data/{ID}/structure/empty.nbt"
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_bytes(gzip.compress(template, mtime=0))
print(f"Generated resources for {len(MACHINES)} machines, a Clibano port, four resource modules, two compressed blocks and four native-material tier installers.")
