#version 150

uniform float GameTime;
uniform vec3 ColorOne;
uniform vec3 ColorTwo;

in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

#define iTime GameTime*1200.0

float ripple(vec2 p){
	vec2 i = p;
	float c = 2.0;
	float inten = .05;

	for (int n = 0; n < 3; n++) {
		float t = iTime * (1.0 - (0.060 / float(n+1)));

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
    vec2 coord = texCoord0.rg * 3.0 + 0.5;
    
    float r = ripple(coord)+2.0;
    
    vec3 color1 = ColorOne;
    vec3 color2 = ColorTwo;
    
    vec3 color = mix(color1, color2, r);
	
	fragColor = vec4(color, 1.0);
}