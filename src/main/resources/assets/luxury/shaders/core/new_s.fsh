#version 130

#moj_import <luxury:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;
in float Time;

uniform vec2 Size;
uniform float u_time;
uniform vec4 u_mouse;
uniform sampler2D u_textures[16];
uniform float u_alpha_speed;
uniform float u_alpha_intensity;
uniform float u_base_alpha;

out vec4 OutColor;

// Константы
const float PI = 3.14159265358979323846;

// Функция вращения
vec2 rot(vec2 v, float a) {
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c) * v;
}

// Упрощённая версия psrdnoise для Minecraft
float psrdnoise(vec2 x, vec2 period, float alpha, out vec2 gradient) {
    // Простой шум для Minecraft
    float n = sin(x.x * 2.0 + cos(x.y * 3.0 + alpha)) * 0.5 + 0.5;
    gradient = vec2(cos(x.x), sin(x.y));
    return n;
}

// Упрощённая bounceIn функция
float bounceIn(float t) {
    return 1.0 - cos(t * PI * 0.5);
}

// Плавная пульсация
float smoothPulse(float time, float speed) {
    return (sin(time * speed) + 1.0) * 0.5;
}

// Волновая прозрачность
float waveAlpha(vec2 coord, float time) {
    float wave1 = sin(coord.x * 5.0 + time * 2.0) * 0.3;
    float wave2 = cos(coord.y * 3.0 + time * 1.5) * 0.2;
    float wave3 = sin(coord.x * 2.0 + coord.y * 2.0 + time * 0.7) * 0.25;
    return 0.5 + wave1 + wave2 + wave3;
}

// Градиентная прозрачность от центра
float radialAlpha(vec2 coord, vec2 center) {
    vec2 diff = coord - center;
    float dist = length(diff);
    return 1.0 - smoothstep(0.0, 0.7, dist);
}

// Динамическая прозрачность с несколькими эффектами
float calculateDynamicAlpha(vec2 st, float time) {
    float alpha = u_base_alpha;

    // 1. Пульсация по времени
    float pulse = smoothPulse(time, u_alpha_speed) * u_alpha_intensity;
    alpha *= (0.7 + pulse * 0.3);

    // 2. Волновая прозрачность
    float wave = waveAlpha(st, time) * 0.4 + 0.6;
    alpha *= wave;

    // 3. Радиальная прозрачность от центра
    float radial = radialAlpha(st, vec2(0.5, 0.5));
    alpha *= radial;

    // 4. Шумовая прозрачность для органичного вида
    vec2 gradient;
    float noise = psrdnoise(st * 2.0 + time * 0.3, vec2(0.0), 0.5, gradient);
    alpha *= (0.8 + noise * 0.2);

    return clamp(alpha, 0.0, 1.0);
}

void main() {
    // Преобразуем координаты
    vec2 st = FragCoord;
    st.x *= Size.x / Size.y;

    // Нормализованные координаты для эффектов
    vec2 normCoord = FragCoord;

    // Поворот
    st = rot(st, -PI / 8.0);

    // Генерируем шум для цвета
    vec2 gradient;
    float n = psrdnoise(vec2(3.0) * st, vec2(0.0), 1.2 * Time, gradient);

    // Создаём линии
    float lines = cos((st.x + n * 0.1 + 0.2) * PI);

    // Смешиваем цвета
    vec3 color1 = vec3(0.949, 0.561, 0.792);
    vec3 color2 = vec3(0.463, 0.169, 0.690);

    vec3 finalColor = mix(color1, color2, bounceIn(lines * 0.5 + 0.5));

    // Вычисляем динамическую прозрачность
    float dynamicAlpha = calculateDynamicAlpha(normCoord, Time);

    // Финальная альфа с учетом цвета вершины
    float finalAlpha = FragColor.a * dynamicAlpha;

    // Эффект исчезновения по краям
    float edgeFade = 1.0 - smoothstep(0.45, 0.5, length(normCoord - 0.5));
    finalAlpha *= edgeFade;

    // Если прозрачность слишком низкая, отбрасываем фрагмент
    if (finalAlpha < 0.01) {
        discard;
    }

    // Применяем цвет с прозрачностью
    OutColor = vec4(finalColor, finalAlpha);

    // Альфа-тест
    if (OutColor.a < 0.01) {
        discard;
    }
}