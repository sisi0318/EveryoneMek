"""Build the review-stage 7x7x7 reactor as real geometry and export editable OBJ/JSON.

This is an art prototype; it does not write game resources or change the build plan.
Coordinates use Minecraft blocks; main structural dimensions follow the 1/16-block grid.
"""
from pathlib import Path
import json
import math
import shutil
import zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'art/models/graybox-v1'
OUT.mkdir(parents=True,exist_ok=True)
MATERIALS={
    'armor':{'color':[.72,.74,.76],'gray':[.72,.72,.72]},
    'steel':{'color':[.48,.52,.56],'gray':[.48,.48,.48]},
    'edge':{'color':[.82,.85,.87],'gray':[.82,.82,.82]},
    'graphite':{'color':[.16,.19,.23],'gray':[.17,.17,.17]},
    'recess':{'color':[.075,.085,.11],'gray':[.075,.075,.075]},
    'lamp':{'color':[.28,.8,.65],'gray':[.68,.68,.68]},
    'energy':{'color':[.19,.075,.30],'gray':[.19,.19,.19]},
    'orbit':{'color':[.61,.36,.87],'gray':[.7,.7,.7]},
    'glass':{'color':[.26,.46,.55],'gray':[.55,.58,.60],'alpha':.13},
}
parts=[]

def box(lo,hi,material):return {'lo':[v/16 for v in lo],'hi':[v/16 for v in hi],'material':material}

def frame():
    # Continuous rails and broad cladding, with real recessed channels between them.
    b=[box([3,0,3],[13,16,13],'armor')]
    for x in [0,13]:
        for z in [0,13]:b.append(box([x,0,z],[x+3,16,z+3],'steel'))
    for z in [1,13]:b.append(box([4,0,z],[12,16,z+2],'edge'))
    for x in [1,13]:b.append(box([x,0,4],[x+2,16,12],'edge'))
    return b

def face_transform(p,normal):
    # Template outward face is -Z. These are proper rotations, not mirrored faces.
    x,y,z=[v-.5 for v in p]
    if normal=='+z':x,z=-x,-z
    elif normal=='+x':x,z=-z,x
    elif normal=='-x':x,z=z,-x
    elif normal=='+y':y,z=-z,y
    elif normal=='-y':y,z=z,-y
    return [x+.5,y+.5,z+.5]

def transformed_box(b,normal):
    a,c=face_transform(b['lo'],normal),face_transform(b['hi'],normal)
    return {'lo':[min(x,y) for x,y in zip(a,c)],'hi':[max(x,y) for x,y in zip(a,c)],'material':b['material']}

def rim(material='armor',depth=3,inset=2):
    return [box([0,0,0],[inset,16,depth],material),box([16-inset,0,0],[16,16,depth],material),
            box([inset,0,0],[16-inset,inset,depth],material),box([inset,16-inset,0],[16-inset,16,depth],material)]

def panel():
    return [box([0,0,3],[16,16,16],'armor'),box([2,2,2],[14,14,3],'graphite')]+rim('steel')

def joint():
    b=[box([2,2,2],[14,14,14],'graphite')]
    # Six recessed square sockets on a chamfered connector; no paper-flat decals.
    cap=[box([2,2,0],[5,14,2],'armor'),box([11,2,0],[14,14,2],'armor'),
         box([5,2,0],[11,5,2],'armor'),box([5,11,0],[11,14,2],'armor'),box([5,5,1.5],[11,11,2],'recess')]
    for normal in ['-x','+x','-y','+y','-z','+z']:b.extend(transformed_box(v,normal) for v in cap)
    return b

def coil():
    # Rear foot -> coil housing -> neck -> recessed emitter; nose occupies 1/4 of the reserved air gap.
    b=[box([0,0,13],[16,16,16],'steel'),box([1,1,6],[15,15,13],'armor'),
       box([3,3,3],[13,13,6],'graphite'),box([4,4,-1],[12,12,3],'steel'),
       box([5,5,-2],[11,11,-1],'recess')]
    b.extend([box([3,3,-4],[5,13,-1],'armor'),box([11,3,-4],[13,13,-1],'armor'),
              box([5,3,-4],[11,5,-1],'armor'),box([5,11,-4],[11,13,-1],'armor')])
    for x in [1,13]:b.append(box([x,6,5],[x+2,10,6],'lamp'))
    # Recessed bands around the wide rear body, rather than a painted vent face.
    for y in [2,12]:b.append(box([2,y,5],[14,y+2,6],'graphite'))
    return b

def port(kind):
    b=panel()
    # Small functional opening, thick surround and a protruding connector collar.
    b.extend([box([4,4,-1],[6,12,2],'steel'),box([10,4,-1],[12,12,2],'steel'),
              box([6,4,-1],[10,6,2],'steel'),box([6,10,-1],[10,12,2],'steel'),box([6,6,1],[10,10,2],'recess')])
    if kind=='controller':
        b=[box([0,0,3],[16,16,16],'armor')]+rim('armor',3,2)
        b.extend([box([3,4,1],[13,12,3],'steel'),box([4,5,.75],[12,11,1],'recess'),box([5,7,.5],[6,9,.75],'lamp')])
    else:b.append(box([7,13,-.25],[9,14,0],'lamp' if kind!='output' else 'orbit'))
    return b

