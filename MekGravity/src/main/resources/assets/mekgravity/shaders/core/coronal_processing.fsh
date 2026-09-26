#version 150
#moj_import <fog.glsl>
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
in vec2 fieldCoord;
in vec4 workState;
in float vertexDistance;
out vec4 fragColor;
void main() {
    // Two analytic wave bands, no texture reads, noise loops, raymarch or full-screen pass.
    float angle = fieldCoord.x;
    float flow = angle*3.0 - workState.y*2.0;
    float center = 0.5 + 0.15*sin(angle*6.0+workState.y*2.0);
    float distance = abs(fieldCoord.y-center);
    float edge = max(fwidth(fieldCoord.y),0.015);
    float filament = 1.0-smoothstep(0.03,0.08+edge,distance);
    float halo = max(0.0,1.0-abs(fieldCoord.y*2.0-1.0));
    float pulse = 0.4 + 0.6*pow(0.5+0.5*cos(flow),3.0);
    vec3 heat = mix(vec3(1.0,0.18,0.02),vec3(1.0,0.72,0.18),workState.x);
    heat = mix(heat,vec3(1.0,0.90,0.62),filament*0.65);
    float alpha = (filament*0.8+halo*halo*0.16)*pulse*workState.a;
    fragColor = vec4(heat,alpha*linear_fog_fade(vertexDistance,FogStart,FogEnd))*ColorModulator;
}
