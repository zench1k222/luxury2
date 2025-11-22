package dev.luxury.utils.shaders;


public class StencilGlsl implements IShader {
    @Override
    public String shader() {
        return """
                #version 120
                
                uniform sampler2D originalTexture;
                uniform sampler2D replaceTexture;
                uniform vec4 multiplier;
                uniform vec2 viewOffset;
                uniform vec2 resolution;
                
                void main() {
                    vec2 screenPos = gl_FragCoord.xy + viewOffset;
                    vec2 replacePos = screenPos / resolution;
                    vec4 sourceColor = texture2D(originalTexture, gl_TexCoord[0].xy);
                    vec4 replaceColor = texture2D(replaceTexture, replacePos);
                    gl_FragColor = replaceColor * multiplier * sourceColor.a;
                }
                """;
    }
}