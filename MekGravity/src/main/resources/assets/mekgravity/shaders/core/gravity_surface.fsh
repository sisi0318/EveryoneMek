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

void main() {
    vec3 n = normalize(surfaceDirection);
    vec3 drift = vec3(flowPhase.y, flowPhase.x, -flowPhase.y) * 0.65;
    float broad = convection(n * 3.2 + drift);
    vec3 flow = n * 7.5 + vec3(broad, -broad, broad * 0.4) * 1.5;
    float current = convection(flow - drift);
    float detail = 0.5;
    if (vertexDistance < 18.0) {
        detail = convection(flow * 2.0 + drift);
    }
    float band = 1.0 - smoothstep(0.025, 0.11, abs(current - 0.52));
    vec3 dark = mix(vec3(0.045, 0.012, 0.085), vec3(0.21, 0.065, 0.34), broad);
    vec3 active = dark + vec3(0.22, 0.11, 0.34) * current;
    active = mix(active, vec3(0.80, 0.59, 1.0), band * (0.20 + detail * 0.37));
    float facing = clamp(dot(normalize(viewNormal), normalize(-viewPosition)), 0.0, 1.0);
    float rim = (1.0 - facing) * (1.0 - facing);
    vec3 color = mix(dark, active, heat) + vec3(0.13, 0.07, 0.22) * rim * heat;
    fragColor = linear_fog(vec4(color, 1.0) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
