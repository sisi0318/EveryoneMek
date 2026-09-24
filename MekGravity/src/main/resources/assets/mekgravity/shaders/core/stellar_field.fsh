#version 150
#moj_import <fog.glsl>
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec2 fieldCoord;
in vec4 fieldColor;
in float vertexDistance;
out vec4 fragColor;
void main() {
    float across = abs(fieldCoord.x);
    float core = max(0.0, 1.0-across);
    float halo = max(0.0, 1.0-across/3.0);
    float profile = core*core + halo*halo*0.09;
    float flow = 0.80 + 0.20*cos(fieldCoord.y);
    vec3 light = mix(fieldColor.rgb, vec3(0.94,0.89,1.0), core*0.26);
    // Additive field must fade to transparency in fog, not add a bright fog-colored veil.
    float visibility = linear_fog_fade(vertexDistance,FogStart,FogEnd);
    fragColor = vec4(light,fieldColor.a*profile*flow*visibility) * ColorModulator;
}
