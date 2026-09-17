// Isometric projection of exported artwork for review; this is not a game screenshot.
const fs=require('node:fs');
const path=require('node:path');
const {createRequire}=require('node:module');
const root=path.resolve(__dirname,'..');
const sharp=createRequire(path.join(root,'art/package.json'))('sharp');
const dir=path.join(root,'src/main/resources/assets/mekfactory/textures/block');
const names=[['Loose frame','frame','frame','frame'],['Formed armor','formed_panel','formed_panel','formed_panel'],
 ['Controller','controller_front','controller_top','controller_side'],['Formed input','formed_port_input','formed_panel','formed_panel']];
(async()=>{
 const tile=async name=>'data:image/png;base64,'+(await sharp(path.join(dir,name+'.png')).resize(192,192,{kernel:'nearest'}).png().toBuffer()).toString('base64');
 let content='';
 for(let i=0;i<names.length;i++){
  const [label,front,top,side]=names[i];
  const image=(src,matrix)=>`<image width="192" height="192" transform="matrix(${matrix})" href="${src}"/>`;
  content+=`<g transform="translate(${i*224+12} 55)">${image(await tile(top),'0.5 0.25 -0.5 0.25 96 0')}${image(await tile(front),'0.5 0.25 0 0.5 0 48')}${image(await tile(side),'0.5 -0.25 0 0.5 96 96')}</g><text x="${i*224+15}" y="32">${label}</text>`;
 }
 const svg=`<svg width="896" height="264" xmlns="http://www.w3.org/2000/svg"><rect width="896" height="264" fill="#22272d"/><style>text{fill:#ddd;font:17px sans-serif}image{image-rendering:pixelated}</style>${content}</svg>`;
 await sharp(Buffer.from(svg)).png().toFile(path.join(root,'art/block-preview.png'));
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
