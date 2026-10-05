#version 150
#moj_import <fog.glsl>
uniform mat4 ProjMat;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform int FogShape;
in vec2 imagePoint;
flat in vec3 centerView;
flat in float radius;
flat in float phase;
out vec4 fragColor;
void main() {
    // A small tilt keeps the optical disk legible from every camera angle.
    vec2 p = mat2(0.987, -0.160, 0.160, 0.987) * imagePoint;
    float r = length(p);
    float aa = max(fwidth(r), 0.003);
    float core = 1.0 - smoothstep(1.0-aa, 1.0+aa, r);
    float er = length(vec2(p.x, (p.y+0.12)/0.29));
    float angle = atan((p.y+0.12)/0.29, p.x);
    float disk = smoothstep(1.04,1.16,er) * (1.0-smoothstep(2.25,2.85,er));
    float threads = 0.86+0.10*sin(er*32.0-angle*4.0-phase*4.0)+0.045*sin(er*71.0+angle*7.0+phase*6.0);
    float streaks = 0.90+0.10*sin(angle*5.0-er*6.0+phase*2.0);
    // Only the near half crosses in front of the event horizon.
    float front = 1.0-smoothstep(-0.16,-0.08,p.y);
    disk *= max(front, 1.0-core);
    float diskGlow = disk*clamp(threads*streaks,0.0,1.0);
    float photon = exp(-pow((r-1.055)/0.035,2.0))*(1.0-core);
    // Lensed image of the far side: a broad arch behind the core, with a much fainter lower echo.
    float archR = length(vec2(p.x,p.y*0.85));
    float arch = exp(-pow((archR-1.15)/0.11,2.0))*(1.0-core)*smoothstep(-0.05,0.35,p.y);
    arch *= 0.72+0.20*sin(archR*85.0-atan(p.y,p.x)*4.0+phase*3.0);
    float echo = exp(-pow((r-1.09)/0.07,2.0))*(1.0-core)*(1.0-smoothstep(-0.6,0.0,p.y))*.3;
    float halo = exp(-max(0.0,r-1.1)*5.0)*(1.0-core)*.12;
    float luminosity = clamp(diskGlow*1.22+photon*.38+arch*.9+echo,0.0,1.0);
    float alpha = max(core,max(disk*.95,clamp(photon+arch+echo+halo,0.0,1.0)));
    if(alpha<0.015)discard;
    vec3 gold = vec3(1.0,0.55,0.16);
    vec3 white = vec3(1.0,0.97,0.84);
    vec3 light = mix(gold,white,smoothstep(0.14,0.85,luminosity));
    vec3 color = light*(0.55+0.45*luminosity);
    color *= 1.0-core;
    color = mix(color,light*(.7+.3*luminosity),clamp(diskGlow*front*1.6,0.0,1.0));
    // Curved core depth prevents the billboard plane from clipping into nearby walls.
    float toward = core*sqrt(max(0.0,1.0-r*r));
    if(disk>0.05&&front>0.5)toward=max(toward,sqrt(max(0.0,er*er-p.x*p.x))*.82);
    vec3 surface = centerView + vec3(imagePoint*radius,toward*radius);
    vec4 clip = ProjMat * vec4(surface,1.0);
    if(clip.w<=0.0)discard;
    gl_FragDepth=clamp(clip.z/clip.w*.5+.5,0.0,1.0);
    float distance = fog_distance(surface,FogShape);
    fragColor = linear_fog(vec4(color,alpha)*ColorModulator,distance,FogStart,FogEnd,FogColor);
}
