#version 150
#moj_import <overloadcore:black_hole_gas.glsl>
uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform mat4 InverseProjection;
uniform mat4 CameraProjection;
uniform vec3 CenterView;
uniform vec3 DiscNormal;
uniform vec3 DiscAxis;
uniform vec2 FrameSize;
uniform float Radius;
uniform float Phase;
uniform float Visibility;
in vec2 screenUV;
out vec4 fragColor;
vec3 viewPoint(vec2 uv, float depth) {
    vec4 p = InverseProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}
vec2 projectRay(vec3 eye, vec3 ray) {
    vec4 p = CameraProjection * vec4(eye + ray, 1.0);
    return p.xy / p.w * 0.5 + 0.5;
}
void main() {
    vec4 eyeH=InverseProjection*vec4(0,0,-1,0);
    vec3 eye=eyeH.xyz/eyeH.w;
    vec3 center=CenterView-eye;
    float distanceToCenter = length(center);
    if (distanceToCenter <= Radius * 1.02) discard;
    vec3 axis = center / distanceToCenter;
    vec3 ray = normalize(viewPoint(screenUV, 1.0)-eye);
    float ahead = dot(ray, center);
    if (ahead <= 0.0) discard;
    vec3 right = normalize(cross(axis, abs(axis.y) < 0.96 ? vec3(0,1,0) : vec3(0,0,1)));
    vec3 up = cross(right, axis);
    // Angular impact parameter follows the projected sphere even at wide FOV and off-centre.
    vec2 impact = vec2(dot(ray,right), dot(ray,up)) * distanceToCenter / Radius;
    float r = length(impact);
    if (r > 3.0 || r < 0.96) discard;
    float sceneDepth = texture(SceneDepth, screenUV).r;
    float intersection=1.0;
    if (sceneDepth < 0.99999) {
        float clearance=length(viewPoint(screenUV,sceneDepth)-eye)-ahead;
        intersection=smoothstep(0.0,max(.06,Radius*.12),clearance);
        if(intersection<=0.0)discard;
    }
    // Compress the near-horizon image toward the optical centre, then join the
    // unchanged world with a zero-slope transition. This is a bounded cubic map,
    // not the reference shader's inverse-radius deflection formula.
    float innerImage=.14+.92*max(r-1.0,0.0);
    float sourceRadius=mix(innerImage,r,smoothstep(1.0,2.60,r));
    float shift=max(0.0,r-sourceRadius);
    shift *= smoothstep(1.03,1.8,distanceToCenter/Radius);
    vec2 sinAngle = impact * (1.0 - shift/max(r,0.1)) * Radius / distanceToCenter;
    vec3 bent = normalize(axis * sqrt(max(0.0,1.0-dot(sinAngle,sinAngle))) + right*sinAngle.x + up*sinAngle.y);
    vec2 sourceUV = projectRay(eye,bent);
    vec2 border = 1.5 / FrameSize;
    float safe = float(all(greaterThan(sourceUV,border)) && all(lessThan(sourceUV,vec2(1)-border)));
    sourceUV = clamp(sourceUV,border,vec2(1)-border);
    float sourceDepth = texture(SceneDepth,sourceUV).r;
    if (sourceDepth < 0.99999 && length(viewPoint(sourceUV,sourceDepth)-eye) < dot(center,bent)) safe=0.0;
    sourceUV=mix(screenUV,sourceUV,safe);
    float lensAlpha=(1.0-smoothstep(2.66,2.98,r))*Visibility*intersection;
    vec3 background=texture(SceneColor,sourceUV).rgb;
    float facing=dot(normalize(DiscNormal),axis);
    vec2 farSide=vec2(dot(DiscNormal,right),dot(DiscNormal,up));
    farSide=normalize(farSide+vec2(0.0001))*(-facing/sqrt(facing*facing+.015));
    float side=dot(impact/max(r,0.001),farSide);
    float inclination=smoothstep(.03,.75,1.0-abs(facing));
    // The core's continuous light envelope is present face-on too. Directional
    // far-side arcs emerge smoothly as the disc turns, without switching off the halo.
    float wrap=mix(.85,.40+.60*smoothstep(-.8,.8,side),inclination);
    vec3 skyDirection=right*impact.x+up*impact.y;
    vec3 discV=normalize(cross(DiscAxis,DiscNormal));
    float azimuth=atan(dot(skyDirection,discV),dot(skyDirection,DiscAxis));
    float arcRadial=clamp((r-1.012)/1.50,0.0,1.0);
    vec3 gas=gasLight(azimuth,arcRadial,Phase)*wrap*1.65;
    float photon=exp(-pow((r-1.028)/.040,2.0))*1.7;
    float envelope=exp(-pow((r-1.12)/.23,2.0));
    vec3 hotBand=vec3(1.0,.96,.82)*envelope*mix(.95,.65,inclination);
    float transmission=exp(-envelope*mix(2.1,1.4,inclination));
    vec3 emission=(gas+hotBand+vec3(1.0,.99,.94)*photon)*Visibility*intersection;
    float alpha=max(lensAlpha,clamp(max(emission.r,max(emission.g,emission.b)),0.0,1.0));
    if(alpha<.003)discard;
    fragColor=vec4(background*lensAlpha*transmission+emission,alpha);
}
