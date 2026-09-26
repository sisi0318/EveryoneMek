"""Coronal chamber resources. Original existing 16px MekGravity materials, no new bitmap art."""
import json
from generate_solar_resources import RES, write, box, mechanical

def build():
    variants={}
    for active in (False,True):
        # North-facing recessed cradle: gray shell, dark rear, narrow thermal collars.
        e=[box([0,0,0],[16,3,16],'#shell'),box([0,13,0],[16,16,16],'#shell'),
           box([0,3,0],[3,13,16],'#shell'),box([13,3,0],[16,13,16],'#shell'),
           box([3,3,12],[13,13,16],'#dark'),box([3,3,1],[13,4,3],'#steel'),
           box([3,12,1],[13,13,3],'#steel'),box([3,4,1],[4,12,3],'#steel'),box([12,4,1],[13,12,3],'#steel')]
        for x in (1,14):
            lamp=box([x,6,-.01],[x+1,10,0],'#lamp',[6,6,10,10]);
            if active:
                for face in lamp['faces'].values():face['neoforge_data']={'block_light':12,'ambient_occlusion':False}
            e.append(lamp)
        model=mechanical(e);model['textures']['lamp']='mekgravity:block/sun_active' if active else 'mekgravity:block/sun_idle'
        name='coronal_chamber'+('_active' if active else '')
        write(f'assets/mekgravity/models/block/{name}.json',model)
        for face,y in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={face},active={str(active).lower()}']={'model':'mekgravity:block/'+name,'y':y}
    write('assets/mekgravity/blockstates/coronal_chamber.json',{'variants':variants})
    write('assets/mekgravity/models/item/coronal_chamber.json',{'parent':'mekgravity:block/coronal_chamber'})
    write('data/mekgravity/loot_table/blocks/coronal_chamber.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'mekgravity:coronal_chamber','functions':[{'function':'minecraft:copy_components','source':'block_entity','include':['mekanism:items','mekanism:owner','mekanism:security','mekanism:redstone_control','mekgravity:coronal_work','minecraft:custom_name']}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    p=RES/'data/minecraft/tags/block/mineable/pickaxe.json';tag=json.loads(p.read_text(encoding='utf-8'));tag['values']=sorted(set(tag['values']+['mekgravity:coronal_chamber']));write('data/minecraft/tags/block/mineable/pickaxe.json',tag)
    write('data/mekgravity/recipe/coronal_chamber.json',{'type':'minecraft:crafting_shaped','pattern':['ABA','CDC','ABA'],'key':{k:{'item':v} for k,v in {'A':'mekgravity:solar_base','B':'mekanism:alloy_atomic','C':'mekanism:ultimate_control_circuit','D':'mekanism:energized_smelter'}.items()},'result':{'id':'mekgravity:coronal_chamber'}})
    for name,inputs,result,count,ticks,energy,tier in [
        ('infused',[('c:ingots/copper',1,True),('minecraft:redstone',1,False)],'mekanism:alloy_infused',1,40,5_000_000_000,0),
        ('reinforced',[('mekanism:alloy_infused',4,False),('mekanism:enriched_diamond',1,False)],'mekanism:alloy_reinforced',4,60,20_000_000_000,1),
        ('atomic',[('mekanism:alloy_reinforced',2,False),('mekanism:enriched_refined_obsidian',1,False)],'mekanism:alloy_atomic',2,80,40_000_000_000,2)]:
        write(f'data/mekgravity/recipe/coronal_processing/{name}.json',{'type':'mekgravity:coronal_processing','inputs':[{'ingredient':{'tag' if tag else 'item':item},'count':n} for item,n,tag in inputs],'result':{'id':result,'count':count},'ticks':ticks,'energy':energy,'tier':tier})
    shader=json.loads((RES/'assets/mekgravity/shaders/core/stellar_field.json').read_text(encoding='utf-8'));shader['vertex']=shader['fragment']='mekgravity:coronal_processing';write('assets/mekgravity/shaders/core/coronal_processing.json',shader)
    words={
      'input':('原料','Inputs'),'output':('产物','Outputs'),'batch':('批量 %s','Batch %s'),'progress':('进度 %s%%','Progress %s%%'),
      'paid':('本批已付：%s','Batch paid: %s'),'energy_used':('日冕加工扣能：%s','Coronal batch debit: %s'),
      'unlinked':('贴在采能翼外侧中心，背面朝向采能翼','Attach rear to the outer center of a collector wing'),
      'access':('无权使用这台微缩太阳','No permission to use this sun'),'paused':('加工已暂停','Processing paused'),
      'cold':('等待恒星点燃','Waiting for a hot star'),'energy':('太阳可用储能不足','Insufficient available solar energy'),
      'tier_low':('需要更高级的恒星约束部件','Higher stellar containment tier required'),'no_recipe':('等待可加工原料','Waiting for processable inputs'),
      'output_full':('产物空间不足','Output space full'),'running':('日冕高温加工中','Coronal processing'),
      'stop':('暂停','Pause'),'start':('启动','Start'),'eject_on':('自动输出：开','Auto-eject: On'),'eject_off':('自动输出：关','Auto-eject: Off'),
      'category':('日冕高温加工','Coronal Processing'),'recipe_info':('等级 %s · %s tick · %s','Tier %s · %s ticks · %s')}
    for lang,i in [('zh_cn',0),('en_us',1)]:
        p=RES/f'assets/mekgravity/lang/{lang}.json';data=json.loads(p.read_text(encoding='utf-8'));data.update({'mekgravity.corona.'+k:v[i] for k,v in words.items()})
        for key in ['block.mekgravity.coronal_chamber','container.mekgravity.coronal_chamber']:data[key]=('日冕加工舱','Coronal Processing Chamber')[i]
        data['description.mekgravity.coronal_chamber']=('贴在采能翼外侧中心。消耗太阳储能批量熔炼、精炼合金；侧面及上下进料，正面出料。','Mount outside the center of a collector wing. Uses solar energy for batch smelting and alloy refining. Inputs: sides, top and bottom. Output: front.')[i]
        write(f'assets/mekgravity/lang/{lang}.json',data)
    print('Generated coronal chamber models, loot, recipes, shader definition and translations.')

if __name__=='__main__':build()
