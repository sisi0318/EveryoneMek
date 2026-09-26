#version 150
#moj_import <fog.glsl>
#moj_import <mekgravity:core_convection.glsl>
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec3 surfaceDirection;
in vec3 viewNormal;
in vec3 viewPosition;
in vec2 flowPhase;
in float heat;
in float vertexDistance;
out vec4 fragColor;
void main(){
    vec3 n=normalize(surfaceDirection);
    vec3 drift=vec3(flowPhase.y,flowPhase.x,-flowPhase.y);
    float broad=convection(n*3.6+drift);
    vec3 p=n*8.0+drift*.7+vec3(broad,-broad,broad)*1.3;
    float cells=convection(p);
    float width=max(length(dFdx(p)),length(dFdy(p)));
    float seam=1.0-smoothstep(.025,.10+width*.1,abs(cells-.50));
    float band=.5+.5*sin(n.y*17.0+n.x*4.0+flowPhase.y*3.0);
    vec3 body=mix(vec3(.42,.065,.012),vec3(1.0,.46,.04),smoothstep(.25,.80,broad));
    body=mix(body,vec3(1.0,.85,.36),seam*(.45+.4*band));
    float face=clamp(dot(normalize(viewNormal),normalize(-viewPosition)),0.0,1.0);
    body*=.76+.24*sqrt(face);
    body=mix(body*.22,body,heat);
    fragColor=linear_fog(vec4(body,1.0)*ColorModulator,vertexDistance,FogStart,FogEnd,FogColor);
}
