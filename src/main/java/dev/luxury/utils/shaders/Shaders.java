package dev.luxury.utils.shaders;

import lombok.Cleanup;
import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;

@SuppressWarnings({"UnusedReturnValue", "unused"})
@Getter
public class Shaders {
    private final int programID;
    private static final IShader vertex = new VertexGlsl();

    public static Shaders stencilShader;


    public static void loadShaders() {
        stencilShader = create(new StencilGlsl());
    }

    private Shaders(IShader fragmentShaderLoc, IShader vertexShaderLoc) {
        int program = glCreateProgram();
        int fragmentShaderID = createShader(new ByteArrayInputStream(fragmentShaderLoc.shader().getBytes()), GL_FRAGMENT_SHADER);
        GL20.glAttachShader(program, fragmentShaderID);
        int vertexShaderID = createShader(new ByteArrayInputStream(vertexShaderLoc.shader().getBytes()), GL_VERTEX_SHADER);
        GL20.glAttachShader(program, vertexShaderID);
        GL20.glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);
        if (status == 0) throw new IllegalStateException("Shader creation failed");
        this.programID = program;
    }

    public static Shaders create(IShader shader) {
        return new Shaders(shader, vertex);
    }

    public static Shaders create(IShader fragShader, IShader vertexShader) {
        return new Shaders(fragShader, vertexShader);
    }

    @SneakyThrows
    public String readInputStream(InputStream inputStream) {
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines().collect(Collectors.joining("\n"));
    }

    private int createShader(InputStream inputStream, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, readInputStream(inputStream));
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            System.out.println(glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile", shaderType));
        }
        return shader;
    }
    public void attach() {
        ARBShaderObjects.glUseProgramObjectARB(programID);
    }
    public void detach() {
        glUseProgram(0);
    }
    public void load() {
        glUseProgram(programID);
    }

    public void unload() {
        glUseProgram(0);
    }

    public int getUniform(String name) {
        return glGetUniformLocation(programID, name);
    }

    public Shaders setUniformf(String name, float... args) {
        int loc = glGetUniformLocation(programID, name);
        switch (args.length) {
            case 1 -> glUniform1f(loc, args[0]);
            case 2 -> glUniform2f(loc, args[0], args[1]);
            case 3 -> glUniform3f(loc, args[0], args[1], args[2]);
            case 4 -> glUniform4f(loc, args[0], args[1], args[2], args[3]);
        }
        return this;
    }
    public static void drawQuads(float x, float y, float width, float height) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + height);
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y + height);
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y);
        glEnd();
    }
    public Shaders setUniformi(String name, int... args) {
        int loc = glGetUniformLocation(programID, name);
        switch (args.length) {
            case 1 -> glUniform1i(loc, args[0]);
            case 2 -> glUniform2i(loc, args[0], args[1]);
            case 3 -> glUniform3i(loc, args[0], args[1], args[2]);
            case 4 -> glUniform4i(loc, args[0], args[1], args[2], args[3]);
        }
        return this;
    }

    public Shaders setMat4fv(String name, FloatBuffer matrix) {
        int loc = glGetUniformLocation(programID, name);
        glUniformMatrix4fv(loc, false, matrix);
        return this;
    }

    public Shaders setMat4fv(String name, float[] matrix) {
        int loc = glGetUniformLocation(programID, name);
        glUniformMatrix4fv(loc, false, matrix);
        return this;
    }

    public Shaders setMat4fv(String name, MatrixStack matrix) {
        setMat4fv(name, matrix.peek().getPositionMatrix());
        return this;
    }

    public Shaders setMat4fv(String name, Matrix4f matrix) {
        FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
        floatBuffer.put(matrix.m00()).put(matrix.m10()).put(matrix.m20()).put(matrix.m30());
        floatBuffer.put(matrix.m01()).put(matrix.m11()).put(matrix.m21()).put(matrix.m31());
        floatBuffer.put(matrix.m02()).put(matrix.m12()).put(matrix.m22()).put(matrix.m32());
        floatBuffer.put(matrix.m03()).put(matrix.m13()).put(matrix.m23()).put(matrix.m33());
        floatBuffer.flip();
        setMat4fv(name, floatBuffer);
        return this;
    }
}