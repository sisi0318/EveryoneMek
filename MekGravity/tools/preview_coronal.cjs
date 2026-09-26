// Inspect actual runtime geometry and original textures with per-pixel depth.
const {render}=require('./raster_preview.cjs');
render('coronal-chamber-preview.png',[
 {label:'Thin dock / runtime geometry',labelX:20,x:175,y:320,scale:130,instances:[{name:'coronal_chamber_active',transform:([x,y,z])=>[1-x,y,1-z]}]},
 {label:'Mounted against the collector wing',labelX:400,x:625,y:330,scale:80,instances:[
   ...Array.from({length:9},(_,i)=>({name:'solar/collector_'+i,transform:([x,y,z])=>[1-i%3+x,Math.floor(i/3)+y-1,z-1]})),
   {name:'coronal_chamber_active',transform:([x,y,z])=>[1-x,y,1-z]}
 ]}
],900,520).catch(e=>{process.stderr.write(e.stack);process.exitCode=1;});
