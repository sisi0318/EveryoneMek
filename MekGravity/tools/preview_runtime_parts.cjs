const {render}=require('./model_preview.cjs');
render('runtime-parts-preview.png',[
  {label:'Column',labelX:20,x:110,y:175,scale:95,instances:[{name:'frame_formed'}]},
  {label:'Joint',labelX:255,x:350,y:175,scale:95,instances:[{name:'frame_joint'}]},
  {label:'Emitter',labelX:490,x:590,y:175,scale:95,instances:[{name:'basic_coil_formed_active',transform:([x,y,z])=>[1-x,y,1-z]}]},
  {label:'Controller',labelX:750,x:850,y:175,scale:95,instances:[{name:'reactor_formed_active',transform:([x,y,z])=>[1-x,y,1-z]}]}
],980,285).catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
