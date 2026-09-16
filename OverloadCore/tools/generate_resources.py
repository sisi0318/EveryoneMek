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
          'mixins': ['WardLivingAccess', 'WardHealthMixin', 'WardPlayerMixin', 'WardRemoveMixin', 'WardSetRemovedMixin',
                     'WardEntityAccess', 'WardSyncedHealthMixin', 'WardLifecycleMixin', 'WardServerLevelAccess', 'WardManagerAccess',
                     'WardLevelCallbackMixin', 'WardManagerRemovalMixin', 'WardLookupRemovalMixin', 'WardTickListMixin', 'WardTrackingEndMixin',
                     'WardSlotAccess', 'WardSlotMutationMixin', 'WardStackMutationMixin', 'WardMenuTransactionMixin',
                     'CachedRecipeAccess', 'MachineEnergyOwner', 'RecipeMonitorMixin', 'CachedEnergyMixin', 'RecipeOutputMixin',
                     'MachineTickMixin', 'TransmitterTickMixin', 'ManualEnergyMixin', 'MachineDataMixin', 'PlayerSprintMixin', 'GeneratorMixin',
                     'GeneratorHeatMixin', 'GeneratorTurbineMixin', 'GeneratorFusionMixin', 'NetworkAccess', 'EnergyNetworkMixin',
                     'FluidNetworkMixin', 'ChemicalNetworkMixin', 'EnergyTargetMixin', 'FluidTargetMixin', 'ChemicalTargetMixin', 'ItemTransportMixin', 'FluidPullMixin', 'LongPullMixin'],
          'client': ['MachineSoundMixin'], 'injectors': {'defaultRequire': 1}})
    write(f'data/{MOD}/curios/slots/overload_core.json', {'size': 1, 'operation': 'SET', 'order': 30, 'icon': 'curios:slot/empty_necklace_slot', 'add_cosmetic': False, 'drop_rule': 'ALWAYS_KEEP'})
    write(f'data/{MOD}/curios/slots/overload_ward.json', {'size': 1, 'operation': 'SET', 'order': 31, 'icon': 'curios:slot/empty_bracelet_slot', 'add_cosmetic': False, 'drop_rule': 'ALWAYS_KEEP'})
    write(f'data/{MOD}/curios/entities/player.json', {'entities': ['minecraft:player'], 'slots': ['overload_core', 'overload_ward']})
    write('data/curios/tags/item/overload_ward.json', {'replace': False, 'values': [f'{MOD}:thunder_ward']})
    write(f'assets/{MOD}/models/item/thunder_ward.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/thunder_ward'}})
    write(f'data/{MOD}/recipe/thunder_ward.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['ACA', 'ESE', 'ADA'],
          'key': {k: {'item': v} for k, v in {'A': 'mekanism:alloy_atomic', 'C': 'mekanism:ultimate_control_circuit',
                    'E': 'mekanism:energy_tablet', 'S': 'minecraft:nether_star', 'D': 'minecraft:echo_shard'}.items()},
          'result': {'id': f'{MOD}:thunder_ward'}})
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
        'item.overloadcore.thunder_ward': ('逆命雷印', 'Thunder Ward'),
        'curios.identifier.overload_ward': ('护命', 'Ward'),
        'overloadcore.ward.lore': ('借万机一瞬之雷，驳回此身既定之死。', 'Borrow the thunder of a thousand engines. Deny the appointed end.'),
        'overloadcore.ward.equip': ('右键佩戴，或放入护命饰品槽。', 'Use to equip, or place in the Ward accessory slot.'),
        'overloadcore.ward.details_hint': ('按住 Shift，窥见逆命之价。', 'Hold Shift to reveal the price of defiance.'),
        'overloadcore.ward.scope': ('雷域 %s 格 · 抽取自有及获授机枢的储能。', 'Thunder domain: %s blocks. Draws from owned or entrusted machines.'),
        'overloadcore.ward.price': ('每次逆命共耗 %s FE，诸机分担，不设冷却。', 'Each reprieve costs %s FE, shared by nearby machines. No cooldown.'),
        'overloadcore.ward.rescue': ('命尽之刻，留存半颗心，再拒死门。', 'At the final blow, keep half a heart and defy death.'),
        'overloadcore.ward.unfunded': ('雷息不足，契印不应；不凭空赊取性命。', 'Without enough power, the seal grants no reprieve.'),
        'overloadcore.ward.removable': ('可自由摘取，可与过载短路核心同佩。', 'Freely removable; can be worn alongside the Overloaded Core.'),
        'overloadcore.ward.custody': ('雷印护形 · 佩戴时抗强夺、抗篡改，死后随身。', 'Seal of Return · Resists forced removal and alteration while worn; kept through death.'),
        'overloadcore.ward.saved': ('逆命雷印 · 万机供雷，此命不绝。', 'Thunder Ward: the engines pay. Your thread holds.'),
        'curios.identifier.overload_core': ('核心', 'Core'),
        'key.categories.overloadcore': ('过载核心', 'Overload Core'), 'key.overloadcore.status': ('切换设备位置提示', 'Toggle Device Diagnostics'),
        'overloadcore.lore': ('以此残躯，续接神明断裂的回路。', 'Let this mortal frame mend the circuit of a broken god.'),
        'overloadcore.warning': ('佩戴即缔结永契，死亦不解。', 'Once worn, the pact cannot be unbound, even by death.'),
        'overloadcore.effects': ('契域 %s 格 · 只及麾下与获授的机枢。', 'Pact domain: %s blocks. Owned or entrusted machines only.'),
        'overloadcore.equip_hint': ('手持长按右键两秒，缔结魂契。', 'Hold use for two seconds with the pendant to seal the pact.'),
        'overloadcore.details_hint': ('按住 Shift，聆听契印低语。', 'Hold Shift to hear the whispers of the pact.'),
        'overloadcore.hold_bind': ('魂契将永固，继续按住右键两秒。', 'Keep holding use for two seconds. The pact will be permanent.'),
        'overloadcore.no_slot': ('需要一个空的核心饰品槽。', 'An empty Core accessory slot is required.'),
        'overloadcore.bound': ('魂契已成。', 'The soul pact is sealed.'),
        'overloadcore.foreign_core': ('此魂契已有其主。', 'This pact already bears another soul.'),
        'overloadcore.state.energy': ('缺电', 'No power'), 'overloadcore.state.output': ('输出阻塞', 'Output blocked'),
        'overloadcore.state.input': ('检查原料', 'Check inputs'), 'overloadcore.state.working': ('工作中', 'Working'), 'overloadcore.state.idle': ('空闲', 'Idle'),
        'overloadcore.hud': ('磁枷：%s 锭当量　劫热：%s%%', 'Metal bind: %s ingots   Soul heat: %s%%'),
        'overloadcore.hud.metal': ('磁枷', 'Metal Bind'),
        'overloadcore.hud.heat': ('劫热', 'Soul Heat'),
        'overloadcore.stored': ('余雷：%s FE', 'Bound lightning: %s FE'),
        'overloadcore.share_done': ('设备授权已更新。', 'Device consent updated.'),
        'overloadcore.not_owner': ('需要对准自己拥有的设备。', 'Aim at a device you own.'),
        'death.attack.overloadcore.electric': ('%1$s 被机器漏电击倒了', '%1$s was struck down by a short circuit'),
        'death.attack.overloadcore.heat': ('%1$s 在过热的车间中倒下了', '%1$s collapsed from workshop heat'),
    }
    curses = [
        ('熔魂契印 · 血肉铸入回路，死亦不能解契。', 'Soul Weld · Bound to the circuit, beyond release or death.'),
        ('雷饕之心 · 诸机索取双倍电流，以饲炉中凶神。', 'Thunder Tithe · Working machines demand twice the power.'),
        ('残阳蚀电 · 产出的雷息，一半归你，一半献祭。', 'Eclipsed Sun · Half of all generated power feeds the void.'),
        ('逆鳞电诏 · 惊扰运转机枢，雷鞭将你击伤斥退。', 'Thunderlash · Working machines strike those who stray close.'),
        ('锁脉禁律 · 百脉皆缚，管路流速折半。', 'Sealed Veins · Shackled conduits carry their burden at half pace.'),
        ('群机呓语 · 轰鸣侵蚀神识，久闻则目眩欲呕。', 'Iron Whispers · The louder chorus turns the senses to nausea.'),
        ('万钧磁枷 · 金属满 %s 锭当量，奔行即被封禁。', 'Iron Fetters · At %s ingots of metal, your sprint is sealed.'),
        ('烬火焚躯 · 近热源则劫火入体，积热盈身，焚蚀不休。', 'Ember Writ · Heat gathers within; at its peak, flesh burns.'),
    ]
    gifts = [
        ('逆铸天工 · 部分矿炼与元件，可一炉双生。', 'Twinned Forge · Chosen ores and components emerge twofold.'),
        ('余雷归脉 · 额外耗电的四分之一，重返随身机甲与机具。', 'Returning Thunder · A quarter of excess power feeds your gear.'),
        ('神机破岳 · MekaTool 电能未竭，掘速增四分之一。', 'Mountain Riven · A powered MekaTool mines a quarter faster.'),
        ('洞明机兆 · 按 %s，窥见机枢方位与凶兆。', 'Augury · Press %s to divine machine locations and their woes.'),
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
