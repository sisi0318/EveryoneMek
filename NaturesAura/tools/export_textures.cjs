// ImageGen creates the artwork. This script only separates faces and exports their pixel resolution.
const fs = require('node:fs/promises');
const path = require('node:path');
const {createRequire} = require('node:module');
const sharp = createRequire(path.resolve(__dirname, '../art/package.json'))('sharp');
const root = path.resolve(__dirname, '..');
const machines = ['universal_aura_generator', 'universal_forest_ritual', 'universal_natural_altar', 'universal_offering'];
const faces = {front: [0, 0], top: [1, 0], side: [0, 1], front_active: [1, 1]};

async function pixelFace(machine, face, transform, shade) {
  const file = path.join(root, 'src/main/resources/assets/naturesmekanism/textures/block', machine, `${face}.png`);
  const {data, info} = await sharp(file).raw().toBuffer({resolveWithObject: true});
  const pixels = [];
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const offset = (y * 16 + x) * info.channels;
    const color = [0, 1, 2].map(c => Math.min(255, Math.round(data[offset + c] * shade)).toString(16).padStart(2, '0')).join('');
    pixels.push(`<rect x="${x}" y="${y}" width="1.01" height="1.01" fill="#${color}"/>`);
  }
  return `<g transform="${transform}" shape-rendering="crispEdges">${pixels.join('')}</g>`;
}

async function renderPreview() {
  const titles = ['通用灵气发生器', '通用森林仪式', '通用自然祭坛', '通用呼唤仪式'];
  const subtitles = ['电力转化 · 绿色能量条', '树苗 · 金叶粉 · 八方原料', '灵气灌注 · 催化接口', '供品托盘 · 呼唤指示灯'];
  const content = [];
  for (let i = 0; i < machines.length; i++) {
    const x = 48 + i * 320;
    const machine = machines[i];
    content.push(`<g transform="translate(${x},170)">`);
    content.push(await pixelFace(machine, 'top', 'matrix(8,4,-8,4,128,-64)', 1.08));
    content.push(await pixelFace(machine, 'front_active', 'matrix(8,4,0,8,0,0)', 1));
    content.push(await pixelFace(machine, 'side', 'matrix(8,-4,0,8,128,64)', 0.78));
    content.push(`</g><text x="${x + 128}" y="403" text-anchor="middle" font-size="22" font-weight="600">${titles[i]}</text>`);
    content.push(`<text x="${x + 128}" y="434" text-anchor="middle" font-size="15" fill="#576359">${subtitles[i]}</text>`);
  }
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="1376" height="520" viewBox="0 0 1376 520">
    <rect width="1376" height="520" fill="#edf0eb"/>
    <g font-family="Microsoft YaHei,Arial,sans-serif" fill="#24302b">
      <text x="48" y="46" font-size="25" font-weight="700">Nature's Mekanism</text>
      <text x="1328" y="44" text-anchor="end" font-size="17" fill="#576359">16×16 · 原版 Mek 像素风格</text>
      ${content.join('')}
      <text x="688" y="491" text-anchor="middle" font-size="15" fill="#576359">方块材质预览 · 运行状态</text>
    </g></svg>`;
  await fs.writeFile(path.join(root, 'art/block-preview.svg'), svg);
  await sharp(Buffer.from(svg)).png().toFile(path.join(root, 'art/block-preview.png'));
}

async function main() {
  for (const machine of machines) {
    const source = path.join(root, 'art/source', `${machine}.png`);
    const metadata = await sharp(source).metadata();
    if (metadata.width !== metadata.height || metadata.width % 2 !== 0) throw new Error(`Invalid atlas: ${machine}`);
    const cell = metadata.width / 2;
    const output = path.join(root, 'src/main/resources/assets/naturesmekanism/textures/block', machine);
    await fs.mkdir(output, {recursive: true});
    for (const [face, [column, row]] of Object.entries(faces)) {
      await sharp(source).extract({left: column * cell, top: row * cell, width: cell, height: cell})
        .resize(16, 16, {kernel: 'nearest'}).png({compressionLevel: 9}).toFile(path.join(output, `${face}.png`));
    }
  }
  const images = [];
  for (let row = 0; row < machines.length; row++) {
    for (const [column, face] of Object.keys(faces).entries()) {
      images.push({
        input: await sharp(path.join(root, 'src/main/resources/assets/naturesmekanism/textures/block', machines[row], `${face}.png`))
          .resize(160, 160, {kernel: 'nearest'}).png().toBuffer(),
        left: column * 176 + 16, top: row * 176 + 16
      });
    }
  }
  await sharp({create: {width: 720, height: 720, channels: 3, background: '#b4b9b5'}})
    .composite(images).png().toFile(path.join(root, 'art/texture-sheet.png'));
  await renderPreview();
  console.log('Exported 16 original 16x16 block face PNGs and a nearest-neighbour review sheet.');
}
main().catch(error => { console.error(error); process.exitCode = 1; });
