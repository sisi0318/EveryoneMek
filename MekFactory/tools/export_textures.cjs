// Mechanical atlas extraction only: no repainting or filtering of generated artwork.
const fs = require('node:fs');
const path = require('node:path');
const {createRequire} = require('node:module');
const root = path.resolve(__dirname, '..');
const sharp = createRequire(path.join(root, 'art/package.json'))('sharp');
const atlases = {
  controller: ['controller_front', 'controller_top', 'controller_side', 'controller_front_active'],
  structure: ['frame', 'formed_panel', 'formed_cell', 'formed_provider'],
  ports: ['port_input', 'port_output', 'formed_port_input', 'formed_port_output']
};
const output = path.join(root, 'src/main/resources/assets/mekfactory/textures/block');
(async () => {
  fs.mkdirSync(output, {recursive:true});
  const previews = [];
  let row = 0;
  for (const [atlas, names] of Object.entries(atlases)) {
    const source = path.join(root, 'art/source', `${atlas}.png`);
    const meta = await sharp(source).metadata();
    if (meta.width !== meta.height || meta.width % 2) throw new Error(`${atlas}: expected equal 2x2 atlas`);
    const cell = meta.width / 2;
    for (let i=0;i<4;i++) {
      const dest = path.join(output, names[i] + '.png');
      // Controller source has a lower padding strip; its actual horizontal panel boundary is y=588.
      // Crop the visible faces consistently so idle/active indicators keep their positions.
      const height = atlas === 'controller' && meta.height === 1254 ? 588 : cell;
      await sharp(source).extract({left:(i%2)*cell,top:Math.floor(i/2)*height,width:cell,height})
        .resize(16,16,{fit:'fill',kernel:'nearest'}).png().toFile(dest);
      previews.push({input:await sharp(dest).resize(192,192,{kernel:'nearest'}).png().toBuffer(),left:i*192,top:row*220+28});
    }
    const labels = `<svg width="768" height="28"><style>text{fill:white;font:13px sans-serif}</style>${names.map((name,i)=>`<text x="${i*192+5}" y="20">${name}</text>`).join('')}</svg>`;
    previews.push({input:Buffer.from(labels),left:0,top:row*220}); row++;
  }
  await sharp({create:{width:768,height:row*220,channels:4,background:'#24272b'}}).composite(previews).png().toFile(path.join(root,'art/texture-sheet.png'));
  process.stdout.write('Exported twelve original 16x16 textures.\n');
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
