// Perspective depth rasterization of the actual weapon JSON + matrices exported by VerifyGearPoses.
// Original texture pixels are sampled; no game assets are edited.
const fs=require('node:fs'),path=require('node:path');
const {model,root}=require('./gear_model_preview.cjs');
const sharp=require(require.resolve('sharp',{paths:[path.join(root,'art'),path.join(root,'../Botania/art')]}));
const dir=path.join(root,'build/weapon-visual-check'),panels=JSON.parse(fs.readFileSync(path.join(dir,'poses.json')));
const mul=(m,p)=>[0,1,2,3].map(r=>m[r]*p[0]+m[4+r]*p[1]+m[8+r]*p[2]+m[12+r]*(p[3]??1));
function box(b){const [x,y,z]=b.from,[X,Y,Z]=b.to,p=[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],out=[];
  for(const f of [[0,1,2,3],[4,7,6,5],[0,4,5,1],[3,2,6,7],[0,3,7,4],[1,5,6,2]])for(const t of [[0,1,2],[0,2,3]])out.push({p:t.map(i=>mul(b.matrix,p[f[i]])),uv:[[0,0],[1,0],[1,1]],color:[82,106,112]});return out;}
(async()=>{
  const textures=new Map();for(const name of ['rail_lance','thunder_blade'])for(const t of model(name+'_fallback'))if(!textures.has(t.texture))textures.set(t.texture,null);
  for(const key of textures.keys()){const {data,info}=await sharp(path.join(root,'src/main/resources/assets/overloadcore/textures',key.split(':')[1]+'.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});textures.set(key,{data,...info});}
  for(const first of [true,false]){
    const group=panels.filter(p=>p.first===first),pw=640,ph=360,h=ph*4,w=pw*2,pixels=Buffer.alloc(w*h*4),depth=new Float32Array(w*h).fill(Infinity);
    for(let i=0;i<w*h;i++){pixels[i*4]=27;pixels[i*4+1]=32;pixels[i*4+2]=39;pixels[i*4+3]=255;}
    for(let n=0;n<group.length;n++){
      const panel=group[n],ox=n%2*pw,oy=Math.floor(n/2)*ph;
      const triangles=model(panel.name).map(t=>({...t,p:t.p.map(p=>mul(panel.matrix,p))})).concat(panel.boxes.flatMap(box));
      for(const t of triangles){
        const p=t.p.map(p=>mul(panel.projection,p));if(p.some(p=>p[3]<=0))continue;
        const q=p.map(p=>[ox+(p[0]/p[3]+1)*pw/2,oy+(1-p[1]/p[3])*ph/2,p[2]/p[3],1/p[3]]),[a,b,c]=q;
        const den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(den)<1e-8)continue;
        const xmin=Math.max(ox,Math.floor(Math.min(a[0],b[0],c[0]))),xmax=Math.min(ox+pw-1,Math.ceil(Math.max(a[0],b[0],c[0]))),ymin=Math.max(oy+27,Math.floor(Math.min(a[1],b[1],c[1]))),ymax=Math.min(oy+ph-1,Math.ceil(Math.max(a[1],b[1],c[1])));
        for(let y=ymin;y<=ymax;y++)for(let x=xmin;x<=xmax;x++){
          const A=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/den,B=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/den,C=1-A-B;if(A<0||B<0||C<0)continue;
          const at=y*w+x,z=A*a[2]+B*b[2]+C*c[2];if(z>=depth[at])continue;depth[at]=z;
          let color=t.color;if(!color){const tex=textures.get(t.texture),total=A*a[3]+B*b[3]+C*c[3],uv=[0,1].map(i=>(A*a[3]*t.uv[0][i]+B*b[3]*t.uv[1][i]+C*c[3]*t.uv[2][i])/total),u=Math.max(0,Math.min(tex.width-1,Math.floor(uv[0]*tex.width))),v=Math.max(0,Math.min(tex.height-1,Math.floor(uv[1]*tex.height)));color=tex.data.subarray((v*tex.width+u)*4,(v*tex.width+u)*4+3);}
          for(let i=0;i<3;i++)pixels[at*4+i]=color[i];
        }
      }
    }
    const svg=`<svg width="${w}" height="${h}"><style>text{font:16px sans-serif;fill:#cdd9da}</style>${group.map((p,i)=>`<text x="${i%2*pw+12}" y="${Math.floor(i/2)*ph+21}">${p.label}</text>${first?`<path d="M${i%2*pw+pw/2-5} ${Math.floor(i/2)*ph+ph/2}h10m-5 -5v10" stroke="#8b9394"/>`:''}`).join('')}</svg>`;
    await sharp(pixels,{raw:{width:w,height:h,channels:4}}).composite([{input:Buffer.from(svg)}]).png().toFile(path.join(dir,first?'first-person.png':'third-person.png'));
  }
})().catch(e=>{console.error(e);process.exitCode=1;});
