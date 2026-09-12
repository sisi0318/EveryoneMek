// Render the actual generated cuboids and UVs for review without launching Minecraft.
const fs = require('node:fs'), path = require('node:path'), cp = require('node:child_process');
const root = path.resolve(__dirname, '..');
const sharp = require(require.resolve('sharp', {paths: [path.join(root, 'art')]}));
const manifest = JSON.parse(fs.readFileSync(path.join(root, 'art/botanical-machine-models.json')));
const modelRoot = path.join(root, 'src/main/resources/assets/botanicalmekanism/models/block');
const names = Object.keys(manifest.models);
const language = JSON.parse(fs.readFileSync(path.join(root, 'src/main/resources/assets/botanicalmekanism/lang/zh_cn.json')));
const temp = path.join(root, 'build/model-reference'); fs.mkdirSync(temp, {recursive: true});
const extract = `import zipfile,json\nfrom pathlib import Path\nr=Path(${JSON.stringify(root.replaceAll('\\','/'))})\nm=json.loads((r/'art/botanical-machine-models.json').read_text())\nz=zipfile.ZipFile(r/'build/dependencies/botania-neoforge-1.21.1-456-SNAPSHOT.jar')\nmc=zipfile.ZipFile(r/'.gradle-home/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar')\nfor loc in set(m['palette'].values()):\n ns,p=loc.split(':'); name=f'assets/{ns}/textures/{p}.png'; archive=z if ns=='botania' else mc\n (r/'build/model-reference'/f'{ns}-{p.replace("/","_")}.png').write_bytes(archive.read(name))\n`;
const extracted = cp.spawnSync('python', ['-X', 'utf8', '-'], {input: extract, encoding: 'utf8'});
if (extracted.status !== 0) throw new Error(extracted.stderr);
const escape = s => s.replaceAll('&', '&amp;').replaceAll('<', '&lt;');
(async () => {
  const textures = {};
  for (const [key, loc] of Object.entries(manifest.palette)) {
    const [ns, p] = loc.split(':'); const file = path.join(temp, `${ns}-${p.replaceAll('/', '_')}.png`);
    textures[key] = (await sharp(file).extract({left: 0, top: 0, width: 16, height: 16}).png().toBuffer()).toString('base64');
  }
  const panels = [];
  for (let i = 0; i < names.length; i++) {
    const name = names[i], model = JSON.parse(fs.readFileSync(path.join(modelRoot, name + '.json')));
    const faces = []; const scale = 7;
    const project = ([x, y, z]) => [140 + (x + z - 16) * .7071 * scale, 167 + ((x - z) * .40825 - y * .8165) * scale];
    const rotate = (p, r) => {
      if (!r) return p; const [ox, oy, oz] = r.origin, angle = r.angle * Math.PI / 180;
      return [ox + (p[0]-ox)*Math.cos(angle) + (p[2]-oz)*Math.sin(angle), p[1], oz - (p[0]-ox)*Math.sin(angle) + (p[2]-oz)*Math.cos(angle)];
    };
    for (const e of model.elements) {
      const [x,y,z] = e.from, [X,Y,Z] = e.to;
      const points = {north:[[X,Y,z],[x,Y,z],[x,y,z],[X,y,z]], east:[[X,Y,Z],[X,Y,z],[X,y,z],[X,y,Z]], up:[[x,Y,z],[X,Y,z],[X,Y,Z],[x,Y,Z]]};
      for (const [side, vertices] of Object.entries(points)) {
        if (!e.faces[side] || side === 'north' && X === x || side === 'east' && Z === z || side === 'up' && (X === x || Z === z)) continue;
        const transformed = vertices.map(p => rotate(p, e.rotation));
        faces.push({points: transformed.map(project), depth: transformed.reduce((s,p) => s + p[0]+p[1]-p[2], 0)/4, side, face:e.faces[side], plant:!!e.rotation});
      }
    }
    faces.sort((a,b) => a.depth - b.depth);
    let rendered = '';
    for (const f of faces) {
      const [a,b,,d] = f.points, uv = f.face.uv;
      const key = f.face.texture.substring(1), texture = textures[key === 'indicator' ? 'crystal' : key];
      const matrix = [b[0]-a[0],b[1]-a[1],d[0]-a[0],d[1]-a[1],a[0],a[1]].join(' ');
      const shade = f.plant || f.side === 'up' ? 0 : f.side === 'north' ? .13 : .27;
      rendered += `<g transform="matrix(${matrix})"><svg width="1" height="1" viewBox="${uv[0]} ${uv[1]} ${uv[2]-uv[0]} ${uv[3]-uv[1]}" preserveAspectRatio="none"><image href="data:image/png;base64,${texture}" width="16" height="16" style="image-rendering:pixelated"/></svg>${shade ? `<rect width="1" height="1" fill="black" opacity="${shade}"/>` : ''}</g>`;
    }
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="280" height="230"><rect x="4" y="4" width="272" height="222" rx="4" fill="#e6e5db"/><ellipse cx="140" cy="181" rx="77" ry="19" fill="#bbb9aa" opacity=".45"/>${rendered}<text x="140" y="209" text-anchor="middle" font-size="15" font-family="Microsoft YaHei,SimHei,sans-serif" fill="#343b35">${escape(language['block.botanicalmekanism.'+name])}</text></svg>`;
    panels.push({input: await sharp(Buffer.from(svg)).png().toBuffer(), left: i%4*280, top: 50+Math.floor(i/4)*230});
  }
  const title = Buffer.from('<svg width="1120" height="50"><text x="560" y="29" text-anchor="middle" font-size="20" font-family="Microsoft YaHei,sans-serif" fill="#dfe7dd">植物机械 · 装置模型预览</text><text x="560" y="46" text-anchor="middle" font-size="11" font-family="Microsoft YaHei,sans-serif" fill="#9faa9e">由运行模型与原版材质渲染 · 工作态 · 非游戏截图</text></svg>');
  panels.push({input:title,left:0,top:0});
  await sharp({create:{width:1120,height:740,channels:4,background:'#27382f'}}).composite(panels).png().toFile(path.join(root, 'art/botanical-machines-preview.png'));
  process.stdout.write('Rendered twelve apparatus models from runtime geometry and native materials.\n');
})().catch(error => {process.stderr.write(error.stack+'\n');process.exitCode=1;});
