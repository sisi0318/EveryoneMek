// Offline inspection of actual JSON/OBJ assets. No Minecraft client or texture repainting.
const fs=require('node:fs'),path=require('node:path');
const {createRequire}=require('node:module');
const root=path.resolve(__dirname,'..');
const sharp=require(require.resolve('sharp',{paths:[path.join(root,'art'),path.join(root,'../Botania/art')]}));
const assets=path.join(root,'src/main/resources/assets/overloadcore');
const modelCache=new Map();
const sub=(a,b)=>a.map((v,i)=>v-b[i]);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
function model(name){
  if(modelCache.has(name))return modelCache.get(name);
  const data=JSON.parse(fs.readFileSync(path.join(assets,'models/item',name+'.json'),'utf8'));
  const tex=key=>data.textures[key.replace('#','')],triangles=[];
  const polygon=(p,uv,texture,emissive=false,tint=null)=>{
    for(let i=1;i<p.length-1;i++)triangles.push({p:[p[0],p[i],p[i+1]],uv:[uv[0],uv[i],uv[i+1]],texture,emissive,tint});
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
        polygon(corners[side].slice().reverse(),[[u,v],[U,v],[U,V],[u,V]].reverse(),tex(f.texture),Boolean(f.neoforge_data?.block_light),f.neoforge_data?.color);
      }
    }
  }
  modelCache.set(name,triangles);return triangles;
}
module.exports={model,root};
