"""Bake the approved graybox into native block models and shared collision boxes.

The review assembly is the geometry source. Existing original 16x16 artwork supplies
material UV regions; this exporter does not draw or edit bitmaps.
"""
import json
from functools import lru_cache
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSEMBLY=json.loads((ROOT/'art/models/graybox-v1/assembly.json').read_text(encoding='utf-8'))
DIRECTIONS={'west':(0,-1),'east':(0,1),'down':(1,-1),'up':(1,1),'north':(2,-1),'south':(2,1)}
MATERIALS={
    'armor':('shell_panel',[3,3,13,13]),
    'edge':('orb_steel',[0,0,16,4]),
    'steel':('orb_steel',[0,6,16,10]),
    'graphite':('orb_inner',[0,0,16,10]),
    'recess':('orb_inner',[0,11,16,16]),
    'lamp':('controller_front',[6,8,7,9]),
    'orbit':('orb_active',[2,2,3,3]),
}

def turn(p,direction):
    x,y,z=[v-.5 for v in p]
    if direction=='south':x,z=-x,-z
    elif direction=='east':x,z=-z,x
    elif direction=='west':x,z=z,-x
    elif direction=='up':y,z=-z,y
    elif direction=='down':y,z=z,-y
    return [x+.5,y+.5,z+.5]

def rotate(boxes,direction):
    result=[]
    for b in boxes:
        a,c=turn(b['lo'],direction),turn(b['hi'],direction)
        result.append({'lo':[min(x,y) for x,y in zip(a,c)],'hi':[max(x,y) for x,y in zip(a,c)],'material':b['material']})
    return result

@lru_cache(None)
def source(kind):
    candidates=[p for p in ASSEMBLY['parts'] if p['kind']==kind]
    if kind=='frame':candidates=[p for p in candidates if p['role']=='column']
    part=next((p for p in candidates if p['normal']=='-z'),candidates[0])
    inverse={'+x':'west','-x':'east','+y':'down','-y':'up','+z':'south','-z':'north'}[part['normal']]
    return rotate(part['boxes'],inverse)

def exposed_faces(boxes):
    """Rectangle union: discard covered/internal faces, then merge each material plane."""
    axes=[sorted({b[k][i] for b in boxes for k in ['lo','hi']}) for i in range(3)]
    shape=[len(a)-1 for a in axes];cells={}
    for x in range(shape[0]):
        for y in range(shape[1]):
            for z in range(shape[2]):
                idx=(x,y,z);mid=[(axes[i][v]+axes[i][v+1])/2 for i,v in enumerate(idx)]
                for box in boxes:
                    if all(box['lo'][i]<mid[i]<box['hi'][i] for i in range(3)):cells[idx]=box['material']
    result=[]
    for face,(axis,sign) in DIRECTIONS.items():
        a,b=[i for i in range(3) if i!=axis]
        for plane in range(len(axes[axis])):
            grid={}
            for u in range(shape[a]):
                for v in range(shape[b]):
                    inside=[0,0,0];inside[axis]=plane-1 if sign>0 else plane;inside[a]=u;inside[b]=v
                    outside=inside.copy();outside[axis]+=sign
                    material=cells.get(tuple(inside))
                    if material is not None and tuple(outside) not in cells:grid[(u,v)]=material
            while grid:
                (u,v),material=min(grid.items());width=1;height=1
                while grid.get((u+width,v))==material:width+=1
                while all(grid.get((u+i,v+height))==material for i in range(width)):height+=1
                for i in range(width):
                    for j in range(height):del grid[(u+i,v+j)]
                lo=[0,0,0];hi=[0,0,0];lo[axis]=hi[axis]=axes[axis][plane]*16
                lo[a]=axes[a][u]*16;hi[a]=axes[a][u+width]*16;lo[b]=axes[b][v]*16;hi[b]=axes[b][v+height]*16
                result.append((lo,hi,face,material))
    return result

@lru_cache(None)
def surfaces(kind):return exposed_faces(source(kind))

