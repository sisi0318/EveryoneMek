"""Original code-native gear models. Reuses the existing original 16px module palette; no bitmap drawing."""
from pathlib import Path
import json, math

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NAMES={
 'phase_heat_sink':('相变热沉单元','Phase-Change Heat Sink Unit','消耗胸甲电量压制劫热；每级提高降温能力。','Consumes bodyarmor energy to suppress heat burden; cooling increases per level.'),
 'magnetic_compensation':('磁荷补偿单元','Magnetic Compensation Unit','消耗护腿电量抵消磁枷负荷；缺电时恢复原负荷。','Uses leggings energy to offset metal burden. Original burden returns without power.'),
 'ward_capacitor':('护命电容单元','Ward Capacitor Unit','储存护命电量，与附近设备合力逆命；每级默认容纳三次消耗。','Stores rescue energy and shares the cost with nearby machines. Holds three rescues per level by default.'),
 'afterguard_stabilizer':('余雷固相单元','Afterguard Stabilizer Unit','增强逆命后的次数盾；不增加护命冷却。','Improves the post-rescue guard without adding a rescue cooldown.'),
 'residual_reservoir':('余雷蓄能单元','Residual Reservoir Unit','扩充过载核心的余雷存量；每级容量翻倍。','Doubles the Overloaded Core recovery capacity per level.'),
 'resonant_discharge':('谐振放电单元','Resonant Discharge Unit','命中后额外耗电，向附近敌人释放连锁电弧。','Uses additional energy after a hit to arc toward nearby hostile targets.'),
 'charge_accelerator':('充能加速单元','Charge Accelerator Unit','缩短武器蓄力时间。','Shortens weapon charge time.'),
 'rail_magazine':('磁轨弹仓单元','Rail Magazine Unit','每级增加四发弹仓容量。','Adds four magazine rounds per level.'),
 'rail_focus':('磁轨聚焦单元','Rail Focus Unit','每级提高十六格射程。','Adds sixteen blocks of range per level.'),
 'rail_piercing':('磁轨贯穿单元','Rail Piercing Unit','每级多贯穿一个目标，后续伤害递减；不能穿墙。','Pierces one additional target per level with diminishing damage. Does not pass through walls.'),
 'blade_field':('刃场扩展单元','Blade Field Unit','提高蓄力斩击的距离与伤害。','Increases the reach and damage of charged slashes.'),
}
UV={'metal':[2,1,3,2],'dark':[7,3,8,4],'energy':[4,7,5,8]}
DISPLAY={'gui':{'rotation':[35,40,0],'scale':[.7,.7,.7]},'ground':{'translation':[0,3,0],'scale':[.3,.3,.3]},'fixed':{'scale':[.65,.65,.65]},'firstperson_righthand':{'rotation':[0,-90,20],'translation':[1,-2,1],'scale':[.55,.55,.55]},'firstperson_lefthand':{'rotation':[0,90,-20],'translation':[1,-2,1],'scale':[.55,.55,.55]},'thirdperson_righthand':{'rotation':[0,-90,0],'translation':[0,2,1],'scale':[.5,.5,.5]},'thirdperson_lefthand':{'rotation':[0,90,0],'translation':[0,2,1],'scale':[.5,.5,.5]}}
NORMALS={'west':(-1,0,0),'east':(1,0,0),'down':(0,-1,0),'up':(0,1,0),'north':(0,0,-1),'south':(0,0,1)}
def box(a,b,material='metal'):
 return {'from':a,'to':b,'faces':{side:{'texture':'#palette','uv':UV['metal' if material=='indicator' else material],**({'neoforge_data':{'color':'FF8DFFB5','block_light':10}} if material=='indicator' else {})} for side in NORMALS}}
def face_vertices(a,b):
 x,y,z=a;X,Y,Z=b
 return {'west':[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],'east':[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)],'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],'north':[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],'south':[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]}
