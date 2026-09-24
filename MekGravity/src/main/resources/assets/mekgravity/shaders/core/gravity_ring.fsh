#version 150

#moj_import <fog.glsl>

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec3 viewPosition;
in vec3 viewNormal;
in vec2 ringFlow;
in vec3 material;
in float vertexDistance;
out vec4 fragColor;

void main() {
    float u = ringFlow.x;
    float v = material.z;
    float load = material.x;
    vec3 normal = normalize(viewNormal);
    vec3 light = normalize(vec3(-0.35, 0.85, 0.45));
    float diffuse = 0.38 + 0.62 * max(dot(normal, light), 0.0);
    float specular = max(dot(reflect(-light, normal), normalize(-viewPosition)), 0.0);
    specular *= specular; specular *= specular; specular *= specular; specular *= specular;
    float edge = 1.0 - smoothstep(0.04, 0.22, min(v, 1.0-v));
    vec3 steel = mix(vec3(0.25, 0.27, 0.32), vec3(0.55, 0.58, 0.65), edge);
    steel *= mix(1.0, 0.55, material.y);
    vec3 color = steel * diffuse + vec3(0.34, 0.36, 0.40) * specular;

    // One center channel in the existing ring surface. Derivative antialiasing prevents
    // the narrow track and segment joins from flickering when the ring becomes small.
    float aa = max(fwidth(v), 0.01);
    float channel = 1.0 - smoothstep(0.17-aa, 0.17+aa, abs(v-0.5));
    float joint = abs(fract(u*12.0)-0.5);
    float seams = smoothstep(0.025, 0.025+max(fwidth(u)*12.0, 0.015), joint);
    float moving = fract(u-ringFlow.y);
    float head = 1.0 - smoothstep(0.035, 0.21, min(moving, 1.0-moving));
    float power = load * channel * seams;
    vec3 energy = mix(vec3(0.22, 0.08, 0.45), vec3(0.86, 0.67, 1.0), head);
    color = mix(color, energy, power * 0.92);
    fragColor = linear_fog(vec4(color, 1.0) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
