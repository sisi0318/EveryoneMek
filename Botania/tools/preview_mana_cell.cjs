// Compare the actual AE item sprites with the runtime mana overlay, without launching Minecraft.
const fs=require('node:fs'), path=require('node:path'), cp=require('node:child_process');
const root=path.resolve(__dirname,'..');
const sharp=require(require.resolve('sharp',{paths:[path.join(root,'art')]}));
const tiers=[1,4,16,64,256], temp=path.join(root,'build/mana-cell-preview');
fs.mkdirSync(temp,{recursive:true});
const result=cp.spawnSync('python',['-X','utf8','-'],{encoding:'utf8',input:`
import zipfile
from pathlib import Path
r=Path(${JSON.stringify(root.replaceAll('\\','/'))})
with zipfile.ZipFile(r/'build/dependencies/appliedenergistics2-19.2.17.jar') as z:
    for t in [1,4,16,64,256]:
        (r/'build/mana-cell-preview'/f'{t}k.png').write_bytes(z.read(f'assets/ae2/textures/item/fluid_storage_cell_{t}k.png'))
with zipfile.ZipFile(r/'build/dependencies/botania-neoforge-1.21.1-456-SNAPSHOT.jar') as z:
    (r/'build/mana-cell-preview/mana.png').write_bytes(z.read('assets/botania/textures/block/mana_water.png'))
`});
if(result.status!==0)throw new Error(result.stderr);
(async()=>{
  const panels=[];
  for(let i=0;i<tiers.length;i++){
    const tier=tiers[i], id='mana_storage_cell'+(tier===1?'':`_${tier}k`);
    const m=JSON.parse(fs.readFileSync(path.join(root,`src/main/resources/assets/botanicalmekanism/models/item/${id}.json`)));
    if(m.children.base.parent!==`ae2:item/fluid_storage_cell_${tier}k`)throw new Error('Unexpected native parent');
    const e=m.children.mana.elements.find(e=>e.faces.south), [x,y]=e.from,[X,Y]=e.to;
    const overlay=await sharp(path.join(temp,'mana.png')).extract({left:0,top:0,width:16,height:16})
      .resize(X-x,Y-y,{kernel:'nearest'}).png().toBuffer();
    const base=path.join(temp,`${tier}k.png`);
    const mana=await sharp(base).composite([{input:overlay,left:x,top:16-Y}]).png().toBuffer();
    panels.push({input:await sharp(base).resize(96,96,{kernel:'nearest'}).png().toBuffer(),left:20+i*150,top:42});
    panels.push({input:await sharp(mana).resize(96,96,{kernel:'nearest'}).png().toBuffer(),left:20+i*150,top:196});
    const title=Buffer.from(`<svg width="150" height="38"><text x="68" y="25" text-anchor="middle" font-family="sans-serif" font-size="17" fill="#e0e6e5">${tier}k</text></svg>`);
    panels.push({input:title,left:i*150,top:146});
  }
  for(const [caption,y] of [['AE 原版流体盘',8],['ME 魔力盘',298],['直接引用原盘 · 下排加动态魔力标识 · 离线首帧预览，不含状态灯',332]]){
    panels.push({input:Buffer.from(`<svg width="750" height="32"><text x="375" y="22" text-anchor="middle" font-family="Microsoft YaHei,sans-serif" font-size="14" fill="#b9cecf">${caption}</text></svg>`),left:0,top:y});
  }
  await sharp({create:{width:750,height:370,channels:4,background:'#293536'}}).composite(panels).png().toFile(path.join(root,'art/mana-cell-preview.png'));
  process.stdout.write('Rendered five native AE cell references and their mana overlays.\n');
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
