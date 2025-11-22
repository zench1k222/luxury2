package dev.luxury.utils.render;


import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@UtilityClass
public class StencilUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private void recreate(final Framebuffer framebuffer) {
        final int depthBuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_STENCIL, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, depthBuffer);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthBuffer);
    }

    public void checkSetupFBO(final Framebuffer framebuffer) {
        if (framebuffer != null) {
            recreate(framebuffer);
        }
    }

    public void enable(boolean bind) {
        mc.getFramebuffer().beginWrite(false);
        checkSetupFBO(mc.getFramebuffer());
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        if (bind) bind();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void enable() {
        enable(true);
    }

    public void bind() {
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 1);
        GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);
    }

    public void read(final int ref) {
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, ref, 1);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
    }

    public void disable() {
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.disableBlend();
    }

    @Getter
    @AllArgsConstructor
    public enum Action {
        INSIDE(0),
        OUTSIDE(1);
        private final int action;
    }

}