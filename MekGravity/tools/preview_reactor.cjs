// Real default layout and solid models. Clear glass borders and dynamic effects omitted.
const fs=require('node:fs'),path=require('node:path');
const {render,root}=require('./model_preview.cjs');
const layout=JSON.parse(fs.readFileSync(path.join(root,'../docs/gravity-reactor/layout.json'),'utf8'));
const special=new Map(layout.blocks.map(b=>[b.pos.join(','),b]));
const instances=[];
for(let x=0;x<7;x++)for(let y=0;y<7;y++)for(let z=0;z<7;z++){
  const spec=special.get([x,y,z].join(',')),edges=[x,y,z].filter(v=>v===0||v===6).length;
  const kind=spec?.kind||(edges>=2?'frame':edges&&(y===0||y===6)?'casing':null);
  const name=kind==='frame'&&edges===3?'frame_joint':{frame:'frame_formed',casing:'casing_formed',controller:'reactor_formed',fuel:'fuel_formed',excitation:'energy_formed',output:'energy_formed_output',coil:'basic_coil_formed_active',core:'core_active'}[kind];
  if(!name)continue;
  instances.push({name,transform:p=>{
    let [X,Y,Z]=p.map(v=>v-.5);
    if(kind==='coil'){
      if(x===1)[X,Z]=[-Z,X];else if(x===5)[X,Z]=[Z,-X];
      else if(y===1)[Y,Z]=[-Z,Y];else if(y===5)[Y,Z]=[Z,-Y];
      else if(z===1)[X,Z]=[-X,-Z];
    }
    if(kind==='frame'&&edges===2){
      if(y===0||y===6){if(z===0||z===6)[X,Y]=[Y,-X];else [Y,Z]=[-Z,Y];}
    }
    if(kind==='casing')[Y,Z]=y===0?[-Z,Y]:[Z,-Y];
    if(kind==='output')[X,Z]=[-X,-Z];
    const worldX=3.5-(x+X+.5),worldZ=3.5-(z+Z+.5),angle=22*Math.PI/180;
    return [3.5+worldX*Math.cos(angle)-worldZ*Math.sin(angle),y+Y+.5,3.5+worldX*Math.sin(angle)+worldZ*Math.cos(angle)];
  }});
}
render('reactor-assembled-preview.png',[
  {label:'Assembled shell - actual models / clear glass omitted',labelX:30,x:510,y:485,scale:64,elevation:.22,instances},
  {label:'Core detail',labelX:925,labelY:610,x:980,y:710,scale:75,instances:[{name:'core_active'}]}
],1100,800).catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
