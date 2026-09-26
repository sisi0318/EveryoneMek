#version 150
#moj_import <fog.glsl>
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec3 localPosition;
in vec3 viewNormal;
in float vertexDistance;
flat in int kind;
flat in int region;
flat in float phase;
flat in float charge;
out vec4 fragColor;
void main(){
    vec3 p=localPosition,n=normalize(viewNormal);
    float diffuse=.55+.45*max(0.0,dot(n,normalize(vec3(-.4,.8,.6))));
    vec3 color;
    if(region<2){
        color=(region==0?vec3(.56,.59,.62):vec3(.12,.15,.18))*diffuse;
        float bevel=pow(max(0.0,dot(n,normalize(vec3(.2,.5,1.0)))),12.0);
        color+=vec3(.13,.14,.15)*bevel;
    }else{
        float wave=.5+.5*sin(p.y*25.0+p.x*9.0+phase*2.0+sin(p.z*19.0-phase));
        float seam=1.0-smoothstep(.07,.15,abs(sin((p.x+p.z)*24.0-phase*2.0)));
        if(kind==0)color=mix(vec3(.12,.04,.20),vec3(.82,.35,.08),seam*.65+wave*.15);
        else color=mix(vec3(.48,.08,.015),vec3(1.0,.72,.17),wave*.75+seam*.25);
        if(kind==1)color*=.68;
        if(kind==4)color*=.18+.82*charge;
        color*=.75+.25*diffuse;
    }
    fragColor=linear_fog(vec4(color,1.0)*ColorModulator,vertexDistance,FogStart,FogEnd,FogColor);
}
