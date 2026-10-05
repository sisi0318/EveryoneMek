"""Mek-framed module icons and original textured equipment models. Bitmap atlases are exported separately."""
from pathlib import Path
import json, math

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NAMES={
 'polarization':('极化联动单元','Polarization Link Unit','远程命中施加极化，蓄力斩击引爆；近战实战命中强化下一发贯穿。','Ranged hits polarize targets; charged slashes detonate marks. Melee combat hits empower the next rail shot.'),
 'magnetic_deflector':('磁偏护盾单元','Magnetic Deflector Unit','按住战术格挡键展开正面护盾，消耗胸甲电量；精准格挡强化下一次蓄力攻击。','Hold the tactical guard key for a directional shield powered by the chestplate. A perfect guard empowers the next charged attack.'),
 'phase_heat_sink':('相变热沉单元','Phase-Change Heat Sink Unit','消耗胸甲电量压制劫热；每级提高降温能力。','Consumes bodyarmor energy to suppress heat burden; cooling increases per level.'),
 'magnetic_compensation':('磁荷补偿单元','Magnetic Compensation Unit','消耗护腿电量抵消磁枷负荷；缺电时恢复原负荷。','Uses leggings energy to offset metal burden. Original burden returns without power.'),
 'ward_capacitor':('护命电容单元','Ward Capacitor Unit','储存护命电量，与附近设备合力逆命；每级默认容纳三次消耗。','Stores rescue energy and shares the cost with nearby machines. Holds three rescues per level by default.'),
 'afterguard_stabilizer':('余雷固相单元','Afterguard Stabilizer Unit','增强逆命后的次数盾；不增加护命冷却。','Improves the post-rescue guard without adding a rescue cooldown.'),
 'residual_reservoir':('旧式余雷蓄能单元','Retired Residual Reservoir Unit','已停用的旧式单元。','This old module has been retired.'),
 'resonant_discharge':('谐振放电单元','Resonant Discharge Unit','命中后额外耗电，向附近敌人释放连锁电弧。','Uses additional energy after a hit to arc toward nearby hostile targets.'),
 'charge_accelerator':('充能加速单元','Charge Accelerator Unit','缩短武器蓄力时间。','Shortens weapon charge time.'),
 'rail_magazine':('磁轨复位单元','Rail Recovery Unit','每级缩短0.1秒射击恢复时间，最低0.1秒。','Shortens the delay after firing by 0.1 seconds per level, down to 0.1 seconds.'),
 'rail_focus':('磁轨聚焦单元','Rail Focus Unit','每级提高十六格射程。','Adds sixteen blocks of range per level.'),
 'rail_piercing':('磁轨贯穿单元','Rail Piercing Unit','每级多贯穿一个目标，后续伤害递减；不能穿墙。','Pierces one additional target per level with diminishing damage. Does not pass through walls.'),
 'blade_field':('刃场扩展单元','Blade Field Unit','提高蓄力斩击的距离与伤害。','Increases the reach and damage of charged slashes.'),
}
TEXTURES={'metal':'gear_alloy','dark':'gear_graphite','grip':'gear_grip','energy':'gear_circuit','indicator':'gear_circuit'}
NORMALS={'west':(-1,0,0),'east':(1,0,0),'down':(0,-1,0),'up':(0,1,0),'north':(0,0,-1),'south':(0,0,1)}
def face_vertices(a,b):
 x,y,z=a;X,Y,Z=b
 return {'west':[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],'east':[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)],'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],'north':[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],'south':[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]}
def modules(write):
 for name in ['residual_coupling_unit',*NAMES]:
  textures={'layer0':'mekanism:item/module_base'}
  if name!='residual_reservoir':textures['layer1']='overloadcore:item/module_'+{'polarization':'resonant_discharge','magnetic_deflector':'ward_capacitor'}.get(name,name)+'_panel'
  write(f'assets/overloadcore/models/item/module_{name}.json',{'parent':'minecraft:item/generated','textures':textures})
  if name in ['residual_coupling_unit','residual_reservoir']:continue
  special={'polarization':'mekanism:teleportation_core','magnetic_deflector':'mekanism:basic_induction_provider','ward_capacitor':'minecraft:echo_shard','afterguard_stabilizer':'minecraft:nether_star','residual_reservoir':'mekanism:energy_tablet','resonant_discharge':'mekanism:alloy_atomic','phase_heat_sink':'mekanism:ingot_tin','magnetic_compensation':'mekanism:ingot_steel','charge_accelerator':'minecraft:redstone_block','rail_magazine':'minecraft:iron_ingot','rail_focus':'minecraft:amethyst_shard','rail_piercing':'mekanism:ingot_refined_obsidian','blade_field':'mekanism:ingot_refined_glowstone'}[name]
  write(f'data/overloadcore/recipe/module_{name}.json',{'type':'minecraft:crafting_shaped','pattern':['ASA','CBC','AEA'],'key':{k:{'item':v} for k,v in {'A':'mekanism:alloy_reinforced','S':special,'C':'mekanism:elite_control_circuit','B':'mekanism:module_base','E':'mekanism:energy_tablet'}.items()},'result':{'id':f'overloadcore:module_{name}'}})
