package com.entropy.client.renderer.models;

import com.entropy.entity.Gateway;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;

public class GatewayOverlayModel extends EntityModel<Gateway> {
    private final ModelPart base;

    public GatewayOverlayModel(ModelPart root) {
        this.base = root.getChild("base");
    }

    public static ModelData getModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData base = modelPartData.addChild("base", ModelPartBuilder.create(), ModelTransform.of(0.0F, 8, 0.0F, 0.0F, 0.0F, 0.0F));

        base.addChild("cube_r1", ModelPartBuilder.create().uv(0,8).cuboid(-8.0F, -8.0F, 0.0F, 16.0F, 16.0F, 0.0F, new Dilation(0.0F)).mirrored(), ModelTransform.of(0.0F, -8.0F, 0.0F, 0.0F, (float) Math.PI, 0.0F));
        return modelData;
    }

    public static TexturedModelData getTexturedModelData() {
        return TexturedModelData.of(getModelData(), 32, 32);
    }

    @Override
    public void setAngles(@NotNull Gateway entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void render(@NotNull MatrixStack matrixStack, @NotNull VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        base.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}