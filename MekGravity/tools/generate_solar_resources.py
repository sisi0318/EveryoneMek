"""Runtime mini-sun resources and semantic geometry. No generated bitmap painting."""
import json,math
from pathlib import Path
from runtime_geometry import block_model
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'src/main/resources'
def write(name,data):
    p=RES/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
def model(name,data):write(f'assets/mekgravity/models/block/solar/{name}.json',data)
def box(a,b,tex,uv=None):return {'from':a,'to':b,'faces':{d:{'texture':tex,'uv':uv or [0,0,16,16]} for d in ['up','down','north','south','east','west']}}
def mechanical(elements):return {'parent':'minecraft:block/block','ambientocclusion':False,'textures':{'shell':'mekgravity:block/shell_panel','steel':'mekgravity:block/orb_steel','dark':'mekgravity:block/orb_inner','lamp':'mekgravity:block/sun_active','particle':'mekgravity:block/shell_panel'},'elements':elements}
def collector(segment,active):
    col,row=segment%3,segment//3;e=[]
    for slice in range(8):
        u=col+(slice+.5)/8;depth=5+(u-1.5)**2*2.5
        b=box([slice*2,0,depth],[slice*2+2,16,depth+3],'#dark',[0,0,16,10])
        b['faces']['north']={'texture':'#absorber','uv':[(col+slice/8)*16/3,(2-row)*16/3,(col+(slice+1)/8)*16/3,(3-row)*16/3]}
        if segment==4 and 2<=slice<=5:
            for low,high in [(0,3),(13,16)]:
                cap=json.loads(json.dumps(b));cap['from'][1]=low;cap['to'][1]=high;e.append(cap)
        else:e.append(b)
    m=mechanical(e);m['textures']['absorber']='mekgravity:block/sun_collector_active' if active else 'mekgravity:block/sun_collector';return m
