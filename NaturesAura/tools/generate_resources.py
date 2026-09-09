"""Generate the addon's JSON models, recipes, translations and development test structure."""
import gzip
import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ID = "naturesmekanism"
MACHINES = {
    "universal_aura_generator": ("通用灵气发生器", "Universal Aura Generator", "naturesaura:gold_leaf"),
    "universal_forest_ritual": ("通用森林仪式", "Universal Forest Ritual", "naturesaura:wood_stand"),
    "universal_natural_altar": ("通用自然祭坛", "Universal Natural Altar", "naturesaura:nature_altar"),
    "universal_offering": ("通用呼唤仪式", "Universal Offering Ritual", "naturesaura:offering_table"),
    "aura_bottler": ("灵气装瓶机", "Aura Bottler", "naturesaura:bottle_two_the_rebottling"),
    "aura_controller": ("灵气调控器", "Aura Controller", "naturesaura:aura_detector"),
}

def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")

zh = {"itemGroup.naturesmekanism": "自然机械", "chemical.naturesmekanism.aura": "灵气"}
en = {"itemGroup.naturesmekanism": "Nature's Mekanism", "chemical.naturesmekanism.aura": "Aura"}
descriptions = [
    ("用 FE 生产灵气。可通过加压管道输出，也可开启环境释放。", "Produces Aura from FE. Supports pressurized tubes and optional environmental release."),
    ("消耗原料、树苗、金叶粉和 FE，执行森林仪式。", "Processes forest recipes using ingredients, a sapling, gold powder and FE."),
    ("消耗原料、灵气和 FE 进行灌注；优先使用储罐灵气，不足时使用环境灵气。催化物不消耗。", "Infuses ingredients using Aura and FE. Uses stored Aura first, then environmental Aura. Catalysts are retained."),
    ("替换原版祭祀台，周围仍须保留完整花阵。一份呼唤物处理一批供品。", "Replaces the Offering Table and requires its flower arrangement. One calling item starts a batch."),
    ("用瓶与塞和 FE 装瓶。半径 30 格灵气至少 100,000 时，每瓶消耗 20,000 灵气并按维度产出；灵气不高于 -100,000 时产出真空瓶。储罐灵气优先。模拟环境模块可解除环境和维度门槛并选择产物。", "Fills Bottle and Cork using FE. Within 30 blocks, at least 100,000 Aura permits a dimension-specific bottle costing 20,000 Aura; at most -100,000 Aura permits vacuum bottles at no Aura cost. Stored Aura is used first. An Environment Simulation Module bypasses environmental and dimension requirements and allows product selection."),
    ("设置环境灵气上下限，过量时回收至储罐，不足时释放储罐灵气。支持单向模式、红石和范围升级；达到目标后停止耗电。输入数值后回车或点击勾号应用。", "Balances environmental Aura between configurable lower and upper limits. Recovers excess Aura into its Chemical tank and releases stored Aura when below the lower limit. Supports one-way modes, redstone and range modules; consumes no FE while at target. Press Enter or click the checkmark to apply a number."),
]

for (name, (cn, english, core)), (desc_cn, desc_en) in zip(MACHINES.items(), descriptions):
    zh[f"block.{ID}.{name}"] = cn
    en[f"block.{ID}.{name}"] = english
    zh[f"container.{ID}.{name}"] = cn
    en[f"container.{ID}.{name}"] = english
    zh[f"description.{ID}.{name}"] = desc_cn
    en[f"description.{ID}.{name}"] = desc_en
    texture = f"{ID}:block/{name}"
    model = {
        "parent": "minecraft:block/cube",
        "textures": {"particle": f"{texture}/side", "north": f"{texture}/front", "up": f"{texture}/top",
                     **{side: f"{texture}/side" for side in ["south", "east", "west", "down"]}},
    }
    write(f"assets/{ID}/models/block/{name}.json", model)
    write(f"assets/{ID}/models/block/{name}_active.json", {
        "parent": f"{ID}:block/{name}", "textures": {"north": f"{texture}/front_active"},
    })
    write(f"assets/{ID}/models/item/{name}.json", {"parent": f"{ID}:block/{name}"})
    write(f"assets/{ID}/blockstates/{name}.json", {"variants": {
        f"facing={face},active={str(active).lower()}": {"model": f"{ID}:block/{name}{'_active' if active else ''}", "y": rotation}
        for face, rotation in [("north", 0), ("east", 90), ("south", 180), ("west", 270)] for active in [False, True]
    }})
    components = ["mekanism:ejector", "mekanism:owner", "mekanism:redstone_control", "mekanism:security", "mekanism:side_config", "mekanism:upgrades", "mekanism:energy", "mekanism:items", "mekanism:chemicals", f"{ID}:environment_output"]
    if name == "aura_bottler":
        components.append(f"{ID}:bottling_mode")
    if name == "aura_controller":
        components.append(f"{ID}:control_settings")
    write(f"data/{ID}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ID}:{name}", "functions": [{"function": "minecraft:copy_name", "source": "block_entity"}, {"function": "minecraft:copy_components", "source": "block_entity", "include": components}]}]}]})
    write(f"data/{ID}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ASA", "CKC", "ASA"], "key": {"A": {"item": "mekanism:alloy_infused"}, "S": {"tag": "c:ingots/steel"}, "C": {"item": "mekanism:basic_control_circuit"}, "K": {"item": core}}, "result": {"id": f"{ID}:{name}", "count": 1}})

