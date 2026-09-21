// Actual static core meshes. Dynamic beams/orbit are intentionally not drawn.
const {render}=require('./model_preview.cjs');
render('core-and-fuel-preview.png',[
  {label:'Core - idle',x:175,y:180,scale:145,instances:[{name:'core'}]},
  {label:'Core - active',x:510,y:180,scale:145,instances:[{name:'core_active'}]}
],700,310).catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
