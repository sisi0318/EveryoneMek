#version 150
#moj_import <fog.glsl>
in vec3 Position;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
out vec2 discUV;
out float vertexDistance;
out float viewDepth;
void main() {
    vec4 p=ModelViewMat*vec4(Position,1.0);
    vertexDistance=fog_distance(p.xyz,FogShape);
    viewDepth=-p.z;
    discUV=UV0;
    gl_Position=ProjMat*p;
}
