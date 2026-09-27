// Mechanical exports from original ImageGen atlases; never redraw symbols or copy Mek pixels into runtime assets.
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..');
const sharp=require(require.resolve('sharp',{paths:[path.join(root,'art'),path.join(root,'../Botania/art')]}));
const names=['residual_coupling_unit','phase_heat_sink','magnetic_compensation','ward_capacitor','afterguard_stabilizer','resonant_discharge','charge_accelerator','rail_magazine','rail_focus','rail_piercing','blade_field'];
(async()=>{
  const out=path.join(root,'src/main/resources/assets/overloadcore/textures/item');fs.mkdirSync(out,{recursive:true});
  const panels=path.join(root,'art/source/mek-module-panels-v2.png'),materials=path.join(root,'art/source/mek-equipment-materials-v2.png');
  const p=await sharp(panels).metadata(),m=await sharp(materials).metadata();
  function cell(meta,cols,rows,index){let x=index%cols,y=Math.floor(index/cols),left=Math.round(x*meta.width/cols),top=Math.round(y*meta.height/rows);return {left,top,width:Math.round((x+1)*meta.width/cols)-left,height:Math.round((y+1)*meta.height/rows)-top};}
  for(let i=0;i<names.length;i++){
    const b=cell(p,4,3,i);b.left+=Math.round(b.width*.15);b.top+=Math.round(b.height*.125);b.width=Math.round(b.width*.7);b.height=Math.round(b.height*.75);
    await sharp(panels).extract(b).resize(8,6,{kernel:'nearest',fit:'fill'}).ensureAlpha()
      .extend({left:5,right:3,top:3,bottom:7,background:{r:0,g:0,b:0,alpha:0}}).png().toFile(path.join(out,'module_'+names[i]+'_panel.png'));
  }
  const tiles=['gear_alloy','gear_graphite','gear_grip','gear_circuit'];
  for(let i=0;i<tiles.length;i++)await sharp(materials).extract(cell(m,2,2,i)).resize(16,16,{kernel:'nearest',fit:'fill'}).png().toFile(path.join(out,tiles[i]+'.png'));
  // Reference-only contact sheet. The upstream frame stays in ignored build/reference, never in the JAR.
  const frame=path.join(root,'build/reference/mek-gear-redesign/module_base.png');
  if(fs.existsSync(frame)){
    const entries=[];for(let i=0;i<names.length;i++){const icon=await sharp(frame).composite([{input:path.join(out,'module_'+names[i]+'_panel.png')}]).png().toBuffer();
      entries.push({input:await sharp(icon).resize(128,128,{kernel:'nearest'}).png().toBuffer(),left:16+(i%4)*160,top:12+Math.floor(i/4)*150});}
    await sharp({create:{width:640,height:450,channels:4,background:'#36383c'}}).composite(entries).png().toFile(path.join(root,'art/mek-module-icons-v2.png'));
  }
  console.log('Exported 11 original display overlays and 4 equipment materials at true 16x16.');
})().catch(e=>{console.error(e);process.exitCode=1;});
