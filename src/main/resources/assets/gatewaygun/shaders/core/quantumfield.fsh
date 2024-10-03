#version 150

uniform float Alpha;
uniform float GameTime;
uniform vec3 Pos;

in vec4 vertexColor;
in vec4 texCoord0;
in vec4 normal;

out vec4 fragColor;

#define iTime GameTime * 1200.0
#define PI 3.1415926535897932384626433832795

vec3 hash(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 367.6)), dot(p, vec3(269.5, 183.3, 739.6)), dot(p, vec3(577.1,683.7,257.3)));
    return fract(sin(p) * 43758.5453);
}

float noise(in vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);

    vec3 u = f * f * (3.0 - 2.0 * f);

    return mix(mix(dot(hash(i + vec3(0.0, 0.0, 0.0)), f - vec3(0.0, 0.0, 0.0)),
                   dot(hash(i + vec3(1.0, 0.0, 0.0)), f - vec3(1.0, 0.0, 0.0)), u.x),
               mix(dot(hash(i + vec3(0.0, 1.0, 0.0)), f - vec3(0.0, 1.0, 0.0)),
                   dot(hash(i + vec3(1.0, 1.0, 0.0)), f - vec3(1.0, 1.0, 0.0)), u.x), u.y);
}

float voronoi(in vec3 x)
{
    vec3 n = floor(x);
    vec3 f = fract(x);

    float F1 = 8.0;
    float F2 = 8.0;

    for (int j = -1; j <= 1; j++) {
        for (int i = -1; i <= 1; i++) {
            for (int h = -1; h <= 1; h++) {
                vec3 g = vec3(h, i, j);
                vec3 o = hash(n + g);

                o = 0.5 + 0.41 * sin(iTime + 6.2831 * o); // animate

                vec3 r = g - f + o;

                float d = dot(r,r);

                if (d < F1)
                {
                    F2 = F1;
                    F1 = d;
                }
                else if (d < F2)
                {
                    F2 = d;
                }
            }
        }
    }

    return F1*F1;
}

float fbm(in vec3 p, int octaves) {
    float s = 0.0;
    float m = 0.0;
    float a = 0.5;

    for (int i = 0; i < octaves; i++) {
        s += a * voronoi(p);
        m += a;
        a *= 0.5;
        p *= 2.0;
    }
    return s / m;
}

void main() {
    vec2 coord = vec2(texCoord0.r - 0.5, texCoord0.g) * 4;
    vec3 pos = vec3(coord, Pos.z);
    float n = noise(pos*16.0);
    pos += 0.05 * vec3(n,-n, n/2);
    float noise = fbm(pos, 3);
    vec4 col = vec4(0, noise, noise, noise * Alpha);
    fragColor = col;
}