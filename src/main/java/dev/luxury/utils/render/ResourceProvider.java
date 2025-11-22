package dev.luxury.utils.render;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class ResourceProvider {
    public static final ShaderProgramKey RECTANGLE_SHADER_KEY;;
    public static final ShaderProgramKey RECTANGLE_BLUR_SHADER_KEY;
    public static final ShaderProgramKey BORDER_SHADER_KEY;
     public static final ShaderProgramKey TEXTURE_SHADER_KEY;
    public static final ShaderProgramKey LIQUID_GLASS_SHADER_KEY;
    static {
        try {
            Identifier shaderId = getShaderIdentifier("rectangle");
            Identifier shaderId3 = getShaderIdentifier("blur");
            Identifier shaderId2 = getShaderIdentifier("border");
            Identifier shaderId4 = getShaderIdentifier("texture");
            Identifier shaderId5 = getShaderIdentifier("liquidglass");
            System.out.println("Trying to load shader: " + shaderId);
            RECTANGLE_SHADER_KEY = new ShaderProgramKey(shaderId, VertexFormats.POSITION_COLOR, Defines.EMPTY);
            RECTANGLE_BLUR_SHADER_KEY = new ShaderProgramKey(shaderId3, VertexFormats.POSITION_COLOR, Defines.EMPTY);
            BORDER_SHADER_KEY = new ShaderProgramKey(shaderId2,VertexFormats.POSITION_COLOR,Defines.EMPTY);
            TEXTURE_SHADER_KEY = new ShaderProgramKey(shaderId4,VertexFormats.POSITION_TEXTURE_COLOR,Defines.EMPTY);
            LIQUID_GLASS_SHADER_KEY = new ShaderProgramKey(shaderId5, VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
            System.out.println("Shader key created successfully");
        } catch (Exception e) {
            System.err.println("Failed to create shader key: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private static Identifier getShaderIdentifier(String name) {
        return Identifier.of("luxury", "core/" + name);
    }
}