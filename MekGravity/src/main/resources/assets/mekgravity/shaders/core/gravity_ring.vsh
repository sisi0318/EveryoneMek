#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out vec3 viewPosition;
out vec3 viewNormal;
out vec2 ringFlow;
out vec3 material;
out float vertexDistance;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    viewPosition = view.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    ringFlow = UV0; // circumferential U and per-ring circular phase
    material = Color.rgb; // operating strength, inner metal, transverse V
    vertexDistance = fog_distance(Position, FogShape);
}
