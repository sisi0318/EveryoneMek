const {render}=require('./raster_preview.cjs');
// Historical alpha.12 UV mesh for the visual comparison; it uses the unchanged old solar tile.
function previousSurface(){
  const result=[],point=(u,v)=>[1.25*Math.sin(v*Math.PI)*Math.cos(u*Math.PI*2),1.25*Math.cos(v*Math.PI),1.25*Math.sin(v*Math.PI)*Math.sin(u*Math.PI*2)];
  for(let y=0;y<16;y++)for(let x=0;x<32;x++){
    const u=x/32,U=(x+1)/32,v=y/16,V=(y+1)/16,uv=y===0?[[(u+U)/2,v],[U,V],[u,V]]:y===15?[[u,v],[U,v],[(u+U)/2,V]]:[[u,v],[U,v],[U,V],[u,V]],p=uv.map(([u,v])=>point(u,v));
    for(let i=1;i<p.length-1;i++){const ids=[0,i,i+1];result.push({p:ids.map(j=>p[j]),uv:ids.map(j=>uv[j]),texture:'mekgravity:block/sun_active',emissive:true});}
  }
  return result;
}
const spin=([x,y,z])=>{const a=.5;return [x*Math.cos(a)+z*Math.sin(a),y,-x*Math.sin(a)+z*Math.cos(a)];};
render('photosphere-comparison.png',[
  {label:'Before / alpha.12',labelX:28,x:310,y:340,scale:144,elevation:.36,instances:[{triangles:previousSurface(),transform:spin}]},
  {label:'After / alpha.13',labelX:648,x:930,y:340,scale:144,elevation:.36,instances:[{name:'solar/sun_active',transform:p=>spin(p.map(v=>v-.5))}]}
],1240,620).catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
