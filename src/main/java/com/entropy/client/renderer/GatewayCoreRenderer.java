package com.entropy.client.renderer;

import java.awt.Color;

import com.entropy.CoreData;
import com.entropy.GatewayGunMod;
import com.entropy.GatewayRecord;
import com.entropy.items.GatewayCore;

import com.entropy.misc.GatewayGunUtils;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class GatewayCoreRenderer extends GeoItemRenderer<GatewayCore> {
	public GatewayCoreRenderer() {
		super(new DefaultedItemGeoModel<>(GatewayGunMod.id("gatewaycore")));
		
		addRenderLayer(new GeoRenderLayer<>(this) {
            @Override
            public void render(MatrixStack poseStack, GatewayCore animatable, BakedGeoModel bakedModel,
                               RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick,
                               int packedLight, int packedOverlay) {
                Color color1 = new Color(GatewayGunUtils.hexToInt(GatewayGunMod.defaultColor1));
                Color color2 = new Color(GatewayGunUtils.hexToInt(GatewayGunMod.defaultColor2));
                if (currentItemStack.getOrCreateNbt().contains("color1")) {
                    color1 = new Color(GatewayGunUtils.hexToInt(currentItemStack.getOrCreateNbt().getString("color1")));
                }
                if (currentItemStack.getOrCreateNbt().contains("color2")) {
                    color2 = new Color(GatewayGunUtils.hexToInt(currentItemStack.getOrCreateNbt().getString("color2")));
                }
                if (CoreData.fromTag(currentItemStack.getOrCreateNbt(), true).restrictSide == GatewayRecord.GatewaySide.ONE) {
                    color2 = color1;
                }
                if (CoreData.fromTag(currentItemStack.getOrCreateNbt(), true).restrictSide == GatewayRecord.GatewaySide.TWO) {
                    color1 = color2;
                }
                Uniform colorOne = GatewayShaders.gatewayCoreShader.getUniform("ColorOne");
                if (colorOne != null) {
                    colorOne.set(color1.getRed() / 255f, color1.getGreen() / 255f, color1.getBlue() / 255f);
                }
                Uniform colorTwo = GatewayShaders.gatewayCoreShader.getUniform("ColorTwo");
                if (colorTwo != null) {
                    colorTwo.set(color2.getRed() / 255f, color2.getGreen() / 255f, color2.getBlue() / 255f);
                }
                renderType = GatewayShaders.gatewayCore();
                getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, renderType, bufferSource.getBuffer(renderType), partialTick, packedLight, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1);
            }
        });
	}
	
	@Override
	public RenderLayer getRenderType(GatewayCore animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
		return GatewayShaders.gatewayCore();
	}
}
