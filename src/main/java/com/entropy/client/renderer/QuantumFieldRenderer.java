package com.entropy.client.renderer;

import com.entropy.GatewayGunConfig;
import com.entropy.blocks.quantumfield.QuantumField;
import com.entropy.blocks.quantumfield.QuantumFieldBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class QuantumFieldRenderer implements BlockEntityRenderer<QuantumFieldBlockEntity> {
    public final BlockRenderManager manager;

    public QuantumFieldRenderer(BlockEntityRendererFactory.Context ctx) {
        manager = ctx.getRenderManager();
    }

    @Override
    public void render(QuantumFieldBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (entity.getWorld() == null) return;
        World world = entity.getWorld();
        BlockPos pos = entity.getPos();
        if (!(world.getBlockState(pos).getBlock() instanceof QuantumField)) return;
        matrices.push();
        BlockState state = world.getBlockState(pos);
        Uniform alpha = GatewayShaders.quantumFieldShader.getUniform("Alpha");
        if (alpha != null) {
            boolean enabled = state.get(Properties.ENABLED);
            alpha.set(enabled ? 1F : 0.1F);
        }
        Uniform posUniform = GatewayShaders.quantumFieldShader.getUniform("Pos");
        if (posUniform != null) {
            posUniform.set((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
        }
        RenderLayer layer = GatewayShaders.quantumField();
        if (GatewayGunConfig.get().staticQuantumFieldRendering) layer = RenderLayer.getTranslucent();
        manager.renderBlock(state, pos, world, matrices, vertexConsumers.getBuffer(layer), true, Random.create());
        matrices.pop();
    }
}
