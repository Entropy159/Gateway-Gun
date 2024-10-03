package com.entropy.client.renderer;

import com.entropy.client.GatewayGunClient;
import com.entropy.client.renderer.models.GatewayOverlayModel;
import com.entropy.GatewayGunConfig;
import com.entropy.entity.Gateway;
import com.entropy.items.GatewayGun;
import com.entropy.misc.GatewayGunUtils;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import qouteall.imm_ptl.core.CHelper;
import qouteall.q_misc_util.my_util.DQuaternion;

import java.awt.*;

import static com.entropy.GatewayGunConstants.*;
import static com.entropy.GatewayGunMod.id;

public class GatewayEntityRenderer extends EntityRenderer<Gateway> {
    private final GatewayOverlayModel model;

    public GatewayEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        model = new GatewayOverlayModel(context.getPart(GatewayGunClient.OVERLAY_MODEL_LAYER));
    }

    @Override
    public void render(
            @NotNull Gateway entity, float yaw, float tickDelta,
            @NotNull MatrixStack matrices, @NotNull VertexConsumerProvider vertexConsumers, int light
    ) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        // don't render overlay from back side
        if (!entity.isInFrontOfPortal(CHelper.getCurrentCameraPos())) {
            return;
        }

        matrices.push();

        matrices.peek().getNormalMatrix().rotate(DQuaternion.rotationByDegrees(new Vec3d(1, 0, 0), -90).toMcQuaternion());

        matrices.peek().getPositionMatrix().rotate(entity.getOrientationRotation().toMcQuaternion());

        matrices.translate(0, 0, overlayOffset);

        float scale1 = (float) (entity.getWidth() / sizeMult);
        float scale2 = (float) (entity.getHeight() / sizeMult);
        float scale3 = 1;

        matrices.scale(scale1, scale2, scale3);

        Color color = new Color(GatewayGunUtils.hexToInt(entity.getColor()));

        float r = color.getRed() / 255F;
        float g = color.getGreen() / 255F;
        float b = color.getBlue() / 255F;
        float alpha = entity.isVisible() ? 0.1F : 1F;

        VertexConsumer consumer = vertexConsumers.getBuffer(GatewayShaders.gateway());
        if (GatewayGunConfig.get().staticGatewayRendering) {
            consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(getTexture(entity)));
            alpha = 1F;
        }

        this.model.render(
                matrices,
                consumer,
                LightmapTextureManager.pack(15, 15),
                OverlayTexture.DEFAULT_UV, r, g, b, alpha
        );

        matrices.scale(1 / scale1, 1 / scale2, 1 / scale3);

        matrices.pop();
    }

    @Override
    public @NotNull Identifier getTexture(@NotNull Gateway entity) {
        return entity.isVisible() ? id("textures/entity/gateway_open.png") : id("textures/entity/gateway_closed.png");
    }
}
