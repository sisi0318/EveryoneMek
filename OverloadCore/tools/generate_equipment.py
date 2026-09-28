"""Mek-framed module icons and original textured equipment models. Bitmap atlases are exported separately."""
from pathlib import Path
import json, math

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NAMES={
 'phase_heat_sink':('相变热沉单元','Phase-Change Heat Sink Unit','消耗胸甲电量压制劫热；每级提高降温能力。','Consumes bodyarmor energy to suppress heat burden; cooling increases per level.'),
 'magnetic_compensation':('磁荷补偿单元','Magnetic Compensation Unit','消耗护腿电量抵消磁枷负荷；缺电时恢复原负荷。','Uses leggings energy to offset metal burden. Original burden returns without power.'),
 'ward_capacitor':('护命电容单元','Ward Capacitor Unit','储存护命电量，与附近设备合力逆命；每级默认容纳三次消耗。','Stores rescue energy and shares the cost with nearby machines. Holds three rescues per level by default.'),
 'afterguard_stabilizer':('余雷固相单元','Afterguard Stabilizer Unit','增强逆命后的次数盾；不增加护命冷却。','Improves the post-rescue guard without adding a rescue cooldown.'),
 'residual_reservoir':('旧式余雷蓄能单元','Retired Residual Reservoir Unit','已停用的旧式单元。','This old module has been retired.'),
 'resonant_discharge':('谐振放电单元','Resonant Discharge Unit','命中后额外耗电，向附近敌人释放连锁电弧。','Uses additional energy after a hit to arc toward nearby hostile targets.'),
 'charge_accelerator':('充能加速单元','Charge Accelerator Unit','缩短武器蓄力时间。','Shortens weapon charge time.'),
 'rail_magazine':('磁轨弹仓单元','Rail Magazine Unit','每级增加四发弹仓容量。','Adds four magazine rounds per level.'),
 'rail_focus':('磁轨聚焦单元','Rail Focus Unit','每级提高十六格射程。','Adds sixteen blocks of range per level.'),
 'rail_piercing':('磁轨贯穿单元','Rail Piercing Unit','每级多贯穿一个目标，后续伤害递减；不能穿墙。','Pierces one additional target per level with diminishing damage. Does not pass through walls.'),
 'blade_field':('刃场扩展单元','Blade Field Unit','提高蓄力斩击的距离与伤害。','Increases the reach and damage of charged slashes.'),
}
TEXTURES={'metal':'gear_alloy','dark':'gear_graphite','grip':'gear_grip','energy':'gear_circuit','indicator':'gear_circuit'}
DISPLAY={'gui':{'rotation':[35,40,0],'scale':[.7,.7,.7]},'ground':{'translation':[0,3,0],'scale':[.3,.3,.3]},'fixed':{'scale':[.65,.65,.65]},'firstperson_righthand':{'rotation':[0,-90,20],'translation':[1,-2,1],'scale':[.55,.55,.55]},'firstperson_lefthand':{'rotation':[0,90,-20],'translation':[1,-2,1],'scale':[.55,.55,.55]},'thirdperson_righthand':{'rotation':[0,-90,0],'translation':[0,2,1],'scale':[.5,.5,.5]},'thirdperson_lefthand':{'rotation':[0,90,0],'translation':[0,2,1],'scale':[.5,.5,.5]}}
NORMALS={'west':(-1,0,0),'east':(1,0,0),'down':(0,-1,0),'up':(0,1,0),'north':(0,0,-1),'south':(0,0,1)}
def box(a,b,material='metal'):
 faces={}
 for side,normal in NORMALS.items():
  axis=next(i for i,n in enumerate(normal) if n);u,v=[i for i in range(3) if i!=axis]
  w,h=max(1,min(16,b[u]-a[u])),max(1,min(16,b[v]-a[v]))
  uv=[0,0,w,h] if material not in ['energy','indicator'] else [6,0,9,max(1,h)]
  faces[side]={'texture':'#'+material,'uv':uv}
  if material in ['energy','indicator']:faces[side]['neoforge_data']={'block_light':10,'ambient_occlusion':False}
 return {'from':a,'to':b,'faces':faces}