def glass():
    # An inset pane with independent mullions and sill, not a flat frame graphic.
    return [box([0,0,3],[1,16,6],'steel'),box([15,0,3],[16,16,6],'steel'),
            box([1,0,3],[15,1,6],'steel'),box([1,15,3],[15,16,6],'steel'),box([1,1,4.5],[15,15,5],'glass')]

def add(kind,pos,normal,boxes,role,label):
    transformed=[transformed_box(b,normal) for b in boxes]
    parts.append({'id':kind+'_'+'_'.join(map(str,pos)),'kind':kind,'position':pos,'normal':normal,
                  'role':role,'label':label,'boxes':transformed,'triangles':[]})

layout=json.loads((ROOT.parent/'docs/gravity-reactor/layout.json').read_text(encoding='utf-8'))
special={tuple(p['pos']):p['kind'] for p in layout['blocks']}
for x in range(7):
    for y in range(7):
        for z in range(7):
            pos=[x,y,z];edges=sum(v in [0,6] for v in pos);kind=special.get(tuple(pos))
            if edges>=2:
                if edges==3:add('joint',pos,'-z',joint(),'corner','转角接头')
                else:
                    # frame() is vertical, so transform Y onto the length of each beam.
                    normal='-z' if x in [0,6] and z in [0,6] else '+y'
                    boxes=frame()
                    if y in [0,6] and z in [0,6]:
                        boxes=[{'lo':[b['lo'][1],b['lo'][0],b['lo'][2]],'hi':[b['hi'][1],b['hi'][0],b['hi'][2]],'material':b['material']} for b in boxes];normal='-z'
                    add('frame',pos,normal,boxes,'top' if y==6 else 'bottom' if y==0 else 'column','横梁' if y in [0,6] else '承重立柱')
                continue
            if kind=='core':continue
            if kind=='coil':
                normal='+x' if x==1 else '-x' if x==5 else '+y' if y==1 else '-y' if y==5 else '+z' if z==1 else '-z'
                add('coil',pos,normal,coil(),'interior','约束发射器');continue
            if kind in ['controller','fuel','excitation','output']:
                add(kind,pos,'-z' if z==0 else '+z',port(kind),'port',{'controller':'主控制器','fuel':'燃料仓','excitation':'励磁接口','output':'发电接口'}[kind]);continue
            if edges:
                if y in [0,6]:add('panel',pos,'+y' if y==0 else '-y',panel(),'floor' if y==0 else 'roof','内凹底板' if y==0 else '内凹顶板')
                else:
                    normal='-x' if x==0 else '+x' if x==6 else '-z' if z==0 else '+z'
                    add('glass',pos,normal,glass(),'front_window' if z==0 else 'back_window' if z==6 else 'side_window','内嵌观察窗')

# Reuse the currently approved energy sphere/two-ring mesh as editable geometry, without texture baking.
positions=[];material='energy';objects={}
for line in (ROOT/'src/main/resources/assets/mekgravity/models/block/core.obj').read_text().splitlines():
    row=line.split()
    if not row:continue
    if row[0]=='v':positions.append([float(v)+3 for v in row[1:]])
    elif row[0]=='o':obj=row[1];objects[obj]=[]
    elif row[0]=='usemtl':material={'energy':'energy','steel':'steel','inner':'graphite'}[row[1]]
    elif row[0]=='f':
        points=[positions[int(v.split('/')[0])-1] for v in row[1:]]
        for i in range(1,len(points)-1):objects[obj].append({'p':[points[0],points[i],points[i+1]],'material':material})
for name,triangles in objects.items():
    parts.append({'id':'core_'+name,'kind':'core','position':[3,3,3],'normal':'-z','role':'interior','label':'悬浮能量核' if name=='energy' else '金属约束环','boxes':[],'triangles':triangles})

# Long windows merge the internal grid into a genuine recessed opening. Keep anchors for future part mapping.
windows=[p for p in parts if p['kind']=='glass'];parts=[p for p in parts if p['kind']!='glass']
for name,normal,position,extent,role in [
    ('left','-x',[0,1,1],[1,5,5],'side_window'),('right','+x',[6,1,1],[1,5,5],'side_window'),
    ('front','-z',[1,2,0],[5,4,1],'front_window'),('back','+z',[1,2,6],[5,4,1],'back_window')]:
    # The overview merges connected panes; the individual logical glass block anchors are recorded below.
    low=position;high=[low[i]+extent[i] for i in range(3)]
    axis=0 if name in ['left','right'] else 2;a,b=[i for i in range(3) if i!=axis]
    depth=low[axis]+(.72 if name in ['left','front'] else .28)
    boxes=[]
    for edge in [a,b]:
        other=b if edge==a else a
        for at in [low[edge],high[edge]-.125]:
            lo=low.copy();hi=high.copy();lo[axis]=depth-.125;hi[axis]=depth+.125;lo[edge]=at;hi[edge]=at+.125
            if edge==b:lo[other]+=.125;hi[other]-=.125
            boxes.append({'lo':lo,'hi':hi,'material':'steel'})
    lo=[v+.125 for v in low];hi=[v-.125 for v in high];lo[axis]=depth-.016;hi[axis]=depth+.016
    boxes.append({'lo':lo,'hi':hi,'material':'glass'})
    parts.append({'id':'window_'+name,'kind':'glass','position':[0,0,0],'normal':normal,'role':role,'label':'连续观察窗 · '+{'left':'左侧','right':'右侧','front':'正面','back':'背面'}[name],'boxes':boxes,'triangles':[],'worldSpace':True})
