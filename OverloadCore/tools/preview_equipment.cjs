// Offline renders of the actual generated JSON models and unchanged 16px palette.
const {render}=require('./gear_raster_preview.cjs');
const names=['phase_heat_sink','magnetic_compensation','ward_capacitor','afterguard_stabilizer','residual_reservoir','resonant_discharge','charge_accelerator','rail_magazine','rail_focus','rail_piercing','blade_field'];
(async()=>{
  await render('equipment-modules-preview.png',names.map((name,i)=>({label:name,labelX:15+i%4*280,labelY:30+Math.floor(i/4)*210,x:140+i%4*280,y:155+Math.floor(i/4)*210,scale:95,elevation:.7,instances:[{name:'module_'+name,transform:([x,y,z])=>[x-.5,y,z-.5]}]})),1120,650);
  await render('weapons-preview.png',['rail_lance','thunder_blade'].map((name,i)=>({label:name+' (baked fallback)',labelX:30+i*540,labelY:35,x:270+i*540,y:320,scale:150,elevation:.35,instances:[{name:name+'_fallback',transform:([x,y,z])=>[x-.5,y-.4,z-.5]}]})),1080,500);
})().catch(error=>{console.error(error);process.exitCode=1;});
