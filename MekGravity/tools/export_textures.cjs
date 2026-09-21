// Mechanical atlas extraction only: no repainting or filtering of generated artwork.
const fs = require('node:fs');
const path = require('node:path');
const {createRequire} = require('node:module');
const root = path.resolve(__dirname, '..');
const sharp = createRequire(path.join(root, 'art/package.json'))('sharp');
const layouts = JSON.parse(fs.readFileSync(path.join(root, 'art/atlas-layout.json'), 'utf8'));
const atlases = {
  assembled: ['assembled_panel','assembled_frame','assembled_port_input','assembled_port_output'],
  controller: ['controller_front', 'controller_top', 'controller_side', 'controller_front_active'],
  coil: ['coil_front', 'coil_top', 'coil_side', 'coil_front_active'],
  structure: ['frame', 'panel', 'cell', 'provider'],
  ports: ['port_input','port_output','formed_port_input','formed_port_output']
};
const output = path.join(root, 'src/main/resources/assets/mekgravity/textures/block');
(async () => {
  fs.mkdirSync(output, {recursive:true});
  const previews = [];
  let row = 0;
  for (const [atlas, names] of Object.entries(atlases)) {
    const source = path.join(root, 'art/source', `${atlas}.png`);
    const meta = await sharp(source).metadata();
    const layout = layouts[atlas];
    if (meta.width !== layout.width || meta.height !== layout.height || layout.panels.length !== 4)
      throw new Error(`${atlas}: source dimensions differ from reviewed atlas-layout.json`);
    for (let i=0;i<4;i++) {
      const dest = path.join(output, names[i] + '.png');
      // Reviewed visible face bounds remove generator padding without repainting pixels.
      const [left, top, width, height] = layout.panels[i];
      await sharp(source).extract({left,top,width,height})
        .affine([16/width,0,0,16/height],{interpolator:sharp.interpolators.nearest,idx:atlas==='coil'?0:0.5,idy:atlas==='coil'?0:0.5,odx:atlas==='coil'?0:-0.5,ody:atlas==='coil'?0:-0.5})
        .png().toFile(dest);
      previews.push({input:await sharp(dest).resize(192,192,{kernel:'nearest'}).png().toBuffer(),left:i*192,top:row*220+28});
    }
    const labels = `<svg width="768" height="28"><style>text{fill:white;font:13px sans-serif}</style>${names.map((name,i)=>`<text x="${i*192+5}" y="20">${name}</text>`).join('')}</svg>`;
    previews.push({input:Buffer.from(labels),left:0,top:row*220}); row++;
  }
  await sharp({create:{width:768,height:row*220,channels:4,background:'#24272b'}}).composite(previews).png().toFile(path.join(root,'art/texture-sheet.png'));
  process.stdout.write('Exported twenty original 16x16 textures.\n');
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
