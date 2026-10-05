#version 150
#moj_import <overloadcore:black_hole_gas.glsl>
uniform float Phase;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
in vec2 discUV;
in float vertexDistance;
out vec4 fragColor;
void main() {
    vec3 light=gasLight(discUV.x*6.2831853,discUV.y,Phase);
    float visibility=1.0-smoothstep(FogStart,FogEnd,vertexDistance);
    light*=visibility*ColorModulator.rgb;
    if(max(light.r,max(light.g,light.b))<.004)discard;
    fragColor=vec4(light,0.0);
}
