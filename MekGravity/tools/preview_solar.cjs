// Project the actual runtime parts from the checked 9x9x9 layout. Dynamic effects are not a game screenshot.
const fs=require('node:fs'),path=require('node:path');
const {render,root}=require('./model_preview.cjs');
const plan=JSON.parse(fs.readFileSync(path.join(root,'art/solar-design/layout.json'),'utf8'));
const turn=([x,y,z],face)=>{
  if(face==='south')[x,z]=[-x,-z];if(face==='east')[x,z]=[-z,x];if(face==='west')[x,z]=[z,-x];
  if(face==='up')[y,z]=[-z,y];if(face==='down')[y,z]=[z,-y];return [x,y,z];
};
const instances=[];
for(const p of plan.blocks){
  const [x,y,z]=p.pos;let name,face=p.face||'north';
  if(p.kind==='seed'){name='sun_active';}
  else if(p.kind==='collector'){
    const column=x===0?5-z:x===8?z-3:z===0?x-3:5-x;
    name=`collector_${(y-3)*3+column}_active`;
  }else name={ring:'ring_basic_active',focus:'focus_basic_active',controller:'controller_active',output:'energy_output',ignition:'energy'}[p.kind]||p.kind;
  instances.push({name:'solar/'+name,transform:q=>{
    let [X,Y,Z]=turn(q.map(v=>v-.5),['focus','collector','controller','output','ignition','fuel'].includes(p.kind)?face:'north');
    const wx=4.5-(x+X+.5),wz=4.5-(z+Z+.5),a=28*Math.PI/180;
    return [4.5+wx*Math.cos(a)-wz*Math.sin(a),y+Y+.5,4.5+wx*Math.sin(a)+wz*Math.cos(a)];
  }});
}
render('solar-runtime-preview.png',[{label:'Miniature sun - runtime geometry / static preview',labelX:24,x:560,y:520,scale:53,elevation:.2,instances}],1140,820).catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
