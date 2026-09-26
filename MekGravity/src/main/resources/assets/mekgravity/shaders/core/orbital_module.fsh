#version 150
#moj_import <fog.glsl>
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
in vec2 fieldCoord;
in vec4 workState;
in float vertexDistance;
out vec4 fragColor;
void main(){
    float kind=floor(workState.x),progress=fract(workState.x)/0.8,time=workState.y;
    vec3 color=kind<0.5?vec3(1.0,0.55,0.10):kind<1.5?vec3(0.74,0.40,1.0):kind<2.5?vec3(0.30,1.0,0.77):kind<3.5?vec3(0.35,0.64,1.0):vec3(0.50,0.87,1.0);
    float alpha=0.0;
    if(kind==2.0||kind==4.0){
        vec2 p=fieldCoord;
        vec2 grid=abs(fract((p+1.0)*vec2(5.0,3.0))-0.5);
        float line=1.0-smoothstep(0.47,0.5,max(grid.x,grid.y));
        float trace=progress*1.25-0.65+0.06*sin(p.x*10.0-time*2.0);
        float curve=1.0-smoothstep(0.016,0.035+fwidth(p.y),abs(p.y-trace));
        float edge=1.0-smoothstep(0.93,1.0,max(abs(p.x),abs(p.y)));
        alpha=edge*(0.045+(1.0-line)*0.12+curve*0.8);
    }else{
        float angle=fieldCoord.x,across=fieldCoord.y;
        float wave=0.5+0.18*sin(angle*(kind<.5?8.0:4.0)+time*2.0);
        float filament=1.0-smoothstep(0.025,0.075+fwidth(across),abs(across-wave));
        float halo=max(0.0,1.0-abs(across*2.0-1.0));
        float flow=0.35+0.65*pow(0.5+0.5*cos(angle*3.0-time*2.0),3.0);
        alpha=(filament*.8+halo*halo*.14)*flow;
        color=mix(color,vec3(1.0,0.95,0.80),filament*(0.25+progress*.3));
    }
    fragColor=vec4(color,alpha*workState.a*linear_fog_fade(vertexDistance,FogStart,FogEnd))*ColorModulator;
}
