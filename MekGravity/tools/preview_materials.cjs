// Contact sheet of actual hidden-GL renders; no runtime textures are modified.
const fs=require('node:fs'),path=require('node:path'),{createRequire}=require('node:module');
const root=path.resolve(__dirname,'..'),sharp=createRequire(path.join(root,'art/package.json'))('sharp');
(async()=>{const labels=['Compressed matter','Fuel preform','Sealed fuel','Stellar alloy','Residual / empty'];const layers=[];
for(let i=0;i<5;i++){const file=i===4?'capsule-empty.png':`material-${i}.png`;layers.push({input:await sharp(path.join(root,'build/material-shader-check',file)).resize(224,224).png().toBuffer(),left:i*244+10,top:42});}
const svg=`<svg width="1220" height="286"><style>text{font:16px sans-serif;fill:#ddd}</style>${labels.map((s,i)=>`<text x="${i*244+12}" y="26">${s}</text>`).join('')}</svg>`;layers.push({input:Buffer.from(svg),left:0,top:0});await sharp({create:{width:1220,height:286,channels:4,background:'#20262d'}}).composite(layers).png().toFile(path.join(root,'art/stellar-materials-preview.png'));})().catch(e=>{process.stderr.write(e.stack);process.exitCode=1;});
