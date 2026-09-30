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
            float tip = smoothstep(0.0,.12,min(flow.x,1.0-flow.x));
            float current = .5+.5*sin(flow.x*42.0-phase*5.0);
            float edgeLight = 1.0-smoothstep(.035,.20,abs(flow.y-.5));
            light = mix(vec3(.06,.48,.25),vec3(.66,.96,.78),edgeLight*.8);
            light += vec3(.025,.12,.06)*current*(.35+.65*edgeLight);
            alpha *= tip*(.68+.32*edgeLight);
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
        if(material == 10) {
            vec2 p=flow*2.0-1.0;float h=max(abs(p.y),abs(p.x)*.866025+abs(p.y)*.5);
            float rim=(1.0-smoothstep(.025,.06,abs(h-.87)));
            float grid=1.0-smoothstep(.025,.06,min(abs(fract(flow.x*8.0)-.5),abs(fract(flow.y*8.0)-.5)));
            alpha=powerAlpha.y*(1.0-smoothstep(.88,.94,h))*(.025+.70*rim+.10*grid);
            light=mix(vec3(.08,.52,.32),vec3(.65,1.0,.80),powerAlpha.x*.6+rim*.3);
        } else if(material == 11) {
            float count=floor(powerAlpha.x*3.0+.5),slot=floor(flow.x*3.0);
            vec2 p=vec2(fract(flow.x*3.0)*2.0-1.0,flow.y*2.0-1.0);
            float d=abs(p.x)+abs(p.y);
            alpha=powerAlpha.y*(1.0-smoothstep(.08,.20,abs(d-.6)))*step(slot+.5,count);
            light=vec3(.16,.95,.55);
        } else if(material == 12) {
            float edge=min(min(flow.x,1.0-flow.x),min(flow.y,1.0-flow.y));
            float rim=1.0-smoothstep(.025,.065,edge),scan=.5+.5*sin(flow.y*60.0-phase*2.0);
            alpha=powerAlpha.y*(.10+.62*rim+.08*scan);light=mix(vec3(.055,.45,.27),vec3(.30,.95,.63),rim);
        }
        if(alpha < .004) discard;
    }
    fragColor = linear_fog(vec4(light, alpha) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
