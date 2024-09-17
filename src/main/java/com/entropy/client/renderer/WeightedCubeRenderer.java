package com.entropy.client.renderer;

import com.entropy.GatewayGunMod;
import com.entropy.entity.WeightedCube;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class WeightedCubeRenderer extends EntityRenderer<WeightedCube> {
    private final BlockRenderManager manager;

    public WeightedCubeRenderer(EntityRendererFactory.Context context){
        super(context);
        manager = context.getBlockRenderManager();
    }

    @Override
    public void render(WeightedCube entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.push();
        matrices.scale(GatewayGunMod.cubeSize, GatewayGunMod.cubeSize, GatewayGunMod.cubeSize);
        matrices.translate(-0.5,0,-0.5);
        manager.renderBlockAsEntity(entity.getDataTracker().get(WeightedCube.BLOCK), matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    @Override
    public Identifier getTexture(WeightedCube entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