def combat(write):
 # Old IDs remain only to decode and convert existing stacks. Both display Mek's native model.
 for name in ['rail_lance','thunder_blade']:
  write(f'assets/overloadcore/models/item/{name}.json',{'parent':'mekanism:item/meka_tool'})
  for suffix in ['_base','_fallback']:(RES/f'assets/overloadcore/models/item/{name}{suffix}.json').unlink(missing_ok=True)
  (RES/f'data/overloadcore/recipe/{name}.json').unlink(missing_ok=True)
 (RES/'overloadcore.enumextensions.json').unlink(missing_ok=True)
 write('assets/overloadcore/models/item/module_combat_form.json',{'parent':'minecraft:item/generated','textures':{'layer0':'mekanism:item/module_base','layer1':'overloadcore:item/module_resonant_discharge_panel'}})
 write('data/overloadcore/recipe/module_combat_form.json',{'type':'minecraft:crafting_shaped','pattern':['APA','CBC','AEA'],'key':{k:{'item':v} for k,v in {'A':'mekanism:alloy_atomic','P':'mekanism:pellet_polonium','C':'mekanism:ultimate_control_circuit','B':'mekanism:module_base','E':'mekanism:energy_tablet'}.items()},'result':{'id':'overloadcore:module_combat_form'}})
 (RES/'data/overloadcore/tags/item/rail_ammunition.json').unlink(missing_ok=True)
 write('data/overloadcore/damage_type/rail.json',{'message_id':'overloadcore.rail','scaling':'never','exhaustion':.1})
 write('data/minecraft/tags/damage_type/is_projectile.json',{'replace':False,'values':['overloadcore:rail']})

def armor():
 objects=[]
 for i,x in enumerate([-.22,-.17,-.12]):objects.append((f'overloadcore_heat_sink_body_fin{i}',[x,1.2,-.25],[x+.035,1.58,-.17],'metal'))
 objects += [('overloadcore_coupler_body_cap',[.10,1.24,-.24],[.23,1.49,-.17],'metal'),('overloadcore_coupler_body_led',[.12,1.28,-.25],[.21,1.44,-.24],'energy')]
 for sign,leg in [(1,'left_leg'),(-1,'right_leg')]:
  x=sign*.125
  for i,(a,b) in enumerate([([x-.14,.66,-.19],[x+.14,.70,-.14]),([x-.14,.66,.14],[x+.14,.70,.19]),([x-.14,.66,-.14],[x-.1,.70,.14]),([x+.1,.66,-.14],[x+.14,.70,.14])]):objects.append((f'overloadcore_magnetic_{leg}_ring{i}',a,b,'energy'))
 lines=['# Original OverloadCore equipment attachments, generated geometry','mtllib equipment_modules.mtl'];index=1
 for name,a,b,mat in objects:
  lines += ['o '+name,'usemtl '+mat]
  for side,verts in face_vertices(a,b).items():
   for x,y,z in verts:lines.append(f'v {x} {y} {z}')
   for u,v in [(0,0),(1,0),(1,1),(0,1)]:lines.append(f'vt {u} {v}')
   for _ in verts:lines.append('vn '+' '.join(map(str,NORMALS[side])))
   lines.append('f '+' '.join(f'{j}/{j}/{j}' for j in range(index,index+4)));index+=4
 out=RES/'assets/overloadcore/models/entity';out.mkdir(parents=True,exist_ok=True)
 (out/'equipment_modules.obj').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 (out/'equipment_modules.mtl').write_text('newmtl metal\nmap_Kd overloadcore:item/gear_alloy\nnewmtl energy\nmap_Kd overloadcore:item/gear_circuit\n',encoding='utf-8')
