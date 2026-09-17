"""Runtime JSON only. Uses Mekanism's installed textures by reference, without copying them."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(p,v):
 t=ROOT/p;t.parent.mkdir(parents=True,exist_ok=True);t.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
grades=['basic','advanced','elite','ultimate'];zhgrades=['初级','高级','精英','终极']
write(Path('pack.mcmeta'),{'pack':{'pack_format':34,'description':'Mek Factory'}})
write(Path('mekfactory.mixins.json'),{'required':True,'package':'dev.everyonemek.factory.mixin','compatibilityLevel':'JAVA_21','mixins':['StructureChangeMixin'],'injectors':{'defaultRequire':1}})
zh={'itemGroup.mekfactory':'并行矩阵工厂'};en={'itemGroup.mekfactory':'Mek Factory'}
blocks=[]
for index,g in enumerate(grades):
 for kind in ['controller','frame','port']:
  name=g+'_'+kind;blocks.append(name)
  zh['block.mekfactory.'+name]=zhgrades[index]+{'controller':'并行矩阵工厂','frame':'工厂框架','port':'通用工厂端口'}[kind]
  en['block.mekfactory.'+name]=g.title()+' '+{'controller':'Parallel Matrix Factory','frame':'Factory Frame','port':'Universal Factory Port'}[kind]
  if kind=='controller':
   zh['container.mekfactory.'+name]=zh['block.mekfactory.'+name]
   en['container.mekfactory.'+name]=en['block.mekfactory.'+name]
   for active in [False,True]:
    suffix='_active' if active else ''
    tex='mekanism:block/enrichment_chamber/front'+('_active' if active else '')
    write(Path(f'assets/mekfactory/models/block/{name}{suffix}.json'),{'parent':'minecraft:block/orientable','textures':{'front':tex,'side':'mekanism:block/steel_casing','top':'mekanism:block/steel_casing'}})
   variants={f'active={str(a).lower()},facing={d}':{'model':f'mekfactory:block/{name}'+('_active' if a else ''),'y':rot} for a in [False,True] for d,rot in [('north',0),('east',90),('south',180),('west',270)]}
  else:
   write(Path(f'assets/mekfactory/models/block/{name}.json'),{'parent':'mekanism:block/steel_casing' if kind=='frame' else 'mekanism:block/sps_port'})
   variants={'':{'model':f'mekfactory:block/{name}'}}
   if kind=='port':
    write(Path(f'assets/mekfactory/models/block/{name}_output.json'),{'parent':'mekanism:block/sps_port_output'})
    variants={'output=false':{'model':f'mekfactory:block/{name}'},'output=true':{'model':f'mekfactory:block/{name}_output'}}
  write(Path(f'assets/mekfactory/blockstates/{name}.json'),{'variants':variants})
for name,tex,z,e in [('casing','steel_casing','工厂外壳','Factory Casing'),('glass','structural_glass','工厂结构玻璃','Factory Structural Glass')]:
 blocks.append(name);zh['block.mekfactory.'+name]=z;en['block.mekfactory.'+name]=e
 write(Path(f'assets/mekfactory/models/block/{name}.json'),{'parent':'minecraft:block/cube_all','render_type':'minecraft:cutout','textures':{'all':'mekanism:block/'+tex}})
 write(Path(f'assets/mekfactory/blockstates/{name}.json'),{'variants':{'':{'model':f'mekfactory:block/{name}'}}})
for name in blocks:
 write(Path(f'assets/mekfactory/models/item/{name}.json'),{'parent':f'mekfactory:block/{name}'})
 functions=[{'function':'minecraft:copy_name','source':'block_entity'},{'function':'minecraft:copy_components','source':'block_entity','include':['mekfactory:factory_data','mekanism:items','mekanism:upgrades','mekanism:security','mekanism:owner','mekanism:redstone_control']}] if name.endswith('controller') else []
 write(Path(f'data/mekfactory/loot_table/blocks/{name}.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'mekfactory:'+name,'functions':functions}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(Path('data/minecraft/tags/block/mineable/pickaxe.json'),{'replace':False,'values':['mekfactory:'+n for n in blocks]})
for name in ['casing','glass']:
 write(Path(f'data/mekfactory/recipe/{name}.json'),{'type':'minecraft:crafting_shaped','pattern':['SSS','SCS','SSS'],'key':{'S':{'item':'mekanism:ingot_steel'},'C':{'item':'mekanism:steel_casing' if name=='casing' else 'mekanism:structural_glass'}},'result':{'id':'mekfactory:'+name,'count':8}})
for i,g in enumerate(grades):
 circuit='mekanism:'+g+'_control_circuit';alloy=['infused','reinforced','atomic','atomic'][i]
 for kind in ['frame','port','controller']:
  middle='mekfactory:casing' if i==0 or kind=='controller' else 'mekfactory:'+grades[i-1]+'_'+kind
  pattern={'frame':['ASA','SCS','ASA'],'port':['ACA','SHS','ACA'],'controller':['ACA','SMS','ACA']}[kind]
  keys={'A':{'item':'mekanism:alloy_'+alloy},'S':{'item':'mekanism:ingot_steel'},'C':{'item':circuit},'H':{'item':'minecraft:hopper'},'M':{'item':middle}}
  if kind=='frame':keys['C']={'item':middle}
  if kind=='port':keys['H']={'item':middle if i else 'minecraft:hopper'}
  used=set(''.join(pattern));keys={k:v for k,v in keys.items() if k in used}
  write(Path(f'data/mekfactory/recipe/{g}_{kind}.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':keys,'result':{'id':f'mekfactory:{g}_{kind}','count':4 if kind=='frame' and i==0 else 1}})
pairs={
 'settings':('结构设置','Structure'),'resources':('资源缓存','Resources'),'empty':('空','Empty'),
 'port_config':('端口配置','Port Configuration'),
 'port_counts':('输入 %s · 输出 %s','Input %s / Output %s'),
 'port_face_hint':('点击切换该面全部端口','Click to switch all ports on a face'),
 'port_corner_hint':('棱角端口的各外侧面共用模式。','A corner port shares its mode across exposed faces.'),
 'auto_eject_on':('自动弹出：开','Auto-eject: On'),'auto_eject_off':('自动弹出：关','Auto-eject: Off'),
 'dimensions':('尺寸 %s × %s × %s','Size %s × %s × %s'),
 'power_hint':('电力接入输入端口外侧','Connect power to an input port'),
 'port_power_hint':('输入模式下，从朝外的一面接入电力和物料。','In input mode, accepts power and materials from its outer face.'),
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
 'output':('等待产物空间','Waiting for output space'),'output_or_energy':('检查产物空间和供能','Check output space and power'),
 'build_small':('结构至少需要 3 × 3 × 3','Structure must be at least 3 x 3 x 3'),'build_complete':('工厂结构已完成','Factory structure complete'),
 'build_blocked':('无法在 %s 放置，请清理或检查权限','Cannot place at %s; check space and permissions'),'build_missing':('缺少 %s，蓝图需要 %s 个','Missing %s; blueprint requires %s'),
}
for k,(z,e) in pairs.items():zh['mekfactory.'+k]=z;en['mekfactory.'+k]=e
write(Path('assets/mekfactory/lang/zh_cn.json'),zh);write(Path('assets/mekfactory/lang/en_us.json'),en)
print('Generated',len(blocks),'factory block resources')
