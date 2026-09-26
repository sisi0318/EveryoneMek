#version 150
#moj_import <fog.glsl>
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in vec2 atlasUv;
in vec2 localUv;
in vec4 tint;
in vec2 work;
in float vertexDistance;
out vec4 fragColor;
float segment(vec2 p,vec2 a,vec2 b){vec2 v=b-a;return length(p-a-v*clamp(dot(p-a,v)/dot(v,v),0.0,1.0));}
void main(){
    vec4 icon;
    if(work.y==1.0){
        vec2 p=localUv*2.0-1.0;
        float d=min(segment(p,vec2(.25,-.72),vec2(-.27,.07)),min(segment(p,vec2(-.27,.07),vec2(.26,-.05)),segment(p,vec2(.26,-.05),vec2(-.24,.72))));
        float a=1.0-smoothstep(.075,.105,d);icon=vec4(1.0,.80,.24,a);
    }else{
        icon=texture(Sampler0,atlasUv)*vec4(tint.rgb,1.0);
        vec2 p=localUv*2.0-1.0;
        if(work.y==2.0){
            float droplet=min(length(p-vec2(0.0,.20))-.48,max(abs(p.x)-.50*(p.y+.70),p.y-.20));
            icon.a*=1.0-smoothstep(0.0,.025,droplet);
        }else if(work.y==3.0){
            vec2 q=abs(p);float hexagon=max(dot(q,vec2(.8660254,.5)),q.y)-.64;
            icon.a*=1.0-smoothstep(0.0,.025,hexagon);
        }
    }
    if(icon.a<.05)discard;
    float scan=.78+.22*smoothstep(.15,.65,fract(localUv.y*10.0-work.x/6.2831853));
    float sweep=pow(max(0.0,cos(localUv.y*6.2831853-work.x)),12.0);
    vec3 color=mix(icon.rgb,vec3(.65,.93,1.0),.10+.12*sweep)*scan;
    fragColor=linear_fog(vec4(color,icon.a*tint.a)*ColorModulator,vertexDistance,FogStart,FogEnd,FogColor);
}
