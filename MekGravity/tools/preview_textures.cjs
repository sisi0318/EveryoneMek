// Isometric projection of exported artwork for review; this is not a game screenshot.
const path = require('node:path');
const {createRequire} = require('node:module');
const root = path.resolve(__dirname, '..');
const sharp = createRequire(path.join(root, 'art/package.json'))('sharp');
const dir = path.join(root, 'src/main/resources/assets/mekgravity/textures/block');
const samples = [
  ['Formed frame', 'assembled_frame', 'assembled_frame', 'assembled_frame'],
  ['Formed armor', 'assembled_panel', 'assembled_panel', 'assembled_panel'],
  ['Formed controller', 'controller_front', 'assembled_frame', 'assembled_panel'],
  ['Formed output', 'assembled_port_output', 'assembled_port_output', 'assembled_port_output']
];
(async () => {
  const tile = async name => 'data:image/png;base64,' + (await sharp(path.join(dir, name + '.png'))
    .resize(192, 192, {kernel: 'nearest'}).png().toBuffer()).toString('base64');
  let content = '';
  for (let i = 0; i < samples.length; i++) {
    const [label, front, top, side] = samples[i];
    const image = (src, matrix) => `<image width="192" height="192" transform="matrix(${matrix})" href="${src}"/>`;
    content += `<g transform="translate(${i * 224 + 12} 55)">${image(await tile(top), '0.5 0.25 -0.5 0.25 96 0')}${image(await tile(front), '0.5 0.25 0 0.5 0 48')}${image(await tile(side), '0.5 -0.25 0 0.5 96 96')}</g><text x="${i * 224 + 15}" y="32">${label}</text>`;
  }
  const svg = `<svg width="896" height="264" xmlns="http://www.w3.org/2000/svg"><rect width="896" height="264" fill="#22272d"/><style>text{fill:#ddd;font:17px sans-serif}image{image-rendering:pixelated}</style>${content}</svg>`;
  await sharp(Buffer.from(svg)).png().toFile(path.join(root, 'art/block-preview.png'));
})().catch(error => { process.stderr.write(error.stack + '\n'); process.exitCode = 1; });
