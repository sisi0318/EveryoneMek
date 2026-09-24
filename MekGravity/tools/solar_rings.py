"""Continuous mitered beams on the existing solar block anchors; no bitmap drawing."""
import math
import json

NODES = sorted([(x, z) for x in range(9) for z in range(9)
                if abs(x-4)+abs(z-4) <= 6 and (x in (0, 8) or z in (0, 8) or (x in (1, 7) and z in (1, 7)))],
               key=lambda p: math.atan2(p[1]-4, p[0]-4))


def rotate(p, turns):
    x, z = p[0]-4, p[1]-4
    for _ in range(turns):
        x, z = -z, x
    return x+4, z+4


REPRESENTATIVES = sorted({min(rotate(p, q) for q in range(4)) for p in NODES})
FACES = ['north', 'east', 'south', 'west']


def state(x, z, crown=False):
    p = (x, z)
    if p in NODES:
        rep = min(rotate(p, q) for q in range(4))
        turn = next(q for q in range(4) if rotate(rep, q) == p)
        return (8 if crown and rep == (0, 4) else REPRESENTATIVES.index(rep)), FACES[turn]
    if p == (4, 4):
        return 6, 'north'
    return 7, 'east' if x == 4 else 'north'


def path(segment):
    p = REPRESENTATIVES[segment]
    index = NODES.index(p)
    before, after = NODES[index-1], NODES[(index+1) % len(NODES)]
    return [((q[0]-p[0])/2, (q[1]-p[1])/2) for q in (before, p, after)]


def outline(points, width):
    """Offset the two beam legs and meet at an exact miter, including the diagonal joints."""
    a, b, c = points
    def unit(p, q):
        dx, dz = q[0]-p[0], q[1]-p[1]
        length = math.hypot(dx, dz)
        return dx/length, dz/length
    d, e = unit(a, b), unit(b, c)
    result = []
    for side in (1, -1):
        n, m = (-d[1]*width/2*side, d[0]*width/2*side), (-e[1]*width/2*side, e[0]*width/2*side)
        v = (n[0]+m[0], n[1]+m[1])
        factor = (width/2)**2 / (v[0]*n[0]+v[1]*n[1])
        offset = [(a[0]+n[0], a[1]+n[1]), (b[0]+v[0]*factor, b[1]+v[1]*factor), (c[0]+m[0], c[1]+m[1])]
        result += offset if side == 1 else offset[::-1]
    return result


def profile(segment, width):
    if segment < 6:
        return outline(path(segment), width)
    h = width/2
    if segment == 7:
        return [(-.5, -h), (.5, -h), (.5, h), (-.5, h)]
    if segment == 8:  # Canonical west ring midpoint, stem toward the center (+X).
        return [(-h, -.5), (h, -.5), (h, -h), (.5, -h), (.5, h), (h, h), (h, .5), (-h, .5)]
    return [(-h, -.5), (h, -.5), (h, -h), (.5, -h), (.5, h), (h, h), (h, .5), (-h, .5), (-h, h), (-.5, h), (-.5, -h), (-h, -h)]


