// Samples existing 16px textures from the actual generated JSON; never modifies bitmap assets.
const {render}=require('./gear_raster_preview.cjs');
render('black-hole-launcher-preview.png',[
  {x:180,y:320,scale:175,label:'BLACK HOLE LAUNCHER / FRONT',labelX:24,instances:[{name:'black_hole_launcher',transform:([x,y,z])=>[x,y,-z]}]},
  {x:655,y:320,scale:175,label:'REAR / CAPACITOR',labelX:530,instances:[{name:'black_hole_launcher'}]}
],1000,430).catch(error=>{console.error(error);process.exitCode=1;});
