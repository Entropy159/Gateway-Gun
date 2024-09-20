#version 150

uniform float GameTime;

in vec4 vertexColor;
in vec4 texCoord0;

out vec4 fragColor;

#define PI 3.1415926535897932384626433832795
#define closeAlpha vertexColor.a
#define timeMult 1200.0

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

vec3 rgbToHsv(vec3 c){
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsvToRgb(float hue, float saturation, float value) {
    float h = float(int(hue * 6.0));
    float f = hue * 6.0 - h;
    float p = value * (1.0 - saturation);
    float q = value * (1.0 - f * saturation);
    float t = value * (1.0 - (1.0 - f) * saturation);

    if(h == 0.0) return vec3(value, t, p);
    if(h == 1.0) return vec3(q, value, p);
    if(h == 2.0) return vec3(p, value, t);
    if(h == 3.0) return vec3(p, q, value);
    if(h == 4.0) return vec3(t, p, value);
    if(h == 5.0) return vec3(value, p, q);
    return vec3(1.0);
}

void main() {
	vec2 coord = vec2((texCoord0.r-0.5)*2, texCoord0.g-0.25);

	float diameters[6];
	diameters[0] = 0.26;//0.165;
	diameters[1] = 0.27;//0.1725;
	diameters[2] = 0.46;//0.2;
	diameters[3] = 0.46;//0.205;
	diameters[4] = 0.51;//0.2375;
	diameters[5] = 0.48;//0.2025;
	float timeMulInner = 2.5;

	vec3 theColor = vertexColor.rgb;

	vec2 sp = vec2(coord.x-0.5, (coord.y*2)-0.5);
	//vec2 sp = (coord - 0.5) / vec2(w, h);

	float realTime = GameTime * timeMult;
	int tint = int(realTime);
	float tfl = realTime - float(tint);
	int tintf = int(realTime * timeMulInner);
	float tflf = realTime * timeMulInner - float(tintf);

	float d1 = sqrt(dot(sp, sp)) * 5.0 + realTime * -0.015;
	float c1 = cos(d1), s1 = sin(d1);
	vec2 p = vec2(sp.x * c1 - sp.y * s1, sp.x * s1 + sp.y * c1);

	float d2 = sqrt(dot(sp, sp)) * 25.0 + realTime * -0.15;
	float c2 = cos(d2), s2 = sin(d2);
	vec2 p2 = vec2(sp.x * c2 - sp.y * s2, sp.x * s2 + sp.y * c2);

	float d = sqrt(dot(p, p));

	float rainbowTime = realTime * 0.5;
	float dist = 75.0;
	float off = float(int(p.y * dist)) * PI * 0.005;

	vec4 color = vec4(0.7, 0.7, 0.7, 0.0);
	vec4 portalColor = vec4(theColor, 1.0);
	if(theColor.r < 0.0){
		portalColor = vec4(hsvToRgb(mod(rainbowTime / 4.0 + p.y + off, 1.0), 1.0, 1.0), 1.0);
		//vec4(0.996, 0.788, 0.157, 1.0);//vec4(0.992, 0.4, 0.0, 1.0);//vec4(0.0, 0.471, 1.0, 1.0);//vec4(0.184, 0.706, 0.357, 1.0);
	}
	vec4 insideColor = vec4(0.0, 0.0, 0.15 * (1.0 - d * 2.0), 0.0);
	vec4 shadowColor = vec4(0.0, 0.0, 0.0, 0.15);

	float noise1 = 10.0;
	float noise2 = 25.0;

	// First pass
	if(closeAlpha > 0.0){
		// Inside
		if(d < 0.2){
			color = insideColor;
		}

		// Coloured inner edge
		float m1 = mix(pNoise(p * noise1 + 10.0 * mod(float(tintf), 10.0), 4), pNoise(p * noise1 + 10.0 * mod(float(tintf) + 1.0, 10.0), 4), tflf) * 0.5 + 0.25;
		float m2 = mix(pNoise(p * noise2 + 10.0 * mod(float(tintf), 10.0), 4), pNoise(p * noise2 + 10.0 * mod(float(tintf) + 1.0, 10.0), 4), tflf);
		float m = mix(m1, m2, 0.5);
		float n1 = mix(pNoise(p * noise1 / 8.0 + 10.0 * mod(float(tint), 10.0), 4), pNoise(p * noise1 / 8.0 + 10.0 * mod(float(tint) + 1.0, 10.0), 4), tfl) * 0.5 + 0.25;
		float n2 = mix(pNoise(p * noise2 / 8.0 + 10.0 * mod(float(tint), 10.0), 4), pNoise(p * noise2 / 8.0 + 10.0 * mod(float(tint) + 1.0, 10.0), 4), tfl);
		float n = mix(n1, n2, 0.5);
		if(closeAlpha > 0.0 && d < diameters[2]){
			vec3 hsv = rgbToHsv(theColor);
			hsv.z *= 1.0-coord.y*coord.y;
			hsv.z *= 1.0-coord.y*coord.y;
			hsv.z *= 1.125;
			hsv.y += coord.y*coord.y*coord.y;
			float r = 0.0;
			const int amt = 10;
			for(int i = 1; i < amt; i++){
				r += sin(ripple(coord * 41.5 * float(i*i*i))*10.0)*0.5;
				r += sin(ripple(coord * 73.2 * float(i*i*i))*10.0)*0.75;
			}
			hsv.z *= (n/(n*0.15+0.85) + 0.375);
			hsv.z += ((r/4.0)*(r/4.0)*0.0625*0.375);
			hsv.z *= 1.25;
			color = insideColor = vec4(hsvToRgb(hsv.x, hsv.y, hsv.z), closeAlpha);
		}
		if((d > diameters[0] && d < diameters[2])){
			float intD = (d - diameters[0]);
			intD /= (diameters[2] - diameters[1]);
			float alpha = ((intD + 1.0) * (intD + 1.0) * (intD + 1.0) - 1.0) / 7.0;
			alpha *= 0.5 + m * (1.0 + (1.0 - intD) * 0.5);
			if(alpha > 0.0){
				float n = 1.0 - sp.y * 3.0 - 1.0;
				if(n < 0.0) n = 0.0;
				//color = mix(color, portalColor, alpha * (1.125 + 1.25 * n));
				color = mix(color, mix(mix(vec4(0.0, 0.0, 0.0, 1.0), portalColor, 0.85), portalColor, (-sp.y + 0.5)), alpha * (1.125 + 1.25 * n));
			}
		}

		// Shadow
		if(d > diameters[2] && d < diameters[3]){
			float intD = (d - diameters[2]) / (diameters[3] - diameters[2]);
			float alpha = 1.0 - ((intD + 1.0) * (intD + 1.0) * (intD + 1.0) - 1.0) / 7.0;
			color = mix(color, mix(color, shadowColor, shadowColor.a), alpha);
		}

		// Coloured outside ring
		m = mix(pNoise(p * 10.0 + 15.0 * mod(float(tint), 10.0), 4), pNoise(p * 10.0 + 15.0 * mod(float(tint) + 1.0, 10.0), 10), sin(tfl * PI / 2.0));
		if((d > diameters[2] && d < diameters[5])){
			float intD = 1.0 - (d - diameters[2]) / (diameters[5] - diameters[2]);
			float alpha = ((intD + 1.0) * (intD + 1.0) * (intD + 1.0) - 1.0) / 7.0;
			alpha *= 1.0 + m * 2.0;
			if(alpha > 0.0){
				float n = 1.0 - sp.y * 5.0 - 1.0;
				if(n < 0.0) n = 0.0;
				color = mix(color, portalColor * 1.1, alpha * (1.125 + 1.25 * n));
			}
		}
	}
		// Coloured outer spinny thing
		float mm = mix(pNoise(p2 * 10.0 + 15.0 * mod(float(tint), 10.0), 4), pNoise(p2 * 10.0 + 15.0 * mod(float(tint) + 1.0, 10.0), 10), sin(tfl * PI / 2.0));
		if((d > diameters[2] && d < diameters[4])){
			float intD = 1.0 - (d - diameters[2]) / (diameters[4] - diameters[2]);
			float alpha = ((intD + 1.0) * (intD + 1.0) * (intD + 1.0) - 1.0) / 7.0;
			alpha *= 0.5 + mm * 2.0;
			color = mix(color, mix(insideColor, portalColor, d * 5.0), alpha * 0.5);
		}

    //vec3 color2 = rgbToHsv(vec3(color.r, color.g, color.b));
    //color = vec4(hsvToRgb(color2.x, 0.0, color2.z), color.a);
	
	fragColor = color;
}