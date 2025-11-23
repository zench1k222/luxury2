package dev.luxury.render.feature;

import dev.luxury.modules.api.ModuleManager;
import dev.luxury.modules.impl.CustomModels;
import dev.luxury.modules.impl.custommodel.ChinchillaModel;
import dev.luxury.modules.impl.custommodel.CrazyRabbitModel;
import dev.luxury.modules.impl.custommodel.CustomPlayerModel;
import dev.luxury.modules.impl.custommodel.DemonModel;
import dev.luxury.modules.impl.custommodel.FreddyBearModel;
import dev.luxury.modules.impl.custommodel.SonicModel;
import dev.luxury.utils.managers.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.render.OverlayTexture;

public class CustomModelFeature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private CustomPlayerModel cachedModel;
    private String cachedMode = "";

    public CustomModelFeature(PlayerEntityRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        CustomModels customModels = ModuleManager.getModule(CustomModels.class);
        if (customModels == null || !customModels.isEnabled()) return;

        boolean apply = shouldApplyCustomModel(state);
        if (!apply) return;

        String mode = customModels.type.get();
        if (!mode.equalsIgnoreCase(cachedMode) || cachedModel == null) {
            cachedMode = mode;
            cachedModel = bakeModel(mode);
        }
        if (cachedModel == null) return;

        PlayerEntityModel ctxModel = this.getContextModel();

        cachedModel.copyRotations(ctxModel.head, ctxModel.body, ctxModel.rightArm, ctxModel.leftArm, ctxModel.rightLeg, ctxModel.leftLeg);

        Identifier tex = modelTexture(mode);
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));

        matrices.push();
        cachedModel.render(matrices, vc, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    private boolean shouldApplyCustomModel(PlayerEntityRenderState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;

        String playerName = getPlayerNameFromState(state);

        if (playerName == null) {
            return isLocalPlayer(state);
        }

        String localPlayerName = mc.player.getName().getString();
        return playerName.equalsIgnoreCase(localPlayerName) ||
                FriendManager.getInstance().isFriend(playerName);
    }

    private String getPlayerNameFromState(PlayerEntityRenderState state) {
        try {
            if (state.name != null) {
                return state.name;
            }
        } catch (Exception e) {
        }
        return null;
    }

    private boolean isLocalPlayer(PlayerEntityRenderState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        return false;
    }

    private CustomPlayerModel bakeModel(String mode) {
        if ("Crazy Rabbit".equalsIgnoreCase(mode)) {
            ModelPart root = CrazyRabbitModel.getTexturedModelData().createModel();
            ModelPart head = root.getChild("head");
            ModelPart body = root.getChild("body");
            ModelPart rightArm = root.getChild("right_arm");
            ModelPart leftArm = root.getChild("left_arm");
            ModelPart rightLeg = root.getChild("right_leg");
            ModelPart leftLeg = root.getChild("left_leg");
            return new CrazyRabbitModel(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        }
        if ("White Demon".equalsIgnoreCase(mode) || "Red Demon".equalsIgnoreCase(mode)) {
            ModelPart root = DemonModel.getTexturedModelData().createModel();
            ModelPart head = root.getChild("head");
            ModelPart body = root.getChild("body");
            ModelPart rightArm = root.getChild("right_arm");
            ModelPart leftArm = root.getChild("left_arm");
            ModelPart rightLeg = root.getChild("right_leg");
            ModelPart leftLeg = root.getChild("left_leg");
            return new DemonModel(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        }
        if ("Freddy Bear".equalsIgnoreCase(mode)) {
            ModelPart root = FreddyBearModel.getTexturedModelData().createModel();
            ModelPart head = root.getChild("body_root").getChild("head");
            ModelPart body = root.getChild("body_root").getChild("torso");
            ModelPart rightArm = root.getChild("body_root").getChild("right_arm");
            ModelPart leftArm = root.getChild("body_root").getChild("left_arm");
            ModelPart rightLeg = root.getChild("body_root").getChild("right_leg");
            ModelPart leftLeg = root.getChild("body_root").getChild("left_leg");
            return new FreddyBearModel(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        }
        if ("Sonic".equalsIgnoreCase(mode)) {
            ModelPart root = SonicModel.getTexturedModelData().createModel();
            ModelPart head = root.getChild("head");
            ModelPart body = root.getChild("body");
            ModelPart rightArm = root.getChild("right_arm");
            ModelPart leftArm = root.getChild("left_arm");
            ModelPart rightLeg = root.getChild("right_leg");
            ModelPart leftLeg = root.getChild("left_leg");
            return new SonicModel(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        }
        if ("Chinchilla".equalsIgnoreCase(mode)) {
            ModelPart root = ChinchillaModel.getTexturedModelData().createModel();
            ModelPart head = root.getChild("head");
            ModelPart body = root.getChild("body");
            ModelPart rightArm = root.getChild("right_arm");
            ModelPart leftArm = root.getChild("left_arm");
            ModelPart rightLeg = root.getChild("right_leg");
            ModelPart leftLeg = root.getChild("left_leg");
            return new ChinchillaModel(root, head, body, rightArm, leftArm, rightLeg, leftLeg);
        }
        return null;
    }

    private Identifier modelTexture(String mode) {
        if ("Crazy Rabbit".equalsIgnoreCase(mode)) return Identifier.of("luxury", "textures/models/rabbit.png");
        if ("Red Demon".equalsIgnoreCase(mode)) return Identifier.of("luxury", "textures/models/reddemon.png");
        if ("Freddy Bear".equalsIgnoreCase(mode)) return Identifier.of("luxury", "textures/models/freddy.png");
        if ("White Demon".equalsIgnoreCase(mode)) return Identifier.of("luxury", "textures/models/whitedemon.png");
        if ("Sonic".equalsIgnoreCase(mode)) return Identifier.of("luxury", "textures/models/sonic.png");
        if ("Chinchilla".equalsIgnoreCase(mode)) return Identifier.of("luxury", "textures/models/chinchilla.png");
        return Identifier.of("textures/entity/steve.png");
    }
}