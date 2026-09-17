// Background/size processing authorized by the user. Preserve every generated RGB value.
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const sharp = require(require.resolve('sharp', {paths: [path.join(root, 'art'), path.join(root, '..', 'Botania', 'art')]}));
(async () => {
  const {data, info} = await sharp(path.join(root, 'art/source/thunder_ward-lightning-v4-raw.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const {width, height} = info;
  if (width !== 1254 || height !== 1254) throw new Error('Reinspect the source before changing this asset-specific mask');
  // The shield's gray interior overlaps the checkerboard palette. Protect the region between
  // its actual dark contour pixels on each row; color-only flood fill would erase the shield.
  const left = new Int32Array(height).fill(width), right = new Int32Array(height).fill(-1);
  for (let y=124;y<=1072;y++) {
    let count=0;
    for (let x=260;x<990;x++) {
      const i=(y*width+x)*4;
      if (Math.max(data[i],data[i+1],data[i+2])<100) { left[y]=Math.min(left[y],x);right[y]=x;count++; }
    }
    if (count<20) { left[y]=width;right[y]=-1; }
  }
  const visited = new Uint8Array(width * height), queue = new Uint32Array(width * height);
  let head = 0, tail = 0;
  function visit(x, y) {
    if (x < 0 || x >= width || y < 0 || y >= height) return;
    const pixel = y * width + x;
    if (visited[pixel]) return;
    visited[pixel] = 1;
    if (x>=left[y] && x<=right[y]) return;
    const i = pixel * 4, lo = Math.min(data[i],data[i+1],data[i+2]), hi = Math.max(data[i],data[i+1],data[i+2]);
    // Measured checkerboard: neutral RGB 94..224, chroma <=6. The darker shield outline,
    // colored contacts and bright ivory/yellow lightning are excluded from this background range.
    if (lo >= 85 && hi <= 235 && hi-lo <= 12) queue[tail++] = pixel;
  }
  for (let x=0;x<width;x++) { visit(x,0); visit(x,height-1); }
  for (let y=0;y<height;y++) { visit(0,y); visit(width-1,y); }
  while (head < tail) {
    const pixel = queue[head++], x = pixel % width, y = Math.floor(pixel / width);
    data[pixel*4+3] = 0;
    visit(x-1,y); visit(x+1,y); visit(x,y-1); visit(x,y+1);
  }
  if (tail < width*height*.25 || tail > width*height*.85) throw new Error('Unexpected background extent');
  await sharp(data,{raw:{width,height,channels:4}}).png().toFile(path.join(root,'art/source/thunder_ward-lightning-v4.png'));
  process.stdout.write(`Removed ${tail} border-connected background pixels; shield/lightning RGB preserved.\n`);
})().catch(error => {process.stderr.write(error.stack+'\n');process.exitCode=1;});