statuses = [("运行中", "Running"), ("红石控制：暂停", "Paused by redstone"), ("花阵不完整", "Flower arrangement incomplete"), ("输出空间不足", "Output full"), ("等待材料或催化物", "Waiting for ingredients or catalyst"), ("灵气不足", "Not enough Aura"), ("电力不足", "Not enough energy"), ("环境未达到装瓶条件", "Bottling environment unsuitable"), ("该目标需要模拟环境模块", "Target needs a simulation module"), ("已在目标范围内", "Within target limits"), ("已停用", "Disabled"), ("目标区块未加载", "Target chunk not loaded"), ("正在回收环境灵气", "Recovering environmental Aura"), ("正在释放储罐灵气", "Releasing stored Aura")]
for index, (cn, english) in enumerate(statuses):
    zh[f"gui.{ID}.status.{index}"] = cn
    en[f"gui.{ID}.status.{index}"] = english
for key, cn, english in [
    ("electric_aura", "电力转化为灵气", "Electricity into Aura"),
    ("environment_on", "输出：管道 + 环境", "Output: tubes + environment"),
    ("environment_off", "输出：管道", "Output: tubes"),
    ("toggle_environment", "切换释放", "Release toggle"),
    ("gold_module_count", "金叶无限模块：%s / 1", "Infinite Gold Leaf: %s / 1"),
    ("gold_module_power", "装入后总功耗 ×%s", "Installed: total energy use x%s"),
    ("environment_aura", "周围灵气（%s 格）：%s", "Aura within %s blocks: %s"),
    ("simulation_module_count", "模拟环境模块：%s / 1", "Environment Simulation: %s / 1"),
    ("bottling_mode.auto", "自动", "Auto"),
    ("bottling_mode.aura", "本地灵气", "Local Aura"),
    ("bottling_mode.vacuum", "真空", "Vacuum"),
    ("bottling_mode.sunlight", "阳光", "Sunlight"),
    ("bottling_mode.ghost", "鬼魂", "Ghosts"),
    ("bottling_mode.darkness", "黑暗", "Darkness"),
    ("bottling_aura_cost", "每瓶：%s 灵气", "Per bottle: %s Aura"),
    ("bottling_work", "基础：%s FE/t · %s ticks", "Base: %s FE/t · %s ticks"),
    ("bottling_threshold", "周围灵气 ≥ 100,000（30 格）", "Nearby Aura >= 100,000 (30 blocks)"),
    ("bottling_vacuum_threshold", "周围灵气 ≤ -100,000（30 格）", "Nearby Aura <= -100,000 (30 blocks)"),
    ("bottling_dimension.overworld", "主世界", "Overworld"),
    ("bottling_dimension.nether", "下界", "Nether"),
    ("bottling_dimension.end", "末地", "The End"),
    ("bottling_dimension.other", "其他对应维度", "Other matching dimensions"),
    ("bottling_dimension.any", "任意维度", "Any dimension"),
    ("bottling_simulation_hint", "可装模拟环境模块解除环境、维度门槛", "Simulation module can bypass these conditions"),
    ("control_mode.balance", "自动平衡", "Automatic balance"),
    ("control_mode.recover", "仅回收至上限", "Recover excess above upper limit"),
    ("control_mode.release", "仅释放至下限", "Release up to lower limit"),
    ("control_mode.hold", "停用调控", "Disable regulation"),
    ("control_lower", "下限：%s", "Lower: %s"),
    ("control_upper", "上限：%s", "Upper: %s"),
    ("range_module_count", "范围升级：%s / 4", "Range Modules: %s / 4"),
    ("range_module_power", "范围升级总功耗 ×%s", "Range energy multiplier: x%s"),
]:
    zh[f"gui.{ID}.{key}"] = cn
    en[f"gui.{ID}.{key}"] = english
