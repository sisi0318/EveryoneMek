"""Five optional machine models and recipes, built from existing original materials."""
import json
from generate_solar_resources import ROOT,RES,write,box,mechanical
from generate_coronal_resources import outer_surface
from runtime_geometry import turn

KINDS=['flare_captor','gravity_forge','core_tuner','gravity_node','stellar_observatory']
LABELS=[('耀斑捕获器','Flare Captor'),('引力锻造舱','Gravity Forge'),('核心调谐器','Core Tuner'),('引力物流节点','Gravity Logistics Node'),('恒星观测站','Stellar Observatory')]
def build():
    shapes=[]
    for index,name in enumerate(KINDS):
        variants={}
        for active in [False,True]:
            e=[box([0,0,0],[16,3,16],'#dark',[0,0,16,10]),box([1,0,1],[15,1,15],'#steel',[0,6,16,10])]
            if index==0:
                e += [box([2,3,3],[4,14,13],'#steel',[0,6,16,10]),box([12,3,3],[14,14,13],'#steel',[0,6,16,10]),box([4,11,10],[12,14,14],'#dark',[0,0,16,10]),box([5,3,5],[11,5,11],'#shell',[3,3,13,13])]
            elif index==1:
                e += [box([0,3,0],[3,16,16],'#dark',[0,0,16,10]),box([13,3,0],[16,16,16],'#dark',[0,0,16,10]),box([3,13,0],[13,16,16],'#shell',[3,3,13,13]),box([4,11,5],[12,13,13],'#steel',[0,6,16,10]),box([4,3,5],[12,5,13],'#steel',[0,6,16,10]),box([3,3,13],[13,13,16],'#dark',[0,11,16,16])]
            elif index==2:
                e += [box([1,3,7],[15,7,15],'#shell',[3,3,13,13]),box([2,7,9],[14,15,13],'#dark',[0,11,16,16]),box([1,7,9],[2,16,13],'#steel',[0,6,16,10]),box([14,7,9],[15,16,13],'#steel',[0,6,16,10])]
            elif index==3:
                # Enclosed Mek-style chassis with a recessed front projection window.
                e=[box([0,0,0],[16,2,16],'#shell',[3,3,13,13]),box([0,14,0],[16,16,16],'#shell',[3,3,13,13]),
                   box([0,2,0],[2,14,16],'#shell',[3,3,13,13]),box([14,2,0],[16,14,16],'#shell',[3,3,13,13]),box([2,2,3],[14,14,16],'#dark',[0,11,16,16]),
                   box([2,2,.5],[14,3,3],'#steel',[0,6,16,10]),box([2,13,.5],[14,14,3],'#steel',[0,6,16,10]),
                   box([2,3,.5],[3,13,3],'#steel',[0,6,16,10]),box([13,3,.5],[14,13,3],'#steel',[0,6,16,10])]
                for a,b in [([0,5,5],[1,11,11]),([15,5,5],[16,11,11]),([5,15,5],[11,16,11]),([5,0,5],[11,1,11]),([5,5,15],[11,11,16])]:
                    e.append(box(a,b,'#dark',[0,0,16,10]))
            else:
                e += [box([5,3,8],[11,9,13],'#steel',[0,6,16,10]),box([1,7,8],[15,16,12],'#dark',[0,11,16,16]),box([0,7,8],[1,16,12],'#shell',[3,3,13,13]),box([15,7,8],[16,16,12],'#shell',[3,3,13,13]),box([1,15,8],[15,16,12],'#steel',[0,6,16,10])]
            for x in [2,12]:
                light=box([x,1,-.02],[x+2,2,.1],'#lamp',[7,7,8,8]);
                if active:
                    for face in light['faces'].values():face['neoforge_data']={'block_light':12,'ambient_occlusion':False}
                e.append(light)
            model=mechanical(outer_surface(e));model['textures']['particle']='mekgravity:block/orb_inner';model['textures']['lamp']='mekgravity:block/sun_collector_active' if active else 'mekgravity:block/sun_collector'
            variant=name+('_active' if active else '')
            write(f'assets/mekgravity/models/block/{variant}.json',model)
            for facing,y in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={facing},active={str(active).lower()}']={'model':'mekgravity:block/'+variant,'y':y}
        shape_names=['CAPTOR','FORGE','TUNER','NODE','OBSERVATORY']
        for direction in ['north','east','south','west']:
            bounds=[]
            for element in e[:-2]:
                corners=[turn([x/16,y/16,z/16],direction) for x in (element['from'][0],element['to'][0]) for y in (element['from'][1],element['to'][1]) for z in (element['from'][2],element['to'][2])]
                lo=[min(p[i] for p in corners)*16 for i in range(3)];hi=[max(p[i] for p in corners)*16 for i in range(3)]
                bounds.append('Block.box('+','.join(str(round(v,4)) for v in lo+hi)+')')
            shapes.append('        SHAPES['+str(index)+']['+str(['north','east','south','west'].index(direction))+']=Shapes.or('+','.join(bounds)+');')
        write(f'assets/mekgravity/blockstates/{name}.json',{'variants':variants})
        write(f'assets/mekgravity/models/item/{name}.json',{'parent':'mekgravity:block/'+name})
        write(f'data/mekgravity/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'mekgravity:'+name,'functions':[{'function':'minecraft:copy_components','source':'block_entity','include':['mekanism:items','mekanism:owner','mekanism:security','mekanism:redstone_control','mekgravity:orbital_module','minecraft:custom_name']+(['mekanism:energy','mekanism:fluids','mekanism:chemicals','mekanism:side_config','mekanism:ejector'] if name=='gravity_node' else [])}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    java='package dev.everyonemek.gravity.expansion;\nimport net.minecraft.core.Direction;\nimport net.minecraft.world.level.block.Block;\nimport net.minecraft.world.phys.shapes.*;\n/** Generated by generate_module_resources.py, from the exact solid model boxes. */\npublic final class ModuleShapes {\n    private static final VoxelShape[][] SHAPES=new VoxelShape[5][4];\n    static{\n'+'\n'.join(shapes)+'\n    }\n    public static VoxelShape get(ModuleKind kind,Direction facing){int i=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};return SHAPES[kind.ordinal()][i];}\n    private ModuleShapes(){}\n}\n'
    (ROOT/'src/main/java/dev/everyonemek/gravity/expansion/ModuleShapes.java').write_text(java,encoding='utf-8')
    # Reuse pixel materials on small original solid models; no raster repainting.
    for name,e in {
        'field_linker':[box([6,1,6],[10,10,10],'#steel',[0,6,16,10]),box([3,10,6],[6,15,10],'#dark'),box([10,10,6],[13,15,10],'#dark'),box([6,11,7],[10,14,9],'#lamp',[7,7,8,8])],
        'stellar_alloy':[box([2,5,4],[14,9,12],'#steel',[0,6,16,10]),box([3,9,5],[13,10,11],'#dark'),box([4,9.9,6],[12,10.1,10],'#lamp',[7,7,8,8])]
    }.items():
        model=mechanical(outer_surface(e));model['textures']['lamp']='mekgravity:block/sun_collector_active';write(f'assets/mekgravity/models/item/{name}.json',model)
    from flare_cell_mesh import generate as generate_flare
    generate_flare(RES,write)
    write('assets/mekgravity/models/item/flare_cell.json',{'parent':'builtin/entity','gui_light':'front','textures':{'particle':'mekgravity:block/sun_collector_active'},'display':{'gui':{'rotation':[20,35,0],'scale':[1.1,1.1,1.1]},'ground':{'translation':[0,3,0],'scale':[.5,.5,.5]},'fixed':{'scale':[.8,.8,.8]},'firstperson_righthand':{'rotation':[0,-30,0],'translation':[0,2,0],'scale':[.8,.8,.8]},'firstperson_lefthand':{'rotation':[0,30,0],'translation':[0,2,0],'scale':[.8,.8,.8]},'thirdperson_righthand':{'translation':[0,2,0],'scale':[.6,.6,.6]},'thirdperson_lefthand':{'translation':[0,2,0],'scale':[.6,.6,.6]}}})
    flare_shader=json.loads((RES/'assets/mekgravity/shaders/core/stellar_surface.json').read_text(encoding='utf-8'));flare_shader['vertex']=flare_shader['fragment']='mekgravity:flare_cell';write('assets/mekgravity/shaders/core/flare_cell.json',flare_shader)
    tag=json.loads((RES/'data/minecraft/tags/block/mineable/pickaxe.json').read_text(encoding='utf-8'));tag['values']=sorted(set(tag['values']+['mekgravity:'+n for n in KINDS]));write('data/minecraft/tags/block/mineable/pickaxe.json',tag)
    def craft(name,pattern,items):write(f'data/mekgravity/recipe/{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in items.items()},'result':{'id':'mekgravity:'+name}})
    craft('field_linker',[' AC',' SB','S  '],{'A':'mekanism:alloy_atomic','B':'mekanism:teleportation_core','C':'mekanism:ultimate_control_circuit','S':'mekanism:ingot_steel'})
    for name,part,alloy in [('flare_captor','mekanism:laser_amplifier','mekanism:alloy_atomic'),('gravity_forge','mekanism:osmium_compressor','mekanism:alloy_atomic'),('core_tuner','mekanism:energy_tablet','mekgravity:stellar_alloy'),('gravity_node','mekanism:teleportation_core','mekgravity:stellar_alloy'),('stellar_observatory','minecraft:comparator','mekanism:alloy_atomic')]:
        craft(name,['ACA','SPS','ACA'],{'A':alloy,'C':'mekanism:ultimate_control_circuit','S':'mekanism:ingot_steel','P':part})
    for name,machine,inputs,result,count,ticks,energy in [
        ('flare','flare_captor',[('mekanism:alloy_atomic',4),('minecraft:quartz',4)],'mekgravity:flare_cell',1,20,50_000_000_000),
        ('stellar_alloy','gravity_forge',[('mekanism:alloy_atomic',4),('mekanism:hdpe_sheet',1),('mekgravity:flare_cell',1)],'mekgravity:stellar_alloy',4,20,2_000_000_000),
        ('stellar_matter','gravity_forge',[('mekgravity:flare_cell',1)],'mekgravity:compressed_stellar_matter',1,40,5_000_000_000)]:
        write(f'data/mekgravity/recipe/orbital/{name}.json',{'type':'mekgravity:orbital_processing','machine':machine,'inputs':[{'ingredient':{'item':item},'count':n} for item,n in inputs],'result':{'id':result,'count':count},'ticks':ticks,'energy':energy,'tier':0})
    shader=json.loads((RES/'assets/mekgravity/shaders/core/coronal_processing.json').read_text(encoding='utf-8'));shader['vertex']=shader['fragment']='mekgravity:orbital_module';write('assets/mekgravity/shaders/core/orbital_module.json',shader)
    snapshot=json.loads((RES/'assets/mekgravity/shaders/core/stellar_surface.json').read_text(encoding='utf-8'));snapshot['vertex']=snapshot['fragment']='mekgravity:node_snapshot';snapshot['samplers']=[{'name':'Sampler0'}];write('assets/mekgravity/shaders/core/node_snapshot.json',snapshot)
    words={
      'burst_hint':('核心运行且缓存未满，消耗1晶核。最长20秒升载，80秒后可再触发。','Requires a running core and buffer room. Uses 1 cell; up to 20s burst, 80s reuse delay.'),
      'tuning_0':('正常功率与燃料消耗','Normal power and fuel consumption'),'tuning_1':('发电80%，新批次加工速度×2','80% generation; new processing batches run twice as fast'),'tuning_2':('发电125%，每单位毛电额外消耗15%燃料','125% generation; 15% extra fuel per gross joule'),
      'unlinked':('请用链接器连接能量源','Connect a power source with the Field Linker'),'range':('链接超出范围或不在同一维度','Link out of range or in another dimension'),
      'unloaded':('等待目标区块加载','Waiting for target chunk'),'missing':('绑定的装置已移除','Bound device is missing'),'access':('没有使用目标装置的权限','Access to target denied'),
      'source_type':('捕获器需太阳，锻造舱需引力堆','Captor needs sun; forge needs gravity reactor'),'linked':('绑定完成','Link established'),
      'output_full':('输出缓存已满','Output buffer full'),'tier':('需要更高等级约束部件','Higher containment tier required'),'energy':('可用储能不足','Insufficient usable energy'),
      'ingredients':('等待原料','Waiting for ingredients'),'running':('正在加工','Processing'),'paused':('已暂停','Paused'),'cold':('源核心未运行','Source core is not running'),
      'peer_missing':('用链接器先点本节点，再点接收节点','Use the linker on this node, then the receiver'),'peer_paused':('接收节点已暂停','Receiving node is paused'),'transferring':('引力传输中','Transferring cargo'),
      'structure':('源结构不完整','Source structure incomplete'),'monitoring':('链接稳定','Link stable'),'receiving':('正在接收物料','Receiving cargo'),
      'select_source':('先右键引力堆或恒星主控','Select a gravity or stellar controller first'),
      'source_selected':('已记录能量源 %s','Source selected: %s'),'node_selected':('已记录接收节点 %s','Receiver selected: %s'),
      'link_failed':('绑定失败：检查类型、距离和权限','Link failed: check type, range and access'),
      'linker_hint':('对空气右键打开场域面板，拖线连接设备。','Use in air to open the Field Panel and connect devices.'),'selected':('已记录：%s · %s','Selected: %s · %s'),
      'progress':('进度 %s%%','Progress %s%%'),'paid':('加工耗能：%s','Processing energy: %s'),'transfer_energy':('传输耗能：%s','Transfer energy: %s'),'pause':('暂停','Pause'),'enable':('启动','Enable'),
      'eject_on':('自动输出：开','Auto-eject: On'),'eject_off':('自动输出：关','Auto-eject: Off'),'profile':('调谐：%s','Tuning: %s'),
      'profile_0':('均衡','Balanced'),'profile_1':('加工聚焦','Processing'),'profile_2':('核心超频','Overclock'),
      'cooldown':('耀斑恢复：%s秒','Flare recovery: %ss'),'burst':('消耗晶核 · 耀斑升载','Use cell · Flare burst'),'burst_left':('耀斑剩余 %s秒','Flare remaining: %ss'),
      'stored':('储能：%s / %s','Energy: %s / %s'),'net':('净发电：%s/t','Net generation: %s/t'),'exported':('外供：%s/t','Exported: %s/t'),
      'signal':('红石输出：%s','Redstone output: %s'),'alarm_0':('告警：储能比例','Signal: energy level'),'alarm_1':('告警：结构或链接异常','Alarm: structure / link'),
      'alarm_2':('告警：燃料不足','Alarm: low fuel'),'alarm_3':('告警：储能不足','Alarm: low energy'),'threshold':('阈值 %s%%','Threshold %s%%'),
      'details':('场域链接与状态','Field Link and Status'),'source_pos':('能量源：%s','Source: %s'),'peer_pos':('接收节点：%s','Receiver: %s'),
      'fuel':('本份燃料：%s','Current fuel: %s'),'spare':('备用燃料：%s份','Spare fuel: %s'),'transferred':('累计送出：%s个','Items sent: %s'),
      'range_hint':('同维度 · 最远%s格','Same dimension · Up to %s blocks'),'profile_hint':('聚焦：加工×2，发电80%','Processing: x2 speed, 80% generation'),
      'unlink':('解除链接','Unlink'),'stock':('库存：%s / %s','Stored: %s / %s'),'input':('原料','Inputs'),'output':('产物','Outputs'),
      'batch':('批量 %s','Batch %s'),'recipe_info':('等级 %s · %s tick · %s','Tier %s · %s ticks · %s')}
    words.update({
      'resources_waiting':('等待传输资源','Waiting for resources'),'receiving_ready':('接收端就绪，由发送端供能','Receiver ready; powered by sender'),'channels_off':('双方没有开启相同资源通道','No common enabled resource channel'),
      'resources':('资源传输','Resource Transfer'),'resource_0':('物品','Items'),'resource_1':('能量','Energy'),'resource_2':('流体','Fluids'),'resource_3':('化学品','Chemicals'),
      'channel_on':('传输：开','Transfer: On'),'channel_off':('传输：关','Transfer: Off'),'instant':('即时传送','Instant'),
      'item_input':('输入：%s / 36864','Input: %s / 36864'),'item_output':('输出：%s / 36864','Output: %s / 36864'),'item_rate':('发送：%s 个/t','Sent: %s items/t'),
      'input_energy':('输入：%s','Input: %s'),'output_energy':('输出：%s','Output: %s'),'energy_rate':('发送：%s/t','Sent: %s/t'),
      'fluid_rate':('发送：%s mB/t','Sent: %s mB/t'),'chemical_rate':('发送：%s 单位/t','Sent: %s units/t'),'node_io_hint':('输入输出由六面配置决定','Configure input/output per side'),
      'unlink_peer':('断开目标','Unlink target'),'unlink_power':('解绑能源','Unlink power'),'selection_cleared':('已清除链接器选择','Linker selection cleared'),
      'power_selection':('能源：%s','Power: %s'),'sender_selected':('发送节点 %s；再右键接收端','Sender %s; use on receiver next'),
      'paired':('已连接 %s → %s','Linked %s → %s'),'paired_need_power':('已配对 %s → %s；请给发送端绑定能源','Paired %s → %s; bind power to sender'),
      'select_different_node':('请选择另一个接收节点','Choose a different receiving node'),'clear_selection_hint':('潜行对空气右键清除选择。','Sneak-use in air to clear selection.')})
    hints=[('绑定运行中的太阳，封装耀斑晶核。','Bind to a running sun to seal flare cells.'),('绑定运行中的引力堆，锻造恒星合金与压缩物质。','Bind to a running gravity reactor to forge stellar alloy and compressed matter.'),('绑定核心以选择调谐，消耗耀斑晶核启动短时升载。','Bind a core to tune it. Consume a flare cell for a temporary burst.'),('选择同一频率，配置各面输入输出；同频组绑定能源后即时传输资源。','Select a frequency and configure sides. Bind power to the group to instantly transfer resources.'),('绑定装置查看状态，并输出可选红石告警。','Bind a reactor to view its status and emit configurable redstone signals.')]
    words.update({
      'panel_title':('场域连接面板','Field Network Panel'),'panel_short':('网','NET'),
      'panel_refresh':('扫描','Scan'),'panel_layout':('整理布局','Arrange'),'panel_fit':('适应视图','Fit view'),'panel_close':('关闭','Close'),
      'panel_search':('搜索设备名称或坐标','Search device name or coordinates'),
      'panel_channel':('%s %s','%s %s'),'panel_remove_power':('解绑能源','Unlink power'),'panel_remove_route':('断开目标','Unlink target'),
      'panel_legend_power':('金线：核心向模块供能','Gold: core powers module'),'panel_legend_route':('蓝框：频率节点自动互通','Blue cards: automatic frequency routing'),
      'panel_help_drag':('拖动标题整理设备','Drag titles to move devices'),
      'panel_help_pan':('拖动空白平移，滚轮缩放','Drag canvas to pan; scroll to zoom'),
      'panel_help_wire':('拖出连接点，或依次点击两端。选线后按 Delete 断开。','Drag a port or click both ends. Select a wire and press Delete to unlink.'),
      'panel_count':('显示 %s / %s 台 · 扫描 %s 格 · 连接 %s 格','Showing %s / %s · Scan %s · Link %s blocks'),
      'panel_settings':('范围设置','Range settings'),'panel_scan_range':('扫描范围：%s 格','Scan radius: %s blocks'),'panel_link_range':('连接距离：%s 格','Link distance: %s blocks'),
      'panel_range_limits':('输入16～512，回车或勾号保存','Enter 16–512; press Enter or checkmark'),
      'panel_range_scope':('扫描只改变自己看到的设备','Scanning changes your view only'),
      'panel_range_world':('连接距离影响全服现有链接','Link distance affects all world links'),
      'panel_range_admin':('修改连接距离需存档主人或OP','Link distance requires world owner / OP'),
      'panel_device_limit':('最多显示最近192台设备','Shows the nearest 192 devices'),
      'panel_range_saved':('范围已保存，设备列表将在1秒内刷新','Range saved; devices refresh within 1s'),
      'panel_range_invalid':('范围必须为16～512格','Range must be 16–512 blocks'),
      'panel_range_error':('保存失败，请检查服务端配置文件','Save failed; check the server config file'),
      'panel_ready':('自动扫描附近已加载、可访问的核心与模块','Scanning nearby loaded, accessible cores and modules'),
      'panel_choose_target':('连接到同色输入点；右键取消','Connect to a matching input; right-click to cancel'),
      'panel_delete_wire':('按 Delete 断开选中连线','Press Delete to remove the selected wire'),
      'panel_power_out':('能源输出 → 模块金色输入点','Power output → module gold input'),
      'panel_power_in':('能源输入 ← 核心金色输出点','Power input ← core gold output'),
      'panel_route_out':('资源发送 → 接收节点蓝色输入点','Send resources → receiver blue input'),
      'panel_route_in':('资源接收 ← 发送节点蓝色输出点','Receive resources ← sender blue output'),
      'panel_applied':('连接设置已更新','Connection settings updated'),
      'panel_rejected':('连接失败：检查设备类型、距离和权限','Cannot connect: check device type, range and access'),
      'panel_stale':('附近设备已变化，请重新选择连线','Nearby devices changed; select the connection again')})
    words.update({
      'frequency_missing':('请选择频率','Select a frequency'),'frequency_power':('请为同频节点绑定能源','Bind a power source to a node on this frequency'),
      'frequency_alone':('等待范围内的同频节点','Waiting for a nearby node on this frequency'),
      'frequency_current':('频率：%s','Frequency: %s'),'frequency_none':('未选择','None'),'frequency_direct':('定向链接','Direct link'),
      'frequency_peers':('已加载节点：%s','Loaded nodes: %s'),'frequency_select':('选择频率','Select frequency'),'frequency_power_button':('能源绑定','Power link'),
      'frequency_public':('公开','Public'),'frequency_private':('私有','Private'),'frequency_join':('使用','Use'),'frequency_leave':('离开','Leave'),'frequency_delete':('删除','Delete'),
      'frequency_name':('输入频率名称，回车或勾号创建并使用','Enter a name; Enter or checkmark creates and selects it'),
      'frequency_help':('相同频率的节点自动互通','Nodes on the same frequency link automatically'),
      'frequency_saved':('频率已更新','Frequency updated'),'frequency_denied':('操作失败：检查权限、名称和频率','Action failed: check access, name and frequency'),
      'frequency_create_hint':('仅频率创建者可以删除','Only the frequency owner can delete it'),
      'frequency_flow':('同频互通','Linked frequency'),'node_send':('发送缓存','Send buffer'),'node_receive':('接收缓存','Receive buffer')})
    retired=['peer_missing','peer_paused','node_selected','peer_pos','receiving_ready','unlink_peer','sender_selected','paired','paired_need_power','select_different_node','panel_remove_route','panel_route_out','panel_route_in','frequency_direct']
    for key in retired:words.pop(key,None)
    for lang,i in [('zh_cn',0),('en_us',1)]:
        data=json.loads((RES/f'assets/mekgravity/lang/{lang}.json').read_text(encoding='utf-8'));[data.pop('mekgravity.module.'+key,None) for key in retired];data.update({'mekgravity.module.'+k:v[i] for k,v in words.items()})
        for name,label,hint in zip(KINDS,LABELS,hints):
            data['block.mekgravity.'+name]=data['container.mekgravity.'+name]=label[i];data['description.mekgravity.'+name]=hint[i]
        for name,label in [('field_linker',('场域链接器','Field Linker')),('flare_cell',('耀斑晶核','Flare Cell')),('stellar_alloy',('恒星合金','Stellar Alloy'))]:data['item.mekgravity.'+name]=label[i]
        write(f'assets/mekgravity/lang/{lang}.json',data)
    print('Generated five orbital modules, original geometry, recipes and paired translations.')

if __name__=='__main__':build()
