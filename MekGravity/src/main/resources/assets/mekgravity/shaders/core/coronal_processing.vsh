#version 150
#moj_import <fog.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
out vec2 fieldCoord;
out vec4 workState;
out float vertexDistance;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    fieldCoord = UV0;
    workState = vec4(Color.r, atan(Color.g*2.0-1.0,Color.b*2.0-1.0),0.0,Color.a);
    vertexDistance = fog_distance(Position, FogShape);
}
