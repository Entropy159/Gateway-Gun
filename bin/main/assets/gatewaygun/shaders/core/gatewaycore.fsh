#version 150

uniform float GameTime;
uniform vec3 ColorOne;
uniform vec3 ColorTwo;

in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

#define timeMult 1200.0
#define PI 3.1415926535897932384626433832795

float rand(vec2 c){
    return fract(sin(dot(c.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

float noise(vec2 p, float freq ){
    float unit = 1.0/freq;
    vec2 ij = floor(p/unit);
    vec2 xy = mod(p,unit)/unit;
    //xy = 3.*xy*xy-2.*xy*xy*xy;
    xy = .5*(1.-cos(PI*xy));
    float a = rand((ij+vec2(0.,0.)));
    float b = rand((ij+vec2(1.,0.)));
    float c = rand((ij+vec2(0.,1.)));
    float d = rand((ij+vec2(1.,1.)));
    float x1 = mix(a, b, xy.x);
    float x2 = mix(c, d, xy.x);
    return mix(x1, x2, xy.y);
}

float pNoise(vec2 p, int res){
    float persistance = .5;
    float n = 0.5;
    float normK = 0.4;
    float f = 4.0;
    float amp = 0.5;
    int iCount = 0;
    for (int i = 0; i<50; i++){
        n+=amp*noise(p, f);
        f*=2.;
        normK+=amp;
        amp*=persistance;
        if (iCount == res) break;
        iCount++;
    }
    float nf = n/normK;
    return nf*nf*nf*nf;
}

float ripple(vec2 p){
	vec2 i = p;
	float c = 2.0;
	float inten = .05;

	for (int n = 0; n < 3; n++) {
		float t = GameTime * timeMult * (1.0 - (0.060 / float(n+1)));

		i = p + vec2(cos(t - i.x) + sin(t + i.y),
		sin(t - i.y) + cos(t + i.x));

		c += 1.0/length(vec2(p.x / (sin(i.x+t)/inten),
		p.y / (cos(i.y+t)/inten)));
	}

	c /= 3.0;
	c = 1.5 - sqrt(c);
	return c;
}

void main() {
    vec2 coord = texCoord0.rg * 3.0;
    
    float time = GameTime * timeMult;
    
    float r = ripple(coord)+3.0;
    
    vec3 color1 = ColorOne;
    vec3 color2 = ColorTwo;
    
    vec3 color = mix(color1, color2, r);
	
	fragColor = vec4(color, 1.0);
}