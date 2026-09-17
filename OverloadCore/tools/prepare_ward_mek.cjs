// User-authorized background processing; generated foreground RGB remains unchanged.
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const sharp = require(require.resolve('sharp', {paths:[path.join(root,'art'),path.join(root,'..','Botania','art')]}));
(async () => {
  const {data,info} = await sharp(path.join(root,'art/source/thunder_ward-mek-v5-raw.png')).ensureAlpha().raw().toBuffer({resolveWithObject:true});
  const {width,height} = info;
  if(width!==1254 || height!==1254)throw new Error('Reinspect this source-specific background mask');
  // Protect the neutral shield interior between its charcoal contour pixels.
  // Require neutral contour colors so an outside dark green arc cannot enlarge that region.
  const left=new Int32Array(height).fill(width),right=new Int32Array(height).fill(-1);
  for(let y=140;y<=1110;y++) {
    let count=0;
    for(let x=200;x<1030;x++) {
      const i=(y*width+x)*4,lo=Math.min(data[i],data[i+1],data[i+2]),hi=Math.max(data[i],data[i+1],data[i+2]);
      if(hi<100 && hi-lo<=16) {left[y]=Math.min(left[y],x);right[y]=x;count++;}
    }
    if(count<20){left[y]=width;right[y]=-1;}
  }
  const seen=new Uint8Array(width*height),queue=new Uint32Array(width*height);
  let read=0,write=0;
  function visit(x,y) {
    if(x<0||x>=width||y<0||y>=height)return;
    const pixel=y*width+x;
    if(seen[pixel])return;
    seen[pixel]=1;
    if(x>=left[y]&&x<=right[y])return;
    const i=pixel*4,lo=Math.min(data[i],data[i+1],data[i+2]),hi=Math.max(data[i],data[i+1],data[i+2]);
    // Measured outer matte: RGB 105..221 with chroma <=5. Excludes green and mint-white arcs.
    if(lo>=85&&hi<=235&&hi-lo<=12)queue[write++]=pixel;
  }
  for(let x=0;x<width;x++){visit(x,0);visit(x,height-1);}
  for(let y=0;y<height;y++){visit(0,y);visit(width-1,y);}
  while(read<write){const p=queue[read++],x=p%width,y=Math.floor(p/width);data[p*4+3]=0;visit(x-1,y);visit(x+1,y);visit(x,y-1);visit(x,y+1);}
  if(write<width*height*.25||write>width*height*.85)throw new Error('Unexpected background extent');
  await sharp(data,{raw:{width,height,channels:4}}).png().toFile(path.join(root,'art/source/thunder_ward-mek-v5.png'));
  process.stdout.write(`Removed ${write} background pixels; foreground RGB preserved.\n`);
})().catch(error=>{process.stderr.write(error.stack+'\n');process.exitCode=1;});
