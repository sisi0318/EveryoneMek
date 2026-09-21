/* Standalone WebGL2 model review. No CDN, engine download, textures or game client. */
(()=>{'use strict';
const $=s=>document.querySelector(s),all=s=>[...document.querySelectorAll(s)];
const canvas=$('#scene'),gl=canvas.getContext('webgl2',{antialias:true,alpha:true,preserveDrawingBuffer:true});
if(!gl){$('#failure').hidden=false;$('#failure').textContent='当前浏览器没有可用的 WebGL 2。建模包仍可下载，在三维编辑器中打开 OBJ。';return;}
const data=window.REACTOR_MODEL,sub=(a,b)=>a.map((x,i)=>x-b[i]),add=(a,b)=>a.map((x,i)=>x+b[i]),mul=(a,s)=>a.map(x=>x*s);
const dot=(a,b)=>a.reduce((s,x,i)=>s+x*b[i],0),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>mul(a,1/Math.hypot(...a));
const mm=(a,b)=>{const o=new Float32Array(16);for(let c=0;c<4;c++)for(let r=0;r<4;r++)for(let k=0;k<4;k++)o[c*4+r]+=a[k*4+r]*b[c*4+k];return o;};
const perspective=(aspect)=>{const f=1/Math.tan(Math.PI/8),n=.1,F=90;return new Float32Array([f/aspect,0,0,0,0,f,0,0,0,0,(F+n)/(n-F),-1,0,0,2*F*n/(n-F),0]);};
const ortho=(r,n,F)=>new Float32Array([1/r,0,0,0,0,1/r,0,0,0,0,-2/(F-n),0,0,0,-(F+n)/(F-n),1]);
function lookAt(e,t){const z=unit(sub(e,t)),x=unit(cross([0,1,0],z)),y=cross(z,x);return new Float32Array([x[0],y[0],z[0],0,x[1],y[1],z[1],0,x[2],y[2],z[2],0,-dot(x,e),-dot(y,e),-dot(z,e),1]);}
function shader(type,source){const s=gl.createShader(type);gl.shaderSource(s,source);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw Error(gl.getShaderInfoLog(s));return s;}
function program(v,f){const p=gl.createProgram();gl.attachShader(p,shader(gl.VERTEX_SHADER,v));gl.attachShader(p,shader(gl.FRAGMENT_SHADER,f));gl.linkProgram(p);if(!gl.getProgramParameter(p,gl.LINK_STATUS))throw Error(gl.getProgramInfoLog(p));return p;}
const vs=`#version 300 es
layout(location=0) in vec3 position;layout(location=1) in vec3 normal;layout(location=2) in vec3 color;layout(location=3) in vec3 gray;layout(location=4) in float alpha;
uniform mat4 vp,lightVP;uniform vec3 offset;uniform bool colored;out vec3 N;out vec3 C;out float A;out vec4 shadow;
void main(){vec4 p=vec4(position+offset,1.);gl_Position=vp*p;N=normal;C=colored?color:gray;A=alpha;shadow=lightVP*p;}`;
const fs=`#version 300 es
precision highp float;in vec3 N;in vec3 C;in float A;in vec4 shadow;uniform sampler2D shadowMap;uniform bool selected;out vec4 frag;
void main(){vec3 n=normalize(N),s=shadow.xyz/shadow.w*.5+.5;float vis=1.;if(s.x>0.&&s.x<1.&&s.y>0.&&s.y<1.&&s.z<1.){vis=0.;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++){float d=texture(shadowMap,s.xy+vec2(x,y)/1536.).r;vis+=s.z-.0018<d?1.:.25;}vis/=9.;}float key=max(dot(n,normalize(vec3(-.55,.85,-.45))),0.);float fill=max(dot(n,normalize(vec3(.6,.3,.6))),0.);vec3 c=C*(.48+.59*key*vis+.15*fill);if(selected)c=mix(c,vec3(.48,.66,.77),.3);frag=vec4(c,A);}`;
const depthVS=`#version 300 es
layout(location=0)in vec3 position;uniform mat4 vp;uniform vec3 offset;void main(){gl_Position=vp*vec4(position+offset,1.);}`;
const depthFS=`#version 300 es
precision highp float;void main(){}`;
const lineFS=`#version 300 es
precision highp float;uniform vec4 tint;out vec4 frag;void main(){frag=tint;}`;
const main=program(vs,fs),depth=program(depthVS,depthFS),lines=program(depthVS,lineFS);
const locations=p=>Object.fromEntries(['vp','lightVP','offset','colored','selected','shadowMap','tint'].map(n=>[n,gl.getUniformLocation(p,n)]));
const m=locations(main),d=locations(depth),l=locations(lines);
function buffer(values,stride=13){const vao=gl.createVertexArray();gl.bindVertexArray(vao);const b=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,b);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(values),gl.STATIC_DRAW);for(const [i,n,o]of(stride===13?[[0,3,0],[1,3,3],[2,3,6],[3,3,9],[4,1,12]]:[[0,3,0]])){gl.enableVertexAttribArray(i);gl.vertexAttribPointer(i,n,gl.FLOAT,false,stride*4,o*4);}return{vao,count:values.length/stride};}
function makePart(part){const opaque=[],transparent=[],wire=[];for(const tri of part.triangles){const mat=data.materials[tri.material],normal=unit(cross(sub(tri.p[1],tri.p[0]),sub(tri.p[2],tri.p[0]))),dest=mat.alpha?transparent:opaque;for(const p of tri.p)dest.push(...p,...normal,...mat.color,...mat.gray,mat.alpha||1);for(let i=0;i<3;i++)wire.push(...tri.p[i],...tri.p[(i+1)%3]);}return{...part,opaque:buffer(opaque),transparent:buffer(transparent),wire:buffer(wire,3),center:part.bounds[0].map((x,i)=>(x+part.bounds[1][i])/2)};}
const parts=data.parts.map(makePart);
const floorTri=[[-14,-.08,-14],[-14,-.08,21],[21,-.08,21],[-14,-.08,-14],[21,-.08,21],[21,-.08,-14]],floor=buffer(floorTri.flatMap(p=>[...p,0,1,0,.09,.12,.145,.09,.12,.145,1]));
const gridPoints=[];for(let i=-8;i<=15;i++)gridPoints.push(i,-.075,-8,i,-.075,15,-8,-.075,i,15,-.075,i);const gridBuffer=buffer(gridPoints,3);
const shadowTexture=gl.createTexture();gl.bindTexture(gl.TEXTURE_2D,shadowTexture);gl.texImage2D(gl.TEXTURE_2D,0,gl.DEPTH_COMPONENT24,1536,1536,0,gl.DEPTH_COMPONENT,gl.UNSIGNED_INT,null);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.NEAREST);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);
const fb=gl.createFramebuffer();gl.bindFramebuffer(gl.FRAMEBUFFER,fb);gl.framebufferTexture2D(gl.FRAMEBUFFER,gl.DEPTH_ATTACHMENT,gl.TEXTURE_2D,shadowTexture,0);gl.drawBuffers([gl.NONE]);gl.readBuffer(gl.NONE);if(gl.checkFramebufferStatus(gl.FRAMEBUFFER)!==gl.FRAMEBUFFER_COMPLETE)throw Error('Shadow framebuffer is incomplete');gl.bindFramebuffer(gl.FRAMEBUFFER,null);
const lightVP=mm(ortho(11,1,50),lookAt([-7,18,-5],[3.5,3,3.5]));
let yaw=-.56,pitch=.35,distance=14.5,target=[3.5,3.15,3.5],mode='cut',colored=false,explode=0,selected=null,queued=false,drawn=0;
function offset(p){if(!explode||p.kind==='core')return[0,0,0];let vector=sub(p.center,[3.5,3.5,3.5]);if(p.kind==='coil')return mul(unit(vector),explode*.95);if(p.role==='roof')return[0,1.8*explode,0];if(p.role==='floor')return[0,-.8*explode,0];return mul(vector,explode*.23);}
function visible(p){if(mode==='inside'&&!['core','coil'].includes(p.kind))return false;if(mode==='cut'&&(p.role==='roof'||p.role==='front_window'))return false;if(['frame','joint','panel'].includes(p.kind)&&!$('#frames').checked)return false;if(p.kind==='glass'&&!$('#windows').checked)return false;if(p.kind==='coil'&&!$('#emitters').checked)return false;if(p.kind==='core'&&!$('#core').checked)return false;return true;}
function eye(){return add(target,[Math.sin(yaw)*Math.cos(pitch)*distance,Math.sin(pitch)*distance,-Math.cos(yaw)*Math.cos(pitch)*distance]);}
function drawMesh(mesh){if(mesh.count){gl.bindVertexArray(mesh.vao);gl.drawArrays(gl.TRIANGLES,0,mesh.count);}}
function render(){queued=false;const rect=canvas.getBoundingClientRect(),ratio=Math.min(devicePixelRatio||1,2),W=Math.round(rect.width*ratio),H=Math.round(rect.height*ratio);if(canvas.width!==W||canvas.height!==H){canvas.width=W;canvas.height=H;}const camera=eye(),vp=mm(perspective(W/H),lookAt(camera,target)),shown=parts.filter(visible);
gl.enable(gl.DEPTH_TEST);gl.enable(gl.CULL_FACE);gl.cullFace(gl.BACK);gl.disable(gl.BLEND);gl.depthMask(true);gl.depthFunc(gl.LESS);
gl.bindFramebuffer(gl.FRAMEBUFFER,fb);gl.viewport(0,0,1536,1536);gl.clear(gl.DEPTH_BUFFER_BIT);gl.useProgram(depth);gl.uniformMatrix4fv(d.vp,false,lightVP);for(const p of shown){gl.uniform3fv(d.offset,offset(p));drawMesh(p.opaque);}gl.bindFramebuffer(gl.FRAMEBUFFER,null);
gl.viewport(0,0,W,H);gl.clearColor(0,0,0,0);gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);gl.useProgram(main);gl.uniformMatrix4fv(m.vp,false,vp);gl.uniformMatrix4fv(m.lightVP,false,lightVP);gl.uniform1i(m.colored,colored);gl.activeTexture(gl.TEXTURE0);gl.bindTexture(gl.TEXTURE_2D,shadowTexture);gl.uniform1i(m.shadowMap,0);gl.uniform1i(m.selected,false);gl.uniform3fv(m.offset,[0,0,0]);drawMesh(floor);
for(const p of shown){gl.uniform3fv(m.offset,offset(p));gl.uniform1i(m.selected,p.id===selected);drawMesh(p.opaque);}
gl.enable(gl.BLEND);gl.blendFunc(gl.SRC_ALPHA,gl.ONE_MINUS_SRC_ALPHA);gl.depthMask(false);gl.disable(gl.CULL_FACE);
for(const p of shown.slice().sort((a,b)=>Math.hypot(...sub(add(b.center,offset(b)),camera))-Math.hypot(...sub(add(a.center,offset(a)),camera)))){gl.uniform3fv(m.offset,offset(p));gl.uniform1i(m.selected,p.id===selected);drawMesh(p.transparent);}
gl.useProgram(lines);gl.uniformMatrix4fv(l.vp,false,vp);gl.depthFunc(gl.LEQUAL);if($('#grid').checked){gl.uniform3fv(l.offset,[0,0,0]);gl.uniform4fv(l.tint,[.32,.4,.46,.19]);gl.bindVertexArray(gridBuffer.vao);gl.drawArrays(gl.LINES,0,gridBuffer.count);}
if($('#wire').checked||selected){for(const p of shown){if(!$('#wire').checked&&p.id!==selected)continue;gl.uniform3fv(l.offset,offset(p));gl.uniform4fv(l.tint,p.id===selected?[.66,.85,1,.9]:[.07,.10,.13,.45]);gl.bindVertexArray(p.wire.vao);gl.drawArrays(gl.LINES,0,p.wire.count);}}
gl.depthMask(true);$('#part-count').textContent=`${shown.length} / ${parts.length} 个模型部件`;canvas.dataset.frames=String(++drawn);canvas.dataset.ready='true';}
function request(){if(!queued){queued=true;requestAnimationFrame(render);}}
function select(id){selected=id;const p=parts.find(p=>p.id===id);$('#selection-title').textContent=p?.label||'点击模型查看';$('#selection-detail').textContent=p?(p.kind==='glass'&&p.worldSpace?'连续窗框与内嵌玻璃，按整面组合展示。':`方块锚点 ${p.position.join(' · ')}${p.kind==='coil'?' · 阶梯发射头伸入腔内 ¼ 格':''}`):'先看轮廓、凹凸和连接，再确定材质。';$('#clear-selection').hidden=!id;request();}
function pick(event){const rect=canvas.getBoundingClientRect(),nx=(event.clientX-rect.left)/rect.width*2-1,ny=1-(event.clientY-rect.top)/rect.height*2,origin=eye(),forward=unit(sub(target,origin)),right=unit(cross(forward,[0,1,0])),up=cross(right,forward);const ray=unit(add(forward,add(mul(right,nx*Math.tan(Math.PI/8)*rect.width/rect.height),mul(up,ny*Math.tan(Math.PI/8)))));let nearest=Infinity,id=null;
for(const p of parts.filter(visible)){if(p.kind==='glass'&&!event.altKey)continue;const off=offset(p);let lo=0,hi=Infinity;for(let i=0;i<3;i++){let a=(p.bounds[0][i]+off[i]-origin[i])/ray[i],b=(p.bounds[1][i]+off[i]-origin[i])/ray[i];if(a>b)[a,b]=[b,a];lo=Math.max(lo,a);hi=Math.min(hi,b);}if(hi<lo||lo>=nearest)continue;
for(const tri of p.triangles){const a=add(tri.p[0],off),e1=sub(tri.p[1],tri.p[0]),e2=sub(tri.p[2],tri.p[0]),h=cross(ray,e2),det=dot(e1,h);if(Math.abs(det)<1e-9)continue;const s=sub(origin,a),u=dot(s,h)/det;if(u<0||u>1)continue;const q=cross(s,e1),v=dot(ray,q)/det;if(v<0||u+v>1)continue;const t=dot(e2,q)/det;if(t>0&&t<nearest){nearest=t;id=p.id;}}}select(id);}
let drag=null;canvas.addEventListener('pointerdown',e=>{canvas.setPointerCapture(e.pointerId);drag={x:e.clientX,y:e.clientY,startX:e.clientX,startY:e.clientY,pan:e.button===2||e.shiftKey};});
canvas.addEventListener('pointermove',e=>{if(!drag)return;const dx=e.clientX-drag.x,dy=e.clientY-drag.y;drag.x=e.clientX;drag.y=e.clientY;if(drag.pan){const f=unit(sub(target,eye())),r=unit(cross(f,[0,1,0])),u=cross(r,f);target=add(target,add(mul(r,-dx*distance*.0012),mul(u,dy*distance*.0012)));}else{yaw-=dx*.008;pitch=Math.max(-.12,Math.min(1.49,pitch+dy*.007));}all('[data-view]').forEach(b=>b.classList.remove('active'));request();});
canvas.addEventListener('pointerup',e=>{if(drag&&Math.hypot(e.clientX-drag.startX,e.clientY-drag.startY)<4&&!drag.pan)pick(e);drag=null;});canvas.addEventListener('pointercancel',()=>drag=null);canvas.addEventListener('contextmenu',e=>e.preventDefault());canvas.addEventListener('wheel',e=>{e.preventDefault();distance=Math.max(6,Math.min(35,distance*Math.exp(e.deltaY*.001)));request();},{passive:false});
canvas.addEventListener('keydown',e=>{if(['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','+','-'].includes(e.key)){e.preventDefault();if(e.key==='ArrowLeft')yaw+=.12;if(e.key==='ArrowRight')yaw-=.12;if(e.key==='ArrowUp')pitch=Math.min(1.49,pitch+.1);if(e.key==='ArrowDown')pitch=Math.max(-.12,pitch-.1);if(e.key==='+')distance=Math.max(6,distance*.9);if(e.key==='-')distance=Math.min(35,distance/ .9);request();}});
function setMode(value){mode=value;all('[data-mode]').forEach(b=>b.setAttribute('aria-pressed',b.dataset.mode===mode));$('#mode-name').textContent={cut:'剖开查看',full:'完整装配',inside:'仅看内腔'}[mode];select(null);request();}
all('[data-mode]').forEach(b=>b.addEventListener('click',()=>setMode(b.dataset.mode)));
all('[data-palette]').forEach(b=>b.addEventListener('click',()=>{colored=b.dataset.palette==='color';all('[data-palette]').forEach(el=>el.setAttribute('aria-pressed',el===b));request();}));
all('input[type=checkbox]').forEach(b=>b.addEventListener('change',request));$('#explode').addEventListener('input',e=>{explode=Number(e.target.value)/100;$('#explode-value').value=Math.round(explode*100)+'%';request();});
function view(name){target=[3.5,3.15,3.5];distance=14.5;[yaw,pitch]={home:[-.56,.35],front:[0,.12],side:[Math.PI/2,.12],top:[0,1.49]}[name];all('[data-view]').forEach(b=>b.classList.toggle('active',b.dataset.view===name));request();}
all('[data-view]').forEach(b=>b.addEventListener('click',()=>view(b.dataset.view)));$('#reset').addEventListener('click',()=>{explode=0;colored=false;all('[data-palette]').forEach(b=>b.setAttribute('aria-pressed',b.dataset.palette==='gray'));$('#explode').value=0;$('#explode-value').value='0%';for(const id of ['frames','windows','emitters','core','grid'])$('#'+id).checked=true;$('#wire').checked=false;setMode('cut');view('home');});$('#clear-selection').addEventListener('click',()=>select(null));
$('#reference-button').addEventListener('click',()=>$('#reference').showModal());$('#close-reference').addEventListener('click',()=>$('#reference').close());$('#reference').addEventListener('click',e=>{if(e.target===$('#reference'))$('#reference').close();});
new ResizeObserver(request).observe(canvas);request();
})();
