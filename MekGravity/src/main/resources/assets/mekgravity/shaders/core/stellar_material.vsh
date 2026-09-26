#version 150
#moj_import <fog.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
out vec3 localPosition;
out vec3 viewNormal;
out float vertexDistance;
flat out int kind;
flat out int region;
flat out float phase;
flat out float charge;
void main(){
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    localPosition=Color.rgb-.5;viewNormal=mat3(ModelViewMat)*Normal;
    int code=int(floor(Color.a*15.0+.5));kind=code/3;region=code%3;
    phase=atan(UV0.y,UV0.x);charge=clamp((length(UV0)-.15)/.85,0.0,1.0);
    vertexDistance=fog_distance(Position,FogShape);
}
