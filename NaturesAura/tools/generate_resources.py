"""Generate the addon's JSON models, recipes, translations and development test structure."""
import gzip
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ID = "naturesmekanism"
MACHINES = {
    "universal_aura_generator": ("通用灵气发生器", "Universal Aura Generator", "minecraft:block/emerald_block", "naturesaura:gold_leaf"),
    "universal_forest_ritual": ("通用森林仪式", "Universal Forest Ritual", "minecraft:block/moss_block", "naturesaura:wood_stand"),
    "universal_natural_altar": ("通用自然祭坛", "Universal Natural Altar", "minecraft:block/oxidized_copper", "naturesaura:nature_altar"),
    "universal_offering": ("通用呼唤仪式", "Universal Offering Ritual", "minecraft:block/gold_block", "naturesaura:offering_table"),
}

def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

zh = {"itemGroup.naturesmekanism": "自然机械", "chemical.naturesmekanism.aura": "灵气"}
en = {"itemGroup.naturesmekanism": "Nature's Mekanism", "chemical.naturesmekanism.aura": "Aura"}
descriptions = [
    ("用 FE 生产灵气。可通过加压管道输出，也可开启环境释放。", "Produces Aura from FE. Supports pressurized tubes and optional environmental release."),
    ("消耗原料、树苗、金叶粉和 FE，执行森林仪式。", "Processes forest recipes using ingredients, a sapling, gold powder and FE."),
    ("消耗原料、灵气和 FE 进行灌注；催化物不消耗。", "Infuses ingredients using Aura and FE. Catalysts are retained."),
    ("替换原版祭祀台，周围仍须保留完整花阵。一份呼唤物处理一批供品。", "Replaces the Offering Table and requires its flower arrangement. One calling item starts a batch."),
]

for (name, (cn, english, accent, core)), (desc_cn, desc_en) in zip(MACHINES.items(), descriptions):
    zh[f"block.{ID}.{name}"] = cn
    en[f"block.{ID}.{name}"] = english
    zh[f"description.{ID}.{name}"] = desc_cn
    en[f"description.{ID}.{name}"] = desc_en
    # Models reference installed Minecraft/Mekanism textures; no external texture files are copied.
    model = {
        "parent": "minecraft:block/block",
        "textures": {"particle": "mekanism:block/steel_casing", "shell": "mekanism:block/steel_casing", "accent": accent, "panel": "minecraft:block/deepslate_tiles"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {d: {"texture": "#shell", "cullface": d} for d in ["north", "south", "east", "west", "up", "down"]}},
            {"from": [2, 2, -0.01], "to": [14, 14, 0], "faces": {"north": {"texture": "#panel"}}},
            {"from": [4, 4, -0.02], "to": [12, 12, -0.01], "faces": {"north": {"texture": "#accent"}}},
        ],
    }
    write(f"assets/{ID}/models/block/{name}.json", model)
    write(f"assets/{ID}/models/item/{name}.json", {"parent": f"{ID}:block/{name}"})
    write(f"assets/{ID}/blockstates/{name}.json", {"variants": {f"facing={face}": {"model": f"{ID}:block/{name}", "y": rotation} for face, rotation in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]}})
    components = ["mekanism:ejector", "mekanism:owner", "mekanism:redstone_control", "mekanism:security", "mekanism:side_config", "mekanism:upgrades", "mekanism:energy", "mekanism:items", "mekanism:chemicals", f"{ID}:environment_output"]
    write(f"data/{ID}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ID}:{name}", "functions": [{"function": "minecraft:copy_name", "source": "block_entity"}, {"function": "minecraft:copy_components", "source": "block_entity", "include": components}]}]}]})
    write(f"data/{ID}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ASA", "CKC", "ASA"], "key": {"A": {"item": "mekanism:alloy_infused"}, "S": {"tag": "c:ingots/steel"}, "C": {"item": "mekanism:basic_control_circuit"}, "K": {"item": core}}, "result": {"id": f"{ID}:{name}", "count": 1}})

statuses = [("运行中", "Running"), ("红石控制：暂停", "Paused by redstone"), ("花阵不完整", "Flower arrangement incomplete"), ("输出空间不足", "Output full"), ("等待材料或催化物", "Waiting for ingredients or catalyst"), ("灵气不足", "Not enough Aura"), ("电力不足", "Not enough energy")]
for index, (cn, english) in enumerate(statuses):
    zh[f"gui.{ID}.status.{index}"] = cn
    en[f"gui.{ID}.status.{index}"] = english
for key, cn, english in [
    ("electric_aura", "电力转化为灵气", "Electricity into Aura"),
    ("environment_on", "输出：管道 + 环境", "Output: tubes + environment"),
    ("environment_off", "输出：管道", "Output: tubes"),
    ("toggle_environment", "切换释放", "Release toggle"),
]:
    zh[f"gui.{ID}.{key}"] = cn
    en[f"gui.{ID}.{key}"] = english
write(f"assets/{ID}/lang/zh_cn.json", zh)
write(f"assets/{ID}/lang/en_us.json", en)
write("data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"{ID}:{n}" for n in MACHINES]})
write("data/minecraft/tags/block/needs_iron_tool.json", {"replace": False, "values": [f"{ID}:{n}" for n in MACHINES]})

# Small air-only structure for server GameTests (NBT, big endian).
def string(s):
    b = s.encode()
    return struct.pack(">H", len(b)) + b
def named(tag_type, name, data):
    return bytes([tag_type]) + string(name) + data
def list_tag(name, element_type, entries):
    return named(9, name, bytes([element_type]) + struct.pack(">i", len(entries)) + b"".join(entries))
nbt = b"\x0a\x00\x00" + named(3, "DataVersion", struct.pack(">i", 3955))
nbt += list_tag("size", 3, [struct.pack(">i", n) for n in [12, 5, 12]])
nbt += list_tag("palette", 10, [named(8, "Name", string("minecraft:air")) + b"\x00"])
nbt += list_tag("blocks", 10, []) + list_tag("entities", 10, []) + b"\x00"
test_structure = ROOT / f"src/gameTest/resources/data/{ID}/structure/empty.nbt"
test_structure.parent.mkdir(parents=True, exist_ok=True)
test_structure.write_bytes(gzip.compress(nbt, mtime=0))
