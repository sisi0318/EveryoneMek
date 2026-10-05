// Original gas field shared by the physical disc and its bent optical image.
float gasHash(vec2 cell) {
    vec3 h=fract(vec3(cell.x,cell.y,cell.x+cell.y)*vec3(.1031,.0973,.1099));
    h+=dot(h,h.yzx+19.19);
    return fract((h.x+h.y)*h.z);
}
float gasNoise(vec2 p) {
    vec2 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(gasHash(cell),gasHash(cell+vec2(1,0)),f.x),mix(gasHash(cell+vec2(0,1)),gasHash(cell+vec2(1)),f.x),f.y);
}
vec3 gasLight(float azimuth,float radial,float time) {
    float r=clamp(radial,0.0,1.0);
    float angle=azimuth-time*(.55+.36/(.4+r));
    vec2 orbit=vec2(cos(angle),sin(angle));
    float cloud=gasNoise(orbit*(4.0+r*6.0)+vec2(r*7.0,-time*.09));
    float stream=smoothstep(.30,.76,cloud);
    float stripe=.5+.5*sin(r*51.0-angle*3.0+cloud*4.5+time*.8);
    float filament=smoothstep(.56,.94,stripe);
    float cover=smoothstep(0.0,.035,r)*(1.0-smoothstep(.64,1.0,r));
    float density=(.035+.70*stream+.65*filament*stream)*exp(-r*2.8);
    float inner=exp(-r*14.0)*.85;
    vec3 tint=mix(vec3(1.0,.46,.10),vec3(1.0,.995,.965),exp(-r*2.2));
    return tint*(density*1.35+inner)*cover;
}
