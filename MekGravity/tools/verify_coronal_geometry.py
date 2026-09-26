"""Catch positive-area coplanar faces, including the dock/collector interface.

An optional JSON path checks a previous model; alpha.21 fails with 57 overlaps.
No game client or graphics driver is needed for this geometric regression.
"""
import itertools,json,math,sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/mekgravity/models/block'
SIDES={'west':(0,0),'east':(0,1),'down':(1,0),'up':(1,1),'north':(2,0),'south':(2,1)}

def load(path):return json.loads(path.read_text(encoding='utf-8'))

def faces(model,transform=lambda p:p):
    result=[]
    for element in model['elements']:
        for side,data in element['faces'].items():
            assert all(math.isfinite(v) and 0<=v<=16 for v in data['uv']), 'Invalid texture coordinates'
            axis,positive=SIDES[side];lo=element['from'].copy();hi=element['to'].copy()
            lo[axis]=hi[axis]=element['to' if positive else 'from'][axis]
            a,b=transform(lo),transform(hi);lo=[min(x,y) for x,y in zip(a,b)];hi=[max(x,y) for x,y in zip(a,b)]
            assert all(hi[k]>lo[k] for k in range(3) if k!=axis), 'Degenerate face'
            result.append((axis,lo[axis],lo,hi))
    return result

def overlap(a,b):
    return a[0]==b[0] and abs(a[1]-b[1])<1e-6 and all(
        min(a[3][k],b[3][k])-max(a[2][k],b[2][k])>1e-6 for k in range(3) if k!=a[0])

def verify(path):
    dock=faces(load(path));count=sum(overlap(a,b) for a,b in itertools.combinations(dock,2))
    assert count==0, f'{path.name}: {count} positive-area coplanar overlaps'
    for i in range(9):
        # The dock faces north; the adjacent collector faces inward (south).
        transform=lambda p:[(i%3)*16-p[0],(i//3-1)*16+p[1],32-p[2]]
        wing=faces(load(ROOT/f'solar/collector_{i}.json'),transform)
        assert not any(overlap(a,b) for a,b in itertools.product(dock,wing)), f'Overlap with collector segment {i}'
    print(f'PASS {path.name}: {len(dock)} quads; no self/wing coplanar overlap; finite bounded UVs')

if __name__=='__main__':
    for path in ([Path(sys.argv[1])] if len(sys.argv)>1 else [ROOT/'coronal_chamber.json',ROOT/'coronal_chamber_active.json']):verify(path)
