#version 130

#moj_import <luxury:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size; // rectangle size
uniform vec4 Radius; // radius for each vertex
uniform float Thickness; // border thickness
uniform vec2 Smoothness; // internal and external edge smoothness
uniform float Shadow; // новый uniform для тени

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float dist;

    if (Shadow > 0.5) {
        // Режим тени: добавляем смещение и увеличиваем размытие
        vec2 shadowOffset = vec2(4.0, 4.0); // Смещение тени (можно настроить)
        dist = rdist(center - ((FragCoord.xy * Size) + shadowOffset), center - 1.0, Radius);
        float shadowBlur = Smoothness.x * 2.0; // Увеличиваем размытие для тени
        float alpha = smoothstep(1.0 - Thickness - shadowBlur, 1.0 - Thickness, dist);
        alpha *= 1.0 - smoothstep(1.0 - shadowBlur, 1.0, dist);
        vec4 shadowColor = vec4(0.0, 0.0, 0.0, 0.5 * alpha); // Полупрозрачная черная тень
        OutColor = shadowColor;
    } else {
        // Обычный режим бордера
        dist = rdist(center - (FragCoord.xy * Size), center - 1.0, Radius);
        float alpha = smoothstep(1.0 - Thickness - Smoothness.x - Smoothness.y,
            1.0 - Thickness - Smoothness.y, dist); // internal edge
        alpha *= 1.0 - smoothstep(1.0 - Smoothness.y, 1.0, dist); // external edge
        vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);
        OutColor = color;
    }

    if (OutColor.a == 0.0) { // alpha test
        discard;
    }
}