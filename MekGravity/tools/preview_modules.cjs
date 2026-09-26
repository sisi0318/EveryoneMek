// Static inspection of the actual generated meshes and original 16px materials.
const {render}=require('./raster_preview.cjs');
const names=['flare_captor','gravity_forge','core_tuner','gravity_node','stellar_observatory'];
render('orbital-modules-preview.png',names.map((name,i)=>({
 label:['Flare captor','Gravity forge','Core tuner','Logistics node','Observatory'][i],
 labelX:20+i*250,x:130+i*250,y:235,scale:95,
 instances:[{name:name+'_active',transform:([x,y,z])=>[1-x,y,1-z]}]
})),1270,360).catch(e=>{process.stderr.write(e.stack);process.exitCode=1;});
