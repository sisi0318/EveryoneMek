#version 150
#moj_import <overloadcore:black_hole_gas.glsl>
uniform float Phase;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform sampler2D SceneDepth;
uniform mat4 DepthProjectionInverse;
uniform vec2 FrameSize;
uniform float SoftDepth;
uniform float SoftRange;
in vec2 discUV;
in float vertexDistance;
in float viewDepth;
out vec4 fragColor;
void main() {
    vec3 light=gasLight(discUV.x*6.2831853,discUV.y,Phase);
    float visibility=1.0-smoothstep(FogStart,FogEnd,vertexDistance);
    if(SoftDepth>.5){
        vec2 uv=gl_FragCoord.xy/FrameSize;
        float depth=texture(SceneDepth,uv).r;
        if(depth<.99999){
            vec4 point=DepthProjectionInverse*vec4(uv*2.0-1.0,depth*2.0-1.0,1.0);
            float clearance=-point.z/point.w-viewDepth;
            visibility*=smoothstep(0.0,SoftRange,clearance);
        }
    }
    light*=visibility*ColorModulator.rgb;
    if(max(light.r,max(light.g,light.b))<.004)discard;
    fragColor=vec4(light,0.0);
}
