package com.entropy.client.renderer;

import com.entropy.GatewayGunMod;
import com.entropy.blocks.cubedispenser.CubeDispenserBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

import static com.entropy.GatewayGunConstants.*;

public class CubeDispenserRenderer implements BlockEntityRenderer<CubeDispenserBlockEntity> {
    public final BlockRenderManager manager;

    public CubeDispenserRenderer(BlockEntityRendererFactory.Context ctx) {
        manager = ctx.getRenderManager();
    }

    @Override
    public void render(CubeDispenserBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        double scale = 0.5-dispenserRenderSize/2;
        manager.renderBlockAsEntity(GatewayGunMod.CUBE_DISPENSER.getDefaultState(), matrices, vertexConsumers, light, overlay);
        matrices.translate(scale, scale, scale);
        matrices.scale(dispenserRenderSize, dispenserRenderSize, dispenserRenderSize);
        matrices.multiply(new Quaternionf().rotateLocalY((entity.getWorld().getTime()+tickDelta)/30F), 0.5F, 0.5F, 0.5F);
        manager.renderBlockAsEntity(entity.state, matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }
}