def face_vertices(a,b):
 x,y,z=a;X,Y,Z=b
 return {'west':[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],'east':[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],'down':[(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)],'up':[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],'north':[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],'south':[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]}
def model(elements,display=DISPLAY):return {'parent':'minecraft:block/block','ambientocclusion':False,'gui_light':'front','textures':{**{k:'overloadcore:item/'+v for k,v in TEXTURES.items()},'particle':'overloadcore:item/gear_alloy'},'elements':elements,'display':display}
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
def modules(write):
 for name in ['residual_coupling_unit',*NAMES]:
  textures={'layer0':'mekanism:item/module_base'}
  if name!='residual_reservoir':textures['layer1']=f'overloadcore:item/module_{name}_panel'
  write(f'assets/overloadcore/models/item/module_{name}.json',{'parent':'minecraft:item/generated','textures':textures})
  if name in ['residual_coupling_unit','residual_reservoir']:continue
  special={'ward_capacitor':'minecraft:echo_shard','afterguard_stabilizer':'minecraft:nether_star','residual_reservoir':'mekanism:energy_tablet','resonant_discharge':'mekanism:alloy_atomic','phase_heat_sink':'mekanism:ingot_tin','magnetic_compensation':'mekanism:ingot_steel','charge_accelerator':'minecraft:redstone_block','rail_magazine':'minecraft:iron_ingot','rail_focus':'minecraft:amethyst_shard','rail_piercing':'mekanism:ingot_refined_obsidian','blade_field':'mekanism:ingot_refined_glowstone'}[name]
  write(f'data/overloadcore/recipe/module_{name}.json',{'type':'minecraft:crafting_shaped','pattern':['ASA','CBC','AEA'],'key':{k:{'item':v} for k,v in {'A':'mekanism:alloy_reinforced','S':special,'C':'mekanism:elite_control_circuit','B':'mekanism:module_base','E':'mekanism:energy_tablet'}.items()},'result':{'id':f'overloadcore:module_{name}'}})
