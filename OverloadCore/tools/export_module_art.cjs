const fs = require('node:fs'), path = require('node:path');
const root = path.resolve(__dirname, '..');
const sharp = require(require.resolve('sharp', {paths: [path.join(root, 'art'), path.join(root, '..', 'Botania', 'art')]}));
(async () => {
  const source = path.join(root, 'art/source/module_residual_coupling_unit.png');
  const meta = await sharp(source).metadata();
  const stats = await sharp(source).stats();
  if (!meta.hasAlpha || stats.isOpaque) throw new Error('Source requires genuine alpha; do not remove background with this script.');
  const target = path.join(root, 'src/main/resources/assets/overloadcore/textures/item/module_residual_coupling_unit.png');
  fs.mkdirSync(path.dirname(target), {recursive: true});
  await sharp(source).trim().resize(16, 16, {kernel: 'nearest', fit: 'contain', background: {r: 0, g: 0, b: 0, alpha: 0}}).png().toFile(target);
  await sharp(target).resize(256, 256, {kernel: 'nearest'}).png().toFile(path.join(root, 'art/coupling-module-preview.png'));
  console.log('Exported coupling module: 16x16, original alpha, nearest-neighbour scaling.');
})().catch(e => { console.error(e); process.exitCode = 1; });
