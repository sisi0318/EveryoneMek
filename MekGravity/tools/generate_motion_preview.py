"""Export the real core OBJ groups for an offline motion/proportion preview."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'art/models/core-motion-v1';out.mkdir(parents=True,exist_ok=True)
materials={'energy':[57,24,89],'steel':[166,172,180],'inner':[60,65,72]}
data={}
for name in ['energy','ring_0','ring_1']:
    positions=[];triangles=[];material='energy'
    for line in (ROOT/f'src/main/resources/assets/mekgravity/models/block/core_{name}.obj').read_text().splitlines():
        row=line.split()
        if not row:continue
        if row[0]=='v':positions.append([float(v)-.5 for v in row[1:]])
        elif row[0]=='usemtl':material=row[1]
        elif row[0]=='f':
            points=[positions[int(v.split('/')[0])-1] for v in row[1:]]
            for i in range(1,len(points)-1):triangles.append({'p':[points[0],points[i],points[i+1]],'material':material})
    data[name]=triangles
(out/'mesh.js').write_text('window.CORE_MESH='+json.dumps(data,separators=(',',':'))+';\n',encoding='utf-8')
print('Exported animation preview from runtime OBJ parts.')
