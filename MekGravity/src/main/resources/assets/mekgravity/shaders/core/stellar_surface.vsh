#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out vec3 surfaceDirection;
out vec3 viewNormal;
out vec3 viewPosition;
out vec2 flowPhase;
out float heat;
out float vertexDistance;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    viewPosition = view.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    // Local sphere direction is packed in RGB; alpha is the independent thermal state.
    // These are attributes, so multiple buffered suns cannot overwrite each other's phase.
    surfaceDirection = Color.rgb * 2.0 - 1.0;
    flowPhase = UV0;
    heat = Color.a;
    vertexDistance = fog_distance(Position, FogShape);
}