def model(elements,display=DISPLAY):return {'parent':'minecraft:block/block','ambientocclusion':False,'gui_light':'front','textures':{'palette':'overloadcore:item/module_residual_coupling_unit','particle':'overloadcore:item/module_residual_coupling_unit'},'elements':elements,'display':display}
def surface_boxes(boxes):
 """Bake only exterior material rectangles, including across static/shader boundaries."""
 axes=[sorted({p[i] for a,b,m in boxes for p in [a,b]}) for i in range(3)]
 shape=[len(a)-1 for a in axes];cells={}
 for x in range(shape[0]):
  for y in range(shape[1]):
   for z in range(shape[2]):
    key=(x,y,z);mid=[(axes[i][v]+axes[i][v+1])/2 for i,v in enumerate(key)]
    for a,b,m in boxes:
     if all(a[i]<mid[i]<b[i] for i in range(3)):cells[key]=m
 result=[]
 for side,normal in NORMALS.items():
  axis=next(i for i,n in enumerate(normal) if n);sign=normal[axis];a,b=[i for i in range(3) if i!=axis]
  for plane in range(len(axes[axis])):
   grid={}
   for u in range(shape[a]):
    for v in range(shape[b]):
     inside=[0,0,0];inside[axis]=plane-1 if sign>0 else plane;inside[a]=u;inside[b]=v;outside=inside.copy();outside[axis]+=sign
     material=cells.get(tuple(inside))
     if material is not None and tuple(outside) not in cells:grid[u,v]=material
   while grid:
    (u,v),material=min(grid.items());width=height=1
    while grid.get((u+width,v))==material:width+=1
    while all(grid.get((u+i,v+height))==material for i in range(width)):height+=1
    for i in range(width):
     for j in range(height):del grid[u+i,v+j]
    lo=[0,0,0];hi=[0,0,0];lo[axis]=hi[axis]=axes[axis][plane];lo[a]=axes[a][u];hi[a]=axes[a][u+width];lo[b]=axes[b][v];hi[b]=axes[b][v+height]
    result.append((lo,hi,side,material))
 return result
def surfaces(elements):
 cubes=[]
 for e in elements:
  f=next(iter(e['faces'].values()));mat='indicator' if f.get('neoforge_data',{}).get('color') else next(k for k,uv in UV.items() if f['uv']==uv)
  cubes.append((e['from'],e['to'],mat))
 return [dict(box(a,b,m),faces={side:box(a,b,m)['faces'][side]}) for a,b,side,m in surface_boxes(cubes)]
