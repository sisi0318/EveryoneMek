#version 150
#moj_import <fog.glsl>
uniform float Phase;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec2 discUV;
in float vertexDistance;
out vec4 fragColor;
float hash(vec2 cell) {
    vec3 h=fract(vec3(cell.x,cell.y,cell.x+cell.y)*vec3(.1031,.0973,.1099));
    h+=dot(h,h.yzx+19.19);
    return fract((h.x+h.y)*h.z);
}
float noise(vec2 p) {
    vec2 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(hash(cell),hash(cell+vec2(1,0)),f.x),mix(hash(cell+vec2(0,1)),hash(cell+vec2(1)),f.x),f.y);
}
void main() {
    float radial=clamp(discUV.y,0.0,1.0);
    float angle=discUV.x*6.2831853-Phase*.48/(.45+radial);
    vec2 orbit=vec2(cos(angle),sin(angle));
    float cloud=noise(orbit*(5.0+radial*8.0)+vec2(radial*3.0,Phase*.08));
    float vein=sin(radial*43.0+cloud*4.0-angle*2.0+Phase*.9);
    // Differentiate the periodic vector, not the unwrapped angle at the UV seam.
    float detail=1.0-smoothstep(.10,.85,fwidth(radial)*43.0+length(fwidth(orbit))*2.0);
    float threads=mix(.65,.5+.5*vein,detail);
    float envelope=smoothstep(0.0,.08,radial)*(1.0-smoothstep(.66,1.0,radial));
    float heat=clamp(1.15-radial*.90+cloud*.10,0.0,1.0);
    vec3 tint=mix(vec3(.95,.36,.07),vec3(1.0,.96,.82),heat);
    float energy=(.70+.22*threads+.13*cloud)*(1.0-radial*.25);
    float alpha=envelope*clamp(.92-radial*.20+cloud*.08,0.0,1.0);
    if(alpha<.003)discard;
    vec4 color=linear_fog(vec4(tint*energy,alpha)*ColorModulator,vertexDistance,FogStart,FogEnd,FogColor);
    fragColor=vec4(color.rgb*color.a,color.a);
}
