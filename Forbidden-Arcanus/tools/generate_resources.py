"""Generate runtime JSON and a separate empty server-test structure. Does not copy upstream assets."""
import gzip
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ID = "forbiddenmekanism"
MACHINES = {
    "forge_controller": ("赫菲斯托斯锻台控制器", "Hephaestus Forge Controller", "forbidden_arcanus:mundabitur_dust",
        "消耗 FE 自动备料、使用锤子并收取成品。需要完整的赫菲斯托斯锻台及其资源。",
        "Uses FE to load materials, operate a hammer and collect products. Requires a complete Hephaestus Forge and its resources."),
    "clibano_controller": ("炽炉控制器", "Clibano Controller", "forbidden_arcanus:clibano_core",
        "消耗 FE，为完整的炽炉供料并收取成品。炽炉仍需燃料和相应灵魂。",
        "Uses FE to supply and collect from a complete Clibano. The furnace still needs fuel and appropriate souls."),
}


def write(path, data):
    destination = RES / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


zh = {f"itemGroup.{ID}": "禁忌机械", f"item.{ID}.infinite_hammer_module": "无限锤子模块"}
en = {f"itemGroup.{ID}": "Forbidden Mekanism", f"item.{ID}.infinite_hammer_module": "Infinite Hammer Module"}
zh[f"description.{ID}.infinite_hammer_module"] = "装入锻台控制器的升级槽，自动启动仪式，无需补充锤子。"
en[f"description.{ID}.infinite_hammer_module"] = "Install in the forge controller's upgrade slot to start rituals without replacing hammers."
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
    write(f"assets/{ID}/blockstates/{name}.json", {"variants": {
        f"facing={face},active={str(active).lower()}": {"model": f"{ID}:block/{name}{'_active' if active else ''}", "y": rotation}
        for face, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for active in (False, True)}})
    components = [f"mekanism:{key}" for key in ("ejector", "owner", "redstone_control", "security", "side_config", "upgrades", "energy", "items")]
    write(f"data/{ID}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": f"{ID}:{name}", "functions": [
            {"function": "minecraft:copy_name", "source": "block_entity"},
            {"function": "minecraft:copy_components", "source": "block_entity", "include": components + [f"{ID}:settings"]}]}]}]})
    write(f"data/{ID}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ASA", "CKC", "AGA"],
        "key": {"A": {"item": "mekanism:alloy_atomic"}, "S": {"item": "mekanism:steel_casing"},
                "C": {"item": "mekanism:ultimate_control_circuit"}, "K": {"item": core},
                "G": {"item": "forbidden_arcanus:arcane_crystal_block"}}, "result": {"id": f"{ID}:{name}", "count": 1}})

write(f"assets/{ID}/models/item/infinite_hammer_module.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{ID}:item/infinite_hammer_module"}})
write(f"data/{ID}/recipe/infinite_hammer_module.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ACA", "GHG", "ACA"],
    "key": {"A": {"item": "mekanism:alloy_atomic"}, "C": {"item": "mekanism:ultimate_control_circuit"},
            "G": {"item": "minecraft:gold_ingot"}, "H": {"item": "forbidden_arcanus:diamond_blacksmith_gavel"}},
    "result": {"id": f"{ID}:infinite_hammer_module", "count": 1}})
for tag in ("mineable/pickaxe", "needs_stone_tool"):
    write(f"data/minecraft/tags/block/{tag}.json", {"replace": False, "values": [f"{ID}:{name}" for name in MACHINES]})
write("pack.mcmeta", {"pack": {"pack_format": 34, "description": "Forbidden Mekanism resources"}})

