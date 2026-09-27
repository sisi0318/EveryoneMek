// Offline renders of the actual generated JSON models and unchanged 16px palette.
const {render}=require('./gear_raster_preview.cjs');
(async()=>{
  // Flat module contact sheet is produced by export_mek_gear.cjs using the actual Mek frame.
  await render('weapons-preview.png',['rail_lance','thunder_blade'].map((name,i)=>({label:name+' (baked fallback)',labelX:30+i*540,labelY:35,x:270+i*540,y:320,scale:150,elevation:.35,instances:[{name:name+'_fallback',transform:([x,y,z])=>[x-.5,y-.4,z-.5]}]})),1080,500);
})().catch(error=>{console.error(error);process.exitCode=1;});
