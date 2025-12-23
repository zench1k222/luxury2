#version 130

#moj_import <luxury:common.glsl>

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float u_time;
uniform float u_alpha_speed;
uniform float u_alpha_intensity;

out vec2 FragCoord;
out vec4 FragColor;
out float Time;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    FragColor = Color;
    Time = u_time;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}