# Preserve the low front/back glass filler modules around functional ports.
for p in windows:
    if p['position'][1]==1 and p['position'][2] in [0,6] and p['position'][0] not in [0,6]:parts.append(p)

def box_triangles(b,offset):
    x,y,z=[b['lo'][i]+offset[i] for i in range(3)];X,Y,Z=[b['hi'][i]+offset[i] for i in range(3)]
    faces=[[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],
           [(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)],[(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)],
           [(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)]]
    return [{'p':[f[0],f[i],f[i+1]],'material':b['material']} for f in faces for i in [1,2]]

scene=[]
for part in parts:
    triangles=part['triangles'][:]
    for b in part['boxes']:triangles.extend(box_triangles(b,[0,0,0] if part.get('worldSpace') else part['position']))
    points=[p for t in triangles for p in t['p']]
    bounds=[[min(p[i] for p in points) for i in range(3)],[max(p[i] for p in points) for i in range(3)]]
    scene.append({k:v for k,v in part.items() if k not in ['boxes','triangles']}|{'triangles':triangles,'bounds':bounds})

assembly={'version':1,'name':'引力堆 · 整机灰模','stage':'已确认并拆分接入alpha.5，预览保留灰模','size':[7,7,7],
          'materials':MATERIALS,'parts':parts,'logicalGlassAnchors':[p['position'] for p in windows],
          'notes':['建模稿使用既有方块锚点；发射头向内伸出0.25格，后续接入需处理选择框。','观察窗在整机模型中合并，实际游戏仍保留各玻璃方块。']}
(OUT/'assembly.json').write_text(json.dumps(assembly,ensure_ascii=False,separators=(',',':'))+'\n',encoding='utf-8')
payload={'materials':MATERIALS,'parts':scene,'size':[7,7,7],'triangles':sum(len(p['triangles']) for p in scene)}
(OUT/'scene.js').write_text('window.REACTOR_MODEL='+json.dumps(payload,ensure_ascii=False,separators=(',',':'))+';\n',encoding='utf-8')

obj=['# Original editable reactor graybox. One unit = one Minecraft block.','mtllib reactor-graybox.mtl'];index=0
for p in scene:
    obj.append('o '+p['id']);last=None;vertices={}
    for t in p['triangles']:
        if last!=t['material']:obj.append('usemtl '+t['material']);last=t['material']
        face=[]
        for v in t['p']:
            key=tuple(round(x,6) for x in v)
            if key not in vertices:
                index+=1;vertices[key]=index;obj.append('v '+' '.join(f'{x:.6f}' for x in key))
            face.append(vertices[key])
        obj.append('f '+' '.join(map(str,face)))
(OUT/'reactor-graybox.obj').write_text('\n'.join(obj)+'\n',encoding='utf-8',newline='\n')
mtl=[]
for name,values in MATERIALS.items():mtl.extend(['newmtl '+name,'Kd '+' '.join(map(str,values['gray'])),'d '+str(values.get('alpha',1)),''])
(OUT/'reactor-graybox.mtl').write_text('\n'.join(mtl),encoding='utf-8',newline='\n')
shutil.copyfile(ROOT/'art/concept-v1.png',OUT/'concept-reference.png')
shutil.copyfile(ROOT/'LICENSE',OUT/'LICENSE')
if (OUT/'README.md').exists():
    with zipfile.ZipFile(OUT/'reactor-graybox-v1.zip','w',zipfile.ZIP_DEFLATED) as bundle:
        for name in ['reactor-graybox.obj','reactor-graybox.mtl','assembly.json','README.md','LICENSE','index.html','viewer.css','viewer.js','scene.js','concept-reference.png']:
            # Stable timestamps keep a rebuild of unchanged source byte-identical.
            info=zipfile.ZipInfo('reactor-graybox-v1/'+name,(2026,9,22,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
            bundle.writestr(info,(OUT/name).read_bytes())
assert len([p for p in scene if p['kind']=='coil'])==6
assert len([p for p in scene if p['kind']=='joint'])==8
assert len([p for p in scene if p['kind']=='frame'])==60
print(f'Graybox: {len(scene)} selectable components, {payload["triangles"]} triangles. OBJ/MTL/assembly JSON exported.')
