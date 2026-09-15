"""Generate runtime data and a bounded, audited Mek recipe list from the target JAR."""
import argparse, gzip, json, struct, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
MOD = 'overloadcore'

def write(path, value):
    target = RES / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')

def generate(mek_jar):
    write('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Overload Core'}})
    write(f'{MOD}.mixins.json', {'required': True, 'package': 'dev.everyonemek.overloadcore.mixin', 'compatibilityLevel': 'JAVA_21',
          'plugin': 'dev.everyonemek.overloadcore.mixin.OptionalMixinPlugin',
          'mixins': ['CachedRecipeAccess', 'MachineEnergyOwner', 'RecipeMonitorMixin', 'CachedEnergyMixin', 'RecipeOutputMixin',
                     'MachineTickMixin', 'TransmitterTickMixin', 'ManualEnergyMixin', 'MachineDataMixin', 'PlayerSprintMixin', 'GeneratorMixin',
                     'GeneratorHeatMixin', 'GeneratorTurbineMixin', 'GeneratorFusionMixin', 'NetworkAccess', 'EnergyNetworkMixin',
                     'FluidNetworkMixin', 'ChemicalNetworkMixin', 'EnergyTargetMixin', 'FluidTargetMixin', 'ChemicalTargetMixin', 'ItemTransportMixin', 'FluidPullMixin', 'LongPullMixin'],
          'client': ['MachineSoundMixin'], 'injectors': {'defaultRequire': 1}})
    write(f'data/{MOD}/curios/slots/overload_core.json', {'size': 1, 'operation': 'SET', 'order': 30, 'icon': 'curios:slot/empty_necklace_slot', 'add_cosmetic': False, 'drop_rule': 'ALWAYS_KEEP'})
    write(f'data/{MOD}/curios/entities/player.json', {'entities': ['minecraft:player'], 'slots': ['overload_core']})
    write('data/curios/tags/item/overload_core.json', {'replace': False, 'values': [f'{MOD}:overloaded_short_circuit_core']})
    write(f'assets/{MOD}/models/item/overloaded_short_circuit_core.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/overloaded_short_circuit_core'}})
    write(f'data/{MOD}/recipe/overloaded_short_circuit_core.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['ACA', 'ESE', 'ANA'],
          'key': {k: {'item': v} for k, v in {'A': 'mekanism:alloy_reinforced', 'C': 'mekanism:elite_control_circuit',
                    'E': 'mekanism:energy_tablet', 'S': 'minecraft:nether_star', 'N': 'mekanism:ingot_steel'}.items()},
          'result': {'id': f'{MOD}:overloaded_short_circuit_core'}})
    for name in ['electric', 'heat']:
        write(f'data/{MOD}/damage_type/{name}.json', {'message_id': f'{MOD}.{name}', 'scaling': 'never', 'exhaustion': .1})
    write('data/minecraft/tags/damage_type/is_fire.json', {'replace': False, 'values': [f'{MOD}:heat']})
    metals = ['iron','gold','copper','osmium','tin','lead','steel','bronze','netherite','aluminum','silver','nickel','zinc','uranium']
    for name, namespace_tag in [('metal_ingots','ingots'),('metal_nuggets','nuggets'),('metal_blocks','storage_blocks')]:
        write(f'data/{MOD}/tags/item/{name}.json', {'replace': False, 'values': [{'id': f'#c:{namespace_tag}/{m}', 'required': False} for m in metals]})
    equipment = [f'minecraft:{m}_{p}' for m in ['iron','golden','chainmail','netherite'] for p in ['helmet','chestplate','leggings','boots']]
    equipment += [f'minecraft:{m}_{p}' for m in ['iron','golden','netherite'] for p in ['sword','pickaxe','axe','shovel','hoe']]
    equipment += [f'mekanism:mekasuit_{p}' for p in ['helmet','bodyarmor','pants','boots']] + ['mekanism:meka_tool']
    write(f'data/{MOD}/tags/item/metal_equipment.json', {'replace': False, 'values': [{'id': item, 'required': False} for item in equipment]})
    bonuses = {}
    with zipfile.ZipFile(mek_jar) as z:
        for name in z.namelist():
            if not name.startswith('data/mekanism/recipe/') or not name.endswith('.json'): continue
            recipe = name[len('data/mekanism/recipe/'):-5]
            parts = recipe.split('/')
            ore = len(parts) >= 4 and parts[0] == 'processing' and parts[1] in ['iron','gold','copper','osmium','tin','lead'] and parts[-1] in ['from_ore','from_raw_ore','from_raw_block']
            if ore or recipe in ['control_circuit/basic', 'metallurgic_infusing/alloy/infused']:
                data = json.loads(z.read(name))
                if data['type'] in ['mekanism:enriching', 'mekanism:purifying', 'mekanism:injecting', 'mekanism:dissolution', 'mekanism:metallurgic_infusing']:
                    if data['type'] != 'mekanism:dissolution' or data['output']['amount'] <= 5000:
                        bonuses['mekanism:'+recipe] = data
    write('overloadcore-bonus-recipes.json', bonuses)
    pairs = {
        'itemGroup.overloadcore': ('过载核心', 'Overload Core'),
        'item.overloadcore.overloaded_short_circuit_core': ('过载短路核心', 'Overloaded Short-Circuit Core'),
        'curios.identifier.overload_core': ('核心', 'Core'),
        'key.categories.overloadcore': ('过载核心', 'Overload Core'), 'key.overloadcore.status': ('切换设备位置提示', 'Toggle Device Diagnostics'),
        'overloadcore.lore': ('额定功率，仅供参考。', 'Rated power is only a suggestion.'),
        'overloadcore.warning': ('装备后无法主动卸下，死亡仍保留。', 'Cannot be removed in survival. Kept on death.'),
        'overloadcore.effects': ('%s 格内，影响自有及授权的 Mek 设备。', 'Affects owned/shared Mek devices within %s blocks.'),
        'overloadcore.equip_hint': ('手持挂坠，长按右键两秒佩戴。', 'Hold the pendant in your hand and use it for two seconds to equip.'),
        'overloadcore.details_hint': ('按住 Shift 查看诅咒与收益。', 'Hold Shift to view curses and benefits.'),
        'overloadcore.hold_bind': ('继续按住右键两秒，将永久装备过载短路核心。', 'Keep holding use for two seconds to permanently equip the core.'),
        'overloadcore.no_slot': ('需要一个空的核心饰品槽。', 'An empty Core accessory slot is required.'),
        'overloadcore.bound': ('已绑定佩戴者。', 'Bound to its wearer.'),
        'overloadcore.foreign_core': ('此核心已绑定另一位佩戴者。', 'This core is bound to another wearer.'),
        'overloadcore.state.energy': ('缺电', 'No power'), 'overloadcore.state.output': ('输出阻塞', 'Output blocked'),
        'overloadcore.state.input': ('检查原料', 'Check inputs'), 'overloadcore.state.working': ('工作中', 'Working'), 'overloadcore.state.idle': ('空闲', 'Idle'),
        'overloadcore.hud': ('金属：%s 锭当量　体热：%s%%', 'Metal: %s ingots   Heat: %s%%'),
        'overloadcore.stored': ('回收电量：%s FE', 'Recovered energy: %s FE'),
        'overloadcore.share_done': ('设备授权已更新。', 'Device consent updated.'),
        'overloadcore.not_owner': ('需要对准自己拥有的设备。', 'Aim at a device you own.'),
        'death.attack.overloadcore.electric': ('%1$s 被机器漏电击倒了', '%1$s was struck down by a short circuit'),
        'death.attack.overloadcore.heat': ('%1$s 在过热的车间中倒下了', '%1$s collapsed from workshop heat'),
    }
    curses = [
        ('无法主动卸下，死亡仍保留。', 'Cannot be removed in survival; kept on death.'),
        ('工作机器耗电翻倍。', 'Machines use twice as much power while working.'),
        ('发电设备新产生的电量减半。', 'Generators produce half as much energy.'),
        ('贴近工作机器会受到电击和击退。', 'Nearby working machines shock and repel you.'),
        ('管道运输速度减半。', 'Affected pipes transport at half speed.'),
        ('机器噪声变大，久留会反胃。', 'Machines sound louder; prolonged exposure causes nausea.'),
        ('携带金属达到 %s 锭当量时无法奔跑。', 'Cannot sprint while carrying %s ingots worth of metal.'),
        ('靠近热源会积累体热，过热后持续受伤。', 'Heat sources raise body heat; overheating causes damage.'),
    ]
    gifts = [
        ('部分矿物加工和基础元件产物翻倍。', 'Double outputs for selected ore and component recipes.'),
        ('回收额外耗电的 25%，给随身机具充电。', 'Recover 25% of the extra power to charge carried gear.'),
        ('有电的 MekaTool 挖掘效率提高 25%。', 'A powered MekaTool mines 25% faster.'),
        ('%s：显示附近设备位置与状态。', '%s: show nearby device locations and status.'),
    ]
    for i,pair in enumerate(curses): pairs[f'overloadcore.curse.{i}'] = pair
    for i,pair in enumerate(gifts): pairs[f'overloadcore.gift.{i}'] = pair
    for lang,index in [('zh_cn',0),('en_us',1)]: write(f'assets/{MOD}/lang/{lang}.json', {key:value[index] for key,value in pairs.items()})
    # Test-only empty structure; never included in the runtime jar.
    def string(text):
        b=text.encode('utf-8'); return struct.pack('>H',len(b))+b
    def named(kind,name,payload): return bytes([kind])+string(name)+payload
    nbt=b'\x0a\x00\x00'+named(3,'DataVersion',struct.pack('>i',3955))
    nbt+=named(9,'size',b'\x03'+struct.pack('>iiii',3,96,24,96))
    nbt+=named(9,'palette',b'\x0a'+struct.pack('>i',1)+named(8,'Name',string('minecraft:air'))+b'\x00')
    nbt+=named(9,'blocks',b'\x0a'+struct.pack('>i',0))+named(9,'entities',b'\x0a'+struct.pack('>i',0))+b'\x00'
    target=ROOT/f'src/gameTest/resources/data/{MOD}/structure/empty.nbt';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(gzip.compress(nbt,mtime=0))
    print('Generated core resources and',len(bonuses),'audited recipes')

if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('--mek-jar',type=Path);args=p.parse_args()
    jar=args.mek_jar
    if jar is None:
        candidates=list((ROOT/'.gradle-home').rglob('Mekanism-1.21.1-10.7.19.85.jar'))+list((ROOT.parent/'Ars-Nouveau/.gradle-home').rglob('Mekanism-1.21.1-10.7.19.85.jar'))
        if not candidates: p.error('Pass the target Mekanism JAR with --mek-jar')
        jar=candidates[0]
    generate(jar)
