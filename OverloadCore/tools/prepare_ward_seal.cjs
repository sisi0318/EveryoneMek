// User-approved background removal only; never redraw/recolor the generated stamp.
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const sharp = require(require.resolve('sharp', {paths: [path.join(root, 'art'), path.join(root, '..', 'Botania', 'art')]}));
(async () => {
  const input = path.join(root, 'art/source/thunder_ward-seal-v2-raw.png');
  const output = path.join(root, 'art/source/thunder_ward-seal-v2.png');
  const {data, info} = await sharp(input).ensureAlpha().raw().toBuffer({resolveWithObject: true});
  const {width, height} = info;
  const seen = new Uint8Array(width * height), queue = new Uint32Array(width * height);
  let read = 0, write = 0;
  function visit(x, y) {
    if (x < 0 || x >= width || y < 0 || y >= height) return;
    const pixel = y * width + x;
    if (seen[pixel]) return;
    seen[pixel] = 1;
    const i = pixel * 4, lo = Math.min(data[i], data[i+1], data[i+2]), hi = Math.max(data[i], data[i+1], data[i+2]);
    // The painted checkerboard is light neutral gray. The stamp has a closed dark outline.
    // Only pixels connected to the outer canvas are removed, preserving its silver highlights.
    if (lo > 120 && hi - lo < 48) queue[write++] = pixel;
  }
  for (let x = 0; x < width; x++) { visit(x, 0); visit(x, height - 1); }
  for (let y = 0; y < height; y++) { visit(0, y); visit(width - 1, y); }
  while (read < write) {
    const pixel = queue[read++], x = pixel % width, y = Math.floor(pixel / width);
    data[pixel * 4 + 3] = 0;
    visit(x-1,y); visit(x+1,y); visit(x,y-1); visit(x,y+1);
  }
  if (write < width * height / 4 || write > width * height * .85) throw new Error('Unexpected background extent; inspect source before exporting');
  await sharp(data, {raw: {width, height, channels: 4}}).png().toFile(output);
  process.stdout.write(`Removed ${write} border-connected checkerboard pixels; RGB values preserved.\n`);
})().catch(error => {process.stderr.write(error.stack+'\n');process.exitCode = 1;});
