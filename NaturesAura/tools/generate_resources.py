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
    "universal_animal_spawner": ("通用降生祭坛", "Universal Animal Spawner", "naturesaura:animal_spawner"),
    "industrial_breeder": ("工业养殖机", "Industrial Breeder", "naturesaura:birth_spirit"),
    "ore_condensation_chamber": ("矿物凝聚室", "Ore Condensation Chamber", "naturesaura:infused_iron"),
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
    ("读取自然灵气的降生配方，消耗原料、FE 和灵气生成生物。可设置世界坐标轴 X/Y/Z 偏移、水平半径和区域数量上限；每个范围模块增加 2 格最大半径。陆生生物需要地面，水生生物需要水。储罐优先，不足从半径 35 格环境补足。生成成功后才扣原料和灵气。", "Spawns entities from Nature's Aura recipes using ingredients, FE and Aura. Configure world-axis X/Y/Z offsets, horizontal radius and area population limit. Each Range Module adds 2 blocks of maximum radius. Land creatures need a floor and aquatic creatures need water. Uses stored Aura first, then environmental Aura within 35 blocks. Ingredients and Aura are consumed only after successful spawning."),
    ("用 FE 和两份食物配对繁殖成年动物，保留繁殖冷却。降生之灵模式要求动物周围 30 格至少 120 万灵气，由自然灵气原版事件产出并自动收集；普通繁殖模式允许低灵气和海龟、青蛙怀卵。动物间需小于 3 格且无遮挡。支持区域与数量上限。", "Breeds adult animals using FE and two food items, preserving cooldowns. Birth Spirits mode requires at least 1.2M Aura within 30 blocks of the parent; the native Nature's Aura event produces spirits for automatic collection. Breeding mode allows low Aura and turtle/frog pregnancy. Parents must be less than 3 blocks apart with line of sight. Supports work area and population limits."),
]

descriptions.append(("3×3×3 中空多方块：控制器位于侧面中心并朝外，其余 25 格用凝聚室外壳或端口，中心留空。输入石头（主世界）或下界岩（下界）、慷慨之粉与 FE；半径 30 格环境灵气须高于 200 万。按原版权重生成矿石并扣灵气，粉末保留。端口潜行空手右键切换输入/输出，普通右键打开控制器。", "Hollow 3x3x3 multiblock: outward-facing controller at the center of a side, 25 casing/port blocks and an empty center. Uses stone in the Overworld or netherrack in the Nether, Powder of the Bountiful Core, FE and Aura. Requires over 2M environmental Aura within 30 blocks. Selects ores using native weights; powder is retained. Sneak with an empty hand to toggle port input/output; use normally to open the controller."))

for (name, (cn, english, core)), (desc_cn, desc_en) in zip(MACHINES.items(), descriptions, strict=True):
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
    if name in ("universal_animal_spawner", "industrial_breeder"):
        components.append(f"{ID}:area_settings")
    if name == "industrial_breeder":
        components.append(f"{ID}:breed_only")
    write(f"data/{ID}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{ID}:{name}", "functions": [{"function": "minecraft:copy_name", "source": "block_entity"}, {"function": "minecraft:copy_components", "source": "block_entity", "include": components}]}]}]})
    write(f"data/{ID}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ASA", "CKC", "ASA"], "key": {"A": {"item": "mekanism:alloy_infused"}, "S": {"tag": "c:ingots/steel"}, "C": {"item": "mekanism:basic_control_circuit"}, "K": {"item": core}}, "result": {"id": f"{ID}:{name}", "count": 1}})

statuses = [("运行中", "Running"), ("红石控制：暂停", "Paused by redstone"), ("花阵不完整", "Flower arrangement incomplete"), ("输出空间不足", "Output full"), ("等待材料或催化物", "Waiting for ingredients or catalyst"), ("灵气不足", "Not enough Aura"), ("电力不足", "Not enough energy"), ("环境未达到装瓶条件", "Bottling environment unsuitable"), ("该目标需要模拟环境模块", "Target needs a simulation module"), ("已在目标范围内", "Within target limits"), ("已停用", "Disabled"), ("目标区块未加载", "Target chunk not loaded"), ("正在回收环境灵气", "Recovering environmental Aura"), ("正在释放储罐灵气", "Releasing stored Aura")]
for index, (cn, english) in enumerate(statuses):
    zh[f"gui.{ID}.status.{index}"] = cn
    en[f"gui.{ID}.status.{index}"] = english
for index, (cn, english) in enumerate([("区域生物数量已达上限", "Area population limit reached"), ("生成区域没有合适空间", "No suitable spawn space"), ("生物生成受阻或已被取消", "Entity spawn blocked or cancelled")], start=14):
    zh[f"gui.{ID}.status.{index}"] = cn
    en[f"gui.{ID}.status.{index}"] = english
