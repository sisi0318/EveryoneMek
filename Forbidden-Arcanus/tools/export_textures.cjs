// Artwork comes from ImageGen. This script only crops, uses nearest-neighbour scaling and renders review previews.
const fs = require('node:fs/promises');
const path = require('node:path');
const {createRequire} = require('node:module');
const sharp = createRequire(path.resolve(__dirname, '../art/package.json'))('sharp');
const root = path.resolve(__dirname, '..');
const id = 'forbiddenmekanism';
const machines = ['forge_controller', 'clibano_controller'];
const faces = {front: [0, 0], top: [1, 0], side: [0, 1], front_active: [1, 1]};
const texture = (machine, face) => path.join(root, `src/main/resources/assets/${id}/textures/block`, machine, `${face}.png`);

async function pixelFace(machine, face, transform, shade) {
  const {data, info} = await sharp(texture(machine, face)).raw().toBuffer({resolveWithObject: true});
  const pixels = [];
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const offset = (y * 16 + x) * info.channels;
    const color = [0, 1, 2].map(c => Math.min(255, Math.round(data[offset + c] * shade)).toString(16).padStart(2, '0')).join('');
    pixels.push(`<rect x="${x}" y="${y}" width="1.01" height="1.01" fill="#${color}"/>`);
  }
  return `<g transform="${transform}" shape-rendering="crispEdges">${pixels.join('')}</g>`;
}
async function main() {
  for (const machine of machines) {
    const source = path.join(root, 'art/source', `${machine}.png`);
    const metadata = await sharp(source).metadata();
    if (metadata.width !== metadata.height || metadata.width % 2) throw new Error(`Invalid atlas: ${machine}`);
    const cell = metadata.width / 2;
    await fs.mkdir(path.dirname(texture(machine, 'front')), {recursive: true});
    for (const [face, [column, row]] of Object.entries(faces)) {
      await sharp(source).extract({left: column * cell, top: row * cell, width: cell, height: cell})
        .resize(16, 16, {kernel: 'nearest'}).png({compressionLevel: 9}).toFile(texture(machine, face));
      const stats = await sharp(texture(machine, face)).stats();
      if (!stats.isOpaque) throw new Error(`Block face must be opaque: ${machine}/${face}`);
    }
  }
  const item = path.join(root, `src/main/resources/assets/${id}/textures/item/infinite_hammer_module.png`);
  await fs.mkdir(path.dirname(item), {recursive: true});
  await sharp(path.join(root, 'art/source/infinite_hammer_module-v4.png')).trim()
    .resize(14, 14, {kernel: 'nearest', fit: 'contain', background: {r: 0, g: 0, b: 0, alpha: 0}})
    .extend({top: 1, bottom: 1, left: 1, right: 1, background: {r: 0, g: 0, b: 0, alpha: 0}}).png().toFile(item);
  if ((await sharp(item).stats()).isOpaque) throw new Error('Module icon must retain its generated transparency');
  const images = [];
  for (let row = 0; row < machines.length; row++) for (const [column, face] of Object.keys(faces).entries()) {
    images.push({input: await sharp(texture(machines[row], face)).resize(160, 160, {kernel: 'nearest'}).png().toBuffer(),
      left: column * 176 + 16, top: row * 176 + 16});
  }
  images.push({input: await sharp(item).resize(160, 160, {kernel: 'nearest'}).png().toBuffer(), left: 16, top: 368});
  await sharp({create: {width: 720, height: 544, channels: 3, background: '#aeb4b3'}}).composite(images).png().toFile(path.join(root, 'art/texture-sheet.png'));
  const names = ['赫菲斯托斯锻台控制器', '炽炉控制器'];
  const content = [];
  for (let i = 0; i < machines.length; i++) {
    content.push(`<g transform="translate(${48 + 320 * i},160)">`);
    content.push(await pixelFace(machines[i], 'top', 'matrix(8,4,-8,4,128,-64)', 1.08));
    content.push(await pixelFace(machines[i], 'front_active', 'matrix(8,4,0,8,0,0)', 1));
    content.push(await pixelFace(machines[i], 'side', 'matrix(8,-4,0,8,128,64)', .78));
    content.push(`</g><text x="${176 + 320 * i}" y="394" text-anchor="middle" font-size="21">${names[i]}</text>`);
  }
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="736" height="450"><rect width="736" height="450" fill="#edf0eb"/>
    <g font-family="Microsoft YaHei,Arial,sans-serif" fill="#24302b"><text x="48" y="44" font-size="25">Forbidden Mekanism</text>
    <text x="688" y="44" text-anchor="end" font-size="16">16×16 · 工作状态</text>${content.join('')}</g></svg>`;
  await fs.writeFile(path.join(root, 'art/block-preview.svg'), svg);
  await sharp(Buffer.from(svg)).png().toFile(path.join(root, 'art/block-preview.png'));
  console.log('Exported 8 opaque 16x16 block faces, one transparent 16x16 module icon and review previews.');
}
main().catch(error => { console.error(error); process.exitCode = 1; });
