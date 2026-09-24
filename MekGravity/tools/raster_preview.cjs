// Offline triangle rasterization of native OBJ/JSON assets with a depth buffer.
// Texture pixels are sampled unchanged with nearest filtering; this never edits game textures.
const fs=require('node:fs'),path=require('node:path'),{createRequire}=require('node:module');
const {model,root}=require('./model_preview.cjs');
const sharp=createRequire(path.join(root,'art/package.json'))('sharp');
const sub=(a,b)=>a.map((v,i)=>v-b[i]);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
async function render(file,panels,width,height){
  const pixels=Buffer.alloc(width*height*4),depth=new Float64Array(width*height).fill(-Infinity),textures=new Map(),triangles=[];
  for(let i=0;i<width*height;i++){pixels[i*4]=48;pixels[i*4+1]=54;pixels[i*4+2]=61;pixels[i*4+3]=255;}
  for(const panel of panels){
    const elevation=panel.elevation??.4,eye=[1,2*elevation,1],verticalScale=Math.sqrt(2/(1+2*elevation*elevation));
    for(const instance of panel.instances)for(const tri of instance.triangles||model(instance.name)){
      const p=tri.p.map(instance.transform||((p)=>p)),normal=cross(sub(p[1],p[0]),sub(p[2],p[0]));
      if(dot(normal,eye)<=1e-10)continue;
      triangles.push({...tri,p:p.map(([x,y,z])=>[panel.x+(x-z)*panel.scale,panel.y+((x+z)*elevation-y)*panel.scale*verticalScale,dot([x,y,z],eye)]),shade:tri.emissive?1:1-Math.max(0,.22-.20*normal[1]/Math.hypot(...normal))});
      if(tri.texture&&!textures.has(tri.texture))textures.set(tri.texture,null);
    }
  }
  await Promise.all([...textures.keys()].map(async id=>{
    const p=path.join(root,'src/main/resources/assets/mekgravity/textures',id.split(':')[1]+'.png');
    const {data,info}=await sharp(p).ensureAlpha().raw().toBuffer({resolveWithObject:true});textures.set(id,{data,...info});
  }));
  function draw(tri,additive){
    const [a,b,c]=tri.p,den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-9)return;
    const minX=Math.max(0,Math.floor(Math.min(a[0],b[0],c[0]))),maxX=Math.min(width-1,Math.ceil(Math.max(a[0],b[0],c[0]))),minY=Math.max(0,Math.floor(Math.min(a[1],b[1],c[1]))),maxY=Math.min(height-1,Math.ceil(Math.max(a[1],b[1],c[1])));
    const tex=textures.get(tri.texture);
    for(let y=minY;y<=maxY;y++)for(let x=minX;x<=maxX;x++){
      const A=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,B=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,C=1-A-B;
      if(A<0||B<0||C<0)continue;const i=y*width+x,z=A*a[2]+B*b[2]+C*c[2];if(z<depth[i]-1e-8)continue;
      if(additive){const [r,g,b,alpha]=tri.color;for(let channel=0;channel<3;channel++)pixels[i*4+channel]=Math.min(255,pixels[i*4+channel]+[r,g,b][channel]*alpha/255);}
      else{
        const u=A*tri.uv[0][0]+B*tri.uv[1][0]+C*tri.uv[2][0],v=A*tri.uv[0][1]+B*tri.uv[1][1]+C*tri.uv[2][1];
        const tx=Math.max(0,Math.min(tex.width-1,Math.floor(u*tex.width))),ty=Math.max(0,Math.min(tex.height-1,Math.floor(v*tex.height))),at=(ty*tex.width+tx)*4;
        if(tex.data[at+3]<128)continue;depth[i]=z;for(let channel=0;channel<3;channel++)pixels[i*4+channel]=Math.round(tex.data[at+channel]*tri.shade);
      }
    }
  }
  for(const tri of triangles)if(!tri.color)draw(tri,false);
  for(const tri of triangles)if(tri.color)draw(tri,true);
  const escape=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;');
  const labels=`<svg width="${width}" height="${height}"><style>text{fill:#ddd;font:17px sans-serif}</style>${panels.map(p=>`<text x="${p.labelX??p.x-100}" y="${p.labelY??30}">${escape(p.label)}</text>`).join('')}</svg>`;
  await sharp(pixels,{raw:{width,height,channels:4}}).composite([{input:Buffer.from(labels)}]).png().toFile(path.join(root,'art',file));
}
module.exports={render,root};
