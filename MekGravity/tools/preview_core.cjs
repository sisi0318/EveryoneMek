// Render the actual JSON cuboids and exported textures for asset review, not a game screenshot.
const fs = require('node:fs');
const path = require('node:path');
const {createRequire} = require('node:module');
const root = path.resolve(__dirname, '..');
const sharp = createRequire(path.join(root, 'art/package.json'))('sharp');
const assets = path.join(root, 'src/main/resources/assets/mekgravity');
(async () => {
  const uri = async texture => 'data:image/png;base64,' + (await sharp(path.join(assets, 'textures', texture.split(':')[1] + '.png'))
    .resize(256, 256, {kernel:'nearest'}).png().toBuffer()).toString('base64');
  const project = ([x,y,z]) => [(x-z)*7, (x+z)*3.5-y*7];
  const model = async (name, offset) => {
    const data = JSON.parse(fs.readFileSync(path.join(assets,'models/block',name+'.json'),'utf8'));
    const textures = {};
    for (const [key,texture] of Object.entries(data.textures)) textures['#'+key] = await uri(texture);
    const surfaces = [];
    for (const e of data.elements) {
      const [x,y,z] = e.from, [X,Y,Z] = e.to;
      for (const [face,corners] of Object.entries({up:[[x,Y,z],[X,Y,z],[x,Y,Z]],south:[[x,Y,Z],[X,Y,Z],[x,y,Z]],east:[[X,Y,Z],[X,Y,z],[X,y,Z]]})) {
        const f = e.faces[face]; if (!f) continue;
        const [p,a,b] = corners.map(project), uv = f.uv || [0,0,16,16];
        const matrix = [(a[0]-p[0])/16,(a[1]-p[1])/16,(b[0]-p[0])/16,(b[1]-p[1])/16,p[0],p[1]];
        const depth = corners.reduce((sum,v)=>sum+v[0]+v[1]+v[2],0)/3;
        const shade = face==='up'?0:face==='south'?.12:.25;
        surfaces.push({depth, svg:`<g transform="matrix(${matrix.join(' ')})"><svg width="16" height="16" viewBox="${uv[0]} ${uv[1]} ${uv[2]-uv[0]} ${uv[3]-uv[1]}" preserveAspectRatio="none"><image width="16" height="16" href="${textures[f.texture]}"/></svg><rect width="16" height="16" fill="black" opacity="${shade}"/></g>`});
      }
    }
    return `<g transform="translate(${offset} 155)">${surfaces.sort((a,b)=>a.depth-b.depth).map(s=>s.svg).join('')}</g>`;
  };
  const pellet = await uri('mekgravity:item/dense_fuel_pellet');
  let content = await model('core',125) + await model('core_active',365);
  content += `<image x="510" y="60" width="192" height="192" href="${pellet}"/>`;
  for (const [x,label] of [[24,'Core - idle'],[264,'Core - active'],[504,'Dense fuel pellet']]) content += `<text x="${x}" y="28">${label}</text>`;
  const svg=`<svg width="736" height="300" xmlns="http://www.w3.org/2000/svg"><rect width="736" height="300" fill="#30363d"/><style>image{image-rendering:pixelated}text{fill:#ddd;font:16px sans-serif}</style>${content}</svg>`;
  await sharp(Buffer.from(svg)).png().toFile(path.join(root,'art/core-and-fuel-preview.png'));
})().catch(error=>{process.stderr.write(error.stack+'\n');process.exitCode=1;});