def training(write):
 textures={'side':'overloadcore:item/gear_alloy','front':'overloadcore:item/gear_alloy','top':'overloadcore:item/gear_graphite','front_active':'overloadcore:item/gear_circuit','particle':'overloadcore:item/gear_alloy'}
 def cube(lo,hi,tex):return {'from':lo,'to':hi,'faces':{side:{'texture':'#'+tex,'uv':[0,0,16,16]} for side in NORMALS}}
 for active in [False,True]:
  elements=[cube([0,0,0],[16,8,16],'side'),cube([1,8,1],[15,9,15],'top'),cube([4,9.02,4],[12,9.07,12],'front_active' if active else 'front'),cube([4,3,-.03],[12,5,0],'front_active' if active else 'top')]
  write('assets/overloadcore/models/block/holographic_projector'+('_active' if active else '')+'.json',{'parent':'minecraft:block/block','textures':textures,'elements':elements})
 variants={}
 for direction,y in [('north',0),('east',90),('south',180),('west',270)]:
  for active in [False,True]:variants[f'active={str(active).lower()},facing={direction}']={'model':'overloadcore:block/holographic_projector'+('_active' if active else ''),'y':y}
 write('assets/overloadcore/blockstates/holographic_projector.json',{'variants':variants})
 write('assets/overloadcore/models/item/holographic_projector.json',{'parent':'overloadcore:block/holographic_projector'})
 write('data/overloadcore/loot_table/blocks/holographic_projector.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'overloadcore:holographic_projector','functions':[{'function':'minecraft:copy_components','source':'block_entity','include':['mekanism:energy','mekanism:owner','mekanism:security','overloadcore:training']}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
 write('data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':['overloadcore:holographic_projector']})
 write('data/overloadcore/recipe/holographic_projector.json',{'type':'minecraft:crafting_shaped','pattern':['AGA','CSC','AEA'],'key':{k:{'item':v} for k,v in {'A':'mekanism:alloy_reinforced','G':'minecraft:glass','C':'mekanism:elite_control_circuit','S':'mekanism:steel_casing','E':'mekanism:energy_tablet'}.items()},'result':{'id':'overloadcore:holographic_projector'}})

def black_hole(write):
 def box(lo,hi,tex):
  # Reuse the four original 16px material tiles; no raster repainting.
  return {'from':lo,'to':hi,'faces':{side:{'texture':'#'+tex,'uv':[0,0,16,16]} for side in NORMALS}}
 parts=[
  ([5,8,1],[11,14,12],'dark'),([4,9,2],[5,13,11],'metal'),([11,9,2],[12,13,11],'metal'),
  ([5.5,8.5,12],[10.5,13.5,15],'metal'),([6.5,1,7],[9.5,8,10],'grip'),([6,1,6.5],[10,2.5,10.5],'metal'),
  ([5.5,6,3],[10.5,8,9],'dark'),([6.5,3.5,2.5],[9.5,6,4],'metal'),([6.5,2.5,3.5],[9.5,3.5,7],'metal'),
  ([3,7,-7],[5,15,2],'metal'),([11,7,-7],[13,15,2],'metal'),([5,7,-7],[11,9,2],'metal'),([5,13,-7],[11,15,2],'metal'),
  ([5,9,-3],[11,13,-2],'dark'),([6,10,-3.1],[10,12,-3],'energy'),
  ([2.5,9,-5],[3,13,1],'dark'),([13,9,-5],[13.5,13,1],'dark'),
  ([3.5,15,-5],[5,16,0],'dark'),([11,15,-5],[12.5,16,0],'dark'),
  ([6,14,4],[10,15,10],'metal'),([7,15,5],[9,17,7],'dark'),([7.3,15.4,4.95],[8.7,16.5,5],'energy'),
  ([3.9,10.4,4],[4,11.6,9],'energy'),([12,10.4,4],[12.1,11.6,9],'energy'),
 ]
 display={
  'gui':{'rotation':[25,135,0],'translation':[0,-1,0],'scale':[.67,.67,.67]},
  'ground':{'translation':[0,4,0],'scale':[.45,.45,.45]},
  'fixed':{'rotation':[0,90,0],'scale':[.65,.65,.65]},
 }
 for hand in ['right','left']:
  display['firstperson_'+hand+'hand']={'rotation':[0,0,0],'translation':[0,2.72,-.68],'scale':[.68,.68,.68]}
  display['thirdperson_'+hand+'hand']={'rotation':[90,0,0],'translation':[0,.7,2.4],'scale':[.7,.7,.7]}
 write('assets/overloadcore/models/item/black_hole_launcher.json',{'gui_light':'side','ambientocclusion':False,'textures':{k:'overloadcore:item/'+v for k,v in TEXTURES.items()},'display':display,'elements':[box(*p) for p in parts]})
 write('data/overloadcore/recipe/black_hole_launcher.json',{'type':'minecraft:crafting_shaped','pattern':['ATA','CNC','AEA'],'key':{k:{'item':v} for k,v in {'A':'mekanism:alloy_atomic','T':'mekanism:teleportation_core','C':'mekanism:ultimate_control_circuit','N':'minecraft:nether_star','E':'mekanism:energy_tablet'}.items()},'result':{'id':'overloadcore:black_hole_launcher'}})
 write('data/overloadcore/damage_type/gravity.json',{'message_id':'overloadcore.gravity','scaling':'never','exhaustion':.1})

