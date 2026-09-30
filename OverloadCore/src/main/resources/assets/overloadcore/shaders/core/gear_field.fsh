#version 150
#moj_import <fog.glsl>
in vec2 flow;
in vec2 powerAlpha;
in float vertexDistance;
in float phase;
flat in int material;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
out vec4 fragColor;
void main() {
    vec3 tint = vec3(0.16, 0.90, 0.62);
    float wave = 0.5 + 0.5 * sin(flow.x * 27.0 - phase);
    float filaments = smoothstep(0.80, 0.99, wave);
    float power = 0.08 + 0.92 * powerAlpha.x;
    vec3 light = mix(tint * (0.45 + wave * 0.45), vec3(0.9, 1.0, 1.0), filaments * 0.65) * power;
    float alpha = powerAlpha.y;
    if (material >= 2) {
        float edge = abs(flow.y * 2.0 - 1.0);
        float aa = max(fwidth(edge), 0.015);
        float band = 1.0 - smoothstep(0.68-aa, 1.0, edge);
        float core = 1.0 - smoothstep(0.05, 0.30+aa, edge);
        light = mix(tint, vec3(0.87,1.0,0.96), core);
        alpha *= band;
        if (material == 3) {
            float taper = sin(clamp(flow.x,0.0,1.0) * 3.1415927);
            float leading = 1.0-smoothstep(.035,.16,abs(flow.y-.72));
            float current = .5+.5*sin(flow.x*35.0-phase*3.0);
            light = mix(vec3(.10,.55,.33),vec3(.38,.90,.62),leading);
            light += vec3(.025,.10,.055)*current*leading;
            alpha *= taper*(.60+.40*leading);
        } else if (material == 4 || material == 7) {
            float arc = abs(flow.y-.5-.22*sin(flow.x*32.0+phase*3.0));
            float filament = 1.0-smoothstep(.02,.06+fwidth(arc),arc);
            light = mix(tint*.4,vec3(.93,1.0,1.0),filament);
            alpha *= .22+.78*filament;
            if(material == 7) {
                alpha *= 1.0-smoothstep(powerAlpha.x-.02,powerAlpha.x+.04,flow.x);
                light = mix(tint*.65,vec3(.8,1.0,.9),filament*.7+core*.3);
            }
        } else if (material == 5) {
            float pulse = .5+.5*sin(flow.x*18.84956-phase*2.0);
            alpha *= .45+.55*pulse;
            light = mix(vec3(.025,.36,.22),vec3(.32,.88,.62),core*(.6+.4*pulse));
        } else if (material == 6) {
            vec2 p = flow*2.0-1.0;
            float r = length(p);
            float spark = pow(max(0.0,1.0-abs(p.x*p.y)*22.0),3.0);
            alpha = powerAlpha.y*(1.0-smoothstep(.08,1.0,r))*(.3+.7*spark);
            light = mix(vec3(1.0,.44,.07),vec3(1.0,.97,.8),1.0-smoothstep(0.0,.4,r));
        } else if (material == 8) {
            // Dense rail pulse with a dark center and a teal induction band.
            light = mix(vec3(.09,.14,.15),vec3(.46,.62,.60),powerAlpha.x);
            float collar = 1.0-smoothstep(.05,.12,abs(flow.x-.25));
            light = mix(light,vec3(.18,.78,.48),collar*.8);
            alpha = 1.0;
        } else if (material == 9) {
            float crack = 1.0-smoothstep(.03,.10+fwidth(flow.y),abs(flow.y-.5-.10*sin(flow.x*28.0-phase*2.0)));
            light = mix(vec3(.10,.45,.28),vec3(.30,.95,.57),core*.35+crack*.65);
            alpha *= sin(clamp(flow.x,0.0,1.0)*3.1415927)*(.55+.45*crack);
        }
        if(alpha < .004) discard;
    }
    fragColor = linear_fog(vec4(light, alpha) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
