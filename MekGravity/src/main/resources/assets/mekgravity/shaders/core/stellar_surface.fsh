#version 150

#moj_import <fog.glsl>

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

#moj_import <mekgravity:core_convection.glsl>

void main() {
    vec3 n = normalize(surfaceDirection);
    vec3 drift = vec3(flowPhase.x, flowPhase.y, -flowPhase.x) * 0.72;
    float broad = convection(n * 3.8 + drift);
    vec3 flow = n * 10.0 + vec3(broad, -broad, broad * 0.6) * 1.8 + drift * 0.65;
    float cells = convection(flow);
    float filaments = convection(flow * 2.1 - drift);
    // Fine granules are omitted past 40 blocks and filtered before subpixel frequencies shimmer.
    float fine = 0.5;
    float detail = 1.0 - smoothstep(16.0, 40.0, vertexDistance);
    float footprint = max(length(dFdx(flow)), length(dFdy(flow)));
    if (detail > 0.01 && footprint < 0.8) {
        fine = mix(0.5, convection(flow * 4.0 + drift), detail * (1.0 - smoothstep(0.2, 0.8, footprint)));
    }
    float temperature = clamp(0.30 + broad * 0.28 + cells * 0.32 + (fine - 0.5) * 0.12, 0.0, 1.0);
    float ribbon = 1.0 - smoothstep(0.035, 0.13 + footprint * 0.08, abs(filaments - 0.52));
    vec3 plasma = mix(vec3(0.79, 0.19, 0.015), vec3(1.0, 0.73, 0.18), smoothstep(0.27, 0.76, temperature));
    plasma = mix(plasma, vec3(1.0, 0.88, 0.52), ribbon * (0.12 + cells * 0.16));
    vec3 ember = mix(vec3(0.19, 0.025, 0.008), vec3(0.72, 0.19, 0.025), temperature);
    float facing = clamp(dot(normalize(viewNormal), normalize(-viewPosition)), 0.0, 1.0);
    // Mild limb darkening preserves the volume; it is self-lit and needs no lightmap sample.
    float limb = 0.70 + 0.30 * sqrt(facing);
    vec3 color = mix(ember, plasma, heat) * limb;
    fragColor = linear_fog(vec4(color, 1.0) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
