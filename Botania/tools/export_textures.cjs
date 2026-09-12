const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');
const root = path.resolve(__dirname, '..');
const sharp = createRequire(path.join(root, 'art', 'package.json'))('sharp');
const ids = ['mana_lotus', 'resonance_flower', 'resonance_bud'];
const output = path.join(root, 'src/main/resources/assets/botanicalmekanism/textures/block');

(async () => {
  fs.mkdirSync(output, { recursive: true });
  const panels = [];
  for (let i = 0; i < ids.length; i++) {
    const source = path.join(root, 'art/source', `${ids[i]}.png`);
    const metadata = await sharp(source).metadata();
    if (!metadata.hasAlpha) throw new Error(`${ids[i]} has no transparency`);
    const destination = path.join(output, `${ids[i]}.png`);
    await sharp(source).resize(16, 16, { fit: 'contain', kernel: 'nearest', background: '#00000000' }).png().toFile(destination);
    const pixels = await sharp(destination).ensureAlpha().raw().toBuffer();
    if (!pixels.some((value, index) => index % 4 === 3 && value === 0) || !pixels.some((value, index) => index % 4 === 3 && value >= 200))
      throw new Error(`${ids[i]} is not a usable transparent cutout`);
    panels.push({ input: await sharp(destination).resize(256, 256, { kernel: 'nearest' }).toBuffer(), left: i * 256, top: 30 });
  }
  const labels = `<svg width="768" height="30"><style>text{font:16px sans-serif;fill:#e3eee8}</style>${ids.map((id, i) => `<text x="${i * 256 + 12}" y="22">${id}</text>`).join('')}</svg>`;
  panels.push({ input: Buffer.from(labels), left: 0, top: 0 });
  await sharp({ create: { width: 768, height: 286, channels: 4, background: '#20312c' } }).composite(panels).png().toFile(path.join(root, 'art/texture-sheet.png'));
  const machine = path.join(root, 'art/source/mechanical_apothecary.png');
  if (fs.existsSync(machine)) {
    const dir = path.join(output, 'mechanical_apothecary'); fs.mkdirSync(dir, { recursive: true });
    const meta = await sharp(machine).metadata();
    if (meta.width % 2 || meta.height % 2) throw new Error('Machine atlas must split into equal quadrants');
    const names = ['front', 'top', 'side', 'front_active'], faces = [], faceData = {};
    for (let i = 0; i < names.length; i++) {
      const name = names[i], file = path.join(dir, `${name}.png`);
      await sharp(machine).extract({left: (i % 2) * meta.width / 2, top: Math.floor(i / 2) * meta.height / 2, width: meta.width / 2, height: meta.height / 2})
        .resize(16, 16, {kernel: 'nearest'}).png().toFile(file);
      const enlarged = await sharp(file).resize(128, 128, {kernel: 'nearest'}).png().toBuffer();
      faceData[name] = enlarged.toString('base64');
      faces.push({input: enlarged, left: (i % 2) * 128, top: Math.floor(i / 2) * 128});
    }
    await sharp({create:{width:256,height:256,channels:4,background:'#202124'}}).composite(faces).png().toFile(path.join(root, 'art/mechanical-apothecary-sheet.png'));
    const cube = `<svg xmlns="http://www.w3.org/2000/svg" width="320" height="288">${[['top','1 .5 -1 .5 160 16'],['front','1 .5 0 1 32 80'],['side','1 -.5 0 1 160 144']].map(([name,matrix]) => `<image href="data:image/png;base64,${faceData[name]}" width="128" height="128" transform="matrix(${matrix})" style="image-rendering:pixelated"/>`).join('')}</svg>`;
    await sharp(Buffer.from(cube)).png().toFile(path.join(root, 'art/mechanical-apothecary-cube.png'));
  }
  process.stdout.write('Exported three 16x16 transparent plant textures and the review sheet.\n');
})().catch(error => { process.stderr.write(`${error.message}\n`); process.exitCode = 1; });
