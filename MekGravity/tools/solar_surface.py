"""Spherified-cube photosphere: even texel density, no longitude seam or pinched poles."""
import math


def generate(resources):
    rows=['# Original MekGravity spherified-cube photosphere, radius 1.25 blocks.', 'mtllib sun.mtl', 'usemtl surface']
    index=0
    # One original 16px tile per cube face rather than stretching one tile around 360 degrees.
    faces={
        'east':lambda u,v:(1,1-2*v,1-2*u), 'west':lambda u,v:(-1,1-2*v,2*u-1),
        'up':lambda u,v:(2*u-1,1,2*v-1), 'down':lambda u,v:(2*u-1,-1,1-2*v),
        'south':lambda u,v:(2*u-1,1-2*v,1), 'north':lambda u,v:(1-2*u,1-2*v,-1)
    }
    def point(face,u,v):
        x,y,z=face(u,v)
        return (x*math.sqrt(1-y*y/2-z*z/2+y*y*z*z/3)*1.25,
                y*math.sqrt(1-z*z/2-x*x/2+z*z*x*x/3)*1.25,
                z*math.sqrt(1-x*x/2-y*y/2+x*x*y*y/3)*1.25)
    for name,face in faces.items():
        rows.append('o '+name)
        for y in range(16):
            for x in range(16):
                uv=[(x/16,y/16),((x+1)/16,y/16),((x+1)/16,(y+1)/16),(x/16,(y+1)/16)]
                points=[point(face,*p) for p in uv];a,b,c=points[:3]
                d,e=[b[i]-a[i] for i in range(3)],[c[i]-a[i] for i in range(3)]
                normal=[d[1]*e[2]-d[2]*e[1],d[2]*e[0]-d[0]*e[2],d[0]*e[1]-d[1]*e[0]]
                if sum(normal[i]*a[i] for i in range(3))<0:points.reverse();uv.reverse()
                for p,t in zip(points,uv):
                    rows.append('v '+' '.join(f'{n+.5:.7f}' for n in p))
                    rows.append('vt '+' '.join(f'{n:.7f}' for n in t))
                    rows.append('vn '+' '.join(f'{n/1.25:.7f}' for n in p))
                rows.append('f '+' '.join(f'{i}/{i}/{i}' for i in range(index+1,index+5)));index+=4
    directory=resources/'assets/mekgravity/models/block/solar'
    (directory/'sun.obj').write_text('\n'.join(rows)+'\n',encoding='utf-8',newline='\n')
    (directory/'sun.mtl').write_text('newmtl surface\nKa 1 1 1\nKd 1 1 1\nmap_Kd #surface\n',encoding='utf-8',newline='\n')
