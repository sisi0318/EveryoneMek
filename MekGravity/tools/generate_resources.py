"""Generate models, loot, recipes and paired language resources for the gravity reactor."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
def cube(texture):return {'parent':'minecraft:block/cube_all','textures':{'all':'mekgravity:block/'+texture}}
def orient(front,top,side):return {'parent':'minecraft:block/orientable','textures':{'front':'mekgravity:block/'+front,'top':'mekgravity:block/'+top,'side':'mekgravity:block/'+side}}
faces=['north','east','south','west','up','down']
def box(lo,hi,texture,omit=()):return {'from':lo,'to':hi,'faces':{f:{'texture':texture} for f in faces if f not in omit}}
def glass_model(elements):
    return {'parent':'minecraft:block/block','render_type':'minecraft:cutout','ambientocclusion':False,'textures':{'all':'mekgravity:block/frame','particle':'mekgravity:block/frame'},'elements':elements}
def glass_parts(face):
    # Four edge strips, then four corners: a-/b-, a-/b+, a+/b-, a+/b+.
    # Separate rectangles share edges but never overlap; bounds also supply the correct mirrored UVs.
    axis=0 if face in ['east','west'] else 1 if face in ['up','down'] else 2
    a,b=(2,1) if axis==0 else (0,2) if axis==1 else (0,1)
    bounds=[(0,1,1,15),(15,16,1,15),(1,15,0,1),(1,15,15,16)]+[(u,u+1,v,v+1) for u in [0,15] for v in [0,15]]
    result=[]
    for amin,amax,bmin,bmax in bounds:
        lo=[0,0,0];hi=[0,0,0];lo[axis]=hi[axis]=16 if face in ['east','up','south'] else 0
        lo[a],hi[a],lo[b],hi[b]=amin,amax,bmin,bmax
        result.append({'from':lo,'to':hi,'faces':{face:{'texture':'#all','cullface':face}}})
    return result
def glass():
    elements=[]
    for face in faces:
        parts=glass_parts(face);elements.extend(parts)
        for i,part in enumerate(parts):write(Path(f'assets/mekgravity/models/block/glass/{face}_{i}.json'),glass_model([part]))
    return glass_model(elements)
def core(active=False):
    # Compact housing and six raised containment collars. Hidden backs are omitted;
    # collars touch the housing without intersecting volumes or coplanar visible faces.
    body=box([3,3,3],[13,13,13],'#side')
    for face in body['faces'].values():face['uv']=[0,0,16,16]
    elements=[body]
    for face,axis,positive,opposite in [('north',2,False,'south'),('south',2,True,'north'),('west',0,False,'east'),('east',0,True,'west'),('down',1,False,'up'),('up',1,True,'down')]:
        lo=[4,4,4];hi=[12,12,12];lo[axis],hi[axis]=(13,14) if positive else (2,3)
        collar=box(lo,hi,'#side',(opposite,))
        for surface in collar['faces'].values():surface['uv']=[0,0,16,2]
        collar['faces'][face]={'texture':'#top' if axis==1 else '#front','uv':[0,0,16,16]}
        elements.append(collar)
    return {'parent':'minecraft:block/block','ambientocclusion':False,'textures':{'front':'mekgravity:block/core_front_active' if active else 'mekgravity:block/core_front','top':'mekgravity:block/core_top','side':'mekgravity:block/core_side','particle':'mekgravity:block/core_side'},'elements':elements}
write(Path('pack.mcmeta'),{'pack':{'pack_format':34,'description':'Mek Gravity'}})
write(Path('mekgravity.mixins.json'),{'required':True,'package':'dev.everyonemek.gravity.mixin','compatibilityLevel':'JAVA_21','mixins':['StructureChangeMixin'],'injectors':{'defaultRequire':1}})
names=['reactor','frame','casing','glass','fuel','coolant','energy','core']+[g+'_coil' for g in ['basic','advanced','elite','ultimate']]
for name in names:
    variants={'':{'model':'mekgravity:block/'+name}}
    if name=='reactor':
        variants={}
        for assembled in [False,True]:
            for active in [False,True]:
                model='reactor'+('_formed' if assembled else '')+('_active' if active else '')
                write(Path(f'assets/mekgravity/models/block/{model}.json'),orient('controller_front_active' if active else 'controller_front','assembled_frame' if assembled else 'controller_top','assembled_panel' if assembled else 'controller_side'))
                for face,rotation in [('north',0),('east',90),('south',180),('west',270)]:variants[f'active={str(active).lower()},facing={face},formed={str(assembled).lower()}']={'model':'mekgravity:block/'+model,'y':rotation}
    elif name.endswith('_coil'):
        variants={}
        for assembled in [False,True]:
            for active in [False,True]:
                model=name+('_formed' if assembled else '')+('_active' if active else '')
                write(Path(f'assets/mekgravity/models/block/{model}.json'),orient('coil_front_active' if active else 'coil_front','assembled_frame' if assembled else 'coil_top','assembled_panel' if assembled else 'coil_side'))
                for face,rx,ry in [('north',0,0),('east',0,90),('south',0,180),('west',0,270),('up',270,0),('down',90,0)]:variants[f'active={str(active).lower()},facing={face},formed={str(assembled).lower()}']={'model':'mekgravity:block/'+model,'x':rx,'y':ry}
    elif name in ['coolant','energy']:
        variants={}
        for assembled in [False,True]:
            for output in [False,True]:
                texture='port_output' if output else 'port_input';model=name+('_formed' if assembled else '')+('_output' if output else '')
                write(Path(f'assets/mekgravity/models/block/{model}.json'),cube('assembled_'+texture if assembled else texture if name=='coolant' else 'formed_'+texture))
                variants[f'formed={str(assembled).lower()},output={str(output).lower()}']={'model':'mekgravity:block/'+model}
    elif name in ['frame','casing','fuel']:
        for assembled in [False,True]:
            model=name+('_formed' if assembled else '')
            texture=({'frame':'assembled_frame','casing':'assembled_panel','fuel':'assembled_port_input'} if assembled else {'frame':'frame','casing':'panel','fuel':'formed_port_input'})[name]
            write(Path(f'assets/mekgravity/models/block/{model}.json'),cube(texture))
        variants={f'formed={str(a).lower()}':{'model':'mekgravity:block/'+name+('_formed' if a else '')} for a in [False,True]}
    elif name=='core':
        for active in [False,True]:write(Path('assets/mekgravity/models/block/core'+('_active' if active else '')+'.json'),core(active))
        variants={f'active={str(active).lower()}':{'model':'mekgravity:block/core'+('_active' if active else '')} for active in [False,True]}
    else:write(Path(f'assets/mekgravity/models/block/{name}.json'),glass())
    write(Path(f'assets/mekgravity/blockstates/{name}.json'),{'variants':variants})
    model={'parent':'mekgravity:block/'+name}
    if name=='core':model['display']={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[0.85,0.85,0.85]},'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[0.5,0.5,0.5]}}
    if name in ['coolant','energy']:model['overrides']=[{'predicate':{'mekgravity:output':1},'model':'mekgravity:block/'+name+'_output'}]
    write(Path(f'assets/mekgravity/models/item/{name}.json'),model)
    components=['mekgravity:reactor_data','mekanism:security','mekanism:owner','mekanism:redstone_control'] if name=='reactor' else ['mekgravity:fuel_stock']
    write(Path(f'data/mekgravity/loot_table/blocks/{name}.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'mekgravity:'+name,'functions':[{'function':'minecraft:copy_components','source':'block_entity','include':components}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(Path('data/minecraft/tags/block/mineable/pickaxe.json'),{'replace':False,'values':['mekgravity:'+n for n in names]})
write(Path('assets/mekgravity/models/item/dense_fuel_pellet.json'),{'parent':'minecraft:item/generated','textures':{'layer0':'mekgravity:item/dense_fuel_pellet'}})
def recipe(name,pattern,key,count=1):
    used=set(''.join(pattern))-{ ' '};write(Path(f'data/mekgravity/recipe/{name}.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in key.items() if k in used},'result':{'id':'mekgravity:'+name,'count':count}})
recipe('frame',['SRS','SPS','SRS'],{'S':'mekanism:ingot_steel','R':'mekanism:ingot_refined_obsidian','P':'mekanism:pellet_polonium'},8)
recipe('casing',['SSS','SOS','SSS'],{'S':'mekanism:ingot_steel','O':'mekanism:ingot_osmium'},8)
recipe('glass',['SGS','G G','SGS'],{'S':'mekanism:ingot_steel','G':'mekanismgenerators:reactor_glass'},8)
recipe('core',['AAA','ASA','AAA'],{'A':'mekanism:pellet_antimatter','S':'mekanism:sps_casing'})
recipe('reactor',['ACA','OSO','APA'],{'A':'mekanism:alloy_atomic','C':'mekanism:ultimate_control_circuit','O':'mekanism:ingot_refined_obsidian','S':'mekgravity:casing','P':'mekanism:pellet_polonium'})
for name,center in [('fuel','minecraft:hopper'),('coolant','mekanism:ingot_osmium'),('energy','mekanism:energy_tablet')]:recipe(name,['ACA','SXS','ACA'],{'A':'mekanism:alloy_atomic','C':'mekanism:elite_control_circuit','S':'mekgravity:casing','X':center})
for i,g in enumerate(['basic','advanced','elite','ultimate']):recipe(g+'_coil',['ACA','OPO','ACA'],{'A':'mekanism:alloy_atomic','C':'mekanism:'+['basic','advanced','elite','ultimate'][i]+'_control_circuit','O':'mekanism:ingot_refined_obsidian','P':'mekanism:pellet_polonium' if i==0 else 'mekgravity:'+['basic','advanced','elite'][i-1]+'_coil'})
write(Path('data/mekgravity/recipe/dense_fuel_pellet.json'),{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'mekanism:block_osmium'},{'item':'mekanism:block_lead'},{'item':'mekanism:block_refined_obsidian'},{'item':'mekanism:pellet_polonium'}],'result':{'id':'mekgravity:dense_fuel_pellet','count':8}})
write(Path('data/mekgravity/recipe/matter_fuel/dense.json'),{'type':'mekgravity:matter_fuel','ingredient':{'item':'mekgravity:dense_fuel_pellet'},'count':1,'energy':200_000_000_000})
zh={'itemGroup.mekgravity':'引力约束反应堆','item.mekgravity.dense_fuel_pellet':'致密燃料丸'};en={'itemGroup.mekgravity':'Mek Gravity','item.mekgravity.dense_fuel_pellet':'Dense Fuel Pellet'}
blocknames={'reactor':('引力约束反应堆','Gravitational Containment Reactor'),'frame':('引力堆框架','Gravity Reactor Frame'),'casing':('引力堆外壳','Gravity Reactor Casing'),'glass':('引力堆玻璃','Gravity Reactor Glass'),'fuel':('引力堆燃料仓','Gravity Reactor Fuel Hatch'),'coolant':('引力堆缓存回收接口','Gravity Reactor Recovery Port'),'energy':('引力堆能量接口','Gravity Reactor Energy Port'),'core':('引力核心','Gravitational Core')}
for i,g in enumerate(['basic','advanced','elite','ultimate']):blocknames[g+'_coil']=(['初级','高级','精英','终极'][i]+'约束线圈',g.title()+' Containment Coil')
for name,(z,e) in blocknames.items():zh['block.mekgravity.'+name]=z;en['block.mekgravity.'+name]=e
zh['container.mekgravity.reactor']=zh['block.mekgravity.reactor'];en['container.mekgravity.reactor']=en['block.mekgravity.reactor']
pairs={
'fuel_limited':('燃料不足，已降载','Fuel limited; reduced load'),'input_rate':('实际输入：%s/t','Actual input: %s/t'),'input_limit':('输入上限：%s/t','Input limit: %s/t'),'output_limit':('输出上限：%s/t','Output limit: %s/t'),'legacy_buffers':('旧版缓存','Legacy Buffers'),'legacy_recovery_hint':('停机后用回收接口和导管取回','Stop the reactor; recover through recovery ports and tubes'),
'syncing':('等待数据同步','Waiting for data'),'charge_stored':('已充入：%s','Charged: %s'),'charge_required':('启动所需：%s','Startup target: %s'),'excitation_hint':('向励磁输入口供电','Supply the excitation input'),
'adjust_parts':('部件齐全，请检查朝向与接口模式','Parts present; check facing and port modes'),
'fuel_port':('缺少燃料仓','Fuel hatch missing'),'cold_port':('缺少钠输入口','Sodium input missing'),'hot_port':('缺少热钠输出口','Hot sodium output missing'),'excitation_port':('缺少励磁输入口','Excitation input missing'),'output_port':('缺少发电输出口','Energy output missing'),
'error_at':('检查位置：%s','Check position: %s'),
'startup_config':('启动与备用电超过缓存容量','Startup and reserve exceed capacity'),
'structure':('等待结构成型','Structure incomplete'),'unloaded':('结构所在区块未加载','Structure chunk not loaded'),'occupied':('部件已接入其他结构','Part belongs to another structure'),'shell':('外壳缺失或放置错误','Invalid or missing casing'),'frame':('棱角需要引力堆框架','Edges require reactor frames'),'interior':('反应腔内需要留空','Clear the reaction chamber'),'core':('中心缺少引力核心','Gravitational core missing'),'coil':('约束线圈未装齐','Containment coils missing'),'coil_facing':('线圈需要朝向核心','Point the coil toward the core'),'ports':('缺少燃料、冷却或能量接口','Missing fuel, coolant or energy ports'),'port_limit':('最多 8 个燃料仓、32 个接口','Maximum 8 fuel hatches and 32 ports'),'ready':('结构完整','Structure formed'),
'stopped':('已停机','Stopped'),'redstone':('等待红石信号','Waiting for redstone'),'fuel_missing':('缺少燃料','Fuel required'),'cold_missing':('缺少冷却钠','Sodium required'),'hot_blocked':('热钠出口堵塞','Hot sodium output blocked'),'charging':('等待启动充能','Waiting for startup power'),'reserve_low':('约束备用电不足','Containment reserve low'),'full':('电缓存已满','Energy buffer full'),'coolant_invalid':('钠冷却数据无效','Invalid sodium cooling data'),'output_limited':('电缓存接近满载','Energy buffer nearly full'),'cold_limited':('冷却不足，已降载','Cooling limited; reduced load'),'running':('运行正常','Running'),
'input':('输入','Input'),'output':('输出','Output'),'facing':('朝向：%s','Facing: %s'),'unlinked':('尚未接入引力堆','Not connected to a reactor'),
'port_hint':('配置器潜行右键切换输入或输出。','Sneak-use a Configurator to switch input/output.'),'coil_hint':('朝向核心放置；六组线圈取最低等级。','Point at the core. The lowest of six coil tiers limits power.'),'fuel_hint':('放入致密燃料丸，或通过物品管道供料。','Insert dense fuel pellets or supply them through item pipes.'),'fuel_title':('燃料仓','Fuel Hatch'),
'net':('净发电：%s/t','Net generation: %s/t'),'exported':('实际输出：%s/t','Actual output: %s/t'),'gross':('毛发电：%s/t','Gross generation: %s/t'),'self_use':('约束自耗：%s/t','Containment: %s/t'),'field_ready':('约束场已建立','Containment established'),'field_waiting':('约束场未建立','Containment not established'),'fuel_remaining':('反应余量','Reaction reserve'),'fuel_energy':('燃料能量：%s','Fuel energy: %s'),
'load':('负载上限 %s%%','Load limit %s%%'),'stop':('停机','Stop'),'start':('启动','Start'),'energy_title':('能量明细','Energy'),'cooling_title':('旧版缓存','Legacy Buffers'),'structure_title':('结构设置','Structure'),'load_title':('负载设置','Load Limit'),'confirmed_load':('当前上限：%s%%','Current limit: %s%%'),'load_range':('范围 1–100；回车或勾号应用','Range 1–100; Enter or checkmark to apply'),
'stored':('储能：%s / %s','Stored: %s / %s'),'startup_cost':('首次启动：%s','Initial startup: %s'),'reserve':('备用电：%s','Protected reserve: %s'),'sodium':('钠','Sodium'),'hot_sodium':('热钠','Hot sodium'),'cold_amount':('钠：%s / %s mB','Sodium: %s / %s mB'),'hot_amount':('热钠：%s / %s mB','Hot sodium: %s / %s mB'),'heat_paid':('冷却带走：%s/t','Cooling heat: %s/t'),
'dimensions':('尺寸：7 × 7 × 7','Size: 7 × 7 × 7'),'coils':('线圈：%s / 6 · %s','Coils: %s / 6 · %s'),'output_ports':('发电输出口：%s','Energy outputs: %s'),'preview':('结构预览','Preview'),'build':('一键搭建','Build'),'eject_on':('自动弹出：开','Auto-eject: On'),'eject_off':('自动弹出：关','Auto-eject: Off'),
'build_small':('结构尺寸无效','Invalid dimensions'),'build_complete':('结构已完成','Structure complete'),'build_blocked':('无法在 %s 放置部件','Cannot place part at %s'),'build_missing':('缺少 %s × %s','Missing %s × %s'),'matter_fuel':('物质燃料','Matter Fuel'),'fuel_budget_hint':('发电与约束共用此能量','Supplies generation and containment')}
for i,(z,e) in enumerate(zip(['初级','高级','精英','终极'],['Basic','Advanced','Elite','Ultimate'])):pairs['grade.'+str(i)]=(z,e)
for direction,z in [('north','北'),('south','南'),('east','东'),('west','西'),('up','上'),('down','下')]:pairs['direction.'+direction]=(z,direction.title())
for key,(z,e) in pairs.items():zh['mekgravity.'+key]=z;en['mekgravity.'+key]=e
write(Path('assets/mekgravity/lang/zh_cn.json'),zh);write(Path('assets/mekgravity/lang/en_us.json'),en)
print('Generated gravity reactor block, recipe, loot and language resources.')
