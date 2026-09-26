// Inspect the actual item OBJ, materials and depth. Static shader-off preview, not a game screenshot.
const fs=require('node:fs'),path=require('node:path');
const {model,root}=require('./model_preview.cjs');
const {render}=require('./raster_preview.cjs');
const rotate=angle=>([x,y,z])=>{x-=.5;y-=.5;z-=.5;return [x*Math.cos(angle)+z*Math.sin(angle),y,-x*Math.sin(angle)+z*Math.cos(angle)];};
const current=model('../item/flare_cell_fallback'),panels=[];
// Optional captured previous geometry isolates the casing change around the same spherical core.
if(process.argv[2])panels.push({label:'Previous frame / same core',triangles:[...JSON.parse(fs.readFileSync(path.resolve(process.argv[2]),'utf8')),...current.filter(t=>t.texture.endsWith('stellar_surface_particle'))],angle:0});
panels.push(...['New / front','New / rear','New / side'].map((label,i)=>({label,triangles:current,angle:[0,Math.PI,Math.PI/2][i]})));
render('flare-cell-restraint-preview.png',panels.map((p,i)=>({label:p.label,labelX:20+i*300,x:150+i*300,y:185,scale:230,elevation:.35,instances:[{triangles:p.triangles,transform:rotate(p.angle)}]})),panels.length*300,340)
 .catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
