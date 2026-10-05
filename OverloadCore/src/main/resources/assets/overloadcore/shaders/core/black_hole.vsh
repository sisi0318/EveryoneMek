#version 150
#moj_import <fog.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
out vec2 imagePoint;
flat out vec3 centerView;
flat out float radius;
flat out float phase;
void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    radius = Color.r * 8.0;
    phase = Color.g * 6.2831853;
    imagePoint = (UV0 * 2.0 - 1.0) * 3.0;
    centerView = view.xyz - vec3(imagePoint * radius, 0.0);
    gl_Position = ProjMat * view;
}