def modules(write):
 for index,name in enumerate(NAMES):
  e=[box([1,1,1],[15,3,15],'dark'),box([1,3,1],[3,4,15]),box([13,3,1],[15,4,15]),box([3,3,1],[13,4,3]),box([3,3,13],[13,4,15])]
  if name=='phase_heat_sink':
   for z in [4,6,8,10,12]:e.append(box([4,3,z],[12,6,z+1], 'energy' if z==8 else 'metal'))
  elif name=='magnetic_compensation':e += [box([4,3,4],[6,6,10]),box([10,3,4],[12,6,10]),box([4,3,10],[12,6,12],'energy')]
  elif name=='ward_capacitor':
   for x in [4,9]:e += [box([x,3,4],[x+3,7,12]),box([x,7,7],[x+3,7.5,9],'energy')]
  elif name=='afterguard_stabilizer':e += [box([4,3,4],[12,4.5,12]),box([5,4.5,5],[11,6,10],'dark'),box([7,6,6],[9,6.5,10],'energy')]
  elif name=='residual_reservoir':
   for y in [3,5]:e += [box([4,y,4],[12,y+1,12]),box([5,y+1,5],[11,y+2,11],'energy')]
  elif name=='resonant_discharge':
   for x,z in [(4,4),(7,7),(10,10)]:e += [box([x,3,z],[x+2,5,z+2],'energy')]
   e += [box([5,3,5],[6,4,9]),box([6,3,8],[11,4,9])]
  elif name=='charge_accelerator':
   for i in range(3):e.append(box([4+i*3,3,4],[6+i*3,4+i,12],'energy' if i==2 else 'metal'))
  elif name=='rail_magazine':
   for x in [4,6,8,10]:e += [box([x,3,4],[x+1,5,11]),box([x,3,11],[x+1,4,12],'energy')]
  elif name=='rail_focus':e += [box([4,3,6],[6,6,10]),box([6,3,7],[10,5,9],'energy'),box([10,3,5],[12,7,11])]
  elif name=='rail_piercing':
   for i in range(3):e.append(box([4+i*3,3,6-i],[6+i*3,5,10+i],'energy' if i==2 else 'metal'))
  else:
   for i in range(4):e.append(box([4+i*2,3,4+i],[6+i*2,5,6+i],'energy'))
  e.append(box([7,4.01,13],[9,4.25,14.5],'indicator'))
  e=surfaces(e)
  write(f'assets/overloadcore/models/item/module_{name}.json',model(e,{'gui':{'rotation':[52,0,0],'scale':[.9,.9,.9]},'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.4,.4,.4]},'fixed':{'rotation':[90,0,0],'scale':[.8,.8,.8]}}))
  special={'ward_capacitor':'minecraft:echo_shard','afterguard_stabilizer':'minecraft:nether_star','residual_reservoir':'mekanism:energy_tablet','resonant_discharge':'mekanism:alloy_atomic','phase_heat_sink':'mekanism:ingot_tin','magnetic_compensation':'mekanism:ingot_steel','charge_accelerator':'minecraft:redstone_block','rail_magazine':'minecraft:iron_ingot','rail_focus':'minecraft:amethyst_shard','rail_piercing':'mekanism:ingot_refined_obsidian','blade_field':'mekanism:ingot_refined_glowstone'}[name]
  write(f'data/overloadcore/recipe/module_{name}.json',{'type':'minecraft:crafting_shaped','pattern':['ASA','CBC','AEA'],'key':{k:{'item':v} for k,v in {'A':'mekanism:alloy_reinforced','S':special,'C':'mekanism:elite_control_circuit','B':'mekanism:module_base','E':'mekanism:energy_tablet'}.items()},'result':{'id':f'overloadcore:module_{name}'}})
def weapons(write):
 cases=[
  [([6,5,2],[10,9,20],'dark'),([5,6,-12],[6,8,6],'metal'),([10,6,-12],[11,8,6],'metal'),([6,6,-12],[10,8,-10],'metal'),([6,5,20],[10,9,24],'metal'),([6.5,0,10],[9.5,5,14],'dark'),([6,9,5],[10,10,12],'metal'),([7,10,6],[9,11,8],'dark'),([5.5,5.5,10],[6,8.5,17],'metal'),([10,5.5,10],[10.5,8.5,17],'metal')],
  [([7,0,7],[9,6,9],'dark'),([3,6,6],[13,8,10],'metal'),([7,8,7],[9,25,9],'dark'),([7.5,25,7],[8.5,26,9],'metal'),([6,0,6],[10,1,10],'metal'),([7.5,9,6.5],[8.5,24,7],'metal')]
 ]
 for z in [-8,-4,0]:cases[0] += [([4.5,5.7,z],[5,8.3,z+1],'dark'),([11,5.7,z],[11.5,8.3,z+1],'dark')]
 cases[0] += [([6.4,2.5,3],[9.6,5,7],'dark'),([6.2,2,3],[9.8,2.5,7],'metal'),([5.45,6,12],[5.55,8,15],'indicator')]
 for y in [10,13,16,19,22]:cases[1].append(([7.1,y,6.25],[8.9,y+.45,6.5],'metal'))
 for y in [2,3.5,5]:cases[1].append(([6.8,y,6.8],[9.2,y+.4,9.2],'metal'))
 lights=[ [([6.25,6.2,-9.5],[9.75,7.8,4],'energy'),([6.5,9.05,12],[9.5,9.35,17],'energy')], [([5.5,8.1,7],[6.9,25,9],'energy'),([9.1,8.1,7],[10.5,25,9],'energy'),([6.2,25,7],[7.4,26,9],'energy'),([8.6,25,7],[9.8,26,9],'energy'),([7,26,7],[9,27,9],'energy'),([7.7,27,7.3],[8.3,28,8.7],'energy')] ]
 arrays=[]
 for i,name in enumerate(['rail_lance','thunder_blade']):
  exterior=surface_boxes(cases[i]+lights[i]);elements=[];lit=[]
  for a,b,side,material in exterior:
   piece=box(a,b,material);piece['faces']={side:piece['faces'][side]};(lit if material=='energy' else elements).append(piece)
  display=json.loads(json.dumps(DISPLAY))
  if i==0:
   for element in lit:
    for face in element['faces'].values():face['uv']=UV['metal'];face['neoforge_data']={'color':'FF6AEDD5','block_light':10}
  if i==0:display['gui']={'rotation':[30,42,0],'scale':[.46,.46,.46]}
  else:display['gui']={'rotation':[0,0,-35],'translation':[0,-3,0],'scale':[.55,.55,.55]};display['firstperson_righthand']={'rotation':[0,0,-15],'translation':[0,-4,0],'scale':[.7,.7,.7]}
  write(f'assets/overloadcore/models/item/{name}_base.json',model(elements,display))
  write(f'assets/overloadcore/models/item/{name}_fallback.json',model(elements+lit,display))
  write(f'assets/overloadcore/models/item/{name}.json',{'parent':'builtin/entity','gui_light':'front','textures':{'particle':'overloadcore:item/module_residual_coupling_unit'},'display':display})
  vertices=[]
  for a,b,side,material in exterior:
   if material=='energy':
    for p in face_vertices(a,b)[side]:vertices+= [v/16 for v in p]+list(NORMALS[side])
  arrays.append(vertices)
  ingredients={'A':'mekanism:alloy_atomic','P':'mekanism:pellet_polonium','E':'mekanism:energy_tablet','C':'mekanism:ultimate_control_circuit'}
  if i==0:ingredients['S']='mekanism:steel_casing'
  write(f'data/overloadcore/recipe/{name}.json',{'type':'minecraft:crafting_shaped','pattern':['APA','ECE','ASA' if i==0 else 'AEA'],'key':{k:{'item':v} for k,v in ingredients.items()},'result':{'id':'overloadcore:'+name}})
 text='package dev.everyonemek.overloadcore.client;\n/** Generated static glow surfaces; no per-frame geometry creation. */\npublic final class GearGlowMesh {\n'
 for i,v in enumerate(arrays):text+=f' public static final float[] {"RAIL" if i==0 else "BLADE"}={{'+','.join(f'{x:.5f}F' for x in v)+'};\n'
 text+=' private GearGlowMesh(){}\n}\n';(ROOT/'src/main/java/dev/everyonemek/overloadcore/client/GearGlowMesh.java').write_text(text,encoding='utf-8')
 write('data/overloadcore/tags/item/rail_ammunition.json',{'replace':False,'values':['minecraft:iron_nugget']})
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
   u,v=([.15625,.90625] if mat=='metal' else [.28125,.53125])
   for _ in verts:lines.append(f'vt {u} {v}')
   for _ in verts:lines.append('vn '+' '.join(map(str,NORMALS[side])))
   lines.append('f '+' '.join(f'{j}/{j}/{j}' for j in range(index,index+4)));index+=4
 out=RES/'assets/overloadcore/models/entity';out.mkdir(parents=True,exist_ok=True)
 (out/'equipment_modules.obj').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 (out/'equipment_modules.mtl').write_text('newmtl metal\nmap_Kd overloadcore:item/module_residual_coupling_unit\nnewmtl energy\nmap_Kd overloadcore:item/module_residual_coupling_unit\n',encoding='utf-8')
def generate(write):
 modules(write);weapons(write);armor()
 uniforms=[]
 for name in ['ModelViewMat','ProjMat']:uniforms.append({'name':name,'type':'matrix4x4','count':16,'values':[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]})
 for name,values,kind in [('ColorModulator',[1,1,1,1],'float'),('FogStart',[0],'float'),('FogEnd',[1000],'float'),('FogColor',[0,0,0,0],'float'),('FogShape',[0],'int')]:uniforms.append({'name':name,'type':kind,'count':len(values),'values':values})
 write('assets/overloadcore/shaders/core/gear_field.json',{'vertex':'overloadcore:gear_field','fragment':'overloadcore:gear_field','samplers':[],'uniforms':uniforms})
 pairs={}
 for k,(z,e,zd,ed) in NAMES.items():pairs['module.overloadcore.'+k]=(z,e);pairs['description.overloadcore.'+k]=(zd,ed)
 for k,pair in {
  'gear.energy':('储能：%s / %s','Stored energy: %s / %s'),
  'weapon.rail_hint':('按住右键蓄力，松开发射。潜行右键装填铁粒。','Hold use to charge; release to fire. Sneak-use to load iron nuggets.'),
  'weapon.blade_hint':('有电时强化挥砍；按住右键蓄力，松开释放刃场。','Powered strikes deal extra damage. Hold use, then release a charged blade field.'),
  'weapon.magazine':('弹仓：%s / %s','Magazine: %s / %s'), 'weapon.charge':('蓄力时间：%s 秒','Charge time: %s s'),
  'weapon.loaded':('已装填：%s / %s','Loaded: %s / %s'),'weapon.no_ammo':('缺少弹药，请携带铁粒','No ammunition; carry iron nuggets'),'weapon.no_energy':('武器电量不足','Weapon energy too low'),
  'service.title':('佩戴饰品改装','Worn Accessory Modification'),'service.tab':('饰','A'),'service.core':('过载核心','Overloaded Core'),'service.ward':('逆命雷印','Thunder Ward'),
  'service.chip':('模块','Module'),'service.action_0':('安装','Install'),'service.action_1':('拆卸','Remove'),'service.action_2':('启用/停用','Toggle'),'service.enable':('启用','Enable'),'service.disable':('停用','Disable'),
  'service.ready':('选择饰品与模块','Select an accessory and module'),'service.missing':('未佩戴该饰品','Accessory not equipped'),'service.no_module':('缺少对应模块','Matching module required'),
  'service.unsupported':('此饰品不支持该模块','Module not supported'),'service.maxed':('已达到安装上限','Installation limit reached'),'service.energy':('改装站能量不足','Station energy too low'),
  'service.space':('背包空间不足','Not enough inventory space'),'service.changed':('饰品已变化，请重新选择','Accessory changed; select it again'),
  'service.busy':('请先取出改装站中的装备','Remove the equipment from the station first'),'service.done':('改装完成','Modification complete'),'service.disabled':('改装站已停机','Station is disabled'),
  'service.power':('改装站：%s · 安装耗能：%s','Station: %s · Installation: %s'),
  'ward.capacitor_hud':('护命电容：%s FE','Ward capacitor: %s FE'),'hud.compensation':('磁荷补偿：-%s','Magnetic offset: -%s'),
 }.items():pairs['overloadcore.'+k]=pair
 pairs.update({'item.overloadcore.rail_lance':('磁轨长枪','Rail Lance'),'item.overloadcore.thunder_blade':('雷铸刃','Thunder-Forged Blade'),'death.attack.overloadcore.rail':('%s被磁轨弹贯穿','%s was pierced by a rail slug'),'death.attack.overloadcore.rail.player':('%s被%s的磁轨弹贯穿','%s was pierced by %s\'s rail slug')})
 print('Generated 11 distinct module models, two weapon models, glow meshes and MekaSuit attachments.')
 return pairs
