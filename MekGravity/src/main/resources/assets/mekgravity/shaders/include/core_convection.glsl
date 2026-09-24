// Integer lattice hashing: no texture reads, trigonometric hashes, or unbounded loops.
float lattice(ivec3 p) {
    uint h = uint(p.x) * 1597334677u ^ uint(p.y) * 3812015801u ^ uint(p.z) * 2798796415u;
    h = (h ^ (h >> 16u)) * 2246822519u;
    h = (h ^ (h >> 13u)) * 3266489917u;
    return float((h ^ (h >> 16u)) & 65535u) / 65535.0;
}

float convection(vec3 p) {
    ivec3 base = ivec3(floor(p));
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec4 lower = vec4(lattice(base), lattice(base + ivec3(1,0,0)), lattice(base + ivec3(0,1,0)), lattice(base + ivec3(1,1,0)));
    vec4 upper = vec4(lattice(base + ivec3(0,0,1)), lattice(base + ivec3(1,0,1)), lattice(base + ivec3(0,1,1)), lattice(base + ivec3(1,1,1)));
    vec4 layer = mix(lower, upper, f.z);
    return mix(mix(layer.x, layer.y, f.x), mix(layer.z, layer.w, f.x), f.y);
}
