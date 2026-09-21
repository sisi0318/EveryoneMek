"""Runtime JSON for original factory artwork and server-synchronized assembled skins."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(p,v):
 t=ROOT/p;t.parent.mkdir(parents=True,exist_ok=True);t.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')

def cube(texture):
 return {'parent':'minecraft:block/cube_all','textures':{'all':'mekfactory:block/'+texture}}

def conversion_model(formed=False):
 # Reuse original red/blue port textures as adjacent half faces. No coincident surfaces or new bitmap.
 elements=[]
 for face,axis,position,u_axis in [('north',2,0,0),('south',2,16,0),('west',0,0,2),('east',0,16,2),('down',1,0,0),('up',1,16,0)]:
  for start,end,texture in [(0,8,'input'),(8,16,'output')]:
   lo=[0,0,0];hi=[16,16,16];lo[axis]=hi[axis]=position;lo[u_axis]=start;hi[u_axis]=end
   elements.append({'from':lo,'to':hi,'faces':{face:{'texture':'#'+texture,'cullface':face}}})
 prefix='formed_' if formed else ''
 return {'parent':'minecraft:block/block','textures':{'input':'mekfactory:block/'+prefix+'port_input','output':'mekfactory:block/'+prefix+'port_output','particle':'mekfactory:block/'+prefix+'port_input'},'elements':elements}

def window_model(texture):
 # Opaque frame rails leave a genuinely clear window; no fake transparent/checkerboard bitmap.
 elements=[]
 for axis in [1,0,2]:
  other=[i for i in range(3) if i!=axis]
  for a in [0,15]:
   for b in [0,15]:
    start=[0,0,0];end=[16,16,16]
    if axis!=1:start[axis]=1;end[axis]=15
    start[other[0]]=a;end[other[0]]=a+1;start[other[1]]=b;end[other[1]]=b+1
    elements.append({'from':start,'to':end,'faces':{side:{'texture':'#all'} for side in ['north','south','east','west','up','down']}})
 return {'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'all':'mekfactory:block/'+texture,'particle':'mekfactory:block/'+texture},'elements':elements}

for name in ['formed_panel','formed_port_input','formed_port_output','formed_cell','formed_provider']:
 write(Path(f'assets/mekfactory/models/block/{name}.json'),cube(name))
write(Path('assets/mekfactory/models/block/formed_glass.json'),window_model('formed_panel'))
write(Path('assets/mekfactory/models/block/formed_chemical_conversion.json'),conversion_model(True))
for facing in ['north','east','south','west']:
 for active in [False,True]:
  suffix='_active' if active else ''
  faces={side:{'texture':'#front' if side==facing else '#shell','cullface':side} for side in ['north','east','south','west','up','down']}
  write(Path(f'assets/mekfactory/models/block/formed_controller_{facing}{suffix}.json'),{
   'parent':'minecraft:block/block','textures':{'front':'mekfactory:block/controller_front'+suffix,'shell':'mekfactory:block/formed_panel','particle':'mekfactory:block/formed_panel'},
   'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
grades=['basic','advanced','elite','ultimate'];zhgrades=['初级','高级','精英','终极']
write(Path('pack.mcmeta'),{'pack':{'pack_format':34,'description':'Mek Factory'}})
write(Path('mekfactory.mixins.json'),{'required':True,'package':'dev.everyonemek.factory.mixin','compatibilityLevel':'JAVA_21','mixins':['StructureChangeMixin'],'injectors':{'defaultRequire':1}})
zh={'itemGroup.mekfactory':'并行矩阵工厂'};en={'itemGroup.mekfactory':'Mek Factory'}
blocks=[]
for index,g in enumerate(grades):
 for kind in ['controller','frame','port','chemical_conversion']:
  name=g+'_'+kind;blocks.append(name)
  zh['block.mekfactory.'+name]=zhgrades[index]+{'controller':'并行矩阵工厂','frame':'工厂框架','port':'工厂物料仓','chemical_conversion':'化学品转换仓'}[kind]
  en['block.mekfactory.'+name]=g.title()+' '+{'controller':'Parallel Matrix Factory','frame':'Factory Frame','port':'Factory Warehouse','chemical_conversion':'Chemical Conversion Hatch'}[kind]
  if kind=='controller':
   zh['container.mekfactory.'+name]=zh['block.mekfactory.'+name]
   en['container.mekfactory.'+name]=en['block.mekfactory.'+name]
   for active in [False,True]:
    suffix='_active' if active else ''
    tex='mekfactory:block/controller_front'+suffix
    write(Path(f'assets/mekfactory/models/block/{name}{suffix}.json'),{'parent':'minecraft:block/orientable','textures':{'front':tex,'side':'mekfactory:block/controller_side','top':'mekfactory:block/controller_top'}})
   variants={f'active={str(a).lower()},facing={d}':{'model':f'mekfactory:block/{name}'+('_active' if a else ''),'y':rot} for a in [False,True] for d,rot in [('north',0),('east',90),('south',180),('west',270)]}
  elif kind=='chemical_conversion':
   write(Path(f'assets/mekfactory/models/block/{name}.json'),conversion_model())
   variants={'':{'model':f'mekfactory:block/{name}'}}
  else:
   write(Path(f'assets/mekfactory/models/block/{name}.json'),cube('frame' if kind=='frame' else 'port_input'))
   variants={'':{'model':f'mekfactory:block/{name}'}}
   if kind=='port':
    write(Path(f'assets/mekfactory/models/block/{name}_output.json'),cube('port_output'))
    variants={'output=false':{'model':f'mekfactory:block/{name}'},'output=true':{'model':f'mekfactory:block/{name}_output'}}
  write(Path(f'assets/mekfactory/blockstates/{name}.json'),{'variants':variants})
for name,tex,z,e in [('casing','controller_side','工厂外壳','Factory Casing'),('glass','frame','工厂结构玻璃','Factory Structural Glass')]:
 blocks.append(name);zh['block.mekfactory.'+name]=z;en['block.mekfactory.'+name]=e
 write(Path(f'assets/mekfactory/models/block/{name}.json'),window_model(tex) if name=='glass' else cube(tex))
 write(Path(f'assets/mekfactory/blockstates/{name}.json'),{'variants':{'':{'model':f'mekfactory:block/{name}'}}})
for name in blocks:
 item_model={'parent':f'mekfactory:block/{name}'}
 if name.endswith('_port'):item_model['overrides']=[{'predicate':{'mekfactory:output':1},'model':f'mekfactory:block/{name}_output'}]
 write(Path(f'assets/mekfactory/models/item/{name}.json'),item_model)
 components=['mekfactory:factory_data','mekanism:items','mekanism:upgrades','mekanism:security','mekanism:owner','mekanism:redstone_control'] if name.endswith('controller') else ['mekfactory:port_data'] if name.endswith(('_port','_chemical_conversion')) else []
 functions=[{'function':'minecraft:copy_name','source':'block_entity'},{'function':'minecraft:copy_components','source':'block_entity','include':components}] if components else []
 write(Path(f'data/mekfactory/loot_table/blocks/{name}.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'mekfactory:'+name,'functions':functions}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(Path('data/minecraft/tags/block/mineable/pickaxe.json'),{'replace':False,'values':['mekfactory:'+n for n in blocks]})
for name in ['casing','glass']:
 write(Path(f'data/mekfactory/recipe/{name}.json'),{'type':'minecraft:crafting_shaped','pattern':['SSS','SCS','SSS'],'key':{'S':{'item':'mekanism:ingot_steel'},'C':{'item':'mekanism:steel_casing' if name=='casing' else 'mekanism:structural_glass'}},'result':{'id':'mekfactory:'+name,'count':8}})
for i,g in enumerate(grades):
 circuit='mekanism:'+g+'_control_circuit';alloy=['infused','reinforced','atomic','atomic'][i]
 for kind in ['frame','port','controller']:
  middle='mekfactory:casing' if i==0 or kind in ['controller','port'] else 'mekfactory:'+grades[i-1]+'_'+kind
  pattern={'frame':['ASA','SCS','ASA'],'port':['ACA','SHS','ACA'],'controller':['ACA','SMS','ACA']}[kind]
  keys={'A':{'item':'mekanism:alloy_'+alloy},'S':{'item':'mekanism:ingot_steel'},'C':{'item':circuit},'H':{'item':'minecraft:hopper'},'M':{'item':middle}}
  if kind=='frame':keys['C']={'item':middle}
  if kind=='port':keys['H']={'item':middle if i else 'minecraft:hopper'}
  used=set(''.join(pattern));keys={k:v for k,v in keys.items() if k in used}
  write(Path(f'data/mekfactory/recipe/{g}_{kind}.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':keys,'result':{'id':f'mekfactory:{g}_{kind}','count':4 if kind=='frame' and i==0 else 1}})
for i,g in enumerate(grades):
 write(Path(f'data/mekfactory/recipe/{g}_chemical_conversion.json'),{'type':'minecraft:crafting_shaped','pattern':['ACA','HSH','ARA'],'key':{'A':{'item':'mekanism:alloy_'+['infused','reinforced','atomic','atomic'][i]},'C':{'item':'mekanism:'+g+'_control_circuit'},'H':{'item':'minecraft:hopper'},'S':{'item':'mekfactory:casing'},'R':{'item':'mekanism:enriched_redstone'}},'result':{'id':f'mekfactory:{g}_chemical_conversion','count':1}})
pairs={
 'conversion_warehouse':('化学品转换仓','Chemical Conversion Hatch'),
 'conversion_hint':('工厂成型并启用后，将辅料转为化学品。','Converts auxiliary items into chemicals in a formed, enabled factory.'),
 'conversion_io':('物品输入，化学品输出。','Items in, chemicals out.'),
 'converter_face':('转换仓：%s · 固定输入/输出','Converters: %s · Fixed I/O'),
 'converter_limit':('最多八个化学品转换仓','Maximum eight chemical conversion hatches'),
 'conversion_status.0':('等待工厂成型','Structure required'),
 'conversion_status.1':('等待辅料','Awaiting items'),
 'conversion_status.2':('正在转换','Converting'),
 'conversion_status.3':('化学品已满','Chemical storage full'),
 'conversion_status.4':('已停机','Stopped'),
 'conversion_status.5':('等待红石','Awaiting redstone'),
 'input_warehouse':('输入仓','Input Warehouse'),'output_warehouse':('输出仓','Output Warehouse'),
 'legacy_stock':('旧版缓存','Legacy Stock'),'legacy_input':('旧版输入缓存','Legacy Input Stock'),'legacy_output':('旧版输出缓存','Legacy Output Stock'),
 'warehouse_slots':('容量：%s 格','Capacity: %s slots'),'page':('%s / %s','%s / %s'),
 'warehouse_amount':('数量：%s / %s','Amount: %s / %s'),
 'fluids':('流体','Fluids'),'chemicals':('化学品','Chemicals'),
 'tank_fluid_amount':('%s / %s mB','%s / %s mB'),'tank_chemical_amount':('%s / %s 单位','%s / %s units'),
 'current_recipe':('配方：%s','Recipe: %s'),'no_recipe':('空闲','Idle'),'recipe_progress':('进度：%s','Progress: %s'),
 'machine_lanes':('%s 台 × %s 线 = %s 并行','%s machines × %s lanes = %s parallel'),
 'frame_limit':('框架上限：%s','Frame limit: %s'),'available_parallel':('可用并行：%s','Available parallel: %s'),
 'parallel_label':('工作 / 可用','Working / Available'),'power_label':('当前耗能','Power usage'),
 'limit_step':('点击调整 1，Shift 调整 16','Click to adjust by 1; Shift by 16'),
 'batch_size':('批量 %s','Batch %s'),'job_page':('任务 %s / %s','Job %s / %s'),
 'tank_number':('储罐 %s','Tank %s'),
 'batch':('批量 %s · 任务 %s / %s','Batch %s · Job %s / %s'),'power_limit':('供能上限：%s FE/t','Power limit: %s FE/t'),
 'settings':('结构设置','Structure'),'resources':('资源缓存','Resources'),'empty':('空','Empty'),
 'port_config':('端口配置','Port Configuration'),
 'port_counts':('输入 %s · 输出 %s','Input %s / Output %s'),
 'port_face_hint':('点击切换该面全部端口','Click to switch all ports on a face'),
 'port_corner_hint':('棱角端口的各外侧面共用模式。','A corner port shares its mode across exposed faces.'),
 'auto_eject_on':('自动弹出：开','Auto-eject: On'),'auto_eject_off':('自动弹出：关','Auto-eject: Off'),
 'dimensions':('尺寸 %s × %s × %s','Size %s × %s × %s'),
 'power_hint':('电力接入输入端口外侧','Connect power to an input port'),
 'port_power_hint':('独立存放物料，成型后从外侧接入物流和电力。','Stores its own materials. Connect transport and power to outer faces after forming.'),
 'port_mode_hint':('潜行持配置器右键切换输入与输出。','Sneak-use a Configurator to switch input and output.'),
 'dimension.0':('宽 %s','Width %s'),'dimension.1':('高 %s','Height %s'),'dimension.2':('深 %s','Depth %s'),
 'template':('主机器','Machine'),'input':('输入 · %s','Input · %s'),'output_page':('输出 · %s','Output · %s'),'pause':('停机','Stop'),'resume':('启用','Run'),'limit':('设置上限 %s','Limit %s'),
 'resource_view.0':('输入流体','Input fluids'),'resource_view.1':('输入化学品','Input chemicals'),'resource_view.2':('输出流体','Output fluids'),'resource_view.3':('输出化学品','Output chemicals'),
 'parallel':('工作 %s / %s','Working %s / %s'),'power':('%s FE/t','%s FE/t'),
 'preview':('结构预览','Preview'),'build':('一键搭建','Build'),'rotary_gas':('冷凝模式','Condense'),'rotary_fluid':('气化模式','Decondense'),
 'unlinked':('尚未连接工厂主控','Not linked to a factory'), 'port_output':('端口：输出','Port: output'),'port_input':('端口：输入','Port: input'),
 'structure':('检查工厂结构','Check factory structure'),'frame':('至少需要一个工厂框架','At least one factory frame required'),
 'shell':('外壳缺失或存在多个主控','Missing shell or multiple controllers'),'interior':('内部只能放感应元件和供应器','Only induction cells/providers allowed inside'),
 'tier':('框架等级不足以支撑当前尺寸','Frame tier too low for this size'),'unloaded':('等待结构区块加载','Waiting for structure chunks'),
 'occupied':('部件已属于其他结构','Part belongs to another structure'),'ports':('至少需要一个输入端口和输出端口','At least one input and output port required'),
 'port_limit':('每个方向最多八个端口','Maximum eight ports per direction'),'induction':('需要感应元件和感应供应器','Induction cell and provider required'),
 'ready':('结构完整','Structure formed'),'idle':('等待工作','Idle'),'working':('正在加工','Processing'),'materials':('等待配方材料','Waiting for ingredients'),
 'machine':('放入支持的主机器','Insert a supported machine'),'template_not_empty':('请先清空主机器内的物料','Empty the machine inventory and tanks first'),
 'paused':('已停机','Stopped'),'redstone':('等待红石条件','Waiting for redstone'),'draining':('正在完成在制任务','Finishing queued work'),'conditions':('加工条件不满足','Processing conditions not met'),'energy':('能量或供应器吞吐不足','Insufficient energy or provider throughput'),
 'secondary':('等待加工所需的化学品','Waiting for process chemicals'),
 'output':('等待产物空间','Waiting for output space'),'output_or_energy':('检查产物空间和供能','Check output space and power'),
 'build_small':('结构至少需要 3 × 3 × 3','Structure must be at least 3 x 3 x 3'),'build_complete':('工厂结构已完成','Factory structure complete'),
 'build_blocked':('无法在 %s 放置，请清理或检查权限','Cannot place at %s; check space and permissions'),'build_missing':('缺少 %s，蓝图需要 %s 个','Missing %s; blueprint requires %s'),
}
for k,(z,e) in pairs.items():zh['mekfactory.'+k]=z;en['mekfactory.'+k]=e
write(Path('assets/mekfactory/lang/zh_cn.json'),zh);write(Path('assets/mekfactory/lang/en_us.json'),en)
print('Generated',len(blocks),'factory block resources')