def generate(resources, art):
    directory = resources/'assets/mekgravity/models/block/solar'
    directory.mkdir(parents=True, exist_ok=True)
    for crown in (False, True):
        kind = 'crown' if crown else 'ring'
        for segment in range(9 if crown else 6):
            rows = ['# Original continuous solar beam; block-local units.', 'mtllib beam.mtl']
            count = 0
            def face(points, uv, normal):
                nonlocal count
                a, b, c = points[:3]
                d, e = [b[i]-a[i] for i in range(3)], [c[i]-a[i] for i in range(3)]
                cross = [d[1]*e[2]-d[2]*e[1], d[2]*e[0]-d[0]*e[2], d[0]*e[1]-d[1]*e[0]]
                if sum(cross[i]*normal[i] for i in range(3)) < 0:
                    points, uv = points[::-1], uv[::-1]
                for p, t in zip(points, uv):
                    rows.extend(['v '+' '.join(f'{v:.7f}' for v in p), 'vt '+' '.join(f'{v:.7f}' for v in t)])
                rows.append('f '+' '.join(f'{i}/{i}' for i in range(count+1, count+len(points)+1)))
                count += len(points)

            def prism(poly, low, high, material, cap_bottom=True, cap_top=True):
                rows.extend(['o '+material, 'usemtl '+material])
                signed = sum(a[0]*b[1]-b[0]*a[1] for a, b in zip(poly, poly[1:]+poly[:1]))
                # A subregion of each original 16px texture, preserving the neutral metal palette.
                u, v, U, V = (8/16, 0, 9/16, 1/16) if material == 'lamp' else (4.1/16, 4.1/16, 4.9/16, 4.9/16) if material == 'steel' else (8.1/16, 8.1/16, 8.9/16, 8.9/16)
                center = (0, 0)  # All profiles are star-shaped about the beam center.
                for a, b in zip(poly, poly[1:]+poly[:1]):
                    for y, sign, enabled in [(low, -1, cap_bottom), (high, 1, cap_top)]:
                        if enabled:
                            face([[q[0]+.5, y, q[1]+.5] for q in [center, a, b]],
                                 [[(u+U)/2, V], [u, v], [U, v]], [0, sign, 0])
                    face([[a[0]+.5, low, a[1]+.5], [b[0]+.5, low, b[1]+.5], [b[0]+.5, high, b[1]+.5], [a[0]+.5, high, a[1]+.5]],
                         [[u,v],[U,v],[U,V],[u,V]], [(b[1]-a[1])*signed, 0, (a[0]-b[0])*signed])

            # Only exposed surfaces: full intermediate caps would be hidden inside the
            # next layer. Ledges join the wider flanges to the narrow dark beam body.
            outer, inner = profile(segment, .625), profile(segment, .5)
            prism(outer, 4/16, 5/16, 'steel', cap_top=False)
            prism(inner, 5/16, 9/16, 'dark', cap_bottom=False, cap_top=False)
            prism(outer, 9/16, 10/16, 'steel', cap_bottom=False)
            rows.append('usemtl steel')
            for y, sign in [(5/16, 1), (9/16, -1)]:
                for i in range(len(outer)):
                    j = (i+1) % len(outer)
                    face([[p[0]+.5, y, p[1]+.5] for p in [outer[i],outer[j],inner[j],inner[i]]],
                         [[4.1/16,4.1/16],[4.9/16,4.1/16],[4.9/16,4.9/16],[4.1/16,4.9/16]], [0,sign,0])
            if not crown and segment != 5:
                prism(profile(segment, .05), 10/16+.001, 10.4/16, 'lamp')
            if segment == 5:  # Pillar mounts at four diagonal corner anchors.
                square = [(-.25,-.25),(.25,-.25),(.25,.25),(-.25,.25)]
                prism(square, 0, 4/16, 'steel', cap_top=False)
                if not crown:
                    prism(square, 10/16, 1, 'steel', cap_bottom=False)
            (directory/f'{kind}_{segment}.obj').write_text('\n'.join(rows)+'\n', encoding='utf-8', newline='\n')
    for active in (False, True):
        material = ''
        for name in ['steel', 'dark', 'lamp']:
            ambient = .8 if active and name == 'lamp' else .2 if name == 'lamp' else 0
            material += f'newmtl {name}\nKa {ambient} {ambient} {ambient}\nKd 1 1 1\nmap_Kd #{name}\n\n'
        (directory/('beam_active.mtl' if active else 'beam.mtl')).write_text(material.rstrip()+'\n', encoding='utf-8', newline='\n')
    mapping = {f'{x},{z}': {'ring': state(x,z), 'crown':state(x,z,True)} for x,z in NODES+[(4,i) for i in range(1,8)]+[(i,4) for i in range(1,8)]}
    (art/'solar-beam-states.json').write_text(json.dumps(mapping, indent=2)+'\n', encoding='utf-8', newline='\n')


def model(segment, crown=False, active=False):
    return {'parent':'minecraft:block/block', 'loader':'neoforge:obj',
            'model':f'mekgravity:models/block/solar/{"crown" if crown else "ring"}_{segment}.obj',
            'mtl_override':f'mekgravity:models/block/solar/beam{"_active" if active else ""}.mtl',
            'automatic_culling':False, 'shade_quads':True, 'emissive_ambient':True,
            'textures':{'steel':'mekgravity:block/orb_steel', 'dark':'mekgravity:block/orb_inner',
                        'lamp':'mekgravity:block/sun_active' if active else 'mekgravity:block/sun_idle',
                        'particle':'mekgravity:block/orb_steel'}}


def java_mapping():
    rows = []
    for x,z in sorted(set(NODES+[(4,i) for i in range(1,8)]+[(i,4) for i in range(1,8)])):
        r, face = state(x,z)
        c, _ = state(x,z,True)
        rows.append(f'  case {x*9+z} -> new int[]{{{r},{c},{FACES.index(face)}}};')
    return ''' private static int[] ringState(int x,int z){return switch(x*9+z){
'''+ '\n'.join(rows)+'''
  default -> new int[]{2,7,0};};}
 public static int ringSegment(int x,int z,boolean crown){return ringState(x,z)[crown?1:0];}
 public static Direction ringFacing(int x,int z,boolean crown){return switch(ringState(x,z)[2]){case 1->Direction.EAST;case 2->Direction.SOUTH;case 3->Direction.WEST;default->Direction.NORTH;};}
'''