def generate(write):
 modules(write);combat(write);armor();training(write);black_hole(write)
 uniforms=[]
 for name in ['ModelViewMat','ProjMat']:uniforms.append({'name':name,'type':'matrix4x4','count':16,'values':[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]})
 for name,values,kind in [('ColorModulator',[1,1,1,1],'float'),('FogStart',[0],'float'),('FogEnd',[1000],'float'),('FogColor',[0,0,0,0],'float'),('FogShape',[0],'int')]:uniforms.append({'name':name,'type':kind,'count':len(values),'values':values})
 write('assets/overloadcore/shaders/core/gear_field.json',{'vertex':'overloadcore:gear_field','fragment':'overloadcore:gear_field','samplers':[],'uniforms':uniforms})
 write('assets/overloadcore/shaders/core/black_hole.json',{'vertex':'overloadcore:black_hole','fragment':'overloadcore:black_hole','samplers':[],'uniforms':uniforms})
 pairs={}
 for k,(z,e,zd,ed) in NAMES.items():pairs['module.overloadcore.'+k]=(z,e);pairs['description.overloadcore.'+k]=(zd,ed)
 for k,pair in {
  'tactical.guard_unavailable':('需要已启用的磁偏护盾单元和足够的胸甲电量','Enable a Magnetic Deflector Unit and charge the chestplate'),
  'tactical.swap_hint':('按 %s 快切战斗形态','Press %s to switch combat forms'),
  'tactical.guard_hint':('按住 %s 展开偏转护盾','Hold %s to deploy the deflection shield'),
  'tactical.guarding':('偏转护盾已展开','Deflection shield active'),
  'tactical.counter_ready':('精准格挡 · 反击就绪','Perfect guard · Counter ready'),
  'tactical.flux_ready':('磁通就绪 · 下一发额外贯穿','Flux ready · Next shot pierces one more target'),
  'training.state_0':('投影已关闭','Projection disabled'),
  'training.state_1':('电量不足，请连接供电','Insufficient energy; connect power'),
  'training.state_2':('上方需要两格空间','Clear two blocks above the projector'),
  'training.state_3':('训练投影运行中','Training projection active'),
  'training.state_4':('检查红石控制设置','Check redstone control'),
  'training.state_5':('投影未能展开，正在重试','Projection unavailable; retrying'),
  'training.ready':('投影就绪，等待命中','Projection ready; awaiting hits'),
  'training.aim':('全息训练靶','Holographic target'),
  'training.hit':('%s 伤害','%s damage'),
  'training.last_label':('本次伤害','Last damage'),
  'training.dps_label':('近5秒 DPS','Last 5s DPS'),
  'training.dps_hint':('最近5秒的总伤害除以5；停止攻击后逐渐回落。','Damage dealt in the last 5 seconds divided by 5. Falls off after attacks stop.'),
  'training.cumulative':('累计伤害：%s','Total damage: %s'),
  'training.hits':('命中次数：%s','Hits: %s'),
  'training.spent_hint':('统计命中本靶的工具耗能；投影运行耗能另计。','Tool energy used on hits to this target. Projector power is counted separately.'),
  'training.power':('投影储能：%s','Projector energy: %s'),
  'training.reset_done':('读数已重置','Readings reset'),
  'training.last':('本次伤害：%s','Last damage: %s'),
  'training.total':('累计：%s · 命中：%s','Total: %s · Hits: %s'),
  'training.dps':('近5秒 DPS：%s','Last 5s DPS: %s'),
  'training.spent':('工具耗能：%s','Tool energy spent: %s'),
  'training.start':('开启投影','Enable projection'),
  'training.stop':('关闭投影','Disable projection'),
  'training.reset':('重置读数','Reset readings'),
  'gear.energy':('储能：%s / %s','Stored energy: %s / %s'),
  'weapon.charge':('蓄力时间：%s 秒','Charge time: %s s'),
  'weapon.no_energy':('武器电量不足','Weapon energy too low'),
  'ward.capacitor_hud':('护命电容：%s FE','Ward capacitor: %s FE'),'hud.compensation':('磁荷补偿：-%s','Magnetic offset: -%s'),
 }.items():pairs['overloadcore.'+k]=pair
 pairs.update({'item.overloadcore.rail_lance':('Meka工具','Meka-Tool'),'item.overloadcore.thunder_blade':('Meka工具','Meka-Tool'),'death.attack.overloadcore.rail':('%s被磁轨弹贯穿','%s was pierced by a rail slug'),'death.attack.overloadcore.rail.player':('%s被%s的磁轨弹贯穿','%s was pierced by %s\'s rail slug')})
 pairs.update({
  'item.overloadcore.black_hole_launcher':('黑洞发射器','Black Hole Launcher'),
  'entity.overloadcore.black_hole':('微型黑洞','Micro Black Hole'),
  'death.attack.overloadcore.gravity':('%s坠入了奇点','%s fell into a singularity'),
  'death.attack.overloadcore.gravity.player':('%s坠入了%s的奇点','%s fell into %s\'s singularity'),
  'overloadcore.black_hole.flavor':('将群星的坟墓，封入一瞬雷霆。','Seal the grave of stars within a moment of thunder.'),
  'overloadcore.black_hole.use':('按住右键蓄力，松开发射；命中后牵引周围目标，不破坏方块。','Hold use to charge, then release. On impact, draws in nearby targets without breaking blocks.'),
  'overloadcore.black_hole.cost':('消耗：%s FE · 蓄力：%s 秒','Cost: %s FE · Charge: %s s'),
  'overloadcore.black_hole.active':('已有黑洞，请等待其消散','Wait for your existing black hole to dissipate'),
  'overloadcore.black_hole.charging':('奇点约束 · %s%%','Singularity containment · %s%%'),
  'overloadcore.black_hole.ready':('奇点就绪 · 松开发射','Singularity ready · Release to fire'),
  'key.overloadcore.combat_swap':('战斗形态快切','Quick-switch combat form'),
  'key.overloadcore.guard':('战术格挡（按住）','Tactical guard (hold)'),
  'block.overloadcore.holographic_projector':('全息靶场控制器','Holographic Range Controller'),
  'container.overloadcore.holographic_projector':('全息靶场','Holographic Range'),
  'description.overloadcore.holographic_projector':('通电后投射训练靶，记录伤害与工具耗能。','Projects a powered training target and records damage and tool energy use.'),
  'entity.overloadcore.training_target':('训练投影','Training Projection'),
  'module.overloadcore.combat_form':('战斗形态单元','Combat Form Unit'),
  'description.overloadcore.combat_form':('赋予Meka工具近战与远程形态；使用模式菜单切换，右键蓄力攻击。','Adds melee and ranged forms to the Meka-Tool. Select a form in the mode menu, then hold use to charge.'),
  'module.overloadcore.combat_mode':('战斗形态','Combat form'),
  'overloadcore.combat.form':('战斗形态','Combat form'),
  'overloadcore.combat.melee':('近战形态','Melee form'),
  'overloadcore.combat.ranged':('远程形态','Ranged form'),
  'overloadcore.combat.selected':('形态：%s','Form: %s'),
  'overloadcore.combat.melee_hint':('按住右键蓄力，松开斩出一道剑气。','Hold use to charge; release a blade of energy.'),
  'overloadcore.combat.ranged_hint':('按住右键蓄力，松开发射磁轨脉冲，仅消耗电量。','Hold use to charge; release a rail pulse using energy only.'),
  'overloadcore.combat.controls':('使用Mek模式菜单切换形态；潜行右键使用原工具功能。','Switch forms with the Mek mode menu; sneak-use retains the original tool action.'),
 })
 print('Generated native Meka-Tool combat module and MekaSuit attachments.')
 return pairs