for key, cn, english in [
    ("status.20", "凝聚室结构不完整或中心未留空", "Chamber shell incomplete or center blocked"),
    ("status.21", "维度不支持或原版矿物凝聚已停用", "Unsupported dimension or ore effect disabled"),
    ("status.22", "等待石头/下界岩与慷慨之粉", "Needs stone/netherrack and ore-effect powder"),
    ("status.23", "需要高于 200 万环境灵气", "Needs more than 2M environmental Aura"),
    ("status.24", "当前矿物权重表没有可用矿石", "No valid ores in the current weighted table"),
    ("chamber_structure", "结构：3×3×3 中空外壳", "Structure: hollow 3x3x3 shell"),
    ("chamber_cost", "本次成本：%s 灵气", "Selected cost: %s Aura"),
    ("chamber_threshold", "半径 30 格环境灵气 > 2,000,000", "Environmental Aura > 2,000,000 within 30 blocks"),
    ("chamber_weight", "权重：%s · 抽取概率约 %s%%", "Weight: %s / Chance: ~%s%%"),
    ("chamber_powder_kept", "慷慨之粉保留 · 需要中空多方块", "Powder retained / Requires hollow multiblock"),
    ("port.input", "凝聚室端口：输入物品、灵气与 FE", "Chamber port: items, Aura and FE input"),
    ("port.output", "凝聚室端口：输出矿石", "Chamber port: ore output"),
    ("breeder_mode.spirit", "降生之灵", "Birth Spirits"),
    ("breeder_mode.breed", "普通繁殖", "Breeding"),
    ("status.17", "等待成年配对动物及食物", "Waiting for adult pair and food"),
    ("status.18", "等待配对、食物及 120 万环境灵气", "Needs pair, food and 1.2M nearby Aura"),
    ("status.19", "正在收集降生之灵", "Collecting birth spirits"),
    ("no_target", "等待配方", "Waiting for recipe"),
    ("area_count", "数量：%s / %s", "Count: %s / %s"),
    ("area_offset", "偏移 X: %s · Y: %s · Z: %s", "Offset X: %s / Y: %s / Z: %s"),
    ("area_radius", "半径：%s / %s", "Radius: %s / %s"),
    ("area_cap", "数量上限：%s", "Limit: %s"),
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
for name, cn, english in [("ore_chamber_casing", "凝聚室外壳", "Ore Chamber Casing"), ("ore_chamber_port", "凝聚室端口", "Ore Chamber Port")]:
    zh[f"block.{ID}.{name}"] = cn
    en[f"block.{ID}.{name}"] = english
    write(f"assets/{ID}/models/item/{name}.json", {"parent": f"{ID}:block/{name}"})
    write(f"assets/{ID}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{ID}:block/ore_condensation_chamber/{'top' if name.endswith('casing') else 'front'}"}})
    if name.endswith("port"):
        write(f"assets/{ID}/models/block/{name}_output.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{ID}:block/ore_condensation_chamber/front_active"}})
        variants = {f"output={str(output).lower()}": {"model": f"{ID}:block/{name}{'_output' if output else ''}"} for output in [False, True]}
    else:
        variants = {"": {"model": f"{ID}:block/{name}"}}
    write(f"assets/{ID}/blockstates/{name}.json", {"variants": variants})
    entry = {"type": "minecraft:item", "name": f"{ID}:{name}"}
    if name.endswith("port"):
        entry["functions"] = [{"function": "minecraft:copy_state", "block": f"{ID}:{name}", "properties": ["output"]}]
    write(f"data/{ID}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry]}]})
write(f"data/{ID}/recipe/ore_chamber_casing.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["SIS", "ICI", "SIS"], "key": {"S": {"tag": "c:ingots/steel"}, "I": {"item": "naturesaura:infused_iron"}, "C": {"item": "mekanism:steel_casing"}}, "result": {"id": f"{ID}:ore_chamber_casing", "count": 8}})
write(f"data/{ID}/recipe/ore_chamber_port.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ACA", "HSH", "ACA"], "key": {"A": {"item": "mekanism:alloy_reinforced"}, "C": {"item": "mekanism:advanced_control_circuit"}, "H": {"item": "minecraft:hopper"}, "S": {"item": f"{ID}:ore_chamber_casing"}}, "result": {"id": f"{ID}:ore_chamber_port", "count": 2}})
# Override the controller's generic recipe with its late-game structure components.
write(f"data/{ID}/recipe/ore_condensation_chamber.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["ACA", "SPS", "ACA"], "key": {"A": {"item": "mekanism:alloy_atomic"}, "C": {"item": "mekanism:elite_control_circuit"}, "S": {"item": f"{ID}:ore_chamber_casing"}, "P": {"type": "neoforge:components", "items": "naturesaura:effect_powder", "components": {"naturesaura:effect_powder_data": {"effect": "naturesaura:ore_spawn"}}}}, "result": {"id": f"{ID}:ore_condensation_chamber", "count": 1}})
write(f"assets/{ID}/lang/zh_cn.json", zh)
write(f"assets/{ID}/lang/en_us.json", en)
all_blocks = [*MACHINES, "ore_chamber_casing", "ore_chamber_port"]
write("data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"{ID}:{n}" for n in all_blocks]})
write("data/minecraft/tags/block/needs_iron_tool.json", {"replace": False, "values": [f"{ID}:{n}" for n in all_blocks]})

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