labels = {
    "stock": ("备料", "Stock"), "output": ("输出", "Output"), "supplies": ("补给", "Supplies"),
    "enhancers": ("增强器", "Enhancers"), "resources": ("资源", "Resources"), "fuel_soul": ("燃料 / 灵魂", "Fuel / Souls"),
    "ritual": ("仪式", "Ritual"), "products": ("成品", "Products"), "hammer": ("锤子", "Hammer"),
    "bind": ("绑定原机", "Bind"), "pause": ("暂停", "Pause"), "resume": ("继续", "Resume"),
    "recipes": ("选择配方", "Recipes"), "reset": ("复位", "Reset"), "xp": ("经验", "XP"),
    "infinite_hammer": ("无限锤子模块", "Infinite Hammer Module"), "installed": ("已安装", "Installed"), "not_installed": ("未安装", "Not installed"),
    "target": ("绑定：%s", "Target: %s"), "unbound": ("未绑定", "Unbound"),
    "unmeasured": ("原机状态尚不可用", "Native machine status unavailable"),
    "tier_progress": ("等级 %s  ·  进度 %s / %s", "Tier %s  ·  Progress %s / %s"),
    "resource_pair": ("%s %s/%s  ·  %s %s/%s", "%s %s/%s  ·  %s %s/%s"),
    "resource.0": ("耀光", "Aureal"), "resource.1": ("灵魂", "Souls"), "resource.2": ("血液", "Blood"), "resource.3": ("经验", "XP"),
    "fire_fuel": ("%s  ·  燃料 %ss  ·  灵魂 %ss", "%s  ·  Fuel %ss  ·  Soul %ss"),
    "fire.0": ("普通火", "Fire"), "fire.1": ("灵魂火", "Soul Fire"), "fire.2": ("附魔火", "Enchanted Fire"),
    "clibano_progress": ("进度 %s/%s  ·  %s/%s", "Progress %s/%s  ·  %s/%s"), "residues": ("残渣 %s / %s", "Residues %s / %s"),
    "automatic": ("自动匹配", "Automatic"), "all_recipes": ("全部配方", "All recipes"), "applicable": ("当前适用", "Applicable now"),
    "search": ("搜索产物名称…", "Search product name…"), "close": ("关闭", "Close"), "no_results": ("没有匹配配方", "No matching recipes"),
    "upgrade_to": ("升级至 %s 级锻台", "Upgrade forge to tier %s"), "upgrade_once": ("升级一次", "Upgrade once"),
    "batch_production": ("连续生产", "Continuous production"), "material": ("%s × %s", "%s × %s"), "missing_ingredient": ("材料不可用", "Unavailable ingredient"),
    "bind_hint": ("用配置器潜行右键原机，再潜行右键控制器。绑定距离为 8 格。", "Sneak-use a Configurator on the native machine, then on the controller. Range: 8 blocks."),
    "target_selected": ("已选择原机，请用配置器潜行右键控制器。", "Target selected. Sneak-use the Configurator on the controller."),
    "bound": ("已绑定原机", "Native machine bound"),
}
statuses = [
    ("等待下一批", "Waiting for next batch"), ("原机加工中", "Native machine processing"), ("请绑定原机", "Bind a native machine"),
    ("检查原机结构与绑定", "Check structure and binding"), ("原机已被其他控制器绑定", "Another controller owns this machine"),
    ("缺少电力", "Not enough energy"), ("已暂停调度", "Scheduling paused"), ("缺少配方材料", "Missing recipe materials"),
    ("放入锤子或安装无限锤子模块", "Insert a hammer or install the module"), ("检查等级、增强器与资源", "Check tier, enhancers and resources"),
    ("输出空间不足", "Output space is full"), ("检查锻台与基座上的材料", "Check forge and pedestal materials"),
    ("原机匹配了其他配方", "Native machine selected another recipe"), ("批次中断，检查原机后复位", "Batch interrupted; check machine and reset"),
    ("等级升级完成", "Forge upgrade complete"), ("缺少炽炉燃料", "Missing Clibano fuel"), ("需要对应的灵魂火焰", "Requires the appropriate soul flame"),
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
print(f"Generated resources for {len(MACHINES)} controllers and the infinite hammer module.")
