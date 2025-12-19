#version 300 es

precision highp float;
precision highp sampler2D;

in vec2 uv;
out vec4 out_color;


uniform vec2 u_resolution;
uniform float u_time;
uniform vec4 u_mouse;
uniform sampler2D u_textures[16];


#include "https://raw.githubusercontent.com/stegu/psrdnoise/main/src/psrdnoise2.glsl"
#include <lygia/animation/easing/bounce>

#pragma region rotate

vec2 rot(vec2 v, float a){
    return mat2x2(cos(a), -sin(a), sin(a), cos(a)) * v;
}

#pragma endregion

void main(){
    vec2 st = uv * vec2(u_resolution.x / u_resolution.y, 1.);
    st = rot(st, -PI / 8.);

    vec2 mouse = u_mouse.xy / u_resolution;

    vec2 gradient;
    float n = psrdnoise(vec2(3.) * st, vec2(0.), 1.2 * u_time + mouse.y * PI, gradient);

    float lines = cos((st.x + n * 0.1 + mouse.x + 0.2) * PI);

    out_color = vec4(
        mix(
            vec3(0.949, 0.561, 0.792),
            vec3(0.463, 0.169, 0.690),
            bounceIn(lines * 0.5 + 0.5)
        ), 
        1.
    );
}