/* Motion preview using runtime OBJ geometry. Canvas rasterization is a preview, not the game renderer. */
(()=>{'use strict';const canvas=document.querySelector('#motion'),ctx=canvas.getContext('2d'),$=s=>document.querySelector(s);
let running=true,load=1,strength=0,phase=0,last=0,yaw=-.42,pitch=.24,zoom=1,drag=null;
const sub=(a,b)=>a.map((x,i)=>x-b[i]),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],dot=(a,b)=>a.reduce((s,x,i)=>s+x*b[i],0);
const rx=([x,y,z],r)=>[x,y*Math.cos(r)-z*Math.sin(r),y*Math.sin(r)+z*Math.cos(r)];
const ry=([x,y,z],r)=>[x*Math.cos(r)+z*Math.sin(r),y,-x*Math.sin(r)+z*Math.cos(r)];
const rz=([x,y,z],r)=>[x*Math.cos(r)-y*Math.sin(r),x*Math.sin(r)+y*Math.cos(r),z];
const rad=d=>d*Math.PI/180,view=p=>rx(ry(p,yaw),pitch);
function draw(now){
 const dt=last?Math.min((now-last)/50,5):0;last=now;const target=running?.25+.75*load:0,decay=Math.exp(-dt/8);phase+=target*dt+(strength-target)*8*(1-decay);strength=target+(strength-target)*decay;if(!target&&strength<.0001)strength=0;
 const r=canvas.getBoundingClientRect(),dpr=Math.min(devicePixelRatio||1,2);if(canvas.width!==Math.round(r.width*dpr)||canvas.height!==Math.round(r.height*dpr)){canvas.width=Math.round(r.width*dpr);canvas.height=Math.round(r.height*dpr);}ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,r.width,r.height);
 const scale=Math.min(r.width,r.height)*.275*zoom,project=p=>[r.width/2+p[0]*scale,r.height/2-p[1]*scale],triangles=[];
 for(const [name,faces]of Object.entries(window.CORE_MESH))for(const face of faces){let points=face.p.map(p=>{
  if(name==='energy'){const size=1+Math.sin(phase*.09)*.022*strength;p=p.map(x=>x*size);p=rx(p,rad(Math.sin(phase*.018)*9*strength));p=ry(p,rad(phase*.55%360));p[1]+=Math.sin(phase*.055)*.035*strength;}
  else p=ry(p,rad(phase*(name==='ring_0'?1.2:-1.6)%360));return view(p);});
  const normal=cross(sub(points[1],points[0]),sub(points[2],points[0]));if(normal[2]<=0)continue;let color=face.material==='energy'?[60+37*strength,25+24*strength,88+52*strength]:face.material==='steel'?[169,178,188]:[59,63,72];
  const shading=face.material==='energy'?.78+.22*Math.max(0,normal[1]/Math.hypot(...normal)):.52+.48*Math.max(0,dot(normal,[-.4,.8,.45])/Math.hypot(...normal));
  triangles.push({points,depth:points.reduce((s,p)=>s+p[2],0)/3,color:color.map(x=>Math.round(x*shading))});
 }
 const segments=[];
 function line(a,b,color,width){a=view(a);b=view(b);segments.push({a,b,depth:(a[2]+b[2])/2,color,width});}
 function arc(radius,width,start,length,count,alpha,transform){for(let i=0;i<count;i++){const a=start+length*i/count,b=start+length*(i+1)/count;line(transform([Math.cos(a)*radius,0,Math.sin(a)*radius]),transform([Math.cos(b)*radius,0,Math.sin(b)*radius]),`rgba(177,134,242,${alpha*(.28+.72*(i+1)/count)/255})`,width*scale);}}
 if(strength>.001){const pulse=.86+.14*Math.sin(phase*.13),inner=p=>ry(rz(p,rad(18)),rad(phase*.65%360));
  arc(.435,.012,0,Math.PI*2,48,130*strength*pulse,inner);arc(.456,.026,-phase*.11,Math.PI*.72,20,225*strength,inner);
  arc(.7,.015,phase*.075,Math.PI*.52,16,160*strength,p=>ry(rz(rx(p,rad(-55)),rad(-20)),rad(-phase*1.6%360)));
  if($('#beams').checked)for(let axis=0;axis<3;axis++)for(const sign of [-1,1]){let a=[0,0,0],b=[0,0,0];a[axis]=sign*.4;b[axis]=sign*1.375;line(a,b,`rgba(150,106,224,${65*strength*pulse/255})`,(.018+.012*strength)*scale);
   for(let spark=0;spark<3;spark++){const f=(phase/24+axis*.17+(sign+1)*.13+spark/3)%1,at=sign*(1.34-f*.91);a=[0,0,0];b=[0,0,0];a[axis]=at-sign*.04;b[axis]=at+sign*.04;line(a,b,`rgba(229,214,255,${220*strength/255})`,(.036+.018*strength)*scale);}}
 }
 for(const shape of [...triangles,...segments].sort((a,b)=>a.depth-b.depth)){ctx.beginPath();if(shape.points){shape.points.map(project).forEach((p,i)=>i?ctx.lineTo(...p):ctx.moveTo(...p));ctx.closePath();ctx.fillStyle=`rgb(${shape.color.join(',')})`;ctx.fill();ctx.strokeStyle=ctx.fillStyle;ctx.lineWidth=.35;ctx.stroke();}else{ctx.moveTo(...project(shape.a));ctx.lineTo(...project(shape.b));ctx.strokeStyle=shape.color;ctx.lineWidth=Math.max(.5,shape.width);ctx.stroke();}}
 $('#state').textContent=running?(strength<target-.03?'逐渐升速':'运行中'):strength>.001?'正在减速':'已停机';canvas.dataset.phase=phase.toFixed(3);canvas.dataset.strength=strength.toFixed(3);requestAnimationFrame(draw);
}
$('#start').onclick=()=>{running=true;$('#start').setAttribute('aria-pressed','true');$('#stop').setAttribute('aria-pressed','false');};$('#stop').onclick=()=>{running=false;$('#start').setAttribute('aria-pressed','false');$('#stop').setAttribute('aria-pressed','true');};$('#load').oninput=e=>{load=Number(e.target.value)/100;$('#load-value').value=e.target.value+'%';};
canvas.onpointerdown=e=>{drag=[e.clientX,e.clientY];canvas.setPointerCapture(e.pointerId);};canvas.onpointermove=e=>{if(!drag)return;yaw+=(e.clientX-drag[0])*.008;pitch=Math.max(-1.1,Math.min(1.1,pitch+(e.clientY-drag[1])*.007));drag=[e.clientX,e.clientY];};canvas.onpointerup=()=>drag=null;canvas.onpointercancel=()=>drag=null;canvas.addEventListener('wheel',e=>{e.preventDefault();zoom=Math.max(.65,Math.min(1.8,zoom*Math.exp(-e.deltaY*.001)));},{passive:false});
document.addEventListener('visibilitychange',()=>last=0);requestAnimationFrame(draw);
})();
