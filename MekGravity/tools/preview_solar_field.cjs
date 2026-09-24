// Actual Java SolarField snapshots; static offline views, not screenshots of Minecraft.
const fs=require('node:fs'),path=require('node:path');
const {render,root}=require('./model_preview.cjs');
const frames=JSON.parse(fs.readFileSync(path.join(root,'build/solar-field-frames.json'),'utf8'));
const sub=(a,b)=>a.map((v,i)=>v-b[i]);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
function ribbon(a,b,width,rgb,alpha){
  const d=sub(b,a),n=cross([1,.52,1],d),length=Math.hypot(...n);
  if(length<1e-10)return [];
  const off=n.map(v=>v/length*width),vertices=[sub(a,off),sub(b,off),b.map((v,i)=>v+off[i]),a.map((v,i)=>v+off[i])];
  return [[0,1,2],[0,2,3]].map(ids=>({p:ids.map(i=>vertices[i]),color:[rgb>>16&255,rgb>>8&255,rgb&255,alpha],emissive:true}));
}
const panels=frames.map((f,index)=>{
  const triangles=[];
  for(const row of f.ribbons){const a=row.slice(0,3),b=row.slice(3,6),[width,rgb,alpha,halo]=row.slice(6);if(halo)triangles.push(...ribbon(a,b,width*3,rgb,Math.floor(alpha/5)));triangles.push(...ribbon(a,b,width,rgb,alpha));}
  const turn=f.phase*1.2*Math.PI/180,tilt=12*Math.PI/180;
  return {label:`Phase ${f.phase} / running`,labelX:25+index*470,labelY:35,x:235+index*470,y:290,scale:65,elevation:.26,instances:[
    {name:'solar/sun_active',transform:([x,y,z])=>{x-=.5;y-=.5;z-=.5;const X=x*Math.cos(tilt)-y*Math.sin(tilt),Y=x*Math.sin(tilt)+y*Math.cos(tilt);return [X*Math.cos(turn)+z*Math.sin(turn),Y,-X*Math.sin(turn)+z*Math.cos(turn)];}},
    {name:'solar/focus_basic_active',transform:([x,y,z])=>[x-.5,-(z-.5)-3,y-.5]},
    {name:'solar/focus_basic_active',transform:([x,y,z])=>[x-.5,z-.5+3,-(y-.5)]},
    {triangles}
  ]};
});
render('solar-field-preview.png',panels,1410,600).catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
