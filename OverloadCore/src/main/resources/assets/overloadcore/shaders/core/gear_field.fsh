#version 150
#moj_import <fog.glsl>
in vec2 flow;
in vec2 powerAlpha;
in float vertexDistance;
flat in int material;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
out vec4 fragColor;
void main() {
    vec3 tint = (material == 0 || material == 2) ? vec3(0.22, 0.90, 0.72) : vec3(0.64, 0.33, 1.0);
    float wave = 0.5 + 0.5 * sin(flow.x * 27.0 - flow.y);
    float filaments = smoothstep(0.80, 0.99, wave);
    float power = 0.08 + 0.92 * powerAlpha.x;
    vec3 light = mix(tint * (0.45 + wave * 0.45), vec3(0.9, 1.0, 1.0), filaments * 0.65) * power;
    fragColor = linear_fog(vec4(light, powerAlpha.y) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