zh[f"item.{ID}.infinite_gold_module"] = "金叶无限模块"
en[f"item.{ID}.infinite_gold_module"] = "Infinite Gold Leaf Module"
zh[f"tooltip.{ID}.infinite_gold_module"] = "免除森林仪式专用槽的金叶粉消耗，提高耗电；最多安装 1 个。"
en[f"tooltip.{ID}.infinite_gold_module"] = "Replaces the forest ritual's dedicated gold powder supply at increased energy cost. Maximum: 1."
zh[f"tooltip.{ID}.infinite_gold_module.install"] = "放入升级窗口的模块槽，或潜行右键森林仪式机安装。树苗和配方材料仍会消耗。"
en[f"tooltip.{ID}.infinite_gold_module.install"] = "Insert into the upgrade window's module slot, or sneak-use on a forest ritual machine. Saplings and recipe ingredients are still consumed."
zh[f"item.{ID}.environment_simulation_module"] = "模拟环境模块"
en[f"item.{ID}.environment_simulation_module"] = "Environment Simulation Module"
zh[f"tooltip.{ID}.simulation_module"] = "解除装瓶机的维度和环境门槛，可选择阳光、鬼魂、黑暗或真空。提高耗电；灵气瓶仍消耗 20,000 灵气。限装 1 个。"
en[f"tooltip.{ID}.simulation_module"] = "Bypasses bottling environment and dimension requirements. Select sunlight, ghosts, darkness or vacuum at increased energy use. Aura bottles still cost 20,000 Aura. Maximum: 1."
zh[f"tooltip.{ID}.simulation_module.install"] = "放入装瓶机升级窗口的模块槽，或潜行右键装瓶机安装。点击主界面按钮选择产物。"
en[f"tooltip.{ID}.simulation_module.install"] = "Insert into the bottler's upgrade-window module slot, or sneak-use on a bottler. Click the main-screen button to select the product."
zh[f"item.{ID}.range_module"] = "范围升级模块"
en[f"item.{ID}.range_module"] = "Range Module"
zh[f"tooltip.{ID}.range_module"] = "扩大支持机器的工作范围，最多安装 4 个。每个模块额外增加一份基础总功耗。"
en[f"tooltip.{ID}.range_module"] = "Extends supported machines' working range. Maximum: 4. Each module adds one extra full share of normal energy use."
zh[f"tooltip.{ID}.range_module.install"] = "放入升级窗口的范围槽，或潜行右键安装。调控器每个模块增加 8 格半径。"
en[f"tooltip.{ID}.range_module.install"] = "Insert into the upgrade-window range slot or sneak-use to install. Each module adds 8 blocks of controller radius."
write(f"assets/{ID}/lang/zh_cn.json", zh)
write(f"assets/{ID}/lang/en_us.json", en)
write(f"assets/{ID}/models/item/infinite_gold_module.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "mekanism:item/upgrade_energy", "layer1": "naturesaura:item/gold_leaf"},
})
write(f"data/{ID}/recipe/infinite_gold_module.json", {
    "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["AGA", "GCG", "AGA"],
    "key": {"A": {"item": "mekanism:alloy_reinforced"}, "G": {"item": "naturesaura:gold_leaf"}, "C": {"item": "mekanism:advanced_control_circuit"}},
    "result": {"id": f"{ID}:infinite_gold_module", "count": 1},
})
write(f"assets/{ID}/models/item/environment_simulation_module.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "mekanism:item/upgrade_filter", "layer1": "naturesaura:item/aura_bottle"},
})
def bottled_ingredient(aura_type):
    return {"type": "neoforge:components", "items": "naturesaura:aura_bottle",
            "components": {"naturesaura:aura_bottle_data": {"aura_type": f"naturesaura:{aura_type}"}}}
write(f"data/{ID}/recipe/environment_simulation_module.json", {
    "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SAG", "ACA", "DAV"],
    "key": {"S": bottled_ingredient("overworld"), "G": bottled_ingredient("nether"), "D": bottled_ingredient("end"),
            "V": {"item": "naturesaura:vacuum_bottle"}, "A": {"item": "mekanism:alloy_reinforced"},
            "C": {"item": "mekanism:elite_control_circuit"}},
    "result": {"id": f"{ID}:environment_simulation_module", "count": 1},
})
write(f"assets/{ID}/models/item/range_module.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": "mekanism:item/upgrade_filter", "layer1": "minecraft:item/ender_pearl"},
})
write(f"data/{ID}/recipe/range_module.json", {
    "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["AEA", "ECE", "AEA"],
    "key": {"A": {"item": "mekanism:alloy_reinforced"}, "E": {"item": "minecraft:ender_pearl"},
            "C": {"item": "mekanism:advanced_control_circuit"}},
    "result": {"id": f"{ID}:range_module", "count": 1},
})
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
