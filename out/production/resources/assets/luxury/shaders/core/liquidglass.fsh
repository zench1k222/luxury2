#version 130

#moj_import <luxury:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float Time;
uniform float BlurRadius;
uniform float DistortionStrength;
uniform float GlowSize;

out vec4 OutColor;

const float PI = 3.14159265359;

vec3 liquidBlur(sampler2D tex, vec2 uv, float radiusPx, int samples, vec2 size) {
    vec3 col = texture(tex, uv).rgb;
    vec2 center = size * 0.5;
    vec2 dir = uv * size - center;

    for (int i = 1; i <= samples; ++i) {
        float f = float(i)/float(samples);
        vec2 offset = normalize(dir) * f * radiusPx;
        col += texture(tex, uv + offset/size).rgb;
    }

    return col / (float(samples)+1.0);
}

vec2 radialDistort(vec2 fragCoord, vec2 size, vec2 uv, float strength, float time) {
    vec2 center = size * 0.5;
    vec2 dir = fragCoord - center;
    float dist = length(dir);
    if (dist < 0.001) return uv;

    float wave = (sin(dist * 0.06 - time * 2.0) * 0.5 + 0.5);
    float att = 1.0 - smoothstep(0.0, min(size.x,size.y)*0.5, dist);
    float offsetPx = wave * strength * att;
    vec2 dirNorm = normalize(dir);

    return uv + dirNorm * (offsetPx / size.x);
}

void main() {
    vec2 uvLocal = FragCoord / Size;
    float shapeAlpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec2 baseUV = TexCoord;

    // Glow снаружи формы
    float glow = pow(clamp((1.0 - shapeAlpha) * (GlowSize / 10.0), 0.0, 1.0), 1.8);

    // Искажение внутри квадрата
    vec2 distortedUV = radialDistort(FragCoord, Size, baseUV, DistortionStrength, Time);

    // Blur внутри квадрата
    vec3 blurred = liquidBlur(Sampler0, distortedUV, BlurRadius, 10, Size);

    vec3 base = texture(Sampler0, baseUV).rgb;
    vec3 glassCol = mix(base, blurred, 1.0);
    glassCol = mix(glassCol, vec3(1.0), 0.06);

    // смешиваем: внутри формы — glassCol, снаружи — base + glow
    vec3 outRgb = mix(base + vec3(glow*0.8), glassCol, shapeAlpha);
    float outA = clamp(shapeAlpha * FragColor.a, 0.0, 1.0);

    if (outA < 0.001 && glow < 0.01) discard;
    OutColor = vec4(outRgb, outA);
}
