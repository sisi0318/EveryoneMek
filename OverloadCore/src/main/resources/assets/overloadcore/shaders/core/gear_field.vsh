#version 150
#moj_import <fog.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
out vec2 flow;
out vec2 powerAlpha;
out float vertexDistance;
out float phase;
flat out int material;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance((ModelViewMat * vec4(Position, 1.0)).xyz, FogShape);
    flow = UV0;
    powerAlpha = Color.ga;
    material = int(Color.r * 255.0 + 0.5);
    phase = material < 2 ? UV0.y : Color.b * 6.2831853;
}