def weapons(write):
 cases=[
  [([6,5,2],[10,9,20],'dark'),([5,6,-12],[6,8,6],'metal'),([10,6,-12],[11,8,6],'metal'),([6,6,-12],[10,8,-10],'metal'),([6,5,20],[10,9,24],'metal'),([6.5,0,10],[9.5,5,14],'grip'),([6,9,5],[10,10,12],'metal'),([7,10,6],[9,11,8],'dark'),([5.5,5.5,10],[6,8.5,17],'metal'),([10,5.5,10],[10.5,8.5,17],'metal')],
  [([7,0,7],[9,6,9],'grip'),([3,6,6],[13,8,10],'metal'),([7,8,7],[9,25,9],'dark'),([7.5,25,7],[8.5,26,9],'metal'),([6,0,6],[10,1,10],'metal'),([7.5,9,6.5],[8.5,24,7],'metal')]
 ]
 for z in [-8,-4,0]:cases[0] += [([4.5,5.7,z],[5,8.3,z+1],'dark'),([11,5.7,z],[11.5,8.3,z+1],'dark')]
 cases[0] += [([6.4,2.5,3],[9.6,5,7],'dark'),([6.2,2,3],[9.8,2.5,7],'metal'),([5.45,6,12],[5.55,8,15],'indicator')]
 for y in [10,13,16,19,22]:cases[1].append(([7.1,y,6.25],[8.9,y+.45,6.5],'metal'))
 for y in [2,3.5,5]:cases[1].append(([6.8,y,6.8],[9.2,y+.4,9.2],'metal'))
 cases[0] += [([7,6.3,-9.8],[9,7.7,2],'dark')]
 cases[1] += [([5.8,8,7],[7,25,9],'metal'),([6.5,25,7],[7.5,26,9],'metal')]
 lights=[ [([6.15,6.8,-9.5],[6.65,7.2,2],'energy'),([9.35,6.8,-9.5],[9.85,7.2,2],'energy'),([7.4,9.05,12],[8.6,9.35,17],'energy')], [([9.05,8.1,7.2],[9.65,25,8.8],'energy'),([8.8,25,7.2],[9.3,26,8.8],'energy'),([7.7,26,7.2],[8.6,27,8.8],'energy'),([7.8,27,7.3],[8.2,28,8.7],'energy')] ]
 arrays=[]
 for i,name in enumerate(['rail_lance','thunder_blade']):
  exterior=surface_boxes(cases[i]+lights[i]);elements=[];lit=[]
  for a,b,side,material in exterior:
   piece=box(a,b,material);piece['faces']={side:piece['faces'][side]};(lit if material=='energy' else elements).append(piece)
  display=json.loads(json.dumps(DISPLAY))
  if i==0:display['gui']={'rotation':[30,42,0],'scale':[.46,.46,.46]}
  else:display['gui']={'rotation':[0,0,-35],'translation':[0,-3,0],'scale':[.55,.55,.55]}
  # First person is grip-anchored by GearPose, without vanilla bow/spear transforms.
  # ItemTransform already mirrors Y/Z rotation and X translation for the left hand.
  for hand in ['righthand','lefthand']:
   display['firstperson_'+hand]={'rotation':[0,0,0],'scale':[1,1,1]}
   if i==0:display['thirdperson_'+hand]={'rotation':[0,0,0],'translation':[0,3.4,-2.72],'scale':[.68,.68,.68]}
   else:display['thirdperson_'+hand]={'rotation':[30,0,0],'translation':[0,5*.65*math.cos(math.pi/6),5*.65*.5],'scale':[.65,.65,.65]}
  write(f'assets/overloadcore/models/item/{name}_base.json',model(elements,display))
  write(f'assets/overloadcore/models/item/{name}_fallback.json',model(elements+lit,display))
  write(f'assets/overloadcore/models/item/{name}.json',{'parent':'builtin/entity','gui_light':'front','textures':{'particle':'overloadcore:item/gear_alloy'},'display':display})
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
   for u,v in [(0,0),(1,0),(1,1),(0,1)]:lines.append(f'vt {u} {v}')
   for _ in verts:lines.append('vn '+' '.join(map(str,NORMALS[side])))
   lines.append('f '+' '.join(f'{j}/{j}/{j}' for j in range(index,index+4)));index+=4
 out=RES/'assets/overloadcore/models/entity';out.mkdir(parents=True,exist_ok=True)
 (out/'equipment_modules.obj').write_text('\n'.join(lines)+'\n',encoding='utf-8')
 (out/'equipment_modules.mtl').write_text('newmtl metal\nmap_Kd overloadcore:item/gear_alloy\nnewmtl energy\nmap_Kd overloadcore:item/gear_circuit\n',encoding='utf-8')
def generate(write):
 modules(write);weapons(write);armor()
 write('overloadcore.enumextensions.json',{'entries':[{'enum':'net/minecraft/client/model/HumanoidModel$ArmPose','name':'OVERLOADCORE_'+name,'constructor':'(ZLnet/neoforged/neoforge/client/IArmPoseTransformer;)V','parameters':{'class':'dev/everyonemek/overloadcore/client/GearArmPoses','field':name}} for name in ['RAIL_HOLD','RAIL_SINGLE','BLADE_READY']]})
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
  'ward.capacitor_hud':('护命电容：%s FE','Ward capacitor: %s FE'),'hud.compensation':('磁荷补偿：-%s','Magnetic offset: -%s'),
 }.items():pairs['overloadcore.'+k]=pair
 pairs.update({'item.overloadcore.rail_lance':('磁轨长枪','Rail Lance'),'item.overloadcore.thunder_blade':('雷铸刃','Thunder-Forged Blade'),'death.attack.overloadcore.rail':('%s被磁轨弹贯穿','%s was pierced by a rail slug'),'death.attack.overloadcore.rail.player':('%s被%s的磁轨弹贯穿','%s was pierced by %s\'s rail slug')})
 print('Generated Mek-framed module icons, textured weapons and MekaSuit attachments.')
 return pairs
