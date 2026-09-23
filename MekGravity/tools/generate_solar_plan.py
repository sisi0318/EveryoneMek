"""Design-only mini-sun layout. Does not write Minecraft runtime resources."""
import json
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'art/solar-design';OUT.mkdir(parents=True,exist_ok=True)
parts={}
def add(kind,x,y,z,face=None,replace=False):
    key=(x,y,z)
    assert replace or key not in parts,(kind,key,parts.get(key))
    parts[key]={'kind':kind,'pos':list(key)}|({'face':face} if face else {})

footprint={(x,z) for x in range(9) for z in range(9) if abs(x-4)+abs(z-4)<=6}
perimeter={p for p in footprint if any((p[0]+a,p[1]+b) not in footprint for a,b in [(1,0),(-1,0),(0,1),(0,-1)])}
for x,z in sorted(footprint):add('base',x,0,z)
for x in [1,7]:
    for z in [1,7]:
        for y in range(1,8):add('support',x,y,z)
for y in [2,6]:
    for x,z in sorted(perimeter):add('ring',x,y,z,replace=True)
for x,z in sorted(perimeter):add('crown',x,8,z)
for axis in [0,2]:
    for k in range(9):
        pos=[4,8,4];pos[axis]=k
        if tuple(pos) not in parts:add('crown',*pos)
for y in [1,7]:add('focus',4,y,4,'up' if y==1 else 'down')
add('seed',4,4,4)
for y in range(3,6):
    for a in range(3,6):
        add('collector',0,y,a,'east');add('collector',8,y,a,'west')
        add('collector',a,y,0,'south');add('collector',a,y,8,'north')
for kind,x,y,z,face in [('controller',4,1,0,'north'),('fuel',2,1,0,'north'),('ignition',6,1,0,'north'),
                       ('output',3,1,8,'south'),('output',5,1,8,'south'),('output',8,1,3,'east'),('output',8,1,5,'east')]:add(kind,x,y,z,face)

counts=Counter(p['kind'] for p in parts.values())
assert len(parts)==220 and counts['collector']==36 and counts['ring']==48
assert all(0<=v<=8 for pos in parts for v in pos)
assert all((x,y,z) not in parts for x in range(2,7) for y in range(2,7) for z in range(2,7) if (x,y,z)!=(4,4,4))
dirs={'north':(0,0,-1),'south':(0,0,1),'east':(1,0,0),'west':(-1,0,0)}
for p in parts.values():
    if p['kind'] in ['controller','fuel','ignition','output']:
        outside=[a+b for a,b in zip(p['pos'],dirs[p['face']])]
        assert any(v<0 or v>8 for v in outside),(p,outside)

kinds={
 'base':('基座','底','#85909b'),'support':('角支撑','柱','#a0adb9'),'ring':('约束环','环','#706996'),
 'crown':('冠架','架','#b8c3cb'),'focus':('聚束器','极','#55b6b3'),'seed':('恒星胚核','核','#f3b452'),
 'collector':('采能翼','采','#ba8960'),'controller':('主控','控','#7eaf86'),'fuel':('燃料仓','料','#b79dce'),
 'ignition':('点火口','入','#bb7770'),'output':('发电口','出','#748fb4')}
layout={'name':'人造微缩太阳','status':'设计提案，尚未实现','size':[9,9,9],'origin':'正面左下角，零基坐标，向上为Y、向后为Z',
 'fuel_mode':'投放长效恒星燃料；用户已选择此玩法','counts':dict(counts),'total_blocks':len(parts),'air':729-len(parts),
 'blocks':list(parts.values()),'proposal':{'gross_fe_per_tick':[32_000_000_000,64_000_000_000,128_000_000_000,256_000_000_000],
 'self_use_percent':5,'fuel_joules':46_080_000_000_000_000,'full_load_seconds':[28800,14400,7200,3600],
 'joules_per_fe':2.5,'ticks_per_second':20,'startup_fe':8_000_000_000_000,'reserve_fe':256_000_000_000,
 'buffer_fe':16_000_000_000_000,'fe_per_output_port_per_tick':64_000_000_000}}
for rate,seconds in zip(layout['proposal']['gross_fe_per_tick'],layout['proposal']['full_load_seconds']):assert rate*5//2*seconds*20==layout['proposal']['fuel_joules']
(OUT/'layout.json').write_text(json.dumps(layout,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')

svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1060" height="1170" viewBox="0 0 1060 1170">',
'<rect width="1060" height="1170" fill="#192128"/><style>text{font-family:Microsoft YaHei,sans-serif;fill:#dce4e9}.title{font-size:26px}.sub{font-size:13px;fill:#9dabb5}.label{font-size:16px}.cell{font-size:12px;text-anchor:middle;dominant-baseline:central;fill:#14202a}</style>',
'<text x="34" y="43" class="title">人造微缩太阳 · 逻辑结构草案</text>',
'<text x="34" y="69" class="sub">9 × 9 × 9 / 220 个实体方块 / 非三维模型 / 从上往下看，图下方为主控正面</text>']
for layer in range(9):
    col=layer%3;row=layer//3;left=34+col*344;top=110+row*288
    count=sum(pos[1]==layer for pos in parts)
    svg.append(f'<text x="{left}" y="{top}" class="label">第 {layer+1} 层 · {count} 块</text>')
    for z in range(9):
        for x in range(9):
            p=parts.get((x,layer,z));cx=left+x*27;cy=top+14+(8-z)*27
            fill=kinds[p['kind']][2] if p else '#26323b'
            svg.append(f'<rect x="{cx}" y="{cy}" width="25" height="25" fill="{fill}"/>')
            if p:svg.append(f'<text x="{cx+12.5}" y="{cy+12.5}" class="cell">{kinds[p["kind"]][1]}</text>')
    svg.append(f'<text x="{left+273}" y="{top+132}" class="sub">Y = {layer}</text>')
for i,(kind,(label,mark,color)) in enumerate(kinds.items()):
    x=34+(i%4)*250;y=1000+(i//4)*42
    svg.append(f'<rect x="{x}" y="{y-18}" width="24" height="24" fill="{color}"/><text x="{x+12}" y="{y-6}" class="cell">{mark}</text><text x="{x+34}" y="{y}" class="sub">{label} × {counts[kind]}</text>')
svg.append('<text x="34" y="1148" class="sub">中央 5×5×5 留作太阳与日冕渲染空间，仅中心放胚核。四组采能翼各占 3×3；层图定义锚点，曲面形体待灰模。</text></svg>')
(OUT/'layout.svg').write_text(''.join(svg),encoding='utf-8',newline='\n')
print('Solar design:',dict(counts),'total',len(parts),'air',729-len(parts),'; four lifetime budgets and exposed ports verified.')
