// Offline views of the shipped JSON geometry and native textures, not game screenshots.
const fs = require('node:fs'), path = require('node:path'), cp = require('node:child_process');
const root = path.resolve(__dirname, '..');
const sharp = require(require.resolve('sharp', {paths: [path.join(root, 'art')]}));
const modelPath = path.join(root, 'src/main/resources/assets/botanicalmekanism/models');
const item = JSON.parse(fs.readFileSync(path.join(modelPath, 'item/mana_storage_cell.json')));
const drive = JSON.parse(fs.readFileSync(path.join(modelPath, 'block/drive/mana_storage_cell.json')));
const temp = path.join(root, 'build/mana-cell-preview');
fs.mkdirSync(temp, {recursive: true});
const extraction = cp.spawnSync('python', ['-X', 'utf8', '-'], {encoding: 'utf8', input: `
import json, zipfile
from pathlib import Path
r=Path(${JSON.stringify(root.replaceAll('\\', '/'))})
m=json.loads((r/'src/main/resources/assets/botanicalmekanism/models/item/mana_storage_cell.json').read_text())
with zipfile.ZipFile(r/'build/dependencies/botania-neoforge-1.21.1-456-SNAPSHOT.jar') as z:
    for key, loc in m['textures'].items():
        ns, p=loc.split(':')
        (r/'build/mana-cell-preview'/f'{key}.png').write_bytes(z.read(f'assets/{ns}/textures/{p}.png'))
`});
if (extraction.status !== 0) throw new Error(extraction.stderr);

const rotate = ([x,y,z], [rx,ry]) => {
  rx *= Math.PI / 180; ry *= Math.PI / 180;
  const Y=y*Math.cos(rx)-z*Math.sin(rx), Z=y*Math.sin(rx)+z*Math.cos(rx);
  return [x*Math.cos(ry)+Z*Math.sin(ry),Y,-x*Math.sin(ry)+Z*Math.cos(ry)];
};
async function render(model, rotation, center, scale, size, textures) {
  const faces=[];
  for (const e of model.elements) {
    const [x,y,z]=e.from, [X,Y,Z]=e.to;
    const sides={
      north: [[0,0,-1], [[X,Y,z],[x,Y,z],[x,y,z],[X,y,z]]],
      south: [[0,0,1], [[x,Y,Z],[X,Y,Z],[X,y,Z],[x,y,Z]]],
      east: [[1,0,0], [[X,Y,Z],[X,Y,z],[X,y,z],[X,y,Z]]],
      west: [[-1,0,0], [[x,Y,z],[x,Y,Z],[x,y,Z],[x,y,z]]],
      up: [[0,1,0], [[x,Y,z],[X,Y,z],[X,Y,Z],[x,Y,Z]]],
      down: [[0,-1,0], [[x,y,Z],[X,y,Z],[X,y,z],[x,y,z]]],
    };
    for (const [side,f] of Object.entries(e.faces)) {
      const [normal,vertices]=sides[side];
      if (rotate(normal,rotation)[2] >= -.001) continue;
      const points=vertices.map(p=>rotate(p.map((v,i)=>v-center[i]),rotation));
      faces.push({points,f,side,depth:points.reduce((sum,p)=>sum+p[2],0)/4,shade:e.shade});
    }
  }
  faces.sort((a,b)=>b.depth-a.depth);
  let svg='';
  for (const {points,f,side,shade} of faces) {
    const [a,b,,d]=points.map(p=>[size/2+p[0]*scale,size/2-p[1]*scale]);
    const matrix=[b[0]-a[0],b[1]-a[1],d[0]-a[0],d[1]-a[1],a[0],a[1]].join(' ');
    const uv=f.uv, dark=shade===false||side==='north'?0:.17;
    svg+=`<g transform="matrix(${matrix})"><svg width="1" height="1" viewBox="${uv[0]} ${uv[1]} ${uv[2]-uv[0]} ${uv[3]-uv[1]}" preserveAspectRatio="none"><image href="data:image/png;base64,${textures[f.texture.slice(1)]}" width="16" height="16" style="image-rendering:pixelated"/></svg>${dark?`<rect width="1" height="1" fill="black" opacity="${dark}"/>`:''}</g>`;
  }
  return sharp(Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}">${svg}</svg>`)).png().toBuffer();
}
(async()=>{
  const textures={};
  for(const key of Object.keys(item.textures)) textures[key]=(await sharp(path.join(temp,key+'.png')).extract({left:0,top:0,width:16,height:16}).png().toBuffer()).toString('base64');
  const panels=[['正面',item,[0,0],[8,7.5,8],16],['厚度',item,[-16,25],[8,7.5,8],16],['驱动器插槽',drive,[0,0],[3,1,1],35]];
  const composites=[];
  for(let i=0;i<panels.length;i++){
    const [name,model,rot,center,scale]=panels[i];
    const png=await render(model,rot,center,scale,256,textures);
    composites.push({input:png,left:i*280+12,top:40});
    const label=Buffer.from(`<svg width="280" height="32"><text x="140" y="23" text-anchor="middle" font-family="Microsoft YaHei,sans-serif" font-size="16" fill="#e0e6e5">${name}</text></svg>`);
    composites.push({input:label,left:i*280,top:8});
    if(i<2)composites.push({input:await sharp(png).resize(16,16,{kernel:'nearest'}).resize(64,64,{kernel:'nearest'}).png().toBuffer(),left:i*280+108,top:300});
  }
  const note=Buffer.from('<svg width="840" height="40"><text x="420" y="25" text-anchor="middle" font-family="Microsoft YaHei,sans-serif" font-size="13" fill="#9aaead">ME 魔力存储盘 · 运行模型离线预览 · 液面取动画首帧 · 不含 AE 状态灯</text></svg>');
  composites.push({input:note,left:0,top:375});
  await sharp({create:{width:840,height:415,channels:4,background:'#293536'}}).composite(composites).png().toFile(path.join(root,'art/mana-cell-preview.png'));
  process.stdout.write('Rendered mana cell item and drive insert.\n');
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
