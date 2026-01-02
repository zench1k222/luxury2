package dev.luxury.render.feature;


import dev.luxury.Luxury;
import dev.luxury.modules.impl.render.SantaHat;
import dev.luxury.modules.impl.other.santahat.SantaHatModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SantaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private final SantaHatModel hatModel;
    private static final Identifier TEXTURE = Identifier.of("luxury", "textures/santa.png");

    public SantaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
        this.hatModel = new SantaHatModel();
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (!isModuleEnabled()) {
            return;
        }

        matrices.push();
        PlayerEntityModel model = getContextModel();
        model.head.rotate(matrices);
        matrices.scale(1.0F, -1.0F, 1.0F);
        hatModel.render(matrices, vertexConsumers, light, TEXTURE);
        matrices.pop();
    }

    private boolean isModuleEnabled() {
        try {
            Luxury luxury = Luxury.getInstance();
            if (luxury != null && luxury.getModuleManager() != null) {
                SantaHat module = luxury.getModuleManager().getModule(SantaHat.class);
                return module != null && module.isEnabled();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}