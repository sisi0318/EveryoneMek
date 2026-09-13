const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');
const root = path.resolve(__dirname, '..');
const sharp = createRequire(path.join(root, 'art/package.json'))('sharp');
const manifest = JSON.parse(fs.readFileSync(path.join(root, 'art/spark-upgrades.json'), 'utf8'));

(async () => {
  const panels = [];
  const labels = ['范围', '效率', 'ME 频道'];
  for (const [index, asset] of manifest.assets.entries()) {
    const source = path.join(root, asset.source), target = path.join(root, asset.output);
    const meta = await sharp(source).metadata();
    if (meta.width !== meta.height) throw new Error(`${asset.id}: module face must be square`);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    await sharp(source).resize(16, 16, { fit: 'contain', kernel: 'nearest', background: '#00000000' }).png().toFile(target);
    const pixels = await sharp(target).ensureAlpha().raw().toBuffer();
    let visible = 0, transparent = 0;
    const colors = new Set();
    for (let i = 0; i < pixels.length; i += 4) {
      if (pixels[i + 3] >= 200) { visible++; colors.add(pixels.subarray(i, i + 4).toString('hex')); }
      if (pixels[i + 3] === 0) transparent++;
    }
    // The full square is the cartridge plate itself, with no external backdrop.
    if (visible !== 256 || transparent !== 0) throw new Error(`${asset.id}: module face must fill all 16x16 pixels`);
    panels.push({ input: await sharp(target).resize(160, 160, { kernel: 'nearest' }).toBuffer(), left: 20 + index * 200, top: 16 });
    panels.push({ input: target, left: 92 + index * 200, top: 190 });
    process.stdout.write(`${asset.id}: 16x16, ${visible} opaque pixels, ${colors.size} opaque colors\n`);
  }
  const captions = `<svg width="600" height="38"><style>text{font-family:'Microsoft YaHei',sans-serif;font-size:18px;fill:#303640;text-anchor:middle}</style>${labels.map((label, i) => `<text x="${100 + i * 200}" y="25">${label}</text>`).join('')}</svg>`;
  panels.push({ input: Buffer.from(captions), left: 0, top: 214 });
  await sharp({ create: { width: 600, height: 252, channels: 4, background: '#c6c6ce' } }).composite(panels).png().toFile(path.join(root, manifest.export.preview));
})().catch(error => { process.stderr.write(`${error.message}\n`); process.exitCode = 1; });
