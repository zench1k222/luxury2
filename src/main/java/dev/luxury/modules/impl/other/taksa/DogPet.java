package dev.luxury.modules.impl.other.taksa;

import dev.luxury.events.impl.client.EventRender3D;
import dev.luxury.events.impl.client.EventTick;
import dev.luxury.events.impl.eventapi.EventTarget;
import dev.luxury.modules.api.Category;
import dev.luxury.modules.api.Module;
import dev.luxury.modules.api.ModuleAnnotation;
import dev.luxury.modules.api.settings.ModeSetting;
import dev.luxury.utils.render.RenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@ModuleAnnotation(
        name = "DogPet",
        desc = "3D питомец собака",
        category = Category.Render
)
public class DogPet extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Identifier TAKSA_TEXTURE = Identifier.of("luxury", "textures/models/taksa.png");
    private static final Identifier DJEK_TEXTURE = Identifier.of("luxury", "textures/models/djekrussel.png");

    final ModeSetting dog = new ModeSetting("Модель", "Такса",new String[]{"Такса", "Джек"});
    
    private TaksaModel model;
    private TaksaBrain brain;
    
    public DogPet() {
        addSettings(dog);
        
        model = new TaksaModel(TaksaModel.getTexturedModelData().createModel());
        brain = new TaksaBrain();
        brain.setEntity(mc.player);
    }
    
    @EventTarget
    public void onTick(EventTick event) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        
        brain.setEntity(mc.player);
        brain.onEvent(event);
        
        TaksaScheduler.onEvent(event);
    }
    
    @EventTarget
    public void onRender(EventRender3D e) {
        if (!isEnabled() || brain == null || model == null || mc.player == null || mc.world == null) return;
        Vec3d dogPos = brain.getPos();
        Vec3d cameraPos = RenderHelper.cameraPos();
        MatrixStack ms = e.getMatrices();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        
        Identifier texture = dog.is("Такса") ? TAKSA_TEXTURE : DJEK_TEXTURE;
        
        ms.push();
        ms.translate(dogPos.subtract(cameraPos));
        
        model.setRotationAngles(mc.player.age, brain);
        
        RenderLayer renderLayer = RenderLayer.getEntityCutoutNoCull(texture);
        VertexConsumer buffer = immediate.getBuffer(renderLayer);
        
        model.render(ms, buffer, 15728880, OverlayTexture.DEFAULT_UV, brain, texture);

        immediate.draw();
        ms.pop();
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        if (brain != null) {
            brain.setEntity(mc.player);
        }
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
    }
}

