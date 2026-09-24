// Offline inspection of actual JSON/OBJ assets. No Minecraft client or texture repainting.
const fs=require('node:fs'),path=require('node:path');
const {createRequire}=require('node:module');
const root=path.resolve(__dirname,'..');
const sharp=createRequire(path.join(root,'art/package.json'))('sharp');
const assets=path.join(root,'src/main/resources/assets/mekgravity');
const modelCache=new Map();
const sub=(a,b)=>a.map((v,i)=>v-b[i]);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
function model(name){
  if(modelCache.has(name))return modelCache.get(name);
  const data=JSON.parse(fs.readFileSync(path.join(assets,'models/block',name+'.json'),'utf8'));
  const tex=key=>data.textures[key.replace('#','')],triangles=[];
  const polygon=(p,uv,texture,emissive=false)=>{
    for(let i=1;i<p.length-1;i++)triangles.push({p:[p[0],p[i],p[i+1]],uv:[uv[0],uv[i],uv[i+1]],texture,emissive});
  };
  if(data.loader==='neoforge:obj'){
    const positions=[],uvs=[],materials={};let current;
    const mtl=fs.readFileSync(path.join(assets,data.mtl_override.split(':')[1]),'utf8');
    for(const line of mtl.split(/\r?\n/)){
      const [op,...v]=line.trim().split(/\s+/);
      if(op==='newmtl')materials[current=v[0]]={};
      if(op==='map_Kd')materials[current].texture=tex(v[0]);
      if(op==='Ka')materials[current].emissive=Number(v[0])>0;
    }
    for(const line of fs.readFileSync(path.join(assets,data.model.split(':')[1]),'utf8').split(/\r?\n/)){
      const [op,...v]=line.trim().split(/\s+/);
      if(op==='v')positions.push(v.map(Number));
      if(op==='vt')uvs.push(v.map(Number));
      if(op==='usemtl')current=v[0];
      if(op==='f'){
        const indices=v.map(s=>s.split('/').map(n=>Number(n)-1));
        polygon(indices.map(i=>positions[i[0]]),indices.map(i=>uvs[i[1]]),materials[current].texture,materials[current].emissive);
      }
    }
  }else{
    let elements=data.elements;
    if(!elements){
      const textures=data.textures,f={};
      for(const side of ['north','south','east','west','up','down'])f[side]={texture:'#'+(textures.all?'all':side==='north'?'front':side==='up'?'top':'side'),uv:[0,0,16,16]};
      elements=[{from:[0,0,0],to:[16,16,16],faces:f}];
    }
    for(const e of elements){
      const [x,y,z]=e.from.map(v=>v/16),[X,Y,Z]=e.to.map(v=>v/16);
      const corners={north:[[X,Y,z],[x,Y,z],[x,y,z],[X,y,z]],south:[[x,Y,Z],[X,Y,Z],[X,y,Z],[x,y,Z]],west:[[x,Y,z],[x,Y,Z],[x,y,Z],[x,y,z]],east:[[X,Y,Z],[X,Y,z],[X,y,z],[X,y,Z]],up:[[x,Y,z],[X,Y,z],[X,Y,Z],[x,Y,Z]],down:[[x,y,Z],[X,y,Z],[X,y,z],[x,y,z]]};
      for(const [side,f]of Object.entries(e.faces)){
        const [u,v,U,V]=(f.uv||[0,0,16,16]).map(n=>n/16);
        polygon(corners[side].slice().reverse(),[[u,v],[U,v],[U,V],[u,V]].reverse(),tex(f.texture),Boolean(f.neoforge_data?.block_light));
      }
    }
  }
  modelCache.set(name,triangles);return triangles;
}
async function render(file,panels,width,height){
  const textureIds=new Map(),filters=new Map(),defs=[];let body='',index=0;
  const texture=async id=>{
    if(!textureIds.has(id)){
      const key='texture'+textureIds.size;textureIds.set(id,key);
      const buffer=await sharp(path.join(assets,'textures',id.split(':')[1]+'.png')).resize(256,256,{kernel:'nearest'}).png().toBuffer();
      defs.push(`<image id="${key}" width="1" height="1" href="data:image/png;base64,${buffer.toString('base64')}"/>`);
    }
    return textureIds.get(id);
  };
  for(const panel of panels){
    const elevation=panel.elevation??.4,eye=[1,elevation*2,1];
    const triangles=[];
    for(const instance of panel.instances){
      const transform=instance.transform||((p)=>p);
      for(const tri of instance.triangles||model(instance.name)){
        const p=tri.p.map(transform),normal=cross(sub(p[1],p[0]),sub(p[2],p[0]));
        if(dot(normal,eye)<=1e-9)continue;
        const depth=p.reduce((s,v)=>s+dot(v,eye),0)/3;
        const length=Math.sqrt(dot(normal,normal)),shade=tri.emissive?0:Math.max(0,.22-.20*normal[1]/length);
        triangles.push({...tri,p,depth,shade});
      }
    }
    triangles.sort((a,b)=>a.depth-b.depth);
    for(const tri of triangles){
      const p=tri.p.map(([x,y,z])=>[panel.x+(x-z)*panel.scale,panel.y+((x+z)*elevation-y)*panel.scale]);
      if(tri.color){
        const [r,g,b,a]=tri.color;body+=`<polygon points="${p.map(q=>q.join(',')).join(' ')}" fill="rgb(${r},${g},${b})" fill-opacity="${a/255}" style="mix-blend-mode:screen"/>`;continue;
      }
      const [a,b,c]=tri.uv,d=sub(b,a),e=sub(c,a),det=d[0]*e[1]-d[1]*e[0];if(Math.abs(det)<1e-12)continue;
      const q=sub(p[1],p[0]),r=sub(p[2],p[0]);
      const A=(q[0]*e[1]-r[0]*d[1])/det,B=(q[1]*e[1]-r[1]*d[1])/det,C=(r[0]*d[0]-q[0]*e[0])/det,D=(r[1]*d[0]-q[1]*e[0])/det;
      const matrix=[A,B,C,D,p[0][0]-A*a[0]-C*a[1],p[0][1]-B*a[0]-D*a[1]];
      // Raster clip edges need a sub-pixel overlap; the 3D model itself is unchanged.
      const center=[p.reduce((s,v)=>s+v[0],0)/3,p.reduce((s,v)=>s+v[1],0)/3];
      const points=p.map(v=>{const dx=v[0]-center[0],dy=v[1]-center[1],length=Math.hypot(dx,dy);return [v[0]+dx/length*.7,v[1]+dy/length*.7].join(',');}).join(' '),clip='face'+index++;
      defs.push(`<clipPath id="${clip}"><polygon points="${points}"/></clipPath>`);
      const slope=(1-tri.shade).toFixed(3);let filter=filters.get(slope);
      if(!filter){filter='light'+filters.size;filters.set(slope,filter);defs.push(`<filter id="${filter}" color-interpolation-filters="sRGB"><feComponentTransfer><feFuncR type="linear" slope="${slope}"/><feFuncG type="linear" slope="${slope}"/><feFuncB type="linear" slope="${slope}"/></feComponentTransfer></filter>`);}
      body+=`<g clip-path="url(#${clip})"><use href="#${await texture(tri.texture)}" transform="matrix(${matrix.join(' ')})" filter="url(#${filter})"/></g>`;
    }
    body+=`<text x="${panel.labelX??panel.x-100}" y="${panel.labelY??30}">${panel.label}</text>`;
  }
  const svg=`<svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg"><defs>${defs.join('')}</defs><rect width="${width}" height="${height}" fill="#30363d"/><style>image{image-rendering:pixelated}polygon{shape-rendering:crispEdges}text{fill:#ddd;font:17px sans-serif}</style>${body}</svg>`;
  await sharp(Buffer.from(svg)).png().toFile(path.join(root,'art',file));
}
module.exports={render,model,root};
