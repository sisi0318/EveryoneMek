// Inspect actual runtime geometry and original textures with per-pixel depth.
const {render}=require('./raster_preview.cjs');
render('coronal-chamber-preview.png',[
 {label:'Idle / runtime model',labelX:20,x:195,y:265,scale:160,instances:[{name:'coronal_chamber',transform:([x,y,z])=>[1-x,y,1-z]}]},
 {label:'Working / runtime model',labelX:420,x:605,y:265,scale:160,instances:[{name:'coronal_chamber_active',transform:([x,y,z])=>[1-x,y,1-z]}]}
],820,405).catch(e=>{process.stderr.write(e.stack);process.exitCode=1;});
