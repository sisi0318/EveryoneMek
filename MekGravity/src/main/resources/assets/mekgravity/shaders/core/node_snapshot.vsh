#version 150
#moj_import <fog.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
out vec2 atlasUv;
out vec2 localUv;
out vec4 tint;
out vec2 work;
out float vertexDistance;
void main(){
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    atlasUv=UV0;localUv=vec2(UV1)/256.0;tint=Color;
    work=vec2(atan(Normal.y,Normal.x)+float(UV2.y)*0.024543693,floor(Normal.z*3.0+.5));
    vertexDistance=fog_distance(Position,FogShape);
}
