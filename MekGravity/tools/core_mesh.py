"""Original low-poly core geometry; textures are exported from image_gen artwork."""
import math


def generate_core(root):
    lines=['# Original MekGravity floating core, in block-local coordinates.', 'mtllib core.mtl']
    count=0

    def face(points,uv,normal):
        nonlocal count
        a,b,c=points[:3]
        ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)]
        cross=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
        if sum(cross[i]*normal[i] for i in range(3))<0:
            points=list(reversed(points));uv=list(reversed(uv))
        for p,t in zip(points,uv):
            lines.append('v '+' '.join(f'{v+.5:.7f}' for v in p))
            lines.append('vt '+' '.join(f'{v:.7f}' for v in t))
        lines.append('f '+' '.join(f'{i}/{i}' for i in range(count+1,count+len(points)+1)))
        count+=len(points)

    def sphere(u,v):
        az=u*math.tau;lat=v*math.pi
        return (.375*math.sin(lat)*math.cos(az),.375*math.cos(lat),.375*math.sin(lat)*math.sin(az))

    lines.extend(['o energy','usemtl energy'])
    for row in range(8):
        for col in range(16):
            u,U=col/16,(col+1)/16;v,V=row/8,(row+1)/8
            uv=[(u,v),(U,v),(U,V),(u,V)]
            if row==0:uv=[((u+U)/2,v),(U,V),(u,V)]
            elif row==7:uv=[(u,v),(U,v),((u+U)/2,V)]
            points=[sphere(*p) for p in uv]
            face(points,uv,[sum(p[i] for p in points) for i in range(3)])

    for index,(radius,tilt,turn) in enumerate([(.53,35,20),(.66,-55,-20)]):
        lines.extend([f'o ring_{index}'])
        rx,rz=math.radians(tilt),math.radians(turn)

        def rotate(p):
            x,y,z=p;y,z=y*math.cos(rx)-z*math.sin(rx),y*math.sin(rx)+z*math.cos(rx)
            return (x*math.cos(rz)-y*math.sin(rz),x*math.sin(rz)+y*math.cos(rz),z)

        def point(angle,r,h):return rotate((math.cos(angle)*r,h,math.sin(angle)*r))

        inner,outer=radius-.018,radius+.018
        for j in range(24):
            a,b=j*math.tau/24,(j+1)*math.tau/24;mid=(a+b)/2
            for h,normal in [(.012,(0,1,0)),(-.012,(0,-1,0))]:
                lines.append('usemtl steel')
                face([point(a,inner,h),point(b,inner,h),point(b,outer,h),point(a,outer,h)],[(j/24,0),((j+1)/24,0),((j+1)/24,1),(j/24,1)],rotate(normal))
            for r,sign,material in [(outer,1,'steel'),(inner,-1,'inner')]:
                lines.append('usemtl '+material)
                face([point(a,r,-.012),point(b,r,-.012),point(b,r,.012),point(a,r,.012)],[(j/24,0),((j+1)/24,0),((j+1)/24,1),(j/24,1)],rotate((sign*math.cos(mid),0,sign*math.sin(mid))))
    directory=root/'assets/mekgravity/models/block';directory.mkdir(parents=True,exist_ok=True)
    (directory/'core.obj').write_text('\n'.join(lines)+'\n',encoding='utf-8',newline='\n')
    # Same approved mesh, split into independently rotating parts. Keep the full OBJ for item rendering.
    positions=[];uvs=[];groups={};group=None;material=None
    for line in lines:
        row=line.split()
        if row[0]=='v':positions.append(row[1:])
        elif row[0]=='vt':uvs.append(row[1:])
        elif row[0]=='o':group=row[1];groups[group]=[]
        elif row[0]=='usemtl':material=row[1]
        elif row[0]=='f':groups[group].append((material,[tuple(map(int,v.split('/'))) for v in row[1:]]))
    for group,faces in groups.items():
        output=['# Original core part; generated from core.obj geometry.','mtllib core.mtl','o '+group];index=0
        for material,face in faces:
            output.append('usemtl '+material)
            for v,t in face:output.extend(['v '+' '.join(positions[v-1]),'vt '+' '.join(uvs[t-1])])
            output.append('f '+' '.join(f'{i}/{i}' for i in range(index+1,index+len(face)+1)));index+=len(face)
        (directory/('core_'+group+'.obj')).write_text('\n'.join(output)+'\n',encoding='utf-8',newline='\n')
    # NeoForge 21.1.241 ObjModel maps MTL Ka to minimum baked light. Only the energy is emissive.
    for active in [False,True]:
        ambient=.8 if active else .27
        material=f'newmtl energy\nKa {ambient} {ambient} {ambient}\nKd 1 1 1\nmap_Kd #energy\n\nnewmtl steel\nKa 0 0 0\nKd 1 1 1\nmap_Kd #steel\n\nnewmtl inner\nKa 0 0 0\nKd 1 1 1\nmap_Kd #inner\n'
        (directory/('core_active.mtl' if active else 'core.mtl')).write_text(material,encoding='utf-8',newline='\n')