def block_model(kind,active=False,output=None):
    materials=MATERIALS.copy()
    if kind=='coil':materials['lamp']=('orb_active' if active else 'orb_idle',[2,2,3,3] if active else [2,13,3,14])
    elif kind=='controller':materials['lamp']=('controller_front_active' if active else 'controller_front',[5,7,6,8] if active else [6,8,7,9])
    if output is not None:
        materials['lamp']=materials['orbit']=('assembled_port_output' if output else 'assembled_port_input',[4,7,5,8] if output else [3,7,4,8])
    textures={k:'mekgravity:block/'+v[0] for k,v in materials.items()};textures['particle']=textures['armor']
    elements=[]
    for lo,hi,face,material in surfaces(kind):
        data={'texture':'#'+material,'uv':materials[material][1]}
        if material in ['lamp','orbit']:data['neoforge_data']={'block_light':12 if active or output is not None else 3,'ambient_occlusion':False}
        elements.append({'from':lo,'to':hi,'faces':{face:data}})
    return {'parent':'minecraft:block/block','ambientocclusion':False,'textures':textures,'elements':elements}

def assembled_glass(face,part=None):
    axis,sign=DIRECTIONS[face];a,b=(2,1) if axis==0 else (0,2) if axis==1 else (0,1)
    center=4.5 if sign>0 else 11.5
    if part is None:
        elements=[]
        for direction in DIRECTIONS:
            normal,s=DIRECTIONS[direction]
            if normal!=axis:continue
            lo=[0,0,0];hi=[16,16,16];lo[axis]=hi[axis]=center+s*.125
            elements.append({'from':lo,'to':hi,'faces':{direction:{'texture':'#pane','uv':[0,0,16,16],'neoforge_data':{'color':'28577780','ambient_occlusion':False}}}})
        return {'parent':'minecraft:block/block','render_type':'minecraft:translucent','ambientocclusion':False,'textures':{'pane':'minecraft:block/white_concrete','particle':'mekgravity:block/orb_steel'},'elements':elements}
    bounds=[(0,2,2,14),(14,16,2,14),(2,14,0,2),(2,14,14,16)]+[(u,u+2,v,v+2) for u in [0,14] for v in [0,14]]
    amin,amax,bmin,bmax=bounds[part];lo=[0,0,0];hi=[0,0,0];lo[axis]=center-2;hi[axis]=center+2;lo[a]=amin;hi[a]=amax;lo[b]=bmin;hi[b]=bmax
    faces={f:{'texture':'#rim','uv':[0,6,16,10]} for f in DIRECTIONS}
    return {'parent':'minecraft:block/block','textures':{'rim':'mekgravity:block/orb_steel','particle':'mekgravity:block/orb_steel'},'elements':[{'from':lo,'to':hi,'faces':faces}]}

def generate_shapes():
    # Geometry and collision come from exactly the same approved coil boxes.
    rows=[]
    for face in DIRECTIONS:
        values=[]
        for box in rotate(source('coil'),face):values.append('new double[]{'+','.join(str(round(v*16,7)) for v in box['lo']+box['hi'])+'}')
        rows.append('        COILS.put(Direction.'+face.upper()+', union(new double[][]{'+','.join(values)+'}));')
    java='''package dev.everyonemek.gravity;
import java.util.EnumMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.*;
/** Generated by tools/runtime_geometry.py from the approved graybox; do not hand-edit. */
public final class ModelShapes {
    private static final EnumMap<Direction,VoxelShape> COILS=new EnumMap<>(Direction.class);
    static {
'''+ '\n'.join(rows)+'''
    }
    private static VoxelShape union(double[][] boxes){var shape=Shapes.empty();for(var b:boxes)shape=Shapes.or(shape,Block.box(b[0],b[1],b[2],b[3],b[4],b[5]));return shape.optimize();}
    public static VoxelShape coil(Direction direction){return COILS.get(direction);}
    private ModelShapes(){}
}
'''
    path=ROOT/'src/main/java/dev/everyonemek/gravity/ModelShapes.java';path.write_text(java,encoding='utf-8',newline='\n')