def build():
    # Maintain the exact reviewed 220-block anchor map as generated common Java data.
    plan=json.loads((ROOT/'art/solar-design/layout.json').read_text(encoding='utf-8'));entries=[]
    for b in plan['blocks']:
        k=b['kind'];kind='null' if k=='controller' else 'SolarBlock.Kind.'+('ENERGY' if k in ['ignition','output'] else k.upper())
        x,y,z=b['pos'];entries.append(f'new Slot({x},{y},{z},{kind},Direction.{b.get("face","north").upper()},{str(k=="output").lower()})')
    source='''package dev.everyonemek.gravity.solar;
import java.util.*;
import net.minecraft.core.Direction;
/** Generated from reviewed art/solar-design/layout.json by tools/generate_solar_resources.py. */
public final class SolarLayout {
 public record Slot(int x,int y,int z,SolarBlock.Kind kind,Direction face,boolean output){}
 public static final List<Slot> SLOTS=List.of(
'''+',\n'.join(entries)+''');
 private static final Map<Integer,Slot> INDEX=new HashMap<>();
 static{for(var slot:SLOTS)INDEX.put(slot.x()*81+slot.y()*9+slot.z(),slot);}
 public static Slot get(int x,int y,int z){return INDEX.get(x*81+y*9+z);}
 public static boolean footprint(int x,int z){return Math.abs(x-4)+Math.abs(z-4)<=6;}
 private SolarLayout(){}
}
'''
    (ROOT/'src/main/java/dev/everyonemek/gravity/solar/SolarLayout.java').write_text(source,encoding='utf-8',newline='\n')
    tiers=['basic','advanced','elite','ultimate'];names=['base','support','crown','seed','fuel','energy','controller']+[f'{kind}_{tier}' for kind in ['ring','focus','collector'] for tier in tiers]
    normals=[('north',0,0),('east',0,90),('south',0,180),('west',0,270),('up',270,0),('down',90,0)]
    for name in names:
        kind=name.split('_')[0];variants={}
        if kind=='collector':
            for segment in range(9):
                for active in [False,True]:
                    sub=f'collector_{segment}'+('_active' if active else '');model(sub,collector(segment,active))
                    for face,rx,ry in normals:variants[f'segment={segment},active={str(active).lower()},facing={face}']={'model':'mekgravity:block/solar/'+sub,'x':rx,'y':ry}
            item={'parent':'mekgravity:block/solar/collector_4'}
        elif kind=='seed':
            variants={'':{'model':'mekgravity:block/solar/sun_idle'}};item={'parent':'mekgravity:block/solar/sun_idle','display':{'gui':{'rotation':[25,225,0],'translation':[0,0,0],'scale':[.28,.28,.28]},'ground':{'translation':[0,3,0],'scale':[.2,.2,.2]}}}
        else:
            for active in ([False,True] if kind in ['ring','focus','controller'] else [False]):
                for output in ([False,True] if kind=='energy' else [False]):
                    sub=name+('_active' if active else '')+('_output' if output else '')
                    if kind in ['focus','controller','fuel','energy']:
                        m=block_model({'focus':'coil','controller':'controller','fuel':'fuel','energy':'excitation'}[kind],active,output if kind=='energy' else None)
                        m['textures']['lamp']='mekgravity:block/sun_active' if active else 'mekgravity:block/sun_idle'
                    elif kind=='support':m=block_model('frame')
                    elif kind in ['ring','crown']:
                        e=[box([0,4,0],[16,10,16],'#dark',[0,0,16,10]),box([0,10,0],[16,12,16],'#steel',[0,0,16,4]),box([0,2,0],[16,4,16],'#shell',[3,3,13,13])]
                        if kind=='ring':e.append(box([2,12,7],[14,12.5,9],'#lamp',[8,0,9,1]))
                        m=mechanical(e);m['textures']['lamp']='mekgravity:block/sun_active' if active else 'mekgravity:block/sun_idle'
                    else:m=mechanical([box([0,0,0],[16,12,16],'#dark',[0,0,16,10]),box([0,12,0],[16,16,16],'#shell',[3,3,13,13])])
                    model(sub,m)
                    for face,rx,ry in (normals[:4] if kind=='controller' else normals if kind in ['focus','energy','fuel'] else [('',0,0)]):
                        key=','.join(([f'facing={face}'] if face else [])+([f'active={str(active).lower()}'] if kind in ['ring','focus','controller'] else [])+([f'output={str(output).lower()}'] if kind=='energy' else []))
                        variants[key]={'model':'mekgravity:block/solar/'+sub,'x':rx,'y':ry}
            item={'parent':'mekgravity:block/solar/'+name}
            if kind=='energy':item['overrides']=[{'predicate':{'mekgravity:output':1},'model':'mekgravity:block/solar/energy_output'}]
        write(f'assets/mekgravity/blockstates/solar_{name}.json',{'variants':variants});write(f'assets/mekgravity/models/item/solar_{name}.json',item)
        components=['mekgravity:solar_data','mekanism:security','mekanism:owner','mekanism:redstone_control'] if kind=='controller' else ['mekgravity:solar_stock']
        write(f'data/mekgravity/loot_table/blocks/solar_{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'mekgravity:solar_'+name,'functions':[{'function':'minecraft:copy_components','source':'block_entity','include':components}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    tags=json.loads((RES/'data/minecraft/tags/block/mineable/pickaxe.json').read_text());tags['values']=sorted(set(tags['values']+['mekgravity:solar_'+n for n in names]));write('data/minecraft/tags/block/mineable/pickaxe.json',tags)
    # New dense solar surface: sphere radius 1.25 block, 32x16 bands, in block-local coordinates.
    obj=['# Original miniature stellar surface','mtllib sun.mtl'];index=0
    def point(u,v):return [.5+1.25*math.sin(v*math.pi)*math.cos(u*math.tau),.5+1.25*math.cos(v*math.pi),.5+1.25*math.sin(v*math.pi)*math.sin(u*math.tau)]
    obj+=['o photosphere','usemtl surface']
    for row in range(16):
        for col in range(32):
            u,U,v,V=col/32,(col+1)/32,row/16,(row+1)/16
            uv=[(u,v),(U,v),(U,V),(u,V)] if row not in [0,15] else [((u+U)/2,v),(U,V),(u,V)] if row==0 else [(u,v),(U,v),((u+U)/2,V)]
            points=[point(*p) for p in uv];a,b,c=points[:3];ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)];cross=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
            if sum(cross[i]*(a[i]-.5) for i in range(3))<0:points.reverse();uv.reverse()
            for p,t in zip(points,uv):obj+=['v '+' '.join(f'{x:.7f}' for x in p),'vt '+' '.join(f'{x:.7f}' for x in t)]
            obj.append('f '+' '.join(f'{i}/{i}' for i in range(index+1,index+len(uv)+1)));index+=len(uv)
    p=RES/'assets/mekgravity/models/block/solar';p.mkdir(parents=True,exist_ok=True);(p/'sun.obj').write_text('\n'.join(obj)+'\n',encoding='utf-8');(p/'sun.mtl').write_text('newmtl surface\nKa 1 1 1\nKd 1 1 1\nmap_Kd #surface\n',encoding='utf-8')
    for active in [False,True]:model('sun_active' if active else 'sun_idle',{'parent':'minecraft:block/block','loader':'neoforge:obj','model':'mekgravity:models/block/solar/sun.obj','mtl_override':'mekgravity:models/block/solar/sun.mtl','automatic_culling':False,'shade_quads':False,'emissive_ambient':True,'textures':{'surface':'mekgravity:block/sun_active' if active else 'mekgravity:block/sun_idle','particle':'mekgravity:block/sun_idle'}})
    for name in ['compressed_stellar_matter','stellar_fuel_preform','stellar_fuel','stellar_fuel_capsule']:
        m=mechanical([box([4,4,4],[12,12,12],'#lamp'),box([3,6,3],[13,10,13],'#steel',[0,0,16,4])]);m['display']={'gui':{'rotation':[25,225,0],'scale':[.9,.9,.9]},'ground':{'translation':[0,3,0],'scale':[.5,.5,.5]}};write('assets/mekgravity/models/item/'+name+'.json',m)
    def recipe(name,pattern,key,count=1):write('data/mekgravity/recipe/'+name+'.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'result':{'id':'mekgravity:'+name,'count':count}})
    recipe('compressed_stellar_matter',['PPP','PPP','PPP'],{'P':'mekgravity:dense_fuel_pellet'})
    recipe('stellar_fuel_preform',['ACA','CHC','ACA'],{'A':'mekanism:pellet_antimatter','C':'mekgravity:compressed_stellar_matter','H':'mekanism:hdpe_sheet'})
    write('data/mekgravity/recipe/stellar_fuel.json',{'type':'mekanism:reaction','item_input':{'count':1,'item':'mekgravity:stellar_fuel_preform'},'fluid_input':{'amount':1000,'tag':'minecraft:water'},'chemical_input':{'amount':8000,'chemical':'mekanismgenerators:fusion_fuel'},'item_output':{'count':1,'id':'mekgravity:stellar_fuel'},'duration':1200,'energy_required':200})
    write('data/mekgravity/recipe/stellar_fuel/standard.json',{'type':'mekgravity:stellar_fuel','ingredient':{'item':'mekgravity:stellar_fuel'},'count':1,'energy':737_280_000_000_000_000})
    recipe('solar_base',['SSS','ROR','SSS'],{'S':'mekanism:ingot_steel','R':'mekanism:ingot_refined_obsidian','O':'mekanism:ingot_osmium'},8)
    for n,center in [('support','mekanism:ultimate_induction_provider'),('crown','mekanism:ultimate_control_circuit'),('fuel','minecraft:hopper'),('energy','mekanism:energy_tablet'),('seed','mekgravity:core'),('controller','mekanism:ultimate_control_circuit')]:recipe('solar_'+n,['BAB','BCB','BAB'],{'B':'mekgravity:solar_base','A':'mekanism:alloy_atomic','C':center},4 if n in ['support','crown'] else 1)
    for kind,center in [('ring','mekgravity:ultimate_coil'),('focus','mekanism:laser_amplifier'),('collector','mekanism:ultimate_induction_cell')]:
        for i,tier in enumerate(tiers):recipe('solar_'+kind+'_'+tier,['ABA','BCB','ABA'],{'A':'mekanism:alloy_atomic','B':'mekanism:'+tier+'_control_circuit','C':center if i==0 else 'mekgravity:solar_'+kind+'_'+tiers[i-1]},4 if kind in ['ring','collector'] and i==0 else 1)
    z={'tab':'人造微缩太阳','unlinked':'尚未接入微缩太阳','structure':'等待结构成型','component':'部件缺失或位置错误','facing':'聚束器或采能翼朝向错误','wing_mixed':'同一组采能翼需要相同等级','energy_ports':'至少保留一个输入口和一个输出口','ready':'结构完整','stopped':'已停机','redstone':'等待红石信号','startup_config':'点火电量超过缓存容量','fuel_missing':'缺少恒星燃料','refill_off':'自动续料已关闭','charging':'等待点火充能','reserve_low':'约束备用电不足','full':'储能已满','standby':'余辉待机','limited':'供能受限','running':'恒星稳定运行','occupied':'部件属于其他微缩太阳','unloaded':'结构所在区块未加载','interior':'反应空间需要留空','fuel_title':'恒星燃料仓','fuel_hint':'放入封装恒星燃料或残余燃料胶囊。','capsule_left':'剩余燃料：%s%%','recovered':'剩余燃料已封装','recover_failed':'请先停机，并留出一个空背包格','paused':'反应已暂停','remaining_time':'剩余 %s小时 %s分 %s秒','net_label':'净发电','output_label':'实际输出','fuel_reserve':'恒星燃料余量','charging_amount':'充能：%s / %s','stop':'停机','ignite':'点火','load':'采能负载 %s%%','page_0':'供能情况','page_1':'结构与采能翼','page_2':'燃料与调载','page_3':'采能负载','auto_on':'自动调载：开','auto_off':'自动调载：关','refill_on':'自动续料：开','refill_off_button':'自动续料：关','recover':'回收剩余燃料','size':'尺寸：9 × 9 × 9','constraint':'约束上限：%s/t','collector':'采能上限：%s/t','unknown':'未完成','wing_0':'左翼：%s','wing_1':'右翼：%s','wing_2':'前翼：%s','wing_3':'后翼：%s','stellar_fuel':'恒星燃料','fuel_energy':'燃料能量：%s','fuel_budget_hint':'供给恒星反应与约束场'}
    en={'tab':'Artificial Miniature Sun','unlinked':'Not connected to a miniature sun','structure':'Structure incomplete','component':'Missing or misplaced component','facing':'Aim the focus or collector inward','wing_mixed':'Each collector wing needs one tier','energy_ports':'An input and an output port are required','ready':'Structure formed','stopped':'Stopped','redstone':'Waiting for redstone','startup_config':'Startup exceeds buffer capacity','fuel_missing':'Stellar fuel required','refill_off':'Automatic refuelling is off','charging':'Waiting for ignition power','reserve_low':'Containment reserve low','full':'Energy buffer full','standby':'Ember standby','limited':'Generation limited','running':'Star stable','occupied':'Component belongs to another sun','unloaded':'Structure chunk not loaded','interior':'Clear the reaction space','fuel_title':'Stellar Fuel Hatch','fuel_hint':'Insert sealed stellar fuel or a residual capsule.','capsule_left':'Fuel remaining: %s%%','recovered':'Residual fuel sealed','recover_failed':'Stop the sun and leave an empty inventory slot','paused':'Reaction paused','remaining_time':'Remaining %sh %sm %ss','net_label':'Net generation','output_label':'Actual output','fuel_reserve':'Stellar fuel reserve','charging_amount':'Charging: %s / %s','stop':'Stop','ignite':'Ignite','load':'Load limit %s%%','page_0':'Power','page_1':'Structure and Collectors','page_2':'Fuel and Load','page_3':'Load Limit','auto_on':'Automatic load: On','auto_off':'Automatic load: Off','refill_on':'Auto-refuel: On','refill_off_button':'Auto-refuel: Off','recover':'Recover remaining fuel','size':'Size: 9 x 9 x 9','constraint':'Containment limit: %s/t','collector':'Collector limit: %s/t','unknown':'Incomplete','wing_0':'Left wing: %s','wing_1':'Right wing: %s','wing_2':'Front wing: %s','wing_3':'Back wing: %s','stellar_fuel':'Stellar Fuel','fuel_energy':'Fuel energy: %s','fuel_budget_hint':'Supplies stellar reaction and containment'}
    labels={'base':('恒星基座','Stellar Base'),'support':('恒星支撑柱','Stellar Support'),'crown':('恒星冠架','Stellar Crown'),'seed':('恒星胚核','Stellar Seed'),'fuel':('恒星燃料仓','Stellar Fuel Hatch'),'energy':('恒星能量接口','Stellar Energy Port'),'controller':('恒星约束器','Stellar Containment Controller'),'ring':('恒星约束环','Stellar Containment Ring'),'focus':('恒星聚束器','Stellar Focus'),'collector':('日冕采能翼','Coronal Collector')}
    for lang,words,i in [('zh_cn',z,0),('en_us',en,1)]:
        p=RES/f'assets/mekgravity/lang/{lang}.json';d=json.loads(p.read_text(encoding='utf-8'));d.update({'mekgravity.solar.'+k:v for k,v in words.items()})
        for n in names:
            kind=n.split('_')[0];name=labels[kind][i]
            if '_' in n:idx=tiers.index(n.split('_')[1]);name=([['初级','高级','精英','终极'][idx],tiers[idx].title()+' '][i])+name
            d['block.mekgravity.solar_'+n]=name
        d['container.mekgravity.solar_controller']=labels['controller'][i]
        for k,pair in {'compressed_stellar_matter':('压缩恒星物质','Compressed Stellar Matter'),'stellar_fuel_preform':('恒星燃料坯','Stellar Fuel Preform'),'stellar_fuel':('封装恒星燃料','Sealed Stellar Fuel'),'stellar_fuel_capsule':('残余恒星燃料胶囊','Residual Stellar Fuel Capsule')}.items():d['item.mekgravity.'+k]=pair[i]
        write(f'assets/mekgravity/lang/{lang}.json',d)
    print('Generated solar layout, three-dimensional parts, solar surface, recipes, loot and paired language resources.')
if __name__=='__main__':build()
